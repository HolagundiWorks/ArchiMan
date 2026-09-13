package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ArchiManDarkColorScheme = darkColorScheme(
    primary = Color(0xFF78A9FF),
    onPrimary = Color(0xFF002D9C),
    primaryContainer = Color(0xFF0043CE),
    onPrimaryContainer = Color(0xFFD0E2FF),
    secondary = Color(0xFFA6C8FF),
    onSecondary = Color(0xFF001141),
    secondaryContainer = Color(0xFF002D9C),
    onSecondaryContainer = Color(0xFFD0E2FF),
    tertiary = Color(0xFF42BE65),
    onTertiary = Color(0xFF002D12),
    tertiaryContainer = Color(0xFF0E6027),
    onTertiaryContainer = Color(0xFFA7F0BA),
    error = Color(0xFFFF8389),
    onError = Color(0xFF520408),
    errorContainer = Color(0xFF750E13),
    onErrorContainer = Color(0xFFFFD7D9),
    background = Color(0xFF101214),
    onBackground = Color(0xFFF2F4F8),
    surface = Color(0xFF101214),
    onSurface = Color(0xFFF2F4F8),
    surfaceVariant = Color(0xFF262A2E),
    onSurfaceVariant = Color(0xFFC1C7CD),
    surfaceContainerLowest = Color(0xFF080A0C),
    surfaceContainerLow = Color(0xFF15181B),
    surfaceContainer = Color(0xFF1D2125),
    surfaceContainerHigh = Color(0xFF272C31),
    surfaceContainerHighest = Color(0xFF30363C),
    outline = Color(0xFF8D96A0),
    outlineVariant = Color(0xFF424A52),
    inverseSurface = Color(0xFFF2F4F8),
    inverseOnSurface = Color(0xFF272C31),
    inversePrimary = Color(0xFF0F62FE),
    scrim = Color.Black
)

/** ArchiMan is intentionally dark and uses Material 3 semantic color roles. */
@Composable
fun ArchiManTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ArchiManDarkColorScheme, content = content)
}
