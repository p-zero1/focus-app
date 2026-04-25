package com.focusapp.domain.usecase

import com.focusapp.domain.model.SessionConfig
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.model.SessionStatus
import com.focusapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import com.focusapp.domain.model.FocusSession
import com.focusapp.domain.model.TagAggregate

class StartSessionUseCaseTest {

    private val fakeRepo = FakeSessionRepository()
    private val useCase = StartSessionUseCase(fakeRepo)

    @Test
    fun `invoke inserts new ACTIVE session and returns generated id`() = runTest {
        val config = SessionConfig(mode = SessionMode.POMODORO, durationSeconds = 25 * 60)
        val startTime = System.currentTimeMillis()

        val id = useCase(config, startTime)

        assertEquals(1L, id)
        assertEquals(1, fakeRepo.insertedSessions.size)
        val inserted = fakeRepo.insertedSessions.first()
        assertEquals(SessionMode.POMODORO, inserted.second.mode)
        assertEquals(25 * 60, inserted.second.durationSeconds)
        assertEquals(startTime, inserted.first)
    }

    @Test
    fun `invoke with STUDY mode and tag passes tag through to repository`() = runTest {
        val config = SessionConfig(mode = SessionMode.STUDY, durationSeconds = 60 * 60, tag = "DSA")
        val startTime = 1_000L

        useCase(config, startTime)

        assertEquals("DSA", fakeRepo.insertedSessions.first().second.tag)
    }
}

// ---- Minimal fake — no mocking framework needed (Constitution Principle VI) ----

private class FakeSessionRepository : SessionRepository {

    val insertedSessions = mutableListOf<Pair<Long, SessionConfig>>()
    private var idCounter = 1L

    override suspend fun startSession(config: SessionConfig, startTime: Long): Long {
        insertedSessions.add(startTime to config)
        return idCounter++
    }

    override suspend fun updateSession(session: FocusSession) {}
    override suspend fun getSessionById(id: Long): FocusSession? = null
    override fun getAllSessions(): Flow<List<FocusSession>> = flowOf(emptyList())
    override fun getSessionsByDateRange(startMs: Long, endMs: Long, isPremium: Boolean): Flow<List<FocusSession>> = flowOf(emptyList())
    override fun getSessionsByTag(tag: String): Flow<List<FocusSession>> = flowOf(emptyList())
    override suspend fun getCompletedCountSince(sinceMs: Long): Int = 0
    override suspend fun getTotalCompletedCount(): Int = 0
    override suspend fun getActiveSession(): FocusSession? = null
    override suspend fun deleteSession(id: Long) {}
}
