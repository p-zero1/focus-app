package com.focusapp.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UpdateStreakUseCaseTest {

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    private fun today() = LocalDate.now().format(fmt)
    private fun yesterday() = LocalDate.now().minusDays(1).format(fmt)
    private fun twoDaysAgo() = LocalDate.now().minusDays(2).format(fmt)

    @Test
    fun `first ever session — empty streak date — sets streak to 1`() = runTest {
        val repo = FakeUserProfileRepository(streakLastUpdatedDate = "")
        UpdateStreakUseCase(repo).invoke()
        assertEquals(1, repo.updatedStreak)
        assertEquals(today(), repo.updatedStreakDate)
    }

    @Test
    fun `consecutive day — increments streak`() = runTest {
        val repo = FakeUserProfileRepository(
            currentStreak = 3,
            streakLastUpdatedDate = yesterday(),
        )
        UpdateStreakUseCase(repo).invoke()
        assertEquals(4, repo.updatedStreak)
    }

    @Test
    fun `same day second session — no update`() = runTest {
        val repo = FakeUserProfileRepository(
            currentStreak = 5,
            streakLastUpdatedDate = today(),
        )
        UpdateStreakUseCase(repo).invoke()
        assertEquals(null, repo.updatedStreak)  // updateStreak never called
    }

    @Test
    fun `gap of 2 days — resets streak to 1`() = runTest {
        val repo = FakeUserProfileRepository(
            currentStreak = 10,
            streakLastUpdatedDate = twoDaysAgo(),
        )
        UpdateStreakUseCase(repo).invoke()
        assertEquals(1, repo.updatedStreak)
    }

    @Test
    fun `new streak of 7 updates longestStreak via DAO`() = runTest {
        // DAO handles MAX(longest, streak) — use case just passes new value
        val repo = FakeUserProfileRepository(
            currentStreak = 6,
            longestStreak = 6,
            streakLastUpdatedDate = yesterday(),
        )
        UpdateStreakUseCase(repo).invoke()
        assertEquals(7, repo.updatedStreak)
    }
}
