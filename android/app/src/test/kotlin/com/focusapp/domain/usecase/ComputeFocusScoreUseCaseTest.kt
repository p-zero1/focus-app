package com.focusapp.domain.usecase

import com.focusapp.domain.model.DailyFocusSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputeFocusScoreUseCaseTest {

    private val useCase = ComputeFocusScoreUseCase()

    @Test
    fun `empty list returns 100`() {
        assertEquals(100, useCase(emptyList()))
    }

    @Test
    fun `perfect sessions with no distractions return 100`() {
        val summaries = listOf(
            DailyFocusSummary("2026-04-21", totalMinutes = 60, completedSessions = 2,
                startedSessions = 2, totalDistractionMinutes = 0),
        )
        assertEquals(100, useCase(summaries))
    }

    @Test
    fun `all sessions incomplete returns score around 0`() {
        val summaries = listOf(
            DailyFocusSummary("2026-04-21", totalMinutes = 60, completedSessions = 0,
                startedSessions = 4, totalDistractionMinutes = 0),
        )
        // completionRatio=0, distractionFreeRatio=1 → 0×70 + 1×30 = 30
        assertEquals(30, useCase(summaries))
    }

    @Test
    fun `half sessions completed with no distractions returns 65`() {
        val summaries = listOf(
            DailyFocusSummary("2026-04-21", totalMinutes = 60, completedSessions = 2,
                startedSessions = 4, totalDistractionMinutes = 0),
        )
        // 0.5×70 + 1.0×30 = 35 + 30 = 65
        assertEquals(65, useCase(summaries))
    }

    @Test
    fun `high distraction rate reduces score`() {
        val summaries = listOf(
            DailyFocusSummary("2026-04-21", totalMinutes = 60, completedSessions = 4,
                startedSessions = 4, totalDistractionMinutes = 30),
        )
        // completionRatio=1.0, distractionFreeRatio=0.5 → 70 + 15 = 85
        assertEquals(85, useCase(summaries))
    }

    @Test
    fun `score is clamped to 0 at minimum`() {
        // Pathological case: more distraction minutes than focus minutes (data anomaly)
        val summaries = listOf(
            DailyFocusSummary("2026-04-21", totalMinutes = 10, completedSessions = 0,
                startedSessions = 4, totalDistractionMinutes = 20),
        )
        val score = useCase(summaries)
        assertTrue("Score must be >= 0, got $score", score >= 0)
    }

    @Test
    fun `score is clamped to 100 at maximum`() {
        val summaries = listOf(
            DailyFocusSummary("2026-04-21", totalMinutes = 120, completedSessions = 5,
                startedSessions = 5, totalDistractionMinutes = 0),
        )
        val score = useCase(summaries)
        assertTrue("Score must be <= 100, got $score", score <= 100)
    }

    @Test
    fun `aggregates across multiple days correctly`() {
        val summaries = listOf(
            DailyFocusSummary("2026-04-21", totalMinutes = 60, completedSessions = 2,
                startedSessions = 2, totalDistractionMinutes = 0),
            DailyFocusSummary("2026-04-22", totalMinutes = 60, completedSessions = 0,
                startedSessions = 2, totalDistractionMinutes = 0),
        )
        // totalCompleted=2, totalStarted=4 → completionRatio=0.5
        // totalDistractionMinutes=0 → distractionFreeRatio=1.0
        // 0.5×70 + 1.0×30 = 65
        assertEquals(65, useCase(summaries))
    }
}
