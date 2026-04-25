package com.focusapp.domain.usecase

import com.focusapp.domain.model.Badge
import com.focusapp.domain.model.BadgeId
import com.focusapp.domain.model.FocusSession
import com.focusapp.domain.model.UserProfile
import com.focusapp.domain.repository.BadgeRepository
import com.focusapp.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * Checks all 5 badge conditions against the current [UserProfile] and the
 * just-completed [FocusSession]. Inserts new [Badge] rows for any newly
 * earned badges (duplicate awards are silently ignored by the DAO).
 *
 * Returns the list of newly awarded badges so the UI can celebrate them.
 *
 * Badge conditions:
 * - FIRST_FOCUS       → totalSessionsCompleted ≥ 1
 * - DEEP_DIVER        → session.actualDuration ≥ 7200 s (2 hours in one session)
 * - WEEK_WARRIOR      → currentStreak ≥ 7
 * - DISTRACTION_FREE  → session.distractionCount == 0 and session is COMPLETED
 * - CENTURY           → totalSessionsCompleted ≥ 100
 */
class EvaluateBadgesUseCase @Inject constructor(
    private val badgeRepository: BadgeRepository,
    private val userProfileRepository: UserProfileRepository,
) {
    suspend operator fun invoke(session: FocusSession): List<Badge> {
        val profile = userProfileRepository.getProfileOnce()
        val now = System.currentTimeMillis()
        val newBadges = mutableListOf<Badge>()

        val candidates = buildList {
            if (profile.totalSessionsCompleted >= 1) add(BadgeId.FIRST_FOCUS)
            if (session.actualDuration >= 7_200) add(BadgeId.DEEP_DIVER)
            if (profile.currentStreak >= 7) add(BadgeId.WEEK_WARRIOR)
            if (session.distractionCount == 0 &&
                session.status == com.focusapp.domain.model.SessionStatus.COMPLETED
            ) add(BadgeId.DISTRACTION_FREE)
            if (profile.totalSessionsCompleted >= 100) add(BadgeId.CENTURY)
        }

        for (badgeId in candidates) {
            val alreadyAwarded = badgeRepository.getBadgeById(badgeId.name) != null
            if (!alreadyAwarded) {
                badgeRepository.awardBadge(badgeId, now)
                newBadges.add(
                    Badge(
                        id = badgeId.name,
                        name = badgeId.displayName,
                        description = badgeId.description,
                        awardedAt = now,
                    )
                )
            }
        }

        return newBadges
    }
}
