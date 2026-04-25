package com.focusapp.ui.analytics

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateToTimer: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Analytics") })
        },
        modifier = modifier,
    ) { innerPadding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val activeData = if (uiState.viewMode == AnalyticsViewMode.WEEKLY) uiState.weeklyData
                         else uiState.dailyData

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- View mode toggle ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.viewMode == AnalyticsViewMode.WEEKLY,
                    onClick = { viewModel.onViewModeChanged(AnalyticsViewMode.WEEKLY) },
                    label = { Text("This Week") },
                )
                FilterChip(
                    selected = uiState.viewMode == AnalyticsViewMode.DAILY,
                    onClick = { viewModel.onViewModeChanged(AnalyticsViewMode.DAILY) },
                    label = { Text("Last 7 Days") },
                )
            }

            // ---- Bar chart or empty state ----
            if (activeData.isEmpty()) {
                EmptyAnalyticsState(onNavigateToTimer = onNavigateToTimer)
            } else {
                FocusBarChart(
                    summaries = activeData,
                    onBarClick = { /* TODO: navigate to history filtered by date */ },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ---- Focus Score card ----
            FocusScoreCard(score = uiState.focusScore)

            // ---- Best time insight (hidden until 5+ sessions) ----
            BestTimeInsightCard(bestTimeLabel = uiState.bestTimeInsight)

            Spacer(modifier = Modifier.height(8.dp))
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
            text = "No sessions yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Complete your first focus session to see your analytics here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToTimer) {
            Text("Start a session")
        }
    }
}
