package com.focusapp.domain.repository

import com.focusapp.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {

    /** Reactive stream of the singleton profile; emits whenever the row changes. */
    fun getProfile(): Flow<UserProfile>

    /** One-shot read for use cases that don't need reactivity. */
    suspend fun getProfileOnce(): UserProfile

    suspend fun addXp(xp: Int)

    suspend fun updateStreak(streak: Int, date: String)

    suspend fun incrementSessionsCompleted()
}
