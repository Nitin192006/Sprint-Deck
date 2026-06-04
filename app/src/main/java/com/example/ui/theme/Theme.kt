package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = PrimaryBlue,
    secondary = LightTextSecondary,
    onSecondary = LightBackground,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = ObsidianNavySecondary,
    onPrimary = ObsidianNavyBackground,
    primaryContainer = ObsidianNavyOutline,
    onPrimaryContainer = ObsidianNavyTextPrimary,
    secondary = ObsidianNavySecondary,
    onSecondary = ObsidianNavyBackground,
    secondaryContainer = ObsidianNavySurface,
    onSecondaryContainer = ObsidianNavyTextPrimary,
    background = ObsidianNavyBackground,
    onBackground = ObsidianNavyTextPrimary,
    surface = ObsidianNavySurface,
    onSurface = ObsidianNavyTextPrimary,
    surfaceVariant = ObsidianNavySurface,
    onSurfaceVariant = ObsidianNavyTextPrimary,
    outline = ObsidianNavyOutline
)

private val MintySerenityColorScheme = lightColorScheme(
    primary = MintyPrimary,
    onPrimary = Color.White,
    primaryContainer = MintySurface,
    onPrimaryContainer = MintyPrimary,
    secondary = MintySecondary,
    onSecondary = Color.White,
    secondaryContainer = MintyOutline,
    onSecondaryContainer = MintySecondary,
    background = MintyBackground,
    onBackground = MintySecondary,
    surface = MintySurface,
    onSurface = MintySecondary,
    surfaceVariant = MintySurface,
    onSurfaceVariant = MintySecondary,
    outline = MintyOutline
)

private val PastelSpringColorScheme = lightColorScheme(
    primary = PastelPrimary,
    onPrimary = Color.White,
    primaryContainer = PastelOutline,
    onPrimaryContainer = PastelPrimary,
    secondary = PastelSecondary,
    onSecondary = Color.White,
    secondaryContainer = PastelSurface,
    onSecondaryContainer = PastelPrimary,
    background = PastelBackground,
    onBackground = PastelPrimary,
    surface = PastelSurface,
    onSurface = PastelPrimary,
    surfaceVariant = PastelOutline,
    onSurfaceVariant = PastelPrimary,
    outline = PastelOutline
)

@Composable
fun MyApplicationTheme(
    themeIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeIndex) {
        1 -> DarkColorScheme
        2 -> MintySerenityColorScheme
        3 -> PastelSpringColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
