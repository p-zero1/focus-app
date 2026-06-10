package com.focusapp.data.repository

import androidx.room.withTransaction
import com.focusapp.data.db.FocusDatabase
import com.focusapp.data.db.dao.DistractionEventDao
import com.focusapp.data.db.dao.FocusSessionDao
import com.focusapp.data.db.entity.DistractionEventEntity
import com.focusapp.domain.model.DistractionEvent
import com.focusapp.domain.model.DistractionType
import com.focusapp.domain.repository.DistractionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DistractionRepositoryImpl @Inject constructor(
    private val db: FocusDatabase,
    private val distractionDao: DistractionEventDao,
    private val sessionDao: FocusSessionDao,
) : DistractionRepository {

    /**
     * Logs the distraction event and updates the parent session's denormalized counts
     * in a single transaction so they never diverge.
     */
    override suspend fun logDistraction(
        sessionId: Long,
        timestamp: Long,
        type: DistractionType,
        appPackageName: String?,
        durationSeconds: Int,
    ) {
        db.withTransaction {
            distractionDao.insert(
                DistractionEventEntity(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    type = type.name,
                    appPackageName = appPackageName,
                    durationSeconds = durationSeconds,
                )
            )
            // Update denormalized fields on the parent session
            val session = sessionDao.getById(sessionId) ?: return@withTransaction
            sessionDao.update(
                session.copy(
                    distractionCount = session.distractionCount + 1,
                    distractionTotalSeconds = session.distractionTotalSeconds + durationSeconds,
                )
            )
        }
    }

    override fun getBySessionId(sessionId: Long): Flow<List<DistractionEvent>> =
        distractionDao.getBySessionId(sessionId).map { list -> list.map { it.toDomain() } }

    override suspend fun getBySessionIdOnce(sessionId: Long): List<DistractionEvent> =
        distractionDao.getBySessionIdOnce(sessionId).map { it.toDomain() }

    // ---- Mapper ----

    private fun com.focusapp.data.db.entity.DistractionEventEntity.toDomain() = DistractionEvent(
        id = id,
        sessionId = sessionId,
        timestamp = timestamp,
        type = DistractionType.valueOf(type),
        appPackageName = appPackageName,
        durationSeconds = durationSeconds,
    )
}
