package com.focusapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusapp.domain.model.Badge
import com.focusapp.domain.model.SessionStatus
import com.focusapp.domain.model.UserProfile
import com.focusapp.domain.repository.BadgeRepository
import com.focusapp.domain.repository.SessionRepository
import com.focusapp.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class ProfileUiState(
    val xp: Int = 0,
    val level: Int = 1,
    val xpInCurrentLevel: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalSessionsCompleted: Int = 0,
    val dailyGoalMinutes: Int = 60,
    val dailyMinutesToday: Int = 0,   // filled by a separate query if needed
    val badges: List<Badge> = emptyList(),
    val newBadge: Badge? = null,      // drives BadgeAwardedDialog
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val badgeRepository: BadgeRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        val zone = ZoneId.systemDefault()
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        combine(
            userProfileRepository.getProfile(),
            badgeRepository.getAllBadges(),
            sessionRepository.getSessionsByDateRange(todayStart, todayEnd),
        ) { profile: UserProfile, badges: List<Badge>, todaySessions ->
            val dailyMinutesToday = todaySessions
                .filter { it.status == SessionStatus.COMPLETED }
                .sumOf { it.actualDuration } / 60
            _uiState.value = _uiState.value.copy(
                xp = profile.totalXp,
                level = profile.level,
                xpInCurrentLevel = profile.xpInCurrentLevel,
                currentStreak = profile.currentStreak,
                longestStreak = profile.longestStreak,
                totalSessionsCompleted = profile.totalSessionsCompleted,
                dailyGoalMinutes = profile.dailyFocusGoalMinutes,
                dailyMinutesToday = dailyMinutesToday,
                badges = badges,
                isLoading = false,
            )
        }.launchIn(viewModelScope)
    }

    /** Called by [BadgeAwardedDialog] when the celebration overlay dismisses. */
    fun onBadgeDismissed() {
        _uiState.value = _uiState.value.copy(newBadge = null)
    }
}
