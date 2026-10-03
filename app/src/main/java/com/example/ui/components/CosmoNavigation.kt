package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CosmoScreen(val title: String, val icon: ImageVector, val tag: String) {
    BROWSER("Browser", Icons.Default.Language, "nav_browser"),
    LIBRARY("My Library", Icons.Default.SportsEsports, "nav_library"),
    FOR_YOU("For You", Icons.Default.AutoAwesome, "nav_for_you"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings"),
    DOWNLOADS("Downloads", Icons.Default.Download, "nav_downloads")
}

@Composable
fun CosmoTopAppBar(
    currentScreen: CosmoScreen,
    onBackClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Only display top app bar on non-browser native screens
    if (currentScreen == CosmoScreen.BROWSER) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (onBackClicked != null && currentScreen == CosmoScreen.DOWNLOADS) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column {
                Text(
                    text = when (currentScreen) {
                        CosmoScreen.BROWSER -> ""
                        CosmoScreen.LIBRARY -> "My Library"
                        CosmoScreen.FOR_YOU -> "For You"
                        CosmoScreen.SETTINGS -> "Settings"
                        CosmoScreen.DOWNLOADS -> "Download Manager"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

@Composable
fun CosmoBottomNavigation(
    currentScreen: CosmoScreen,
    onTabSelected: (CosmoScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        CosmoScreen.BROWSER,
        CosmoScreen.LIBRARY,
        CosmoScreen.FOR_YOU,
        CosmoScreen.SETTINGS
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val selectedColor = if (isDark) Color.White else Color.Black
    val unselectedColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)
    val indicatorColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            .testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        navItems.forEach { screen ->
            val isSelected = currentScreen == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(screen) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedColor,
                    selectedTextColor = selectedColor,
                    indicatorColor = indicatorColor,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                ),
                modifier = Modifier.testTag(screen.tag)
            )
        }
    }
}
