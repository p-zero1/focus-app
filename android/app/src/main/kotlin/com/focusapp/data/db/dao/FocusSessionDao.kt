package com.focusapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.focusapp.data.db.entity.FocusSessionEntity
import com.focusapp.domain.model.DailyFocusSummary
import com.focusapp.domain.model.TagAggregate
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

    // ---- Analytics queries (T047) ----

    /**
     * Aggregates sessions by calendar day within [weekStartMs, weekEndMs).
     * Returns one [DailyFocusSummary] row per day that has at least one session.
     * Column names must match [DailyFocusSummary] field names exactly for Room mapping.
     */
    @Query(
        """
        SELECT
            date(start_time / 1000, 'unixepoch', 'localtime') AS date,
            SUM(actual_duration) / 60                          AS totalMinutes,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedSessions,
            COUNT(*)                                           AS startedSessions,
            SUM(distraction_total_seconds) / 60               AS totalDistractionMinutes
        FROM focus_sessions
        WHERE start_time >= :weekStartMs AND start_time < :weekEndMs
        GROUP BY date(start_time / 1000, 'unixepoch', 'localtime')
        ORDER BY date ASC
        """
    )
    fun getWeeklySessions(weekStartMs: Long, weekEndMs: Long): Flow<List<DailyFocusSummary>>

    /**
     * Aggregates sessions for a single calendar [date] (format "YYYY-MM-DD").
     * Returns null if no sessions exist for that day.
     */
    @Query(
        """
        SELECT
            date(start_time / 1000, 'unixepoch', 'localtime') AS date,
            SUM(actual_duration) / 60                          AS totalMinutes,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedSessions,
            COUNT(*)                                           AS startedSessions,
            SUM(distraction_total_seconds) / 60               AS totalDistractionMinutes
        FROM focus_sessions
        WHERE date(start_time / 1000, 'unixepoch', 'localtime') = :date
        """
    )
    suspend fun getDailyFocusMinutes(date: String): DailyFocusSummary?

    /**
     * Total number of sessions (any status) started on or after [sinceMs].
     * Used by [GetBestFocusTimeUseCase] to check the 5-session threshold.
     */
    @Query("SELECT COUNT(*) FROM focus_sessions WHERE start_time >= :sinceMs")
    suspend fun getSessionCountSince(sinceMs: Long): Int

    /**
     * Aggregates completed sessions by tag. Only non-null, non-empty tags are included.
     * Column names must match [TagAggregate] field names for Room mapping.
     */
    @Query(
        """
        SELECT
            tag,
            SUM(actual_duration) / 60 AS totalMinutes,
            COUNT(*)                  AS sessionCount
        FROM focus_sessions
        WHERE tag IS NOT NULL AND tag != ''
        GROUP BY tag
        ORDER BY totalMinutes DESC
        """
    )
    fun getTagAggregates(): Flow<List<TagAggregate>>
}
