package com.focusapp.domain.usecase

import com.focusapp.domain.repository.UserProfileRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Updates the user's consecutive-day streak after a session completes.
 *
 * Rules:
 * - If [streakLastUpdatedDate] is already today → no change (streak already counted today)
 * - If [streakLastUpdatedDate] is yesterday → increment streak
 * - Otherwise (gap > 1 day, or first session ever) → reset to 1
 * - Updates [longestStreak] if the new streak exceeds it
 */
class UpdateStreakUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
) {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend operator fun invoke() {
        val profile = userProfileRepository.getProfileOnce()
        val today = LocalDate.now()
        val todayStr = today.format(isoFormatter)

        val lastUpdated = runCatching {
            LocalDate.parse(profile.streakLastUpdatedDate, isoFormatter)
        }.getOrNull()

        val newStreak = when {
            lastUpdated == null -> 1
            lastUpdated == today -> return  // Already updated today
            ChronoUnit.DAYS.between(lastUpdated, today) == 1L -> profile.currentStreak + 1
            else -> 1
        }

        userProfileRepository.updateStreak(newStreak, todayStr)
    }
}
