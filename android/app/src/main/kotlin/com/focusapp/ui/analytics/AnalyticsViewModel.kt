package com.focusapp.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusapp.domain.model.DailyFocusSummary
import com.focusapp.domain.repository.SessionRepository
import com.focusapp.domain.usecase.ComputeFocusScoreUseCase
import com.focusapp.domain.usecase.GetBestFocusTimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class AnalyticsViewMode { DAILY, WEEKLY }

data class AnalyticsUiState(
    val viewMode: AnalyticsViewMode = AnalyticsViewMode.WEEKLY,
    val dailyData: List<DailyFocusSummary> = emptyList(),
    val weeklyData: List<DailyFocusSummary> = emptyList(),
    val focusScore: Int = 100,
    val bestTimeInsight: String? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val computeFocusScore: ComputeFocusScoreUseCase,
    private val getBestFocusTime: GetBestFocusTimeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadWeeklyData()
        loadBestTimeInsight()
    }

    fun onViewModeChanged(mode: AnalyticsViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
        if (mode == AnalyticsViewMode.DAILY) loadDailyData() else loadWeeklyData()
    }

    private fun loadWeeklyData() {
        viewModelScope.launch {
            val (weekStart, weekEnd) = currentWeekBounds()
            sessionRepository.getWeeklySessions(weekStart, weekEnd).collectLatest { summaries ->
                val score = computeFocusScore(summaries)
                _uiState.value = _uiState.value.copy(
                    weeklyData = summaries,
                    focusScore = score,
                    isLoading = false,
                )
            }
        }
    }

    private fun loadDailyData() {
        // Show the last 7 individual days so the bar chart has meaningful data
        viewModelScope.launch {
            val today = LocalDate.now()
            val sevenDaysAgo = today.minusDays(6)
            val startMs = sevenDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            sessionRepository.getWeeklySessions(startMs, endMs).collectLatest { summaries ->
                val score = computeFocusScore(summaries)
                _uiState.value = _uiState.value.copy(
                    dailyData = summaries,
                    focusScore = score,
                    isLoading = false,
                )
            }
        }
    }

    private fun loadBestTimeInsight() {
        viewModelScope.launch {
            // Use last 30 days of sessions for best-time computation
            val thirtyDaysAgo = LocalDate.now().minusDays(30)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = System.currentTimeMillis()
            sessionRepository.getSessionsByDateRange(thirtyDaysAgo, endMs).collectLatest { sessions ->
                val insight = getBestFocusTime(sessions)
                _uiState.value = _uiState.value.copy(bestTimeInsight = insight)
            }
        }
    }

    // ---- Helpers ----

    private fun currentWeekBounds(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusWeeks(1)
        val startMs = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = weekEnd.atStartOfDay(zone).toInstant().toEpochMilli()
        return startMs to endMs
    }
}
