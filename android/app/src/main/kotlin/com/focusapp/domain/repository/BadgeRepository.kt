package com.focusapp.domain.repository

import com.focusapp.domain.model.Badge
import com.focusapp.domain.model.BadgeId
import kotlinx.coroutines.flow.Flow

interface BadgeRepository {

    /** Award a badge; silently ignored if already awarded (IGNORE conflict strategy). */
    suspend fun awardBadge(badgeId: BadgeId, awardedAt: Long)

    fun getAllBadges(): Flow<List<Badge>>

    /** Returns null if the badge has not yet been awarded. */
    suspend fun getBadgeById(id: String): Badge?
}
