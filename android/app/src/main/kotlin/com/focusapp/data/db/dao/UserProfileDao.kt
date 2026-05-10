package com.focusapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusapp.data.db.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    /**
     * Insert or replace the singleton profile row (id = 1).
     * Use this for the initial seed and for full-row updates.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    /** Reactive stream — UI observes this to stay in sync. */
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfileEntity?>

    /** One-shot read for use cases that don't need reactivity. */
    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileOnce(): UserProfileEntity?

    @Query("UPDATE user_profile SET total_xp = total_xp + :xp WHERE id = 1")
    suspend fun addXp(xp: Int)

    @Query(
        """
        UPDATE user_profile
        SET current_streak = :streak,
            longest_streak = MAX(longest_streak, :streak),
            streak_last_updated_date = :date
        WHERE id = 1
        """
    )
    suspend fun updateStreak(streak: Int, date: String)

    @Query("UPDATE user_profile SET total_sessions_completed = total_sessions_completed + 1 WHERE id = 1")
    suspend fun incrementSessionsCompleted()

    @Query("UPDATE user_profile SET daily_focus_goal_minutes = :minutes WHERE id = 1")
    suspend fun setDailyGoal(minutes: Int)
}
