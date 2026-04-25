package com.focusapp.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.focusapp.domain.model.DailyFocusSummary

private val MAX_BAR_HEIGHT = 140.dp
private val MIN_BAR_HEIGHT = 4.dp
private val BAR_WIDTH = 36.dp

/**
 * Horizontal-scrollable bar chart visualising daily focus minutes.
 *
 * Bar height scales relative to the day with the most focus minutes.
 * Tapping a bar invokes [onBarClick] with the ISO date string of that day.
 */
@Composable
fun FocusBarChart(
    summaries: List<DailyFocusSummary>,
    onBarClick: (date: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxMinutes = summaries.maxOfOrNull { it.totalMinutes }?.coerceAtLeast(1) ?: 1

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(summaries) { summary ->
            BarColumn(
                summary = summary,
                maxMinutes = maxMinutes,
                onClick = { onBarClick(summary.date) },
            )
        }
    }
}

@Composable
private fun BarColumn(
    summary: DailyFocusSummary,
    maxMinutes: Int,
    onClick: () -> Unit,
) {
    val heightFraction = summary.totalMinutes.toFloat() / maxMinutes
    val barHeight = (MAX_BAR_HEIGHT * heightFraction).coerceAtLeast(MIN_BAR_HEIGHT)
    val shortDate = summary.date.takeLast(5) // "MM-DD"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .width(BAR_WIDTH)
            .height(MAX_BAR_HEIGHT + 28.dp)
            .semantics { contentDescription = "${summary.totalMinutes} min on ${summary.date}" }
            .clickable(onClick = onClick),
    ) {
        if (summary.totalMinutes > 0) {
            Text(
                text = "${summary.totalMinutes}m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        Box(
            modifier = Modifier
                .width(BAR_WIDTH)
                .height(barHeight)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(
                    if (summary.totalMinutes > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = shortDate,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
