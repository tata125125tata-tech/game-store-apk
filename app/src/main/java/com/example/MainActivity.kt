package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.downloader.CosmoDownloadManager
import com.example.ui.components.CosmoBottomNavigation
import com.example.ui.components.CosmoScreen
import com.example.ui.components.CosmoTopAppBar
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.ForYouScreen
import com.example.ui.screens.MyLibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CosmoBackgroundDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CosmoApp()
            }
        }
    }
}

@Composable
fun CosmoApp() {
    val context = LocalContext.current
    val downloadManager = remember { CosmoDownloadManager.getInstance(context) }
    val activeDownloadsCount by downloadManager.activeDownloadsCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf(CosmoScreen.BROWSER) }
    var previousScreen by remember { mutableStateOf(CosmoScreen.BROWSER) }

    fun navigateTo(screen: CosmoScreen) {
        if (currentScreen != screen) {
            previousScreen = currentScreen
            currentScreen = screen
        }
    }

    // Handle back button when on sub-screens
    BackHandler(enabled = currentScreen != CosmoScreen.BROWSER) {
        currentScreen = if (currentScreen == CosmoScreen.DOWNLOADS) previousScreen else CosmoScreen.BROWSER
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmoBackgroundDark)
            .testTag("cosmo_main_scaffold"),
        topBar = {
            CosmoTopAppBar(
                currentScreen = currentScreen,
                activeDownloadsCount = activeDownloadsCount,
                onDownloadsClicked = {
                    if (currentScreen == CosmoScreen.DOWNLOADS) {
                        currentScreen = previousScreen
                    } else {
                        navigateTo(CosmoScreen.DOWNLOADS)
                    }
                }
            )
        },
        bottomBar = {
            CosmoBottomNavigation(
                currentScreen = if (currentScreen == CosmoScreen.DOWNLOADS) previousScreen else currentScreen,
                onTabSelected = { screen ->
                    navigateTo(screen)
                },
                activeDownloadsCount = activeDownloadsCount
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Keep Browser WebView preserved so page navigation and scroll state are never lost
            val isBrowser = currentScreen == CosmoScreen.BROWSER
            Box(
                modifier = if (isBrowser) Modifier.fillMaxSize() else Modifier.fillMaxSize().alpha(0f)
            ) {
                BrowserScreen(
                    isVisible = isBrowser,
                    onNavigateToDownloads = { navigateTo(CosmoScreen.DOWNLOADS) },
                    snackbarHostState = snackbarHostState
                )
            }

            // Native screens rendered when active
            when (currentScreen) {
                CosmoScreen.BROWSER -> {
                    // Browser is shown directly above
                }
                CosmoScreen.LIBRARY -> {
                    MyLibraryScreen(
                        onNavigateToBrowser = { navigateTo(CosmoScreen.BROWSER) }
                    )
                }
                CosmoScreen.FOR_YOU -> {
                    ForYouScreen(
                        onNavigateToDownloads = { navigateTo(CosmoScreen.DOWNLOADS) },
                        snackbarHostState = snackbarHostState
                    )
                }
                CosmoScreen.SETTINGS -> {
                    SettingsScreen()
                }
                CosmoScreen.DOWNLOADS -> {
                    DownloadsScreen(
                        onNavigateToBrowser = { navigateTo(CosmoScreen.BROWSER) },
                        onBack = { currentScreen = previousScreen }
                    )
                }
            }
        }
    }
}
