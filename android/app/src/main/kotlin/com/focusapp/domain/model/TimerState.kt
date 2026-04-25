package com.focusapp.domain.model

/**
 * Snapshot of timer state emitted by [com.focusapp.service.TimerService] every second.
 *
 * UI observes this via [com.focusapp.ui.timer.TimerViewModel].
 */
data class TimerState(
    val status: TimerStatus = TimerStatus.IDLE,
    val remainingSeconds: Int = 0,
    val elapsedSeconds: Int = 0,
    val currentSessionId: Long? = null,
    val distractionCount: Int = 0,
    val pomodoroIntervalsDone: Int = 0,
) {
    companion object {
        val IDLE = TimerState()
    }
}
