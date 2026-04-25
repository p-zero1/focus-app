package com.focusapp.domain.model

data class UserProfile(
    val totalXp: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val streakLastUpdatedDate: String,
    val totalSessionsCompleted: Int,
    val isPremium: Boolean,
    val dailyFocusGoalMinutes: Int,
) {
    /** XP level — increases every 500 XP. */
    val level: Int get() = (totalXp / 500) + 1

    /** XP progress within the current level (0–499). */
    val xpInCurrentLevel: Int get() = totalXp % 500
}
