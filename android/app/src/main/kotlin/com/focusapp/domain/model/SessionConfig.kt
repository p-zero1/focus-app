package com.focusapp.domain.model

/**
 * Immutable configuration passed to [com.focusapp.service.TimerService] when starting a session.
 *
 * @param durationSeconds Total planned focus duration in seconds.
 * @param tag Optional user label (only shown/stored when mode is STUDY or CUSTOM).
 * @param focusStrictness Enforcement level applied when a distraction is detected.
 */
data class SessionConfig(
    val mode: SessionMode,
    val durationSeconds: Int,
    val tag: String? = null,
    val focusStrictness: FocusStrictness = FocusStrictness.RELAXED,
) {
    companion object {
        /** Default Pomodoro preset: 25-minute focus interval. */
        val DEFAULT_POMODORO = SessionConfig(
            mode = SessionMode.POMODORO,
            durationSeconds = 25 * 60,
        )
    }
}
