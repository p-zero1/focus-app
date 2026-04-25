package com.focusapp.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
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
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Polls [UsageStatsManager] every 4 seconds to detect app-switches during a focus session.
 * Screen-unlock events are fed in externally via [onScreenUnlock] (called from [ScreenUnlockReceiver]).
 *
 * Emits three event types on [events]:
 * - [Event.AppSwitch]    — user left the focus app
 * - [Event.ScreenUnlock] — screen was unlocked
 * - [Event.UserReturned] — user is back; includes the duration they were away
 */
@Singleton
class DistractionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    sealed class Event {
        data class AppSwitch(val packageName: String, val timestampMs: Long) : Event()
        data class ScreenUnlock(val timestampMs: Long) : Event()
        data class UserReturned(
            val packageName: String?,
            val timestampMs: Long,
            val awayDurationMs: Long,
        ) : Event()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 16)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private var pollingJob: Job? = null
    private var isDistracted = false
    private var distractionStartMs = 0L
    private var distractionPackage: String? = null

    private val focusPackageName: String = context.packageName

    /** Start polling for distraction events. Call on session start. */
    fun startMonitoring(scope: CoroutineScope) {
        isDistracted = false
        pollingJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(4_000L)
                pollForegroundApp()
            }
        }
        Timber.d("DistractionMonitor: started")
    }

    /** Stop polling. Call on session end or pause. */
    fun stopMonitoring() {
        pollingJob?.cancel()
        pollingJob = null
        isDistracted = false
        Timber.d("DistractionMonitor: stopped")
    }

    /**
     * Called by [ScreenUnlockReceiver] on ACTION_USER_PRESENT.
     * May be called from any thread; uses tryEmit (buffered, non-blocking).
     */
    fun onScreenUnlock() {
        val now = System.currentTimeMillis()
        _events.tryEmit(Event.ScreenUnlock(now))
        if (!isDistracted) {
            isDistracted = true
            distractionStartMs = now
            distractionPackage = null
        }
        Timber.d("DistractionMonitor: screen unlock")
    }

    private suspend fun pollForegroundApp() {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return
        val now = System.currentTimeMillis()
        val eventLog = usm.queryEvents(now - 6_000L, now)
        val event = UsageEvents.Event()
        var latestForeground: String? = null
        while (eventLog.hasNextEvent()) {
            eventLog.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                latestForeground = event.packageName
            }
        }
        latestForeground ?: return

        if (latestForeground != focusPackageName) {
            if (!isDistracted) {
                isDistracted = true
                distractionStartMs = now
                distractionPackage = latestForeground
                _events.tryEmit(Event.AppSwitch(latestForeground, now))
                Timber.d("DistractionMonitor: distraction — $latestForeground")
            }
        } else if (isDistracted) {
            val awayMs = now - distractionStartMs
            _events.tryEmit(Event.UserReturned(distractionPackage, now, awayMs))
            Timber.d("DistractionMonitor: returned after ${awayMs}ms")
            isDistracted = false
        }
    }
}
