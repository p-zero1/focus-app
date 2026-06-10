package com.focusapp.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Polls [UsageStatsManager] every [POLL_INTERVAL_MS] to detect app-switches during a focus
 * session.
 *
 * ## Thread safety
 * All mutable state is accessed exclusively from [Dispatchers.Main].  Because Android's
 * main thread IS Dispatchers.Main (single-threaded), all coroutines and callbacks that
 * touch this object's state are serialized without any explicit locking:
 *   - [pollingJob] / [timeoutJob] → launched on Dispatchers.Main (no override)
 *   - [onScreenUnlock] → called by [ScreenUnlockReceiver] on the main thread
 * Heavy binder calls inside [queryCurrentForeground] are dispatched to [Dispatchers.IO]
 * via [withContext]; the results are handed back to Main before any state mutation.
 *
 * ## Why a fixed window instead of an incremental cursor
 * The previous implementation maintained a [lastEventQueryMs] cursor and queried only
 * events since the last poll.  On Samsung / Xiaomi and other OEM ROMs, UsageStats delivers
 * events in batches with delays of up to 3–5 s.  If a batch arrives AFTER the cursor moved
 * past the event's timestamp those events were permanently dropped, causing the state
 * machine to desynchronize and miss detections.
 *
 * Instead, every poll re-queries the last [EVENT_WINDOW_MS] from scratch.  The window is
 * wide enough to absorb any OEM batch delay while keeping the per-poll event count small.
 *
 * ## State machine
 * [lastKnownForeground] is the last app definitively identified as foreground:
 * - A FOREGROUND event appears in the window → update [lastKnownForeground], return pkg
 * - No events in window (user idle in same app) → return [lastKnownForeground] unchanged
 * - The previously known app's BACKGROUND event is in the window but no new FOREGROUND yet
 *   → set [lastKnownForeground] = null, return null (transitioning; hold state one poll)
 *
 * A [SETTLE_DELAY_MS] pause before committing to a new distraction lets brief launcher
 * flashes clear.  Launcher packages are excluded as distraction targets.
 */
@Singleton
class DistractionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    sealed class Event {
        data class AppSwitch(
            val packageName: String,
            val appDisplayName: String,
            val timestampMs: Long,
        ) : Event()
        data class ScreenUnlock(val timestampMs: Long) : Event()
        data class UserReturned(
            val packageName: String?,
            val timestampMs: Long,
            val awayDurationMs: Long,
        ) : Event()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 16)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    // ---- State — all accessed on Dispatchers.Main only ----
    private var pollingJob: Job? = null
    private var timeoutJob: Job? = null
    private var monitoringScope: CoroutineScope? = null

    private var isDistracted = false
    private var distractionStartMs = 0L
    private var distractionPackage: String? = null

    private val focusPackageName: String = context.packageName

    /** The foreground package as of the last successful detection; null = transitioning. */
    private var lastKnownForeground: String? = null

    /** Resolved display names — only accessed from Main. */
    private val appNameCache = HashMap<String, String>()

    private val launcherPackages: Set<String> by lazy {
        val intent = Intent(Intent.ACTION_MAIN).also { it.addCategory(Intent.CATEGORY_HOME) }
        context.packageManager
            .queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .toSet()
    }

    companion object {
        private const val POLL_INTERVAL_MS      = 2_000L
        private const val SETTLE_DELAY_MS       = 800L
        private const val DISTRACTION_TIMEOUT_MS = 30_000L
        /**
         * Width of the event-query window on every poll.
         * 12 s comfortably covers OEM batch-delivery delays (typically ≤ 5 s) while
         * keeping the number of events processed per poll small (usually < 10).
         */
        private const val EVENT_WINDOW_MS = 12_000L
    }

    // ---- Public API -------------------------------------------------------

    /**
     * Start polling.  [scope] should use [Dispatchers.Main] (e.g. [TimerService.serviceScope]).
     * Calling [startMonitoring] while already running restarts cleanly.
     */
    fun startMonitoring(scope: CoroutineScope) {
        stopMonitoring()
        monitoringScope = scope
        isDistracted = false
        lastKnownForeground = null
        // No Dispatchers.Default override — inherit Main from scope so all state
        // mutations in this object happen on the same single thread.
        pollingJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                try {
                    pollForegroundApp()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    // Swallow poll errors (binder failures, SecurityException, etc.) so a
                    // single bad poll never crashes the service process.
                    Timber.w(e, "DistractionMonitor: poll error (suppressed)")
                }
            }
        }
        Timber.d("DistractionMonitor: started")
    }

    /** Stop polling and reset all state. */
    fun stopMonitoring() {
        pollingJob?.cancel()
        timeoutJob?.cancel()
        pollingJob = null
        timeoutJob = null
        monitoringScope = null
        // Flush any open distraction so it isn't silently dropped when a session ends mid-distraction.
        if (isDistracted) {
            val now = System.currentTimeMillis()
            val awayMs = (now - distractionStartMs).coerceAtLeast(1L)
            _events.tryEmit(Event.UserReturned(distractionPackage, now, awayMs))
            Timber.d("DistractionMonitor: flushing open distraction on stop after ${awayMs}ms")
        }
        isDistracted = false
        lastKnownForeground = null
        Timber.d("DistractionMonitor: stopped")
    }

    /**
     * Called by [ScreenUnlockReceiver] on ACTION_USER_PRESENT (Android main thread).
     * Counts every screen-unlock during a session as a distraction start.
     */
    fun onScreenUnlock() {
        val now = System.currentTimeMillis()
        _events.tryEmit(Event.ScreenUnlock(now))
        if (!isDistracted) {
            isDistracted = true
            distractionStartMs = now
            distractionPackage = null
            startDistractionTimeout()
        }
        Timber.d("DistractionMonitor: screen unlock")
    }

    // ---- Internal logic ---------------------------------------------------

    /**
     * True for packages that represent invisible system processes and should never
     * update [lastKnownForeground] or be counted as distractions.
     *
     * A compact deny-list is used instead of [android.content.pm.PackageManager.getLaunchIntentForPackage]
     * because Android 11+ (API 30) package-visibility restrictions cause that call to
     * return null for every third-party app unless QUERY_ALL_PACKAGES is declared,
     * turning all user apps into false "system" packages.
     */
    private fun isSystemProcess(pkg: String): Boolean =
        pkg == "android" ||
        pkg == "com.android.systemui" ||
        pkg.startsWith("com.google.android.inputmethod") ||
        pkg.startsWith("com.samsung.android.inputmethod") ||
        pkg.startsWith("com.swiftkey") ||
        pkg.startsWith("com.nuance")

    private fun resolveAppName(packageName: String): String {
        appNameCache[packageName]?.let { return it }
        val isLauncher = packageName in launcherPackages ||
                         packageName.contains("launcher", ignoreCase = true)
        val display = if (isLauncher) {
            "Home Screen"
        } else {
            try {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (_: Exception) {
                // QUERY_ALL_PACKAGES should prevent this, but handle gracefully.
                // Skip generic reverse-DNS segments; take the first meaningful one.
                val skip = setOf("com", "org", "net", "android", "google", "app", "apps")
                packageName.split('.')
                    .firstOrNull { it !in skip && it.length > 2 }
                    ?.replaceFirstChar { it.uppercaseChar() }
                    ?: packageName.substringAfterLast('.')
                        .replaceFirstChar { it.uppercaseChar() }
            }
        }
        appNameCache[packageName] = display
        return display
    }

    /**
     * Queries the last [EVENT_WINDOW_MS] of usage events on [Dispatchers.IO], derives the
     * current foreground app, updates [lastKnownForeground], and returns it.
     *
     * Return values:
     *  - Non-null → definitive foreground package, or last-known if no new events in window
     *  - null     → transitioning (tracked app just backgrounded; new FG not yet delivered)
     */
    private suspend fun queryCurrentForeground(): String? {
        // Data class local to this function; no shared-state concerns.
        data class Scan(val fg: String?, val hasFgEvent: Boolean, val lastBgPkg: String?)

        val scan: Scan? = try { withContext(Dispatchers.IO) {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return@withContext null
            val now = System.currentTimeMillis()
            // queryEvents() rethrows binder RemoteExceptions as RuntimeException; catch below.
            val eventLog = usm.queryEvents(now - EVENT_WINDOW_MS, now)
                ?: return@withContext null  // null on some devices when permission is absent
            val ev = UsageEvents.Event()

            var fg: String? = null
            var hasFgEvent = false
            var lastBgPkg: String? = null

            while (eventLog.hasNextEvent()) {
                eventLog.getNextEvent(ev)
                val pkg = ev.packageName
                if (isSystemProcess(pkg)) continue
                @Suppress("DEPRECATION")
                when (ev.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        fg = pkg
                        hasFgEvent = true
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (pkg == fg) fg = null
                        lastBgPkg = pkg
                    }
                }
            }
            Scan(fg, hasFgEvent, lastBgPkg)
        } } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Timber.w(e, "DistractionMonitor: queryEvents error (suppressed)")
            null
        }

        // Back on Main — interpret the scan and update lastKnownForeground.
        if (scan == null) return lastKnownForeground   // UsageStatsManager unavailable or error

        return when {
            scan.hasFgEvent -> {
                // Definitive new reading — trust it unconditionally.
                lastKnownForeground = scan.fg
                scan.fg
            }
            scan.lastBgPkg != null && scan.lastBgPkg == lastKnownForeground -> {
                // The app we were tracking backgrounded but the new FG hasn't arrived yet.
                // Signal "transitioning" so the caller holds state for one more poll.
                lastKnownForeground = null
                null
            }
            else -> {
                // No relevant events — user has been in the same app for >${EVENT_WINDOW_MS}ms.
                // Return the preserved last-known state (keeps distraction tracking intact).
                lastKnownForeground
            }
        }
    }

    private suspend fun pollForegroundApp() {
        val current = queryCurrentForeground()

        when {
            // ── User returned to the focus app ──────────────────────────────────
            current == focusPackageName && isDistracted -> {
                val now = System.currentTimeMillis()
                val awayMs = now - distractionStartMs
                timeoutJob?.cancel()
                timeoutJob = null
                _events.tryEmit(Event.UserReturned(distractionPackage, now, awayMs))
                Timber.d("DistractionMonitor: returned after ${awayMs}ms")
                isDistracted = false
            }

            // ── User navigated to home screen while distracted ───────────────────
            // Treat the launcher as a return point: the user left the distraction app.
            // A new distraction (if they open another app from home) will be tracked
            // independently on the next poll cycle.
            current != null && current in launcherPackages && isDistracted -> {
                val now = System.currentTimeMillis()
                val awayMs = now - distractionStartMs
                timeoutJob?.cancel()
                timeoutJob = null
                _events.tryEmit(Event.UserReturned(distractionPackage, now, awayMs))
                Timber.d("DistractionMonitor: returned to home after ${awayMs}ms")
                isDistracted = false
            }

            // ── Possible new distraction ─────────────────────────────────────────
            // Exclude null (transitioning), focusPackageName, and launcher packages
            // before entering the settle delay so we don't waste 800 ms on home-screen
            // flashes that are already filtered out.
            current != null &&
            current != focusPackageName &&
            current !in launcherPackages &&
            !isDistracted -> {
                // 800 ms settle: lets brief launcher flashes and immediate round-trips clear
                delay(SETTLE_DELAY_MS)
                val settled = queryCurrentForeground()
                when {
                    settled == null                 -> return   // still transitioning
                    settled == focusPackageName     -> return   // user returned within settle window
                    settled in launcherPackages     -> return   // ended up on home screen
                    else -> {
                        isDistracted = true
                        distractionStartMs = System.currentTimeMillis()
                        distractionPackage = settled
                        val displayName = resolveAppName(settled)
                        _events.tryEmit(Event.AppSwitch(settled, displayName, distractionStartMs))
                        startDistractionTimeout()
                        Timber.d("DistractionMonitor: distraction — $displayName ($settled)")
                    }
                }
            }

            // ── All other cases → hold state ─────────────────────────────────────
            // current == null                                      → transitioning / system overlay
            // current in launcherPackages && !isDistracted         → home screen pre-distraction
            // isDistracted && current != focusPackageName
            //                && current !in launcherPackages       → still in distraction app
        }
    }

    private fun startDistractionTimeout() {
        timeoutJob?.cancel()
        // Launched without Dispatchers.Default override → inherits Main from monitoringScope
        timeoutJob = monitoringScope?.launch {
            delay(DISTRACTION_TIMEOUT_MS)
            if (isDistracted) {
                val now = System.currentTimeMillis()
                val awayMs = now - distractionStartMs
                _events.tryEmit(Event.UserReturned(distractionPackage, now, awayMs))
                isDistracted = false
                Timber.d("DistractionMonitor: timeout — distraction recorded after 30s")
            }
        }
    }
}
