package com.focusapp.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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

/** Emoji icon for each badge. */
private fun BadgeId.emoji(): String = when (this) {
    BadgeId.FIRST_FOCUS -> "\uD83C\uDF1F"        // 🌟
    BadgeId.DEEP_DIVER -> "\uD83E\uDD3F"          // 🤿
    BadgeId.WEEK_WARRIOR -> "\uD83D\uDCAA"        // 💪
    BadgeId.DISTRACTION_FREE -> "\uD83E\uDDD8"    // 🧘
    BadgeId.CENTURY -> "\uD83C\uDFC6"             // 🏆
}

/**
 * Shows all 5 predefined badges as a 3-column grid.
 * Earned badges (present in [earnedBadges]) are full-colour;
 * unearned badges are greyed-out with a lock icon overlaid.
 */
@Composable
fun BadgeGallery(
    earnedBadges: List<Badge>,
    modifier: Modifier = Modifier,
) {
    val earnedIds = earnedBadges.map { it.id }.toSet()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
    val alpha = if (earned) 1f else 0.35f
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (earned)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .alpha(alpha)
            .semantics {
                contentDescription = if (earned)
                    "${badgeId.displayName} — earned"
                else
                    "${badgeId.displayName} — locked"
            },
    ) {
        Column(
            modifier = Modifier.size(100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (earned) badgeId.emoji() else "\uD83D\uDD12",  // 🔒
                fontSize = 28.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = badgeId.displayName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
