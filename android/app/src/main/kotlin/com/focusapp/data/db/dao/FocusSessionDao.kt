package com.focusapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.focusapp.data.db.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: FocusSessionEntity): Long

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun getById(id: Long): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions ORDER BY start_time DESC")
    fun getAll(): Flow<List<FocusSessionEntity>>

    /**
     * Returns sessions whose start_time falls within [startMs, endMs).
     * Used by analytics to build daily/weekly summaries.
     */
    @Query(
        """
        SELECT * FROM focus_sessions
        WHERE start_time >= :startMs AND start_time < :endMs
        ORDER BY start_time ASC
        """
    )
    fun getByDateRange(startMs: Long, endMs: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE tag = :tag ORDER BY start_time DESC")
    fun getByTag(tag: String): Flow<List<FocusSessionEntity>>

    /** Count of sessions that started on or after [sinceMs]; used by badge evaluator. */
    @Query("SELECT COUNT(*) FROM focus_sessions WHERE start_time >= :sinceMs AND status = 'COMPLETED'")
    suspend fun getCompletedCountSince(sinceMs: Long): Int

    /** Total completed sessions ever; used by CENTURY badge. */
    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED'")
    suspend fun getTotalCompletedCount(): Int

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Returns the currently ACTIVE session if one exists. */
    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSession(): FocusSessionEntity?
}
