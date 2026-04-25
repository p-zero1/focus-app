package com.focusapp.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays the current streak as a large flame emoji + number.
 * Colour shifts to orange at 7-day milestone (vs default onSurface).
 */
@Composable
fun StreakCounter(
    streak: Int,
    modifier: Modifier = Modifier,
) {
    val streakColor = if (streak >= 7) Color(0xFFFF6D00) else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier.semantics {
            contentDescription = "$streak day streak"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "\uD83D\uDD25",  // 🔥
            fontSize = 48.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$streak",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = streakColor,
        )
        Text(
            text = if (streak == 1) "day streak" else "day streak",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
