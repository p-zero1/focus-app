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
import com.focusapp.data.prefs.AppPreferences
import com.focusapp.domain.model.DistractionType
import com.focusapp.domain.model.SessionConfig
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.model.TimerState
import com.focusapp.domain.model.TimerStatus
import com.focusapp.domain.usecase.CompleteSessionUseCase
import com.focusapp.domain.usecase.LogDistractionUseCase
import com.focusapp.domain.usecase.StartSessionUseCase
import com.focusapp.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
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
    @Inject lateinit var appPreferences: AppPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var countdownJob: Job? = null
    private var sessionStartTimeMs: Long = 0L

    // Wake lock — PARTIAL keeps CPU alive when screen is off
    private var wakeLock: PowerManager.WakeLock? = null

    // Dynamic receiver registered only while a session is running
    private var screenUnlockReceiver: ScreenUnlockReceiver? = null

    // Pomodoro cycle tracking — persists across intervals in a chain
    private var pomodoroIntervalsDone = 0
    private var currentMode = SessionMode.POMODORO

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
     * AppSwitch/ScreenUnlock bump the live counter; UserReturned persists the event.
     */
    private fun collectDistractionEvents() {
        serviceScope.launch {
            distractionMonitor.events.collect { event ->
                val sessionId = _timerState.value.currentSessionId ?: return@collect
                when (event) {
                    is DistractionMonitor.Event.AppSwitch,
                    is DistractionMonitor.Event.ScreenUnlock -> {
                        _timerState.value = _timerState.value.copy(
                            distractionCount = _timerState.value.distractionCount + 1,
                        )
                    }
                    is DistractionMonitor.Event.UserReturned -> {
                        val awaySeconds = (event.awayDurationMs / 1000).toInt().coerceAtLeast(1)
                        _timerState.value = _timerState.value.copy(
                            lastDistractionAwaySeconds = awaySeconds,
                        )
                        val type = if (event.packageName != null) DistractionType.APP_SWITCH
                                   else DistractionType.SCREEN_UNLOCK
                        serviceScope.launch {
                            logDistractionUseCase(
                                sessionId = sessionId,
                                timestampMs = event.timestampMs,
                                type = type,
                                appPackageName = event.packageName,
                                awayDurationMs = event.awayDurationMs,
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Public API (called via binder) ----

    fun startSession(config: SessionConfig) {
        countdownJob?.cancel()
        sessionStartTimeMs = System.currentTimeMillis()
        currentMode = config.mode

        acquireWakeLock()
        registerScreenReceiver()
        distractionMonitor.startMonitoring(serviceScope)
        if (config.mode == SessionMode.DEEP_WORK) enableDnd()

        serviceScope.launch {
            val sessionId = startSessionUseCase(config, sessionStartTimeMs)
            _timerState.value = TimerState(
                status = TimerStatus.ACTIVE,
                remainingSeconds = config.durationSeconds,
                elapsedSeconds = 0,
                currentSessionId = sessionId,
                distractionCount = 0,
                pomodoroIntervalsDone = pomodoroIntervalsDone,
            )
            runCountdown(config.durationSeconds, sessionId)
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

    fun endSession() {
        countdownJob?.cancel()
        val state = _timerState.value
        val sessionId = state.currentSessionId ?: return

        serviceScope.launch {
            val newBadges = completeSessionUseCase(
                sessionId = sessionId,
                actualDuration = state.elapsedSeconds,
                endTime = System.currentTimeMillis(),
            )
            if (newBadges.isNotEmpty()) {
                _timerState.value = TimerState.IDLE.copy(newlyAwardedBadges = newBadges)
            } else {
                _timerState.value = TimerState.IDLE
            }
            updateNotification("Ready")
        }
        disableDnd()
        stopSessionMonitoring()
        Timber.d("Session ended manually after ${state.elapsedSeconds}s")
    }

    /** Skip the running break and return to IDLE. */
    fun skipBreak() {
        countdownJob?.cancel()
        _timerState.value = TimerState.IDLE
        updateNotification("Ready")
        Timber.d("Break skipped")
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
        // Stop distraction monitoring — user is between intervals, not in focus
        stopSessionMonitoring()

        val newBadges = completeSessionUseCase(
            sessionId = sessionId,
            actualDuration = state.elapsedSeconds,
            endTime = System.currentTimeMillis(),
        )
        Timber.d("Focus session $sessionId finished — badges: ${newBadges.map { it.id }}")

        if (currentMode == SessionMode.POMODORO) {
            pomodoroIntervalsDone++
            val intervalsBeforeLong = appPreferences.pomodoroIntervalsBeforeLong.first()
            val breakMinutes = if (pomodoroIntervalsDone % intervalsBeforeLong == 0) {
                appPreferences.pomodoroLongBreakMinutes.first()
            } else {
                appPreferences.pomodoroShortBreakMinutes.first()
            }
            _timerState.value = state.copy(
                status = TimerStatus.BREAK,
                remainingSeconds = breakMinutes * 60,
                elapsedSeconds = 0,
                pomodoroIntervalsDone = pomodoroIntervalsDone,
                newlyAwardedBadges = newBadges,
            )
            updateNotification("Break time! ${breakMinutes}m")
            runBreakCountdown(breakMinutes * 60)
        } else {
            _timerState.value = state.copy(
                status = TimerStatus.FINISHED,
                remainingSeconds = 0,
                newlyAwardedBadges = newBadges,
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

    // ---- DND (Deep Work only — C4) ----

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

    // ---- Wake lock (U2) ----

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FocusApp:TimerWakeLock")
        wakeLock?.acquire(3L * 60 * 60 * 1_000) // 3 h max — prevents runaway hold
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
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus Timer",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows live countdown while a focus session is active"
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
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
        const val NOTIFICATION_ID = 1001
    }
}
