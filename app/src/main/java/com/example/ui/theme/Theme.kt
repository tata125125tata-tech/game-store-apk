package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CosmoDarkColorScheme = darkColorScheme(
    primary = CosmoCyan,
    onPrimary = Color(0xFF031622),
    primaryContainer = Color(0xFF004D59),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = CosmoPurple,
    onSecondary = Color(0xFF2E1065),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = CosmoGreen,
    onTertiary = Color(0xFF064E3B),
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
    primary = CosmoBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = CosmoPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = CosmoGreen,
    onTertiary = Color.White,
    background = CosmoBackgroundLight,
    onBackground = CosmoTextPrimaryLight,
    surface = CosmoSurfaceLight,
    onSurface = CosmoTextPrimaryLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = CosmoTextSecondaryLight,
    outline = CosmoCardBorderLight,
    error = CosmoRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek cosmic dark theme
    dynamicColor: Boolean = false, // Keep brand cosmic colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CosmoDarkColorScheme else CosmoLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
