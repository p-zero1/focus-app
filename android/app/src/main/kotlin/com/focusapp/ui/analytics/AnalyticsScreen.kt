package com.focusapp.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    onNavigateToTimer: () -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val activeData = if (uiState.viewMode == AnalyticsViewMode.WEEKLY) uiState.weeklyData
                     else uiState.dailyData

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text       = "Analytics",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            PillToggle(
                selectedMode = uiState.viewMode,
                onModeSelected = viewModel::onViewModeChanged,
            )
        }

        if (activeData.isEmpty()) {
            EmptyAnalyticsState(onNavigateToTimer = onNavigateToTimer)
        } else {
            FocusBarChart(
                summaries = activeData,
                onBarClick = {},
                modifier  = Modifier.fillMaxWidth(),
            )
        }

        FocusScoreCard(score = uiState.focusScore)
        BestTimeInsightCard(bestTimeLabel = uiState.bestTimeInsight)

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PillToggle(
    selectedMode: AnalyticsViewMode,
    onModeSelected: (AnalyticsViewMode) -> Unit,
) {
    val pillShape = RoundedCornerShape(50.dp)
    Row(
        modifier = Modifier
            .clip(pillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        listOf(AnalyticsViewMode.WEEKLY to "This Week", AnalyticsViewMode.DAILY to "Last 7 Days").forEach { (mode, label) ->
            val selected = selectedMode == mode
            TextButton(
                onClick = { onModeSelected(mode) },
                modifier = Modifier
                    .clip(pillShape)
                    .background(if (selected) Color(0xFF6C63FF) else Color.Transparent),
            ) {
                Text(
                    text  = label,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun EmptyAnalyticsState(onNavigateToTimer: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text  = "No sessions yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text      = "Complete your first focus session to see your analytics here.",
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToTimer) { Text("Start a session") }
    }
}
