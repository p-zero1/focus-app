package com.focusapp.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusapp.domain.model.Badge
import com.focusapp.domain.model.BadgeId

private fun BadgeId.emoji(): String = when (this) {
    BadgeId.FIRST_FOCUS      -> "🌟"
    BadgeId.DEEP_DIVER       -> "🤿"
    BadgeId.WEEK_WARRIOR     -> "💪"
    BadgeId.DISTRACTION_FREE -> "🧘"
    BadgeId.CENTURY          -> "🏆"
}

@Composable
fun BadgeGallery(
    earnedBadges: List<Badge>,
    modifier: Modifier = Modifier,
) {
    val earnedIds = earnedBadges.map { it.id }.toSet()

    LazyVerticalGrid(
        columns             = GridCells.Fixed(2),
        modifier            = modifier,
        contentPadding      = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(BadgeId.entries) { badgeId ->
            val earned = badgeId.name in earnedIds
            BadgeItem(badgeId = badgeId, earned = earned)
        }
    }
}

@Composable
private fun BadgeItem(
    badgeId: BadgeId,
    earned: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (earned)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (earned) 1f else 0.35f)
            .semantics {
                contentDescription = if (earned)
                    "${badgeId.displayName} — earned"
                else
                    "${badgeId.displayName} — locked"
            },
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text     = if (earned) badgeId.emoji() else "🔒",
                fontSize = 36.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = badgeId.displayName,
                style     = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (earned) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text      = badgeId.description,
                    style     = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
