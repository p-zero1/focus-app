package com.focusapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusapp.domain.preferences.FocusPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Reads [FocusPreferences.onboardingComplete] once at app start to determine whether
 * the user should land on the Timer screen or the Onboarding flow.
 *
 * Exposes `null` while the DataStore read is in-flight so the caller can show
 * a loading placeholder and avoid a flash of the wrong screen.
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    focusPreferences: FocusPreferences,
) : ViewModel() {

    /** Null = still loading; non-null = route is determined. */
    val startDestination: StateFlow<String?> = focusPreferences.onboardingComplete
        .map { complete -> if (complete) Routes.TIMER else Routes.ONBOARDING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
