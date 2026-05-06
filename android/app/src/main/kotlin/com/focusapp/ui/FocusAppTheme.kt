package com.focusapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FocusDarkColors = darkColorScheme(
    background           = Color(0xFF0F0F1A),
    onBackground         = Color(0xFFFFFFFF),
    surface              = Color(0xFF1A1A2E),
    surfaceVariant       = Color(0xFF23233D),
    onSurface            = Color(0xFFFFFFFF),
    onSurfaceVariant     = Color(0xFF9E9E9E),
    surfaceTint          = Color.Transparent,
    primary              = Color(0xFF6C63FF),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFF3A2F8F),
    onPrimaryContainer   = Color(0xFFC8C2FF),
    secondary            = Color(0xFFFFB347),
    onSecondary          = Color(0xFF1A1208),
    secondaryContainer   = Color(0xFF8F5A1F),
    onSecondaryContainer = Color(0xFFFFE5B0),
    tertiary             = Color(0xFF4CAF50),
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFF1F5F22),
    onTertiaryContainer  = Color(0xFFC5F0C8),
    error                = Color(0xFFFF6B6B),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFF8F2A2A),
    onErrorContainer     = Color(0xFFFFC8C8),
    outline              = Color(0xFF5C5C70),
    outlineVariant       = Color(0x1EFFFFFF),
    scrim                = Color(0xFF000000),
)

@Composable
fun FocusAppTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FocusDarkColors,
        content = content,
    )
}
