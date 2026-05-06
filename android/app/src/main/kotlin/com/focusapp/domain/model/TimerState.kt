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
    /** Seconds the user was away in the most recent confirmed distraction. 0 = no distraction yet. */
    val lastDistractionAwaySeconds: Int = 0,
    /** Badges newly awarded at the end of the most recent session; empty otherwise. */
    val newlyAwardedBadges: List<Badge> = emptyList(),
    /** Display name of the app that triggered the most recent distraction (null for screen unlock). */
    val lastDistractionAppName: String? = null,
    /** Enforcement level active for the current session. */
    val focusStrictness: FocusStrictness = FocusStrictness.RELAXED,
    /** Computed outcome once the session completes; null while session is running. */
    val sessionOutcome: SessionOutcome? = null,
) {
    companion object {
        val IDLE = TimerState()
    }
}
