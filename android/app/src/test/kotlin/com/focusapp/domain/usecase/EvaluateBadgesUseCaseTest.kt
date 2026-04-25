package com.focusapp.domain.usecase

import com.focusapp.domain.model.Badge
import com.focusapp.domain.model.BadgeId
import com.focusapp.domain.model.FocusSession
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.model.SessionStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateBadgesUseCaseTest {

    private fun makeSession(
        actualDuration: Int = 1500,
        distractionCount: Int = 0,
        status: SessionStatus = SessionStatus.COMPLETED,
    ) = FocusSession(
        id = 1L,
        startTime = 0L,
        endTime = 1_500_000L,
        plannedDuration = 1500,
        actualDuration = actualDuration,
        mode = SessionMode.POMODORO,
        tag = null,
        status = status,
        distractionCount = distractionCount,
        distractionTotalSeconds = 0,
        xpAwarded = 50,
        focusScore = 100,
    )

    @Test
    fun `FIRST_FOCUS awarded when totalSessionsCompleted is 1`() = runTest {
        val badgeRepo = FakeBadgeRepository()
        val profileRepo = FakeUserProfileRepository(totalSessionsCompleted = 1)
        val result = EvaluateBadgesUseCase(badgeRepo, profileRepo).invoke(makeSession())
        assertTrue(result.any { it.id == BadgeId.FIRST_FOCUS.name })
    }

    @Test
    fun `FIRST_FOCUS not re-awarded if already earned`() = runTest {
        val badgeRepo = FakeBadgeRepository(
            existing = listOf(Badge(BadgeId.FIRST_FOCUS.name, "", "", 0L))
        )
        val profileRepo = FakeUserProfileRepository(totalSessionsCompleted = 5)
        val result = EvaluateBadgesUseCase(badgeRepo, profileRepo).invoke(makeSession())
        assertTrue(result.none { it.id == BadgeId.FIRST_FOCUS.name })
    }

    @Test
    fun `DEEP_DIVER awarded for session with actualDuration ge 7200`() = runTest {
        val badgeRepo = FakeBadgeRepository()
        val profileRepo = FakeUserProfileRepository()
        val result = EvaluateBadgesUseCase(badgeRepo, profileRepo)
            .invoke(makeSession(actualDuration = 7_200))
        assertTrue(result.any { it.id == BadgeId.DEEP_DIVER.name })
    }

    @Test
    fun `DEEP_DIVER not awarded for session under 7200 seconds`() = runTest {
        val badgeRepo = FakeBadgeRepository()
        val profileRepo = FakeUserProfileRepository()
        val result = EvaluateBadgesUseCase(badgeRepo, profileRepo)
            .invoke(makeSession(actualDuration = 7_199))
        assertTrue(result.none { it.id == BadgeId.DEEP_DIVER.name })
    }

    @Test
    fun `WEEK_WARRIOR awarded when streak is 7`() = runTest {
        val badgeRepo = FakeBadgeRepository()
        val profileRepo = FakeUserProfileRepository(currentStreak = 7)
        val result = EvaluateBadgesUseCase(badgeRepo, profileRepo).invoke(makeSession())
        assertTrue(result.any { it.id == BadgeId.WEEK_WARRIOR.name })
    }

    @Test
    fun `DISTRACTION_FREE awarded for completed session with 0 distractions`() = runTest {
        val badgeRepo = FakeBadgeRepository()
        val profileRepo = FakeUserProfileRepository()
        val result = EvaluateBadgesUseCase(badgeRepo, profileRepo)
            .invoke(makeSession(distractionCount = 0, status = SessionStatus.COMPLETED))
        assertTrue(result.any { it.id == BadgeId.DISTRACTION_FREE.name })
    }

    @Test
    fun `DISTRACTION_FREE not awarded for partial session`() = runTest {
        val badgeRepo = FakeBadgeRepository()
        val profileRepo = FakeUserProfileRepository()
        val result = EvaluateBadgesUseCase(badgeRepo, profileRepo)
            .invoke(makeSession(distractionCount = 0, status = SessionStatus.PARTIAL))
        assertTrue(result.none { it.id == BadgeId.DISTRACTION_FREE.name })
    }

    @Test
    fun `CENTURY awarded when totalSessionsCompleted is 100`() = runTest {
        val badgeRepo = FakeBadgeRepository()
        val profileRepo = FakeUserProfileRepository(totalSessionsCompleted = 100)
        val result = EvaluateBadgesUseCase(badgeRepo, profileRepo).invoke(makeSession())
        assertTrue(result.any { it.id == BadgeId.CENTURY.name })
    }

    @Test
    fun `no badges when no conditions met`() = runTest {
        val badgeRepo = FakeBadgeRepository()
        val profileRepo = FakeUserProfileRepository(
            totalSessionsCompleted = 0,
            currentStreak = 0,
        )
        val result = EvaluateBadgesUseCase(badgeRepo, profileRepo)
            .invoke(makeSession(actualDuration = 100, distractionCount = 1))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `multiple badges awarded in one session`() = runTest {
        val badgeRepo = FakeBadgeRepository()
        val profileRepo = FakeUserProfileRepository(
            totalSessionsCompleted = 1,
            currentStreak = 7,
        )
        val result = EvaluateBadgesUseCase(badgeRepo, profileRepo)
            .invoke(makeSession(distractionCount = 0, status = SessionStatus.COMPLETED))
        assertTrue(result.size >= 3)  // FIRST_FOCUS + WEEK_WARRIOR + DISTRACTION_FREE
    }
}
