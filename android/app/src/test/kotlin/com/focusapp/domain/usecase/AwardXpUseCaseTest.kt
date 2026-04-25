package com.focusapp.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AwardXpUseCaseTest {

    private val fakeRepo = FakeUserProfileRepository()
    private val useCase = AwardXpUseCase(fakeRepo)

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
}
