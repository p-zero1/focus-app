package com.focusapp.domain.usecase

import com.focusapp.domain.model.SessionConfig
import com.focusapp.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Inserts a new ACTIVE session row and returns its generated ID.
 *
 * Called by [com.focusapp.service.TimerService] when the user taps Start.
 */
class StartSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(config: SessionConfig, startTime: Long): Long =
        sessionRepository.startSession(config, startTime)
}
