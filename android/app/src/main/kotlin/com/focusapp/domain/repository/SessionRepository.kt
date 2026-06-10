package com.focusapp.domain.repository

import com.focusapp.domain.model.DailyFocusSummary
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

    /**
     * Returns sessions whose [FocusSession.startTime] is in [[startMs], [endMs]).
     *
     * When [isPremium] is `false` the repository clamps [startMs] to the free-tier
     * analytics window (last 7 days), enforcing FR-015 at the data layer as required
     * by the constitution Monetisation Boundary principle.
     */
    fun getSessionsByDateRange(startMs: Long, endMs: Long, isPremium: Boolean = true): Flow<List<FocusSession>>

    fun getSessionsByTag(tag: String): Flow<List<FocusSession>>

    /** Returns aggregated tag summaries ordered by total focus minutes descending. */
    fun getTagAggregates(): Flow<List<TagAggregate>>

    suspend fun getCompletedCountSince(sinceMs: Long): Int

    suspend fun getTotalCompletedCount(): Int

    suspend fun getActiveSession(): FocusSession?

    suspend fun deleteSession(id: Long)

    /** Returns per-day summaries for sessions within [weekStartMs, weekEndMs). */
    fun getWeeklySessions(weekStartMs: Long, weekEndMs: Long): Flow<List<DailyFocusSummary>>

    /** Returns the day summary for [date] ("YYYY-MM-DD"), or null if no sessions. */
    suspend fun getDailyFocusMinutes(date: String): DailyFocusSummary?

    /** Total sessions (any status) started on or after [sinceMs]. */
    suspend fun getSessionCountSince(sinceMs: Long): Int
}
