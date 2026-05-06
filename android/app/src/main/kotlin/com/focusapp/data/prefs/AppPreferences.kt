package com.focusapp.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focusapp.domain.preferences.FocusPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(private val context: Context) : FocusPreferences {

    // ---- Keys ----

    private object Keys {
        val POMODORO_FOCUS_MINUTES = intPreferencesKey("pomodoro_focus_minutes")
        val POMODORO_SHORT_BREAK_MINUTES = intPreferencesKey("pomodoro_short_break_minutes")
        val POMODORO_LONG_BREAK_MINUTES = intPreferencesKey("pomodoro_long_break_minutes")
        val POMODORO_INTERVALS_BEFORE_LONG = intPreferencesKey("pomodoro_intervals_before_long")
        val USAGE_STATS_PERMISSION_ASKED = booleanPreferencesKey("usage_stats_permission_asked")
        val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val THEME = stringPreferencesKey("theme")
    }

    // ---- Reads ----

    override val pomodoroFocusMinutes: Flow<Int> = context.dataStore.data
        .map { it[Keys.POMODORO_FOCUS_MINUTES] ?: 25 }

    override val pomodoroShortBreakMinutes: Flow<Int> = context.dataStore.data
        .map { it[Keys.POMODORO_SHORT_BREAK_MINUTES] ?: 5 }

    override val pomodoroLongBreakMinutes: Flow<Int> = context.dataStore.data
        .map { it[Keys.POMODORO_LONG_BREAK_MINUTES] ?: 15 }

    override val pomodoroIntervalsBeforeLong: Flow<Int> = context.dataStore.data
        .map { it[Keys.POMODORO_INTERVALS_BEFORE_LONG] ?: 4 }

    override val usageStatsPermissionAsked: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.USAGE_STATS_PERMISSION_ASKED] ?: false }

    override val notificationPermissionAsked: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.NOTIFICATION_PERMISSION_ASKED] ?: false }

    override val onboardingComplete: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    /** Theme name: DEFAULT | DARK | AMOLED */
    override val theme: Flow<String> = context.dataStore.data
        .map { it[Keys.THEME] ?: "DEFAULT" }

    // ---- Writes ----

    suspend fun setPomodoroFocusMinutes(value: Int) {
        context.dataStore.edit { it[Keys.POMODORO_FOCUS_MINUTES] = value }
    }

    suspend fun setPomodoroShortBreakMinutes(value: Int) {
        context.dataStore.edit { it[Keys.POMODORO_SHORT_BREAK_MINUTES] = value }
    }

    suspend fun setPomodoroLongBreakMinutes(value: Int) {
        context.dataStore.edit { it[Keys.POMODORO_LONG_BREAK_MINUTES] = value }
    }

    suspend fun setPomodoroIntervalsBeforeLong(value: Int) {
        context.dataStore.edit { it[Keys.POMODORO_INTERVALS_BEFORE_LONG] = value }
    }

    suspend fun setUsageStatsPermissionAsked(value: Boolean) {
        context.dataStore.edit { it[Keys.USAGE_STATS_PERMISSION_ASKED] = value }
    }

    suspend fun setNotificationPermissionAsked(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_PERMISSION_ASKED] = value }
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = value }
    }

    suspend fun setTheme(value: String) {
        context.dataStore.edit { it[Keys.THEME] = value }
    }
}
