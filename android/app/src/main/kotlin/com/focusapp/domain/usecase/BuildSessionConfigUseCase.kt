package com.focusapp.domain.usecase

import com.focusapp.domain.model.SessionConfig
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.preferences.FocusPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Builds a [SessionConfig] from the user's mode selection and current preferences.
 *
 * Encapsulates the mode → duration mapping that was previously in [TimerViewModel],
 * satisfying Constitution Principle II (business logic belongs in use cases).
 *
 * Duration rules:
 * - POMODORO    → pomodoroFocusMinutes from preferences
 * - DEEP_WORK   → 2 × pomodoroFocusMinutes
 * - CUSTOM/STUDY → caller-supplied [customDurationMinutes]
 */
class BuildSessionConfigUseCase @Inject constructor(
    private val prefs: FocusPreferences,
) {
    suspend operator fun invoke(
        mode: SessionMode,
        customDurationMinutes: Int,
        tag: String?,
    ): SessionConfig {
        val durationSeconds = when (mode) {
            SessionMode.POMODORO -> prefs.pomodoroFocusMinutes.first() * 60
            SessionMode.DEEP_WORK -> prefs.pomodoroFocusMinutes.first() * 60 * 2
            SessionMode.CUSTOM, SessionMode.STUDY -> customDurationMinutes * 60
        }
        return SessionConfig(mode = mode, durationSeconds = durationSeconds, tag = tag)
    }
}
