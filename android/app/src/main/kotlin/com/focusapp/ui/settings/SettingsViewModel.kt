package com.focusapp.ui.settings

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusapp.data.prefs.AppPreferences
import com.focusapp.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SettingsUiState(
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroShortBreakMinutes: Int = 5,
    val pomodoroLongBreakMinutes: Int = 15,
    val dailyGoalMinutes: Int = 60,
    val hasUsageStatsPermission: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @SuppressLint("StaticFieldLeak")
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        combine(
            appPreferences.pomodoroFocusMinutes,
            appPreferences.pomodoroShortBreakMinutes,
            appPreferences.pomodoroLongBreakMinutes,
            userProfileRepository.getProfile(),
        ) { focusMin, shortBreak, longBreak, profile ->
            _uiState.value = SettingsUiState(
                pomodoroFocusMinutes = focusMin,
                pomodoroShortBreakMinutes = shortBreak,
                pomodoroLongBreakMinutes = longBreak,
                dailyGoalMinutes = profile.dailyFocusGoalMinutes,
                hasUsageStatsPermission = checkUsageStatsPermission(),
                isLoading = false,
            )
        }.launchIn(viewModelScope)
    }

    fun refreshPermissions() {
        _uiState.value = _uiState.value.copy(
            hasUsageStatsPermission = checkUsageStatsPermission(),
        )
    }

    fun setPomodoroFocusMinutes(minutes: Int) {
        viewModelScope.launch { appPreferences.setPomodoroFocusMinutes(minutes) }
    }

    fun setPomodoroShortBreakMinutes(minutes: Int) {
        viewModelScope.launch { appPreferences.setPomodoroShortBreakMinutes(minutes) }
    }

    fun setPomodoroLongBreakMinutes(minutes: Int) {
        viewModelScope.launch { appPreferences.setPomodoroLongBreakMinutes(minutes) }
    }

    fun setDailyGoalMinutes(minutes: Int) {
        viewModelScope.launch { userProfileRepository.setDailyGoal(minutes) }
    }

    private fun checkUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Timber.w(e, "Could not check usage stats permission")
            false
        }
    }
}
