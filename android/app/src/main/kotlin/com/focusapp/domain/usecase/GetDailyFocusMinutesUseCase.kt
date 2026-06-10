package com.focusapp.domain.usecase

import com.focusapp.domain.model.SessionStatus
import com.focusapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Returns a [Flow] of total completed focus minutes for today.
 *
 * Emits a new value whenever the session repository changes (e.g., after a session completes).
 * Uses today's local midnight as the start bound and tomorrow's midnight as the end bound.
 */
class GetDailyFocusMinutesUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<Int> {
        val zone = ZoneId.systemDefault()
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        return sessionRepository.getSessionsByDateRange(todayStart, todayEnd)
            .map { sessions ->
                sessions
                    .filter { it.status == SessionStatus.COMPLETED }
                    .sumOf { it.actualDuration } / 60
            }
    }
}
