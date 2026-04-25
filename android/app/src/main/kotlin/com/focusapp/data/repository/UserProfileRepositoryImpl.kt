package com.focusapp.data.repository

import com.focusapp.data.db.dao.UserProfileDao
import com.focusapp.data.db.entity.UserProfileEntity
import com.focusapp.domain.model.UserProfile
import com.focusapp.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val profileDao: UserProfileDao,
) : UserProfileRepository {

    /** Seed the singleton row on first emission if it doesn't exist yet. */
    override fun getProfile(): Flow<UserProfile> =
        profileDao.getProfile()
            .onStart { ensureProfileExists() }
            .map { it?.toDomain() ?: UserProfileEntity().toDomain() }

    override suspend fun getProfileOnce(): UserProfile {
        ensureProfileExists()
        return profileDao.getProfileOnce()?.toDomain() ?: UserProfileEntity().toDomain()
    }

    override suspend fun addXp(xp: Int) {
        ensureProfileExists()
        profileDao.addXp(xp)
    }

    override suspend fun updateStreak(streak: Int, date: String) {
        ensureProfileExists()
        profileDao.updateStreak(streak, date)
    }

    override suspend fun incrementSessionsCompleted() {
        ensureProfileExists()
        profileDao.incrementSessionsCompleted()
    }

    private suspend fun ensureProfileExists() {
        if (profileDao.getProfileOnce() == null) {
            profileDao.upsert(UserProfileEntity())
        }
    }

    // ---- Mapper ----

    private fun UserProfileEntity.toDomain() = UserProfile(
        totalXp = totalXp,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        streakLastUpdatedDate = streakLastUpdatedDate,
        totalSessionsCompleted = totalSessionsCompleted,
        isPremium = isPremium,
        dailyFocusGoalMinutes = dailyFocusGoalMinutes,
    )
}
