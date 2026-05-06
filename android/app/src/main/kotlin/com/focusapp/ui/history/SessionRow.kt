package com.focusapp.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.focusapp.domain.model.FocusSession
import com.focusapp.domain.model.SessionMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val rowShape = RoundedCornerShape(12.dp)
private val tagShape = RoundedCornerShape(50.dp)

@Composable
fun SessionRow(
    session: FocusSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Session on ${formatDate(session.startTime)}" }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Mode icon — 38 dp square card (F07: purple-tinted bg + icon)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x1A6C63FF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector    = session.mode.icon,
                contentDescription = session.mode.displayName,
                modifier       = Modifier.size(20.dp),
                tint           = Color(0xFFB5AFFF),
            )
        }

        // Main content (F06: date moves here below mode name)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text  = session.mode.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                )
                // Tag badge pill (F08: purple-tinted chip)
                session.tag?.let { tag ->
                    Text(
                        text  = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB5AFFF),
                        modifier = Modifier
                            .clip(tagShape)
                            .background(Color(0x1A6C63FF))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text  = formatDate(session.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (session.distractionCount > 0) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector    = Icons.Default.Warning,
                            contentDescription = null,
                            modifier       = Modifier.size(12.dp),
                            tint           = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text  = "${session.distractionCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        // Duration on right (F06: bold, prominent)
        Text(
            text       = formatDuration(session.actualDuration),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val SessionMode.icon: ImageVector
    get() = when (this) {
        SessionMode.POMODORO  -> Icons.Default.Timer
        SessionMode.CUSTOM    -> Icons.Default.Schedule
        SessionMode.STUDY     -> Icons.Default.Psychology
        SessionMode.DEEP_WORK -> Icons.Default.WorkspacePremium
    }

private val SessionMode.displayName: String
    get() = when (this) {
        SessionMode.POMODORO  -> "Pomodoro"
        SessionMode.CUSTOM    -> "Custom"
        SessionMode.STUDY     -> "Study"
        SessionMode.DEEP_WORK -> "Deep Work"
    }

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
