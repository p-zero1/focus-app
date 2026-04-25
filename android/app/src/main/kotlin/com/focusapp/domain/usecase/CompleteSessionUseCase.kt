package com.focusapp.domain.usecase

import com.focusapp.domain.model.Badge
import com.focusapp.domain.model.SessionStatus
import com.focusapp.domain.repository.SessionRepository
import com.focusapp.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * Finalises an active session: sets status to COMPLETED or PARTIAL, records
 * actual duration, computes XP and Focus Score, then atomically:
 * - Awards XP to the user profile
 * - Updates the daily streak
 * - Evaluates badge conditions
 * - Increments the total sessions-completed counter
 *
 * Returns any newly awarded [Badge]s so callers can trigger celebrations.
 */
class CompleteSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userProfileRepository: UserProfileRepository,
    private val awardXp: AwardXpUseCase,
    private val updateStreak: UpdateStreakUseCase,
    private val evaluateBadges: EvaluateBadgesUseCase,
    private val computeFocusScore: ComputeFocusScoreUseCase,
) {
    suspend operator fun invoke(
        sessionId: Long,
        actualDuration: Int,
        endTime: Long,
    ): List<Badge> {
        val session = sessionRepository.getSessionById(sessionId) ?: return emptyList()

        val status = if (actualDuration >= session.plannedDuration) {
            SessionStatus.COMPLETED
        } else {
            SessionStatus.PARTIAL
        }

        // Delegate XP computation to AwardXpUseCase (single source of truth for the formula)
        // We call it early so xp is available for the session row — the use case also
        // updates the profile internally, so we skip the standalone awardXp() call below.
        val xp = awardXp(actualDuration)
        val focusScore = computeFocusScore(
            completed = status == SessionStatus.COMPLETED,
            distractionTotalSeconds = session.distractionTotalSeconds,
            actualDurationSeconds = actualDuration,
        )  // delegates to ComputeFocusScoreUseCase — single source of truth for the formula

        val updatedSession = session.copy(
            status = status,
            actualDuration = actualDuration,
            endTime = endTime,
            xpAwarded = xp,
            focusScore = focusScore,
        )
        sessionRepository.updateSession(updatedSession)

        // Gamification pipeline — XP already awarded above; remaining steps run after persist
        userProfileRepository.incrementSessionsCompleted()
        updateStreak()
        val newBadges = evaluateBadges(updatedSession)

        return newBadges
    }

}
