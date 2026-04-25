package com.focusapp.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Circular gauge card displaying a Focus Score from 0–100.
 *
 * Colour gradient: red (0) → yellow (50) → green (100), interpolated linearly.
 */
@Composable
fun FocusScoreCard(
    score: Int,
    modifier: Modifier = Modifier,
    gaugeSize: Dp = 120.dp,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Focus Score",
                style = MaterialTheme.typography.titleMedium,
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(gaugeSize)
                    .semantics { contentDescription = "Focus score $score out of 100" },
            ) {
                ScoreArc(score = score, size = gaugeSize)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        fontSize = 28.sp,
                        color = scoreColor(score),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "/ 100",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = scoreLabel(score),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScoreArc(score: Int, size: Dp) {
    val sweepAngle = (score / 100f) * 270f  // 270° arc, starts at bottom-left
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val arcColor = scoreColor(score)

    Canvas(modifier = Modifier.size(size)) {
        val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
        val inset = 14.dp.toPx() / 2
        val arcSize = androidx.compose.ui.geometry.Size(
            width = size.toPx() - inset * 2,
            height = size.toPx() - inset * 2,
        )
        // Track (background arc)
        drawArc(
            color = trackColor,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = arcSize,
            style = stroke,
        )
        // Score arc
        if (score > 0) {
            drawArc(
                color = arcColor,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
        }
    }
}

private fun scoreColor(score: Int): Color {
    return when {
        score >= 70 -> Color(0xFF4CAF50) // green
        score >= 40 -> Color(0xFFFFC107) // amber
        else        -> Color(0xFFF44336) // red
    }
}

private fun scoreLabel(score: Int): String = when {
    score >= 80 -> "Excellent focus!"
    score >= 60 -> "Good focus"
    score >= 40 -> "Room to improve"
    else        -> "Keep going!"
}
