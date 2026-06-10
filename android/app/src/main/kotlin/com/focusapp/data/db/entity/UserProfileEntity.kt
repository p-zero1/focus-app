package com.focusapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Singleton row — always id = 1. Seeded on first access via upsert. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "total_xp")
    val totalXp: Int = 0,

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "longest_streak")
    val longestStreak: Int = 0,

    /** ISO date string YYYY-MM-DD of the last day a session was completed */
    @ColumnInfo(name = "streak_last_updated_date")
    val streakLastUpdatedDate: String = "",

    @ColumnInfo(name = "total_sessions_completed")
    val totalSessionsCompleted: Int = 0,

    @ColumnInfo(name = "is_premium")
    val isPremium: Boolean = false,

    @ColumnInfo(name = "daily_focus_goal_minutes")
    val dailyFocusGoalMinutes: Int = 60,
)
