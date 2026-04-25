package com.focusapp.domain.usecase

import com.focusapp.domain.model.DailyFocusSummary
import javax.inject.Inject

/**
 * Computes a Focus Score (0–100) from a list of [DailyFocusSummary] records.
 *
 * Formula (from research.md §7):
 *   score = clamp(completionRatio × 70 + distractionFreeRatio × 30, 0, 100)
 *
 * Where:
 *   completionRatio    = completedSessions / startedSessions   (clamped to [0,1])
 *   distractionFreeRatio = 1 − (totalDistractionMinutes / totalMinutes)  (clamped to [0,1])
 *
 * Returns 100 when the list is empty (no data → perfect score as empty state default).
 */
class ComputeFocusScoreUseCase @Inject constructor() {

    operator fun invoke(summaries: List<DailyFocusSummary>): Int {
        if (summaries.isEmpty()) return 100

        val totalStarted = summaries.sumOf { it.startedSessions }
        val totalCompleted = summaries.sumOf { it.completedSessions }
        val totalMinutes = summaries.sumOf { it.totalMinutes }
        val totalDistractionMinutes = summaries.sumOf { it.totalDistractionMinutes }

        val completionRatio = if (totalStarted == 0) 1.0
                              else (totalCompleted.toDouble() / totalStarted).coerceIn(0.0, 1.0)

        val distractionFreeRatio = if (totalMinutes == 0) 1.0
                                   else (1.0 - totalDistractionMinutes.toDouble() / totalMinutes)
                                       .coerceIn(0.0, 1.0)

        return score(completionRatio, distractionFreeRatio)
    }

    /**
     * Per-session variant: takes raw session values instead of aggregated daily summaries.
     * Used by [CompleteSessionUseCase] to persist a focus score alongside the session row.
     *
     * Formula mirrors the aggregate variant:
     *   completedRatio = 1.0 if completed, else 0.0
     *   distractionFreeRatio = 1 − (distractionTotalSeconds / actualDurationSeconds)
     */
    operator fun invoke(
        completed: Boolean,
        distractionTotalSeconds: Int,
        actualDurationSeconds: Int,
    ): Int {
        if (actualDurationSeconds == 0) return 100
        val completedRatio = if (completed) 1.0 else 0.0
        val distractionFreeRatio =
            (1.0 - distractionTotalSeconds.toDouble() / actualDurationSeconds).coerceIn(0.0, 1.0)
        return score(completedRatio, distractionFreeRatio)
    }

    private fun score(completedRatio: Double, distractionFreeRatio: Double): Int =
        (completedRatio * 70.0 + distractionFreeRatio * 30.0).toInt().coerceIn(0, 100)
}
