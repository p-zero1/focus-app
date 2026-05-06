package com.focusapp.ui.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusapp.domain.model.Badge
import com.focusapp.domain.model.FocusStrictness
import com.focusapp.domain.model.SessionConfig
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.model.TimerState
import com.focusapp.domain.model.TimerStatus
import com.focusapp.domain.usecase.BuildSessionConfigUseCase
import com.focusapp.service.TimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val buildSessionConfigUseCase: BuildSessionConfigUseCase,
) : ViewModel() {

    private var timerService: TimerService? = null

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _selectedMode = MutableStateFlow(SessionMode.POMODORO)
    val selectedMode: StateFlow<SessionMode> = _selectedMode.asStateFlow()

    // Duration in seconds — default 25 min; updated when mode changes or user scrolls the picker
    private val _customDurationSeconds = MutableStateFlow(SessionMode.POMODORO.defaultDurationSeconds)
    val customDurationSeconds: StateFlow<Int> = _customDurationSeconds.asStateFlow()

    private val _customTag = MutableStateFlow("")
    val customTag: StateFlow<String> = _customTag.asStateFlow()

    private val _selectedStrictness = MutableStateFlow(FocusStrictness.RELAXED)
    val selectedStrictness: StateFlow<FocusStrictness> = _selectedStrictness.asStateFlow()

    private val _showBreakPrompt = MutableStateFlow(false)
    val showBreakPrompt: StateFlow<Boolean> = _showBreakPrompt.asStateFlow()

    private val _showDistractionWarning = MutableStateFlow(false)
    val showDistractionWarning: StateFlow<Boolean> = _showDistractionWarning.asStateFlow()

    private val _distractionAppName = MutableStateFlow<String?>(null)
    val distractionAppName: StateFlow<String?> = _distractionAppName.asStateFlow()

    private val _distractionAwaySeconds = MutableStateFlow(0)
    val distractionAwaySeconds: StateFlow<Int> = _distractionAwaySeconds.asStateFlow()

    private val _newBadge = MutableStateFlow<Badge?>(null)
    val newBadge: StateFlow<Badge?> = _newBadge.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as? TimerService.TimerBinder)?.getService() ?: return
            timerService = service
            viewModelScope.launch {
                var prevDistractionCount = 0
                var prevStatus = TimerStatus.IDLE
                var prevAwaySeconds = 0
                service.timerState.collect { state ->
                    if (state.distractionCount > prevDistractionCount) {
                        prevDistractionCount = state.distractionCount
                        _distractionAppName.value = state.lastDistractionAppName
                        triggerDistractionWarning()
                    }
                    if (state.lastDistractionAwaySeconds != prevAwaySeconds && state.lastDistractionAwaySeconds > 0) {
                        prevAwaySeconds = state.lastDistractionAwaySeconds
                        _distractionAwaySeconds.value = state.lastDistractionAwaySeconds
                    }
                    if (state.newlyAwardedBadges.isNotEmpty() && _newBadge.value == null) {
                        _newBadge.value = state.newlyAwardedBadges.first()
                    }
                    val comingFromBreak = prevStatus == TimerStatus.BREAK
                    if (state.status == TimerStatus.FINISHED && !comingFromBreak) {
                        _showBreakPrompt.value = true
                    }
                    prevStatus = state.status
                    _timerState.value = state
                }
            }
            Timber.d("TimerService connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            Timber.d("TimerService disconnected")
        }
    }

    fun bindService() {
        val intent = Intent(context, TimerService::class.java)
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() {
        runCatching { context.unbindService(serviceConnection) }
        timerService = null
    }

    // ---- User actions ----

    fun onStartSession() {
        timerService?.startSession(buildSessionConfig())
        _showBreakPrompt.value = false
    }

    fun onPause() = timerService?.pauseSession()

    fun onResume() = timerService?.resumeSession()

    fun onStop() = timerService?.endSession()

    fun onSkipBreak() = timerService?.skipBreak()

    fun onStartBreak(breakSeconds: Int = 300) {
        _showBreakPrompt.value = false
        timerService?.startBreak(breakSeconds)
    }

    fun onDismissBreakPrompt() {
        _showBreakPrompt.value = false
        _timerState.value = TimerState.IDLE
    }

    fun onDismissDistractionWarning() {
        _showDistractionWarning.value = false
    }

    fun onDismissBadge() {
        _newBadge.value = null
        timerService?.clearNewlyAwardedBadges()
        _timerState.value = _timerState.value.copy(newlyAwardedBadges = emptyList())
    }

    fun onModeSelected(mode: SessionMode) {
        _selectedMode.value = mode
        // defaultDurationSeconds is a UI-layer constant (see extension below), not a domain rule.
        // Intentionally not reading FocusPreferences here to keep the ViewModel preference-free.
        _customDurationSeconds.value = mode.defaultDurationSeconds
    }

    fun onCustomDurationSecondsChanged(seconds: Int) {
        _customDurationSeconds.value = seconds.coerceIn(
            _selectedMode.value.minSeconds,
            _selectedMode.value.maxSeconds,
        )
    }

    fun onTagChanged(tag: String) {
        _customTag.value = tag
    }

    fun onStrictnessSelected(strictness: FocusStrictness) {
        _selectedStrictness.value = strictness
    }

    private fun buildSessionConfig(): SessionConfig =
        buildSessionConfigUseCase(
            mode = _selectedMode.value,
            durationSeconds = _customDurationSeconds.value,
            tag = _customTag.value.trim().ifEmpty { null },
            strictness = _selectedStrictness.value,
        )

    private fun triggerDistractionWarning() {
        _showDistractionWarning.value = true
        viewModelScope.launch {
            delay(4_000L)
            _showDistractionWarning.value = false
        }
    }

    override fun onCleared() {
        unbindService()
        super.onCleared()
    }
}

// Mode-specific picker bounds and defaults used by TimerScreen and TimerWheelPicker
val SessionMode.minSeconds: Int get() = when (this) {
    SessionMode.POMODORO -> 300       // 5 min
    SessionMode.DEEP_WORK -> 1800     // 30 min
    SessionMode.CUSTOM, SessionMode.STUDY -> 300  // 5 min
}

val SessionMode.maxSeconds: Int get() = when (this) {
    SessionMode.POMODORO -> 5400      // 90 min
    SessionMode.DEEP_WORK -> 10800    // 3 h
    SessionMode.CUSTOM, SessionMode.STUDY -> 10800
}

val SessionMode.defaultDurationSeconds: Int get() = when (this) {
    SessionMode.POMODORO -> 1500      // 25 min
    SessionMode.DEEP_WORK -> 5400     // 90 min
    SessionMode.CUSTOM, SessionMode.STUDY -> 1500
}
