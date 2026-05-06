package com.focusapp.domain.usecase

import com.focusapp.domain.model.SessionOutcome
import com.focusapp.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * Computes and awards XP to the user profile after a session completes.
 *
 * Base formula: `floor(actualDuration / 300) × 10` — 10 XP per 5 minutes.
 * Clean session bonus: +50% for zero distractions (encourages focus quality).
 *
 * Returns the XP amount awarded so callers can store it in the session row.
 */
class AwardXpUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
) {
    suspend operator fun invoke(
        actualDurationSeconds: Int,
        sessionOutcome: SessionOutcome? = null,
    ): Int {
        val baseXp = (actualDurationSeconds / 300) * 10
        val xp = if (sessionOutcome == SessionOutcome.CLEAN) (baseXp * 1.5f).toInt() else baseXp
        if (xp > 0) userProfileRepository.addXp(xp)
        return xp
    }
}
