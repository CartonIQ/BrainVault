package com.cartoniq.brainvault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00E5FF)
val NeonPurple = Color(0xFFB388FF)
val SuccessGreen = Color(0xFF00E676)
val ErrorRed = Color(0xFFFF5370)
val CoinGold = Color(0xFFFFC400)

private val Dark = darkColorScheme(
    primary = NeonCyan, onPrimary = Color.Black,
    secondary = NeonPurple, onSecondary = Color.Black,
    tertiary = SuccessGreen, onTertiary = Color.Black,
    background = Color(0xFF0A0A0A), onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF15151B), onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF22222B), onSurfaceVariant = Color(0xFFA1A1AA),
    error = ErrorRed, outline = Color(0xFF3A3A46),
)

private val Light = lightColorScheme(
    primary = Color(0xFF006C7A), onPrimary = Color.White,
    secondary = Color(0xFF6B3FD1), onSecondary = Color.White,
    tertiary = Color(0xFF00793A), onTertiary = Color.White,
    background = Color(0xFFF6F6FA), onBackground = Color(0xFF111116),
    surface = Color.White, onSurface = Color(0xFF111116),
    surfaceVariant = Color(0xFFE6E6EE), onSurfaceVariant = Color(0xFF4A4A55),
    error = Color(0xFFB3261E), outline = Color(0xFFB8B8C4),
)

/** themeMode: 0 = system, 1 = dark, 2 = light */
@Composable
fun BrainVaultTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) { 1 -> true; 2 -> false; else -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
