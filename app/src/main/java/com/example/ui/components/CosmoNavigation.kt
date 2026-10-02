package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CosmoCardBorder
import com.example.ui.theme.CosmoCyan
import com.example.ui.theme.CosmoSurfaceDark

enum class CosmoScreen(val title: String, val icon: ImageVector, val tag: String) {
    BROWSER("Browser", Icons.Default.Language, "nav_browser"),
    LIBRARY("My Library", Icons.Default.SportsEsports, "nav_library"),
    FOR_YOU("For You", Icons.Default.AutoAwesome, "nav_for_you"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings"),
    DOWNLOADS("Downloads", Icons.Default.Language, "nav_downloads")
}

@Composable
fun CosmoTopAppBar(
    currentScreen: CosmoScreen,
    onBackClicked: (() -> Unit)? = null,
    onRefreshClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(CosmoSurfaceDark)
            .border(width = 0.5.dp, color = CosmoCardBorder)
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
                    text = "Cosmo Game Store",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CosmoCyan,
                        fontSize = 16.sp
                    )
                )
                Text(
                    text = when (currentScreen) {
                        CosmoScreen.BROWSER -> "Store Browser"
                        CosmoScreen.LIBRARY -> "My Library"
                        CosmoScreen.FOR_YOU -> "For You"
                        CosmoScreen.SETTINGS -> "Settings"
                        CosmoScreen.DOWNLOADS -> "Download Manager"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Action icon: Web Refresh only when in browser
        if (onRefreshClicked != null && currentScreen == CosmoScreen.BROWSER) {
            IconButton(
                onClick = onRefreshClicked,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("top_bar_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Web Store",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = CosmoCardBorder)
            .testTag("bottom_navigation_bar"),
        containerColor = CosmoSurfaceDark,
        tonalElevation = 6.dp
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
                    selectedIconColor = CosmoCyan,
                    selectedTextColor = CosmoCyan,
                    indicatorColor = CosmoCyan.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(screen.tag)
            )
        }
    }
}
