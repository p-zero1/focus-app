package com.focusapp.domain.usecase

import com.focusapp.domain.model.SessionOutcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AwardXpUseCaseTest {

    private val fakeRepo = FakeUserProfileRepository()
    private val useCase = AwardXpUseCase(fakeRepo)

    // ---- Flat formula (no outcome) ----

    @Test
    fun `25 minutes awards 50 XP`() = runTest {
        useCase(25 * 60)
        assertEquals(50, fakeRepo.addedXp)
    }

    @Test
    fun `5 minutes awards 10 XP`() = runTest {
        useCase(5 * 60)
        assertEquals(10, fakeRepo.addedXp)
    }

    @Test
    fun `exactly 4 minutes awards 0 XP and does not call addXp`() = runTest {
        useCase(4 * 60)  // 240 s → floor(240/300)×10 = 0
        assertEquals(0, fakeRepo.addXpCallCount)
    }

    @Test
    fun `0 seconds awards 0 XP and does not call addXp`() = runTest {
        useCase(0)
        assertEquals(0, fakeRepo.addXpCallCount)
    }

    @Test
    fun `50 minutes awards 100 XP`() = runTest {
        useCase(50 * 60)  // 3000 s → floor(3000/300)×10 = 100
        assertEquals(100, fakeRepo.addedXp)
    }

    @Test
    fun `formula floors to 5-minute intervals`() = runTest {
        useCase(9 * 60 + 59)  // 599 s → floor(599/300)×10 = 10
        assertEquals(10, fakeRepo.addedXp)
    }

    // ---- Clean session bonus (1.5×) ----

    @Test
    fun `CLEAN outcome applies 1_5x multiplier to base XP`() = runTest {
        // 25 min → baseXp = 50; × 1.5f = 75
        val xp = useCase(25 * 60, SessionOutcome.CLEAN)
        assertEquals(75, xp)
        assertEquals(75, fakeRepo.addedXp)
    }

    @Test
    fun `CLEAN outcome with 10 minutes awards 15 XP`() = runTest {
        // 10 min → baseXp = 20; × 1.5f = 30
        val xp = useCase(10 * 60, SessionOutcome.CLEAN)
        assertEquals(30, xp)
        assertEquals(30, fakeRepo.addedXp)
    }

    @Test
    fun `CLEAN outcome with zero base XP does not call addXp`() = runTest {
        // 4 min → baseXp = 0; × 1.5f = 0 → no repo call
        useCase(4 * 60, SessionOutcome.CLEAN)
        assertEquals(0, fakeRepo.addXpCallCount)
    }

    @Test
    fun `CLEAN outcome returns awarded XP value`() = runTest {
        val returned = useCase(25 * 60, SessionOutcome.CLEAN)
        assertEquals(75, returned)
    }

    // ---- Non-clean outcomes — flat formula ----

    @Test
    fun `INTERRUPTED outcome awards flat XP with no multiplier`() = runTest {
        val xp = useCase(25 * 60, SessionOutcome.INTERRUPTED)
        assertEquals(50, xp)
        assertEquals(50, fakeRepo.addedXp)
    }

    @Test
    fun `FAILED outcome awards flat XP with no multiplier`() = runTest {
        val xp = useCase(25 * 60, SessionOutcome.FAILED)
        assertEquals(50, xp)
        assertEquals(50, fakeRepo.addedXp)
    }

    @Test
    fun `null outcome awards flat XP with no multiplier`() = runTest {
        val xp = useCase(25 * 60, null)
        assertEquals(50, xp)
        assertEquals(50, fakeRepo.addedXp)
    }
}
