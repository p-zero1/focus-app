package com.focusapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.focusapp.domain.model.DistractionType
import com.focusapp.domain.model.FocusStrictness
import com.focusapp.domain.model.SessionConfig
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.model.SessionOutcome
import com.focusapp.domain.model.TimerState
import com.focusapp.domain.model.TimerStatus
import com.focusapp.domain.preferences.FocusPreferences
import com.focusapp.domain.usecase.CompleteSessionUseCase
import com.focusapp.domain.usecase.LogDistractionUseCase
import com.focusapp.domain.usecase.StartSessionUseCase
import com.focusapp.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    @Inject lateinit var startSessionUseCase: StartSessionUseCase
    @Inject lateinit var completeSessionUseCase: CompleteSessionUseCase
    @Inject lateinit var logDistractionUseCase: LogDistractionUseCase
    @Inject lateinit var distractionMonitor: DistractionMonitor
    @Inject lateinit var focusPreferences: FocusPreferences

    private val serviceScope = CoroutineScope(
        SupervisorJob() +
        Dispatchers.Main +
        // Without a handler, any child-coroutine exception bypasses SupervisorJob and reaches
        // Thread.UncaughtExceptionHandler, killing the process. Log instead of crash.
        CoroutineExceptionHandler { _, throwable ->
            Timber.e(throwable, "TimerService: unhandled coroutine exception")
        }
    )

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var startSessionJob: Job? = null
    private var countdownJob: Job? = null
    private var sessionStartTimeMs: Long = 0L

    // Wake lock — PARTIAL keeps CPU alive when screen is off
    private var wakeLock: PowerManager.WakeLock? = null

    // Dynamic receiver registered only while a session is running
    private var screenUnlockReceiver: ScreenUnlockReceiver? = null

    // Pomodoro cycle tracking — persists across intervals in a chain
    private var pomodoroIntervalsDone = 0
    private var currentMode = SessionMode.POMODORO
    private var currentStrictness = FocusStrictness.RELAXED

    // Resolved display name of the distraction app (set on AppSwitch, cleared on session end)
    private var distractionDisplayName: String? = null

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = TimerBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        collectDistractionEvents()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Ready"))
        return START_STICKY
    }

    override fun onDestroy() {
        stopSessionMonitoring()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ---- Distraction event pipeline ----

    /**
     * Collects [DistractionMonitor.events] for the lifetime of the service.
     * AppSwitch/ScreenUnlock bump the live counter and apply enforcement;
     * UserReturned persists the completed distraction to the database.
     */
    private fun collectDistractionEvents() {
        serviceScope.launch {
            distractionMonitor.events.collect { event ->
                val sessionId = _timerState.value.currentSessionId ?: return@collect
                when (event) {
                    is DistractionMonitor.Event.AppSwitch -> {
                        distractionDisplayName = event.appDisplayName
                        _timerState.value = _timerState.value.copy(
                            distractionCount = _timerState.value.distractionCount + 1,
                            lastDistractionAppName = event.appDisplayName,
                        )
                        applyEnforcement()
                    }
                    is DistractionMonitor.Event.ScreenUnlock -> {
                        distractionDisplayName = null
                        _timerState.value = _timerState.value.copy(
                            distractionCount = _timerState.value.distractionCount + 1,
                        )
                        applyEnforcement()
                    }
                    is DistractionMonitor.Event.UserReturned -> {
                        val awaySeconds = (event.awayDurationMs / 1000).toInt().coerceAtLeast(1)
                        _timerState.value = _timerState.value.copy(
                            lastDistractionAwaySeconds = awaySeconds,
                        )
                        val type = if (event.packageName != null) DistractionType.APP_SWITCH
                                   else DistractionType.SCREEN_UNLOCK
                        val displayName = distractionDisplayName
                        distractionDisplayName = null
                        serviceScope.launch {
                            logDistractionUseCase(
                                sessionId = sessionId,
                                timestampMs = event.timestampMs,
                                type = type,
                                appPackageName = displayName ?: event.packageName,
                                awayDurationMs = event.awayDurationMs,
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Applies enforcement action based on [currentStrictness].
     * Called immediately after every AppSwitch / ScreenUnlock event.
     */
    private fun applyEnforcement() {
        when (currentStrictness) {
            FocusStrictness.RELAXED  -> { /* log only — no automated action */ }
            FocusStrictness.STRICT   -> {
                if (_timerState.value.status == TimerStatus.ACTIVE) {
                    Timber.d("Enforcement: STRICT — auto-pausing session")
                    pauseSession()
                }
            }
            FocusStrictness.HARDCORE -> {
                if (_timerState.value.status == TimerStatus.ACTIVE) {
                    Timber.d("Enforcement: HARDCORE — ending session")
                    endSession(forced = true)
                }
            }
        }
    }

    // ---- Public API (called via binder) ----

    fun startSession(config: SessionConfig) {
        // Cancel any in-flight DB insert or countdown from a previous start attempt.
        startSessionJob?.cancel()
        countdownJob?.cancel()
        sessionStartTimeMs = System.currentTimeMillis()
        currentMode = config.mode
        currentStrictness = config.focusStrictness

        acquireWakeLock()
        registerScreenReceiver()
        distractionMonitor.startMonitoring(serviceScope)
        if (config.mode == SessionMode.DEEP_WORK) enableDnd()

        Timber.d("startSession: mode=${config.mode} duration=${config.durationSeconds}s")
        startSessionJob = serviceScope.launch {
            try {
                val sessionId = startSessionUseCase(config, sessionStartTimeMs)
                Timber.d("startSession: DB insert OK — sessionId=$sessionId")
                _timerState.value = TimerState(
                    status = TimerStatus.ACTIVE,
                    remainingSeconds = config.durationSeconds,
                    elapsedSeconds = 0,
                    currentSessionId = sessionId,
                    distractionCount = 0,
                    pomodoroIntervalsDone = pomodoroIntervalsDone,
                    focusStrictness = config.focusStrictness,
                    sessionGoal = config.tag,
                )
                runCountdown(config.durationSeconds, sessionId)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Timber.e(e, "startSession: failed to create session — resetting to IDLE")
                stopSessionMonitoring()
                _timerState.value = TimerState.IDLE
            }
        }
    }

    fun pauseSession() {
        val state = _timerState.value
        if (state.status != TimerStatus.ACTIVE) return
        countdownJob?.cancel()
        _timerState.value = state.copy(status = TimerStatus.PAUSED)
        disableDnd()
        updateNotification("Paused — ${formatSeconds(state.remainingSeconds)}")
        Timber.d("Session paused at ${state.elapsedSeconds}s elapsed")
    }

    fun resumeSession() {
        val state = _timerState.value
        if (state.status != TimerStatus.PAUSED) return
        _timerState.value = state.copy(status = TimerStatus.ACTIVE)
        if (currentMode == SessionMode.DEEP_WORK) enableDnd()
        runCountdown(state.remainingSeconds, state.currentSessionId ?: return)
    }

    fun endSession(forced: Boolean = false) {
        startSessionJob?.cancel()
        countdownJob?.cancel()
        val state = _timerState.value
        val sessionId = state.currentSessionId ?: return

        serviceScope.launch {
            val (newBadges, outcome) = completeSessionUseCase(
                sessionId = sessionId,
                actualDuration = state.elapsedSeconds,
                endTime = System.currentTimeMillis(),
                forceFail = forced,
            )
            val idleState = TimerState.IDLE.copy(
                newlyAwardedBadges = newBadges,
                sessionOutcome = outcome,
            )
            _timerState.value = idleState
            updateNotification("Ready")
        }
        disableDnd()
        stopSessionMonitoring()
        Timber.d("Session ended (forced=$forced) after ${state.elapsedSeconds}s")
    }

    /** Manually start a break countdown (used by non-Pomodoro modes from the break prompt). */
    fun startBreak(breakSeconds: Int = 300) {
        countdownJob?.cancel()
        _timerState.value = _timerState.value.copy(
            status = TimerStatus.BREAK,
            remainingSeconds = breakSeconds,
            elapsedSeconds = 0,
        )
        updateNotification("Break time! ${breakSeconds / 60}m")
        runBreakCountdown(breakSeconds)
        Timber.d("Break started manually for ${breakSeconds}s")
    }

    /** Skip the running break and return to IDLE. */
    fun skipBreak() {
        countdownJob?.cancel()
        _timerState.value = TimerState.IDLE
        updateNotification("Ready")
        Timber.d("Break skipped")
    }

    /** Called by ViewModel after the badge dialog is dismissed — prevents re-show on rebind. */
    fun clearNewlyAwardedBadges() {
        _timerState.value = _timerState.value.copy(newlyAwardedBadges = emptyList())
    }

    // ---- Countdown ----

    private fun runCountdown(fromSeconds: Int, sessionId: Long) {
        countdownJob = serviceScope.launch {
            var remaining = fromSeconds
            while (remaining > 0) {
                delay(1_000L)
                remaining--
                val elapsed = _timerState.value.elapsedSeconds + 1
                _timerState.value = _timerState.value.copy(
                    remainingSeconds = remaining,
                    elapsedSeconds = elapsed,
                )
                if (elapsed % 5 == 0) {
                    updateNotification(formatSeconds(remaining))
                }
            }
            onFocusTimerFinished(sessionId)
        }
    }

    private suspend fun onFocusTimerFinished(sessionId: Long) {
        val state = _timerState.value
        disableDnd()
        stopSessionMonitoring()

        val (newBadges, outcome) = completeSessionUseCase(
            sessionId = sessionId,
            actualDuration = state.elapsedSeconds,
            endTime = System.currentTimeMillis(),
        )
        Timber.d("Focus session $sessionId finished — outcome=$outcome badges: ${newBadges.map { it.id }}")

        if (currentMode == SessionMode.POMODORO) {
            pomodoroIntervalsDone++
            val intervalsBeforeLong = focusPreferences.pomodoroIntervalsBeforeLong.first()
            val breakMinutes = if (pomodoroIntervalsDone % intervalsBeforeLong == 0) {
                focusPreferences.pomodoroLongBreakMinutes.first()
            } else {
                focusPreferences.pomodoroShortBreakMinutes.first()
            }
            _timerState.value = state.copy(
                status = TimerStatus.BREAK,
                remainingSeconds = breakMinutes * 60,
                elapsedSeconds = 0,
                pomodoroIntervalsDone = pomodoroIntervalsDone,
                newlyAwardedBadges = newBadges,
                sessionOutcome = outcome,
            )
            updateNotification("Break time! ${breakMinutes}m")
            runBreakCountdown(breakMinutes * 60)
        } else {
            if (currentMode == SessionMode.STUDY) fireStudyAlarm()
            _timerState.value = state.copy(
                status = TimerStatus.FINISHED,
                remainingSeconds = 0,
                newlyAwardedBadges = newBadges,
                sessionOutcome = outcome,
            )
            updateNotification("Session complete!")
        }
    }

    private fun runBreakCountdown(fromSeconds: Int) {
        countdownJob = serviceScope.launch {
            var remaining = fromSeconds
            while (remaining > 0) {
                delay(1_000L)
                remaining--
                _timerState.value = _timerState.value.copy(
                    remainingSeconds = remaining,
                    elapsedSeconds = fromSeconds - remaining,
                )
            }
            _timerState.value = _timerState.value.copy(
                status = TimerStatus.FINISHED,
                remainingSeconds = 0,
            )
            updateNotification("Break over! Ready for your next session.")
            Timber.d("Break finished (interval ${pomodoroIntervalsDone})")
        }
    }

    // ---- Session monitoring helpers ----

    private fun stopSessionMonitoring() {
        distractionMonitor.stopMonitoring()
        unregisterScreenReceiver()
        releaseWakeLock()
    }

    // ---- DND (Deep Work only) ----

    private fun enableDnd() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            Timber.w("DND: ACCESS_NOTIFICATION_POLICY not granted — skipping")
            return
        }
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        Timber.d("DND enabled for Deep Work session")
    }

    private fun disableDnd() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) return
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        Timber.d("DND disabled")
    }

    // ---- Wake lock ----

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FocusApp:TimerWakeLock")
        wakeLock?.acquire(3L * 60 * 60 * 1_000)
        Timber.d("WakeLock acquired")
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            Timber.d("WakeLock released")
        }
        wakeLock = null
    }

    // ---- Screen-unlock receiver ----

    private fun registerScreenReceiver() {
        if (screenUnlockReceiver != null) return
        screenUnlockReceiver = ScreenUnlockReceiver { distractionMonitor.onScreenUnlock() }
        registerReceiver(screenUnlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    private fun unregisterScreenReceiver() {
        screenUnlockReceiver?.let {
            runCatching { unregisterReceiver(it) }
            screenUnlockReceiver = null
        }
    }

    // ---- Notification ----

    private fun createNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val timerChannel = NotificationChannel(
            CHANNEL_ID,
            "Focus Timer",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows live countdown while a focus session is active"
        }

        val alarmChannel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Session Complete Alert",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alarm-style alert fired when a Study Mode session countdown reaches zero"
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 400L, 200L, 400L)
        }

        nm.createNotificationChannel(timerChannel)
        nm.createNotificationChannel(alarmChannel)
    }

    private fun buildNotification(contentText: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Timer")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()

    private fun fireStudyAlarm() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setContentTitle("Study session complete!")
            .setContentText("Great work — your countdown has finished.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0L, 400L, 200L, 400L))
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 1,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()
        nm.notify(ALARM_NOTIFICATION_ID, notification)
        Timber.d("Study alarm fired")
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun formatSeconds(totalSeconds: Int): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%02d:%02d".format(m, s)
    }

    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val ALARM_CHANNEL_ID = "focus_alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ALARM_NOTIFICATION_ID = 1002
    }
}
