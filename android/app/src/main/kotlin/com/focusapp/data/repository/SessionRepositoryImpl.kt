package com.focusapp.data.repository

import com.focusapp.data.db.dao.DistractionEventDao
import com.focusapp.data.db.dao.FocusSessionDao
import com.focusapp.data.db.entity.FocusSessionEntity
import com.focusapp.domain.model.DailyFocusSummary
import com.focusapp.domain.model.FocusSession
import com.focusapp.domain.model.FocusStrictness
import com.focusapp.domain.model.SessionConfig
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.model.SessionOutcome
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
            focusStrictness = config.focusStrictness.name,
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

    override fun getSessionsByDateRange(startMs: Long, endMs: Long, isPremium: Boolean): Flow<List<FocusSession>> =
        sessionDao.getByDateRange(clampStartForTier(isPremium = isPremium, startMs = startMs), endMs)
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

    // ---- Tier gating (FR-015 / FR-026 — Constitution Monetisation Boundary) ----

    /**
     * Clamps [startMs] to the free-tier analytics window when [isPremium] is false.
     *
     * Free users are limited to the last [FREE_TIER_ANALYTICS_DAYS] days (FR-015).
     * Premium users receive the unmodified [startMs].
     *
     * Enforcement is at the repository query level per the constitution:
     * "Free-tier access limits MUST be enforced at the repository query level — not
     *  in the ViewModel or UI layer."
     */
    private fun clampStartForTier(isPremium: Boolean, startMs: Long): Long {
        if (isPremium) return startMs
        val freeWindowStartMs = System.currentTimeMillis() - FREE_TIER_ANALYTICS_DAYS * 24 * 60 * 60 * 1000L
        return maxOf(startMs, freeWindowStartMs)
    }

    companion object {
        /** Free-tier analytics window in days (FR-015). */
        private const val FREE_TIER_ANALYTICS_DAYS = 7L
    }

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
        focusStrictness = runCatching { FocusStrictness.valueOf(focusStrictness) }
            .getOrDefault(FocusStrictness.RELAXED),
        sessionOutcome = sessionOutcome?.let {
            runCatching { SessionOutcome.valueOf(it) }.getOrNull()
        },
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
        focusStrictness = focusStrictness.name,
        sessionOutcome = sessionOutcome?.name,
    )
}
