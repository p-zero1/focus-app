package com.focusapp.domain.usecase

import com.focusapp.domain.model.DistractionType
import com.focusapp.domain.repository.DistractionRepository
import javax.inject.Inject

class LogDistractionUseCase @Inject constructor(
    private val distractionRepository: DistractionRepository,
) {
    suspend operator fun invoke(
        sessionId: Long,
        timestampMs: Long,
        type: DistractionType,
        appPackageName: String?,
        awayDurationMs: Long,
    ) {
        val durationSeconds = (awayDurationMs / 1_000L).toInt().coerceAtLeast(1)
        distractionRepository.logDistraction(
            sessionId = sessionId,
            timestamp = timestampMs,
            type = type,
            appPackageName = appPackageName,
            durationSeconds = durationSeconds,
        )
    }
}
