package com.focusapp.domain.usecase

import com.focusapp.domain.model.SessionStatus
import com.focusapp.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Finalises an active session: sets status to COMPLETED or PARTIAL, records
 * actual duration, computes and stores XP and Focus Score.
 *
 * Called by [com.focusapp.service.TimerService] when the countdown finishes or
 * the user manually ends a session.
 */
class CompleteSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        sessionId: Long,
        actualDuration: Int,
        endTime: Long,
    ) {
        val session = sessionRepository.getSessionById(sessionId) ?: return

        val status = if (actualDuration >= session.plannedDuration) {
            SessionStatus.COMPLETED
        } else {
            SessionStatus.PARTIAL
        }

        val xp = computeXp(actualDuration)
        val focusScore = computeFocusScore(
            completed = status == SessionStatus.COMPLETED,
            distractionTotalSeconds = session.distractionTotalSeconds,
            actualDuration = actualDuration,
        )

        sessionRepository.updateSession(
            session.copy(
                status = status,
                actualDuration = actualDuration,
                endTime = endTime,
                xpAwarded = xp,
                focusScore = focusScore,
            )
        )
    }

    /** XP = floor(actualDuration / 300) × 10  (10 XP per 5 minutes) */
    private fun computeXp(actualDurationSeconds: Int): Int =
        (actualDurationSeconds / 300) * 10

    /**
     * Focus Score = clamp(completedRatio×70 + distractionFreeRatio×30, 0, 100)
     *
     * - completedRatio: 1.0 if completed, 0.0 if partial (simple per-session version)
     * - distractionFreeRatio: 1 − (distractionSeconds / actualDuration)
     */
    private fun computeFocusScore(
        completed: Boolean,
        distractionTotalSeconds: Int,
        actualDuration: Int,
    ): Int {
        if (actualDuration == 0) return 100
        val completedRatio = if (completed) 1.0 else 0.0
        val distractionRatio = (distractionTotalSeconds.toDouble() / actualDuration).coerceIn(0.0, 1.0)
        val distractionFreeRatio = 1.0 - distractionRatio
        val raw = completedRatio * 70 + distractionFreeRatio * 30
        return raw.toInt().coerceIn(0, 100)
    }
}
