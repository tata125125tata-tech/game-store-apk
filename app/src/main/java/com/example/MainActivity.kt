package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import com.example.downloader.DownloadNotificationHelper
import com.example.settings.SettingsManager
import com.example.ui.components.CosmoBottomNavigation
import com.example.ui.components.CosmoScreen
import com.example.ui.components.CosmoTopAppBar
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.ForYouScreen
import com.example.ui.screens.MyLibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val navDestination = mutableStateOf<String?>(null)
    private val deepLinkUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            val settingsManager = remember { SettingsManager.getInstance(this) }
            val themeMode by settingsManager.themeMode.collectAsState()

            MyApplicationTheme(themeMode = themeMode) {
                CosmoApp(
                    initialDestination = navDestination.value,
                    initialDeepLinkUrl = deepLinkUrl.value,
                    onDestinationHandled = { navDestination.value = null },
                    onDeepLinkHandled = { deepLinkUrl.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        // 1. Check Deep Link: https://cosmo-game.pages.dev/
        if (Intent.ACTION_VIEW == intent.action && intent.data != null) {
            val uri: Uri? = intent.data
            if (uri != null && uri.host?.contains("cosmo-game.pages.dev") == true) {
                deepLinkUrl.value = uri.toString()
                navDestination.value = "browser"
                return
            }
        }

        // 2. Check Notification Navigation
        val dest = intent.getStringExtra(DownloadNotificationHelper.EXTRA_DESTINATION)
        if (!dest.isNullOrBlank()) {
            navDestination.value = dest
        }
    }
}

@Composable
fun CosmoApp(
    initialDestination: String?,
    initialDeepLinkUrl: String?,
    onDestinationHandled: () -> Unit,
    onDeepLinkHandled: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf(CosmoScreen.BROWSER) }
    var previousScreen by remember { mutableStateOf(CosmoScreen.BROWSER) }
    var activeDeepLinkUrl by remember { mutableStateOf<String?>(null) }

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

    // Handle deep link URL
    LaunchedEffect(initialDeepLinkUrl) {
        if (!initialDeepLinkUrl.isNullOrBlank()) {
            activeDeepLinkUrl = initialDeepLinkUrl
            previousScreen = currentScreen
            currentScreen = CosmoScreen.BROWSER
            onDeepLinkHandled()
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
                "browser" -> {
                    previousScreen = currentScreen
                    currentScreen = CosmoScreen.BROWSER
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

    // System BackHandler
    BackHandler(enabled = currentScreen != CosmoScreen.BROWSER) {
        currentScreen = if (currentScreen == CosmoScreen.DOWNLOADS) previousScreen else CosmoScreen.BROWSER
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("cosmo_main_scaffold"),
        topBar = {
            // Note: CosmoTopAppBar internally returns empty if currentScreen == CosmoScreen.BROWSER
            // to eliminate duplicate browser/header bar
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
            // WebView state preservation: BrowserScreen is kept alive in background
            val isBrowser = currentScreen == CosmoScreen.BROWSER
            Box(
                modifier = if (isBrowser) Modifier.fillMaxSize() else Modifier.fillMaxSize().alpha(0f)
            ) {
                BrowserScreen(
                    isVisible = isBrowser,
                    deepLinkUrl = activeDeepLinkUrl,
                    onNavigateToDownloads = { navigateTo(CosmoScreen.DOWNLOADS) },
                    snackbarHostState = snackbarHostState
                )
            }

            // Smooth short fade/slide animation for native tab switching
            if (!isBrowser) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        val isForward = targetState.ordinal > initialState.ordinal
                        (fadeIn(animationSpec = tween(180)) + slideInHorizontally(
                            animationSpec = tween(180),
                            initialOffsetX = { if (isForward) it / 6 else -it / 6 }
                        )).togetherWith(
                            fadeOut(animationSpec = tween(140)) + slideOutHorizontally(
                                animationSpec = tween(140),
                                targetOffsetX = { if (isForward) -it / 6 else it / 6 }
                            )
                        )
                    },
                    label = "native_tab_transition"
                ) { targetScreen ->
                    when (targetScreen) {
                        CosmoScreen.BROWSER -> {
                            // Rendered by persistent Box above
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
    }
}
