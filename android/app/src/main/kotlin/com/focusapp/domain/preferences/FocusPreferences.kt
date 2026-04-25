package com.focusapp.domain.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer interface for user-configurable focus session preferences.
 *
 * Implemented by [com.focusapp.data.prefs.AppPreferences] in the data layer.
 * This interface keeps the domain layer free of Android DataStore dependencies
 * (Constitution Principle I — no cross-layer imports downward from UI to data).
 */
interface FocusPreferences {
    val pomodoroFocusMinutes: Flow<Int>
    val pomodoroShortBreakMinutes: Flow<Int>
    val pomodoroLongBreakMinutes: Flow<Int>
    val pomodoroIntervalsBeforeLong: Flow<Int>
    val usageStatsPermissionAsked: Flow<Boolean>
    val notificationPermissionAsked: Flow<Boolean>
    val onboardingComplete: Flow<Boolean>
    val theme: Flow<String>
}
