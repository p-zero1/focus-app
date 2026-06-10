package com.focusapp.domain.usecase

import com.focusapp.domain.model.DailyFocusSummary
import com.focusapp.domain.model.FocusSession
import com.focusapp.domain.model.SessionConfig
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.model.SessionStatus
import com.focusapp.domain.model.TagAggregate
import com.focusapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetDailyFocusMinutesUseCaseTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    // ---- helpers ----

    private fun todayEpochMs(): Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun makeSession(
        id: Long,
        startTime: Long,
        actualDuration: Int,
        status: SessionStatus,
    ) = FocusSession(
        id = id,
        startTime = startTime,
        endTime = startTime + actualDuration * 1000L,
        plannedDuration = actualDuration,
        actualDuration = actualDuration,
        mode = SessionMode.POMODORO,
        tag = null,
        status = status,
        distractionCount = 0,
        distractionTotalSeconds = 0,
        xpAwarded = 0,
        focusScore = null,
    )

    // ---- tests ----

    @Test
    fun `returns zero when no sessions today`() = runTest {
        val repo = FakeSessionRepository(sessions = emptyList())
        val result = GetDailyFocusMinutesUseCase(repo)().first()
        assertEquals(0, result)
    }

    @Test
    fun `sums completed sessions in minutes`() = runTest {
        val today = todayEpochMs()
        val sessions = listOf(
            makeSession(1L, today, 1500, SessionStatus.COMPLETED), // 25 min
            makeSession(2L, today, 900, SessionStatus.COMPLETED),  // 15 min
        )
        val repo = FakeSessionRepository(sessions)
        val result = GetDailyFocusMinutesUseCase(repo)().first()
        assertEquals(40, result)
    }

    @Test
    fun `excludes PARTIAL sessions from total`() = runTest {
        val today = todayEpochMs()
        val sessions = listOf(
            makeSession(1L, today, 1500, SessionStatus.COMPLETED), // 25 min
            makeSession(2L, today, 600, SessionStatus.PARTIAL),    // excluded
        )
        val repo = FakeSessionRepository(sessions)
        val result = GetDailyFocusMinutesUseCase(repo)().first()
        assertEquals(25, result)
    }

    @Test
    fun `excludes ACTIVE sessions from total`() = runTest {
        val today = todayEpochMs()
        val sessions = listOf(
            makeSession(1L, today, 1500, SessionStatus.COMPLETED), // 25 min
            makeSession(2L, today, 300, SessionStatus.ACTIVE),     // excluded
        )
        val repo = FakeSessionRepository(sessions)
        val result = GetDailyFocusMinutesUseCase(repo)().first()
        assertEquals(25, result)
    }

    @Test
    fun `integer division truncates to whole minutes`() = runTest {
        val today = todayEpochMs()
        // 91 seconds = 1 minute (truncated)
        val sessions = listOf(makeSession(1L, today, 91, SessionStatus.COMPLETED))
        val repo = FakeSessionRepository(sessions)
        val result = GetDailyFocusMinutesUseCase(repo)().first()
        assertEquals(1, result)
    }
}

// ---- Fake ----

private class FakeSessionRepository(
    private val sessions: List<FocusSession>,
) : SessionRepository {

    override fun getSessionsByDateRange(startMs: Long, endMs: Long, isPremium: Boolean): Flow<List<FocusSession>> =
        flowOf(sessions.filter { it.startTime in startMs until endMs })

    // All other members are unsupported for this test
    override suspend fun startSession(config: SessionConfig, startTime: Long): Long = 0L
    override suspend fun updateSession(session: FocusSession) = Unit
    override suspend fun getSessionById(id: Long): FocusSession? = null
    override fun getAllSessions(): Flow<List<FocusSession>> = flowOf(sessions)
    override fun getSessionsByTag(tag: String): Flow<List<FocusSession>> = flowOf(emptyList())
    override fun getTagAggregates(): Flow<List<TagAggregate>> = flowOf(emptyList())
    override suspend fun getCompletedCountSince(sinceMs: Long): Int = 0
    override suspend fun getTotalCompletedCount(): Int = 0
    override suspend fun getActiveSession(): FocusSession? = null
    override suspend fun deleteSession(id: Long) = Unit
    override fun getWeeklySessions(weekStartMs: Long, weekEndMs: Long): Flow<List<DailyFocusSummary>> = flowOf(emptyList())
    override suspend fun getDailyFocusMinutes(date: String): DailyFocusSummary? = null
    override suspend fun getSessionCountSince(sinceMs: Long): Int = 0
}
