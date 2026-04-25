package com.focusapp.data.repository

import com.focusapp.data.db.dao.DistractionEventDao
import com.focusapp.data.db.dao.FocusSessionDao
import com.focusapp.data.db.entity.FocusSessionEntity
import com.focusapp.domain.model.DailyFocusSummary
import com.focusapp.domain.model.FocusSession
import com.focusapp.domain.model.SessionConfig
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.model.SessionStatus
import com.focusapp.domain.model.TagAggregate
import com.focusapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: FocusSessionDao,
    private val distractionDao: DistractionEventDao,
) : SessionRepository {

    override suspend fun startSession(config: SessionConfig, startTime: Long): Long {
        val entity = FocusSessionEntity(
            startTime = startTime,
            endTime = null,
            plannedDuration = config.durationSeconds,
            actualDuration = 0,
            mode = config.mode.name,
            tag = config.tag,
            status = SessionStatus.ACTIVE.name,
            focusScore = null,
        )
        return sessionDao.insert(entity)
    }

    override suspend fun updateSession(session: FocusSession) {
        sessionDao.update(session.toEntity())
    }

    override suspend fun getSessionById(id: Long): FocusSession? =
        sessionDao.getById(id)?.toDomain()

    override fun getAllSessions(): Flow<List<FocusSession>> =
        sessionDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getSessionsByDateRange(startMs: Long, endMs: Long): Flow<List<FocusSession>> =
        sessionDao.getByDateRange(clampStartForTier(startMs = startMs), endMs)
            .map { list -> list.map { it.toDomain() } }

    override fun getSessionsByTag(tag: String): Flow<List<FocusSession>> =
        sessionDao.getByTag(tag).map { list -> list.map { it.toDomain() } }

    override fun getTagAggregates(): Flow<List<TagAggregate>> =
        sessionDao.getTagAggregates()

    override suspend fun getCompletedCountSince(sinceMs: Long): Int =
        sessionDao.getCompletedCountSince(sinceMs)

    override suspend fun getTotalCompletedCount(): Int =
        sessionDao.getTotalCompletedCount()

    override suspend fun getActiveSession(): FocusSession? =
        sessionDao.getActiveSession()?.toDomain()

    override suspend fun deleteSession(id: Long) {
        distractionDao.deleteBySessionId(id)
        sessionDao.deleteById(id)
    }

    override fun getWeeklySessions(weekStartMs: Long, weekEndMs: Long): Flow<List<DailyFocusSummary>> =
        sessionDao.getWeeklySessions(weekStartMs, weekEndMs)

    override suspend fun getDailyFocusMinutes(date: String): DailyFocusSummary? =
        sessionDao.getDailyFocusMinutes(date)

    override suspend fun getSessionCountSince(sinceMs: Long): Int =
        sessionDao.getSessionCountSince(sinceMs)

    // ---- Tier gating (dormant — H4) ----

    /** Identity function today (isPremium hardcoded true). Wired in as the hook point for
     *  future free-tier date clamping without changing callers. */
    private fun clampStartForTier(isPremium: Boolean = true, startMs: Long): Long = startMs

    // ---- Mappers ----

    private fun FocusSessionEntity.toDomain() = FocusSession(
        id = id,
        startTime = startTime,
        endTime = endTime,
        plannedDuration = plannedDuration,
        actualDuration = actualDuration,
        mode = SessionMode.valueOf(mode),
        tag = tag,
        status = SessionStatus.valueOf(status),
        distractionCount = distractionCount,
        distractionTotalSeconds = distractionTotalSeconds,
        xpAwarded = xpAwarded,
        focusScore = focusScore,
    )

    private fun FocusSession.toEntity() = FocusSessionEntity(
        id = id,
        startTime = startTime,
        endTime = endTime,
        plannedDuration = plannedDuration,
        actualDuration = actualDuration,
        mode = mode.name,
        tag = tag,
        status = status.name,
        distractionCount = distractionCount,
        distractionTotalSeconds = distractionTotalSeconds,
        xpAwarded = xpAwarded,
        focusScore = focusScore,
    )
}
