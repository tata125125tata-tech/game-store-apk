package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.downloader.CosmoDownloadManager
import com.example.settings.SettingsManager
import com.example.ui.theme.CosmoCyan
import kotlinx.coroutines.launch

private const val COSMO_STORE_URL = "https://cosmo-game.pages.dev/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    isVisible: Boolean,
    deepLinkUrl: String? = null,
    onNavigateToDownloads: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val downloadManager = remember { CosmoDownloadManager.getInstance(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    val jsEnabled by settingsManager.javaScriptEnabled.collectAsState()
    val domStorageEnabled by settingsManager.domStorageEnabled.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableFloatStateOf(0f) }
    var isPageLoading by remember { mutableStateOf(true) }
    var pageError by remember { mutableStateOf<String?>(null) }

    fun triggerNativeDownload(url: String, title: String? = null) {
        val downloadId = downloadManager.startDownload(url = url, suggestedTitle = title)
        scope.launch {
            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle deep link when provided
    LaunchedEffect(deepLinkUrl) {
        if (!deepLinkUrl.isNullOrBlank() && webViewInstance != null) {
            webViewInstance?.loadUrl(deepLinkUrl)
        }
    }

    // Handle system back navigation within WebView
    BackHandler(enabled = isVisible && webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("browser_screen")
    ) {
        // Pure WebView without any duplicate header
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = jsEnabled
                        this.domStorageEnabled = domStorageEnabled
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        allowFileAccess = true
                        allowContentAccess = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        userAgentString = "$userAgentString CosmoGameStoreAndroid/1.0"
                    }

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    // Native DownloadListener catches all download links from website
                    setDownloadListener(DownloadListener { url, _, _, _, _ ->
                        triggerNativeDownload(url)
                    })

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            webProgress = newProgress / 100f
                            isPageLoading = newProgress < 100
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isPageLoading = true
                            pageError = null
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isPageLoading = false
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val targetUrl = request?.url?.toString() ?: return false

                            // Intercept APK / XAPK download URLs directly
                            if (targetUrl.contains(".apk", ignoreCase = true) ||
                                targetUrl.contains(".xapk", ignoreCase = true) ||
                                (targetUrl.contains("download", ignoreCase = true) && targetUrl.contains("file"))
                            ) {
                                triggerNativeDownload(targetUrl)
                                return true
                            }

                            // Allow normal website navigation & redirects within WebView
                            if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                                return false
                            }

                            return true
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            if (failingUrl == COSMO_STORE_URL) {
                                pageError = description ?: "Connection error"
                            }
                        }
                    }

                    loadUrl(deepLinkUrl ?: COSMO_STORE_URL)
                    webViewInstance = this
                }
            },
            update = { webView ->
                webView.settings.javaScriptEnabled = jsEnabled
                webView.settings.domStorageEnabled = domStorageEnabled
            },
            modifier = Modifier.fillMaxSize()
        )

        // Subtle Page Loading Indicator at the very top of WebView
        AnimatedVisibility(
            visible = isPageLoading && webProgress < 1f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                progress = { webProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = CosmoCyan,
                trackColor = Color.Transparent
            )
        }

        // Connection Error Card
        if (pageError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Connection Failed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pageError ?: "Unable to connect to Cosmo Game Store",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row {
                            Button(
                                onClick = {
                                    pageError = null
                                    webViewInstance?.reload()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Retry", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
