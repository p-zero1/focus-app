package com.focusapp.domain.usecase

import com.focusapp.domain.model.DistractionEvent
import com.focusapp.domain.model.DistractionType
import com.focusapp.domain.repository.DistractionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LogDistractionUseCaseTest {

    private val fakeRepo = FakeDistractionRepository()
    private val useCase = LogDistractionUseCase(fakeRepo)

    @Test
    fun `invoke passes all parameters to repository unchanged`() = runTest {
        useCase(
            sessionId = 42L,
            timestampMs = 1_000L,
            type = DistractionType.APP_SWITCH,
            appPackageName = "com.instagram.android",
            awayDurationMs = 15_000L,
        )

        val call = fakeRepo.calls.single()
        assertEquals(42L, call.sessionId)
        assertEquals(1_000L, call.timestamp)
        assertEquals(DistractionType.APP_SWITCH, call.type)
        assertEquals("com.instagram.android", call.appPackageName)
        assertEquals(15, call.durationSeconds)   // 15_000 ms → 15 s
    }

    @Test
    fun `duration is coerced to at least 1 second for very short events`() = runTest {
        useCase(
            sessionId = 1L,
            timestampMs = 0L,
            type = DistractionType.SCREEN_UNLOCK,
            appPackageName = null,
            awayDurationMs = 500L,  // under 1 second
        )

        assertEquals(1, fakeRepo.calls.single().durationSeconds)
    }

    @Test
    fun `screen unlock event has null appPackageName`() = runTest {
        useCase(
            sessionId = 1L,
            timestampMs = 0L,
            type = DistractionType.SCREEN_UNLOCK,
            appPackageName = null,
            awayDurationMs = 5_000L,
        )

        assertEquals(null, fakeRepo.calls.single().appPackageName)
    }
}

// ---- Minimal fake ----

private data class LogCall(
    val sessionId: Long,
    val timestamp: Long,
    val type: DistractionType,
    val appPackageName: String?,
    val durationSeconds: Int,
)

private class FakeDistractionRepository : DistractionRepository {
    val calls = mutableListOf<LogCall>()

    override suspend fun logDistraction(
        sessionId: Long,
        timestamp: Long,
        type: DistractionType,
        appPackageName: String?,
        durationSeconds: Int,
    ) {
        calls.add(LogCall(sessionId, timestamp, type, appPackageName, durationSeconds))
    }

    override fun getBySessionId(sessionId: Long): Flow<List<DistractionEvent>> = flowOf(emptyList())
    override suspend fun getBySessionIdOnce(sessionId: Long): List<DistractionEvent> = emptyList()
}
