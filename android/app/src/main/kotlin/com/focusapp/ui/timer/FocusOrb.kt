package com.focusapp.ui.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.focusapp.domain.model.TimerStatus

private data class OrbPalette(
    val core: Color,
    val mid: Color,
    val deep: Color,
    val glow: Color,
)

private val focusPalette = OrbPalette(
    core = Color(0xFFC8C2FF),
    mid  = Color(0xFF8E85FF),
    deep = Color(0xFF3A2F8F),
    glow = Color(0x8C8E85FF),
)
private val breakPalette = OrbPalette(
    core = Color(0xFFA8E5D0),
    mid  = Color(0xFF4CAF92),
    deep = Color(0xFF1F5F4A),
    glow = Color(0x804CAF92),
)
private val distractPalette = OrbPalette(
    core = Color(0xFFFFC8C8),
    mid  = Color(0xFFFF6B6B),
    deep = Color(0xFF8F2A2A),
    glow = Color(0x80FF6B6B),
)
private val successPalette = OrbPalette(
    core = Color(0xFFC5F0C8),
    mid  = Color(0xFF4CAF50),
    deep = Color(0xFF1F5F22),
    glow = Color(0x804CAF50),
)

/**
 * Plasma orb — the visual centerpiece of the timer screen.
 *
 * Breathes continuously at 4 s period. Color shifts based on timer and distraction state.
 * Guide rings are static; only the sphere breathes.
 */
@Composable
fun FocusOrb(
    timerStatus: TimerStatus,
    isDistracting: Boolean,
    modifier: Modifier = Modifier,
) {
    val target = when {
        isDistracting                        -> distractPalette
        timerStatus == TimerStatus.BREAK     -> breakPalette
        timerStatus == TimerStatus.FINISHED  -> successPalette
        else                                 -> focusPalette
    }

    val easeInOut = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    val coreColor by animateColorAsState(target.core, tween(800), label = "orb_core")
    val midColor  by animateColorAsState(target.mid,  tween(800), label = "orb_mid")
    val deepColor by animateColorAsState(target.deep, tween(800), label = "orb_deep")
    val glowColor by animateColorAsState(target.glow, tween(800), label = "orb_glow")

    val infiniteTransition = rememberInfiniteTransition(label = "orb_breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue  = 1.04f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4000, easing = easeInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orb_scale",
    )

    Canvas(modifier = modifier) {
        val cx   = size.width / 2f
        val cy   = size.height / 2f
        val maxR = size.minDimension / 2f
        val orbR = maxR * 0.62f * breathScale

        // Static guide rings (do not breathe)
        drawCircle(
            color  = Color(0x1E6C63FF),
            radius = maxR,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color  = Color(0x2E6C63FF),
            radius = maxR * 0.88f,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.dp.toPx()),
        )

        // Glow layers — soft halo around the sphere
        drawCircle(color = glowColor.copy(alpha = 0.07f), radius = orbR * 1.65f, center = Offset(cx, cy))
        drawCircle(color = glowColor.copy(alpha = 0.15f), radius = orbR * 1.30f, center = Offset(cx, cy))
        drawCircle(color = glowColor.copy(alpha = 0.28f), radius = orbR * 1.10f, center = Offset(cx, cy))

        // Sphere — radial gradient with off-center highlight for 3-D illusion (CSS: at 38% 32%)
        val gradCx = cx - orbR * 0.24f
        val gradCy = cy - orbR * 0.36f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f    to coreColor,
                    0.55f to midColor,
                    1f    to deepColor,
                ),
                center = Offset(gradCx, gradCy),
                radius = orbR * 1.9f,
            ),
            radius = orbR,
            center = Offset(cx, cy),
        )

        // Subtle inner highlight
        drawCircle(
            color  = Color.White.copy(alpha = 0.12f),
            radius = orbR * 0.42f,
            center = Offset(cx - orbR * 0.18f, cy - orbR * 0.22f),
        )
    }
}
