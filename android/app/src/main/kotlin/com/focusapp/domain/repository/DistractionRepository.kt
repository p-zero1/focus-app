package com.focusapp.domain.repository

import com.focusapp.domain.model.DistractionEvent
import com.focusapp.domain.model.DistractionType
import kotlinx.coroutines.flow.Flow

interface DistractionRepository {

    /**
     * Persist a completed distraction event and update the parent session's
     * denormalized [distractionCount] and [distractionTotalSeconds] fields atomically.
     */
    suspend fun logDistraction(
        sessionId: Long,
        timestamp: Long,
        type: DistractionType,
        appPackageName: String?,
        durationSeconds: Int,
    )

    fun getBySessionId(sessionId: Long): Flow<List<DistractionEvent>>

    suspend fun getBySessionIdOnce(sessionId: Long): List<DistractionEvent>
}
