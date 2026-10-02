package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.example.downloader.CosmoDownloadManager
import com.example.downloader.DownloadNotificationHelper
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

    private val navDestination = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                CosmoApp(
                    initialDestination = navDestination.value,
                    onDestinationHandled = { navDestination.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val dest = intent?.getStringExtra(DownloadNotificationHelper.EXTRA_DESTINATION)
        if (!dest.isNullOrBlank()) {
            navDestination.value = dest
        }
    }
}

@Composable
fun CosmoApp(
    initialDestination: String?,
    onDestinationHandled: () -> Unit
) {
    val context = LocalContext.current
    val downloadManager = remember { CosmoDownloadManager.getInstance(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf(CosmoScreen.BROWSER) }
    var previousScreen by remember { mutableStateOf(CosmoScreen.BROWSER) }

    // Request notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Handle deep navigation from notifications
    LaunchedEffect(initialDestination) {
        if (initialDestination != null) {
            when (initialDestination) {
                DownloadNotificationHelper.DESTINATION_LIBRARY -> {
                    previousScreen = currentScreen
                    currentScreen = CosmoScreen.LIBRARY
                }
                DownloadNotificationHelper.DESTINATION_DOWNLOADS -> {
                    previousScreen = currentScreen
                    currentScreen = CosmoScreen.DOWNLOADS
                }
            }
            onDestinationHandled()
        }
    }

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
                onBackClicked = {
                    currentScreen = previousScreen
                }
            )
        },
        bottomBar = {
            CosmoBottomNavigation(
                currentScreen = if (currentScreen == CosmoScreen.DOWNLOADS) previousScreen else currentScreen,
                onTabSelected = { screen ->
                    navigateTo(screen)
                }
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
                        onNavigateToBrowser = { navigateTo(CosmoScreen.BROWSER) },
                        onNavigateToDownloads = { navigateTo(CosmoScreen.DOWNLOADS) }
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
                        onNavigateToLibrary = { navigateTo(CosmoScreen.LIBRARY) },
                        onBack = { currentScreen = previousScreen }
                    )
                }
            }
        }
    }
}
