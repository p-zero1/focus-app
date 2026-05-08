package com.focusapp.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val bannerShape        = RoundedCornerShape(16.dp)
private val bannerBg           = Color(0x2EFF6B6B)   // translucent red glass ~18% opacity
private val bannerBorder       = Color(0x40FF6B6B)   // red border, 25% opacity
private val bannerIconTint     = Color(0xFFFF6B6B)
private val bannerTextColor    = Color(0xFFFFC8C8)

/**
 * Slide-in banner shown when a distraction is detected.
 * Uses glass-morphism red card to match the design system.
 */
@Composable
fun DistractionWarningBanner(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    awaySeconds: Int = 0,
    appName: String? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            onClick         = onDismiss,
            color           = bannerBg,
            shape           = bannerShape,
            border          = BorderStroke(1.dp, bannerBorder),
            modifier        = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Icon(
                    imageVector    = Icons.Default.Warning,
                    contentDescription = null,
                    tint           = bannerIconTint,
                )
                Spacer(modifier = Modifier.width(8.dp))
                val text = when {
                    appName != null  -> "Switched to $appName — stay focused!"
                    awaySeconds > 0  -> "You were away for ${awaySeconds}s — stay on track!"
                    else             -> "You broke focus! Stay on track 💪"
                }
                Text(
                    text     = text,
                    color    = bannerTextColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
