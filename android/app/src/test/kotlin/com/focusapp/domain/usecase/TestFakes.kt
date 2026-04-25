package com.focusapp.domain.usecase

import com.focusapp.domain.model.Badge
import com.focusapp.domain.model.BadgeId
import com.focusapp.domain.model.UserProfile
import com.focusapp.domain.repository.BadgeRepository
import com.focusapp.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// ---- FakeUserProfileRepository ----

internal class FakeUserProfileRepository(
    totalXp: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val streakLastUpdatedDate: String = "",
    val totalSessionsCompleted: Int = 0,
) : UserProfileRepository {

    var addedXp: Int = 0
    var addXpCallCount: Int = 0
    var updatedStreak: Int? = null
    var updatedStreakDate: String? = null

    private val profile = UserProfile(
        totalXp = totalXp,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        streakLastUpdatedDate = streakLastUpdatedDate,
        totalSessionsCompleted = totalSessionsCompleted,
        isPremium = false,
        dailyFocusGoalMinutes = 60,
    )

    override fun getProfile(): Flow<UserProfile> = flowOf(profile)

    override suspend fun getProfileOnce(): UserProfile = profile

    override suspend fun addXp(xp: Int) {
        addedXp += xp
        addXpCallCount++
    }

    override suspend fun updateStreak(streak: Int, date: String) {
        updatedStreak = streak
        updatedStreakDate = date
    }

    override suspend fun incrementSessionsCompleted() {}
}

// ---- FakeBadgeRepository ----

internal class FakeBadgeRepository(
    existing: List<Badge> = emptyList(),
) : BadgeRepository {

    private val awarded = existing.associateBy { it.id }.toMutableMap()
    val awardedBadgeIds = mutableListOf<String>()

    override suspend fun awardBadge(badgeId: BadgeId, awardedAt: Long) {
        val badge = Badge(
            id = badgeId.name,
            name = badgeId.displayName,
            description = badgeId.description,
            awardedAt = awardedAt,
        )
        awarded[badgeId.name] = badge
        awardedBadgeIds.add(badgeId.name)
    }

    override fun getAllBadges(): Flow<List<Badge>> = flowOf(awarded.values.toList())

    override suspend fun getBadgeById(id: String): Badge? = awarded[id]
}
