package com.focusapp.domain.usecase

import com.focusapp.domain.model.FocusStrictness
import com.focusapp.domain.model.SessionConfig
import com.focusapp.domain.model.SessionMode
import javax.inject.Inject

/**
 * Builds a [SessionConfig] from the user's mode selection and caller-supplied duration.
 * All modes use caller-supplied [durationSeconds], clamped to valid bounds.
 */
class BuildSessionConfigUseCase @Inject constructor() {
    operator fun invoke(
        mode: SessionMode,
        durationSeconds: Int,
        tag: String?,
        strictness: FocusStrictness = FocusStrictness.RELAXED,
    ): SessionConfig {
        val clamped = durationSeconds.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)
        return SessionConfig(mode = mode, durationSeconds = clamped, tag = tag, focusStrictness = strictness)
    }

    companion object {
        const val MIN_DURATION_SECONDS = 300    // 5 minutes
        const val MAX_DURATION_SECONDS = 10800  // 3 hours
    }
}
