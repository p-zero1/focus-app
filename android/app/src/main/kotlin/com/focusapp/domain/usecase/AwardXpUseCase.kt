package com.focusapp.domain.usecase

import com.focusapp.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * Computes and awards XP to the user profile after a session completes.
 *
 * Formula: `floor(actualDuration / 300) × 10` — 10 XP per 5 minutes of focus.
 *
 * Returns the XP amount awarded (0 if under 5 minutes) so callers can store
 * it in the session row without duplicating the formula.
 */
class AwardXpUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
) {
    suspend operator fun invoke(actualDurationSeconds: Int): Int {
        val xp = (actualDurationSeconds / 300) * 10
        if (xp > 0) {
            userProfileRepository.addXp(xp)
        }
        return xp
    }
}
