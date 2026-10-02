package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CosmoCardBorder
import com.example.ui.theme.CosmoCyan
import com.example.ui.theme.CosmoPurple
import com.example.ui.theme.CosmoSurfaceDark

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
    activeDownloadsCount: Int,
    onDownloadsClicked: () -> Unit,
    onRefreshClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(CosmoSurfaceDark)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Brand & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(CosmoCyan, CosmoPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "COSMO",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = CosmoCyan
                    )
                )
                Text(
                    text = when (currentScreen) {
                        CosmoScreen.BROWSER -> "Store Browser"
                        CosmoScreen.LIBRARY -> "My Installed Games"
                        CosmoScreen.FOR_YOU -> "Curated For You"
                        CosmoScreen.SETTINGS -> "Settings & Storage"
                        CosmoScreen.DOWNLOADS -> "Download Manager"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Action Icons: Refresh + Download Manager Badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onRefreshClicked != null && currentScreen == CosmoScreen.BROWSER) {
                IconButton(
                    onClick = onRefreshClicked,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("top_bar_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Web Store",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Downloads Icon with Badge
            IconButton(
                onClick = onDownloadsClicked,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("top_bar_downloads_button")
            ) {
                BadgedBox(
                    badge = {
                        if (activeDownloadsCount > 0) {
                            Badge(
                                containerColor = CosmoCyan,
                                contentColor = Color.Black
                            ) {
                                Text(
                                    text = "$activeDownloadsCount",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloads Manager",
                        tint = if (currentScreen == CosmoScreen.DOWNLOADS) CosmoCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CosmoBottomNavigation(
    currentScreen: CosmoScreen,
    onTabSelected: (CosmoScreen) -> Unit,
    activeDownloadsCount: Int,
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
        tonalElevation = 8.dp
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
