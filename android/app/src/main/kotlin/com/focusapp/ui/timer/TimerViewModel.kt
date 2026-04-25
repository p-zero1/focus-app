package com.focusapp.ui.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusapp.domain.model.Badge
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val buildSessionConfigUseCase: BuildSessionConfigUseCase,
) : ViewModel() {

    private var timerService: TimerService? = null

    // UI-facing timer state — bridged from the service once bound
    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    // Selected mode and custom duration (UI input)
    private val _selectedMode = MutableStateFlow(SessionMode.POMODORO)
    val selectedMode: StateFlow<SessionMode> = _selectedMode.asStateFlow()

    private val _customDurationMinutes = MutableStateFlow(25)
    val customDurationMinutes: StateFlow<Int> = _customDurationMinutes.asStateFlow()

    private val _customTag = MutableStateFlow("")
    val customTag: StateFlow<String> = _customTag.asStateFlow()

    // Break prompt shown when non-break session reaches FINISHED
    private val _showBreakPrompt = MutableStateFlow(false)
    val showBreakPrompt: StateFlow<Boolean> = _showBreakPrompt.asStateFlow()

    // Distraction warning banner — auto-dismissed after 4 s
    private val _showDistractionWarning = MutableStateFlow(false)
    val showDistractionWarning: StateFlow<Boolean> = _showDistractionWarning.asStateFlow()

    // Seconds the user was away in the most recent confirmed distraction (H3)
    private val _distractionAwaySeconds = MutableStateFlow(0)
    val distractionAwaySeconds: StateFlow<Int> = _distractionAwaySeconds.asStateFlow()

    // Badge newly awarded at session end — drives BadgeAwardedDialog (H6)
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
                    // Distraction warning — trigger whenever count increases
                    if (state.distractionCount > prevDistractionCount) {
                        prevDistractionCount = state.distractionCount
                        triggerDistractionWarning()
                    }
                    // Update away-duration when UserReturned fires (H3)
                    if (state.lastDistractionAwaySeconds != prevAwaySeconds && state.lastDistractionAwaySeconds > 0) {
                        prevAwaySeconds = state.lastDistractionAwaySeconds
                        _distractionAwaySeconds.value = state.lastDistractionAwaySeconds
                    }
                    // Badge celebration — show first newly awarded badge (H6)
                    if (state.newlyAwardedBadges.isNotEmpty() && _newBadge.value == null) {
                        _newBadge.value = state.newlyAwardedBadges.first()
                    }
                    // Break prompt — only when a focus interval finishes (not when a break ends)
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
        viewModelScope.launch {
            val config = buildSessionConfig()
            timerService?.startSession(config)
            _showBreakPrompt.value = false
        }
    }

    fun onPause() = timerService?.pauseSession()

    fun onResume() = timerService?.resumeSession()

    fun onStop() = timerService?.endSession()

    fun onSkipBreak() = timerService?.skipBreak()

    fun onDismissBreakPrompt() {
        _showBreakPrompt.value = false
        _timerState.value = TimerState.IDLE
    }

    fun onDismissDistractionWarning() {
        _showDistractionWarning.value = false
    }

    fun onDismissBadge() {
        _newBadge.value = null
        // Clear badges from service state so dialog doesn't reappear on recompose
        _timerState.value = _timerState.value.copy(newlyAwardedBadges = emptyList())
    }

    fun onModeSelected(mode: SessionMode) {
        _selectedMode.value = mode
    }

    fun onCustomDurationChanged(minutes: Int) {
        _customDurationMinutes.value = minutes.coerceIn(5, 180)
    }

    fun onTagChanged(tag: String) {
        _customTag.value = tag
    }

    private suspend fun buildSessionConfig(): SessionConfig =
        buildSessionConfigUseCase(
            mode = _selectedMode.value,
            customDurationMinutes = _customDurationMinutes.value,
            tag = _customTag.value.trim().ifEmpty { null },
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
