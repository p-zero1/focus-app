package com.focusapp.domain.usecase

import com.focusapp.domain.model.Badge
import com.focusapp.domain.model.SessionOutcome
import com.focusapp.domain.model.SessionStatus
import com.focusapp.domain.repository.SessionRepository
import com.focusapp.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * Finalises an active session: computes [SessionOutcome], sets status to COMPLETED or PARTIAL,
 * records actual duration, computes XP and Focus Score, then atomically:
 * - Awards XP to the user profile
 * - Updates the daily streak
 * - Evaluates badge conditions
 * - Increments the total sessions-completed counter
 *
 * Returns a [Pair] of (newly awarded badges, session outcome) so callers can
 * trigger celebrations and update the live timer state.
 *
 * @param forceFail When true (e.g. HARDCORE enforcement), outcome is always [SessionOutcome.FAILED]
 *                  regardless of distraction count.
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
        forceFail: Boolean = false,
    ): Pair<List<Badge>, SessionOutcome> {
        val session = sessionRepository.getSessionById(sessionId)
            ?: return Pair(emptyList(), SessionOutcome.FAILED)

        val status = if (actualDuration >= session.plannedDuration) {
            SessionStatus.COMPLETED
        } else {
            SessionStatus.PARTIAL
        }

        val outcome = when {
            forceFail || session.distractionCount > 3 -> SessionOutcome.FAILED
            session.distractionCount == 0             -> SessionOutcome.CLEAN
            else                                      -> SessionOutcome.INTERRUPTED
        }

        val xp = awardXp(actualDuration)
        val focusScore = computeFocusScore(
            completed = status == SessionStatus.COMPLETED,
            distractionTotalSeconds = session.distractionTotalSeconds,
            actualDurationSeconds = actualDuration,
        )

        val updatedSession = session.copy(
            status = status,
            actualDuration = actualDuration,
            endTime = endTime,
            xpAwarded = xp,
            focusScore = focusScore,
            sessionOutcome = outcome,
        )
        sessionRepository.updateSession(updatedSession)

        userProfileRepository.incrementSessionsCompleted()
        updateStreak()
        val newBadges = evaluateBadges(updatedSession)

        return Pair(newBadges, outcome)
    }
}
