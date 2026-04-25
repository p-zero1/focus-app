package com.focusapp.data.repository

import com.focusapp.data.db.dao.BadgeDao
import com.focusapp.data.db.entity.BadgeEntity
import com.focusapp.domain.model.Badge
import com.focusapp.domain.model.BadgeId
import com.focusapp.domain.repository.BadgeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BadgeRepositoryImpl @Inject constructor(
    private val badgeDao: BadgeDao,
) : BadgeRepository {

    override suspend fun awardBadge(badgeId: BadgeId, awardedAt: Long) {
        badgeDao.insert(
            BadgeEntity(
                id = badgeId.name,
                name = badgeId.displayName,
                description = badgeId.description,
                awardedAt = awardedAt,
            )
        )
    }

    override fun getAllBadges(): Flow<List<Badge>> =
        badgeDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getBadgeById(id: String): Badge? =
        badgeDao.getById(id)?.toDomain()

    // ---- Mapper ----

    private fun BadgeEntity.toDomain() = Badge(
        id = id,
        name = name,
        description = description,
        awardedAt = awardedAt,
    )
}
