package com.focusapp.domain.repository

import com.focusapp.domain.model.FocusSession
import com.focusapp.domain.model.SessionConfig
import com.focusapp.domain.model.TagAggregate
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    /** Insert a new ACTIVE session row; returns its generated ID. */
    suspend fun startSession(config: SessionConfig, startTime: Long): Long

    /** Persist changes to an existing session (status, actualDuration, scores, etc.). */
    suspend fun updateSession(session: FocusSession)

    suspend fun getSessionById(id: Long): FocusSession?

    fun getAllSessions(): Flow<List<FocusSession>>

    fun getSessionsByDateRange(startMs: Long, endMs: Long): Flow<List<FocusSession>>

    fun getSessionsByTag(tag: String): Flow<List<FocusSession>>

    suspend fun getCompletedCountSince(sinceMs: Long): Int

    suspend fun getTotalCompletedCount(): Int

    suspend fun getActiveSession(): FocusSession?

    suspend fun deleteSession(id: Long)
}
