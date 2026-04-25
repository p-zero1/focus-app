package com.focusapp.ui.onboarding

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusapp.domain.preferences.FocusPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage: Int = 0,
    val hasUsageStatsPermission: Boolean = false,
    val totalPages: Int = 3,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val focusPreferences: FocusPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        _uiState.value = _uiState.value.copy(
            hasUsageStatsPermission = checkUsageStatsPermission(),
        )
    }

    fun onNextPage() {
        val current = _uiState.value.currentPage
        if (current < _uiState.value.totalPages - 1) {
            _uiState.value = _uiState.value.copy(currentPage = current + 1)
        }
    }

    fun onPreviousPage() {
        val current = _uiState.value.currentPage
        if (current > 0) {
            _uiState.value = _uiState.value.copy(currentPage = current - 1)
        }
    }

    /** Called when the user taps "Finish" or "Skip" — marks onboarding complete in DataStore. */
    fun onComplete(onDone: () -> Unit) {
        viewModelScope.launch {
            // FocusPreferences is read-only; writes go to AppPreferences directly via Hilt binding.
            // We write through the concrete implementation cast at runtime.
            val prefs = focusPreferences as? com.focusapp.data.prefs.AppPreferences
            prefs?.setOnboardingComplete(true)
            Timber.d("Onboarding complete")
            onDone()
        }
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
