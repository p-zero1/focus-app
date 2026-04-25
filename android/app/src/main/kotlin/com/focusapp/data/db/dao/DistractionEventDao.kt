package com.focusapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusapp.data.db.entity.DistractionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DistractionEventDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: DistractionEventEntity): Long

    @Query("SELECT * FROM distraction_events WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun getBySessionId(sessionId: Long): Flow<List<DistractionEventEntity>>

    @Query("SELECT * FROM distraction_events WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySessionIdOnce(sessionId: Long): List<DistractionEventEntity>

    @Query("DELETE FROM distraction_events WHERE session_id = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)
}
