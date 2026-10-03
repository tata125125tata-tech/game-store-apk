package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.settings.ThemeMode

private val CosmoDarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF26262E),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFD4D4D8),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = Color(0xFFE4E4E7),
    tertiary = CosmoCyan,
    onTertiary = Color.Black,
    background = CosmoBackgroundDark,
    onBackground = CosmoTextPrimary,
    surface = CosmoSurfaceDark,
    onSurface = CosmoTextPrimary,
    surfaceVariant = CosmoSurfaceVariantDark,
    onSurfaceVariant = CosmoTextSecondary,
    outline = CosmoCardBorder,
    error = CosmoRed
)

private val CosmoLightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E4E7),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF3F3F46),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4F4F5),
    onSecondaryContainer = Color(0xFF18181B),
    tertiary = CosmoBlue,
    onTertiary = Color.White,
    background = CosmoBackgroundLight,
    onBackground = CosmoTextPrimaryLight,
    surface = CosmoSurfaceLight,
    onSurface = CosmoTextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = CosmoTextSecondaryLight,
    outline = CosmoCardBorderLight,
    error = CosmoRed
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (isDark) CosmoDarkColorScheme else CosmoLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
