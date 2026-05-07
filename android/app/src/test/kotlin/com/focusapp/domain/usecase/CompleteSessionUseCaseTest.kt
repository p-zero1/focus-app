package com.focusapp.domain.usecase

import com.focusapp.domain.model.DailyFocusSummary
import com.focusapp.domain.model.FocusSession
import com.focusapp.domain.model.SessionConfig
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.model.SessionStatus
import com.focusapp.domain.model.TagAggregate
import com.focusapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompleteSessionUseCaseTest {

    private val fakeSessionRepo = FakeCompleteSessionRepo()
    private val fakeUserProfileRepo = FakeUserProfileRepository()
    private val fakeBadgeRepo = FakeBadgeRepository()
    private val useCase = CompleteSessionUseCase(
        sessionRepository = fakeSessionRepo,
        userProfileRepository = fakeUserProfileRepo,
        awardXp = AwardXpUseCase(fakeUserProfileRepo),
        updateStreak = UpdateStreakUseCase(fakeUserProfileRepo),
        evaluateBadges = EvaluateBadgesUseCase(fakeBadgeRepo, fakeUserProfileRepo),
        computeFocusScore = ComputeFocusScoreUseCase(),
    )

    @Test
    fun `completed session when actualDuration meets planned gets COMPLETED status`() = runTest {
        val sessionId = 1L
        fakeSessionRepo.sessions[sessionId] = buildSession(id = sessionId, plannedDuration = 1500)

        useCase(sessionId, actualDuration = 1500, endTime = 9_000L)

        val updated = fakeSessionRepo.updatedSessions.last()
        assertEquals(SessionStatus.COMPLETED, updated.status)
        assertEquals(1500, updated.actualDuration)
        assertEquals(9_000L, updated.endTime)
    }

    @Test
    fun `early end gives PARTIAL status when actualDuration less than planned`() = runTest {
        val sessionId = 2L
        fakeSessionRepo.sessions[sessionId] = buildSession(id = sessionId, plannedDuration = 1500)

        useCase(sessionId, actualDuration = 600, endTime = 8_000L)

        assertEquals(SessionStatus.PARTIAL, fakeSessionRepo.updatedSessions.last().status)
    }

    @Test
    fun `xp awarded is proportional to actual duration (10 XP per 5 minutes)`() = runTest {
        val sessionId = 3L
        fakeSessionRepo.sessions[sessionId] = buildSession(id = sessionId, plannedDuration = 1500)

        useCase(sessionId, actualDuration = 1500, endTime = 0L)

        // 1500 s = 25 min, distractionCount=0 → CLEAN → floor(1500/300)×10×1.5 = 75 XP
        assertEquals(75, fakeSessionRepo.updatedSessions.last().xpAwarded)
    }

    @Test
    fun `zero xp for very short session under 5 minutes`() = runTest {
        val sessionId = 4L
        fakeSessionRepo.sessions[sessionId] = buildSession(id = sessionId, plannedDuration = 1500)

        useCase(sessionId, actualDuration = 120, endTime = 0L)

        assertEquals(0, fakeSessionRepo.updatedSessions.last().xpAwarded)
    }

    @Test
    fun `focus score is 100 for distraction-free completed session`() = runTest {
        val sessionId = 5L
        fakeSessionRepo.sessions[sessionId] = buildSession(
            id = sessionId,
            plannedDuration = 1500,
            distractionTotalSeconds = 0,
        )

        useCase(sessionId, actualDuration = 1500, endTime = 0L)

        val score = fakeSessionRepo.updatedSessions.last().focusScore
        assertNotNull(score)
        assertEquals(100, score)
    }

    @Test
    fun `focus score is lower when distractions are present`() = runTest {
        val sessionId = 6L
        fakeSessionRepo.sessions[sessionId] = buildSession(
            id = sessionId,
            plannedDuration = 1500,
            distractionTotalSeconds = 750, // 50% of session time
        )

        useCase(sessionId, actualDuration = 1500, endTime = 0L)

        val score = fakeSessionRepo.updatedSessions.last().focusScore!!
        assertTrue("Expected score < 100, got $score", score < 100)
        assertTrue("Expected score >= 0, got $score", score >= 0)
    }

    @Test
    fun `no-op when session id not found`() = runTest {
        // sessionId 99 doesn't exist in the fake repo
        useCase(sessionId = 99L, actualDuration = 500, endTime = 0L)

        assertTrue(fakeSessionRepo.updatedSessions.isEmpty())
    }

    // ---- Helpers ----

    private fun buildSession(
        id: Long,
        plannedDuration: Int = 1500,
        distractionTotalSeconds: Int = 0,
    ) = FocusSession(
        id = id,
        startTime = 0L,
        endTime = null,
        plannedDuration = plannedDuration,
        actualDuration = 0,
        mode = SessionMode.POMODORO,
        tag = null,
        status = SessionStatus.ACTIVE,
        distractionCount = 0,
        distractionTotalSeconds = distractionTotalSeconds,
        xpAwarded = 0,
        focusScore = null,
    )
}

// ---- Fake specific to this test (unique name avoids conflict with other test files) ----

private class FakeCompleteSessionRepo : SessionRepository {

    val sessions = mutableMapOf<Long, FocusSession>()
    val updatedSessions = mutableListOf<FocusSession>()

    override suspend fun startSession(config: SessionConfig, startTime: Long): Long = 1L
    override suspend fun updateSession(session: FocusSession) { updatedSessions.add(session) }
    override suspend fun getSessionById(id: Long): FocusSession? = sessions[id]
    override fun getAllSessions(): Flow<List<FocusSession>> = flowOf(emptyList())
    override fun getSessionsByDateRange(startMs: Long, endMs: Long, isPremium: Boolean): Flow<List<FocusSession>> = flowOf(emptyList())
    override fun getSessionsByTag(tag: String): Flow<List<FocusSession>> = flowOf(emptyList())
    override fun getTagAggregates(): Flow<List<TagAggregate>> = flowOf(emptyList())
    override suspend fun getCompletedCountSince(sinceMs: Long): Int = 0
    override suspend fun getTotalCompletedCount(): Int = 0
    override suspend fun getActiveSession(): FocusSession? = null
    override suspend fun deleteSession(id: Long) {}
    override fun getWeeklySessions(weekStartMs: Long, weekEndMs: Long): Flow<List<DailyFocusSummary>> = flowOf(emptyList())
    override suspend fun getDailyFocusMinutes(date: String): DailyFocusSummary? = null
    override suspend fun getSessionCountSince(sinceMs: Long): Int = 0
}
