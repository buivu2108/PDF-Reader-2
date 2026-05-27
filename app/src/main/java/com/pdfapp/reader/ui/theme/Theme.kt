package com.pdfapp.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.pdfapp.reader.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = AccentContainer,
    onPrimaryContainer = AccentDark,
    secondary = Accent,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = ErrorColor
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentLight,
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = AccentDark,
    onPrimaryContainer = AccentLight,
    secondary = AccentLight,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = ErrorColor
)

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        content = content
    )
}

@Composable
fun getDynamicItemColor(seed: String): androidx.compose.ui.graphics.Color {
    val colors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.errorContainer
    )
    return colors[Math.abs(seed.hashCode()) % colors.size]
}
