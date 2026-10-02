package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
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
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.downloader.CosmoDownloadManager
import com.example.ui.theme.CosmoBackgroundDark
import com.example.ui.theme.CosmoCyan
import com.example.ui.theme.CosmoPurple
import kotlinx.coroutines.launch

private const val COSMO_STORE_URL = "https://cosmo-game.pages.dev/"

class CosmoNativeDownloadBridge(
    private val onDownloadRequested: (url: String, title: String?) -> Unit
) {
    @JavascriptInterface
    fun download(url: String, title: String?) {
        onDownloadRequested(url, title)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    isVisible: Boolean,
    onNavigateToDownloads: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val downloadManager = remember { CosmoDownloadManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableFloatStateOf(0f) }
    var isPageLoading by remember { mutableStateOf(true) }
    var pageError by remember { mutableStateOf<String?>(null) }

    fun triggerNativeDownload(url: String, title: String? = null) {
        val downloadId = downloadManager.startDownload(url = url, suggestedTitle = title)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Started download: ${title ?: url.substringBefore("?").substringAfterLast("/")}",
                actionLabel = "View",
                withDismissAction = true
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                onNavigateToDownloads()
            }
        }
    }

    // Handle back button inside WebView only when visible
    BackHandler(enabled = isVisible && webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmoBackgroundDark)
            .testTag("browser_screen")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
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

                    // Add JavaScript bridge for download interception
                    addJavascriptInterface(
                        CosmoNativeDownloadBridge { url, title ->
                            post { triggerNativeDownload(url, title) }
                        },
                        "CosmoNativeBridge"
                    )

                    // Native DownloadListener
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
                            // Inject click listeners on all download links to guarantee native handover
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    document.addEventListener('click', function(e) {
                                        var target = e.target.closest('a');
                                        if (target && target.href) {
                                            var href = target.href;
                                            if (href.match(/\.(apk|xapk)($|\?)/i) || target.hasAttribute('download')) {
                                                e.preventDefault();
                                                e.stopPropagation();
                                                if (window.CosmoNativeBridge) {
                                                    window.CosmoNativeBridge.download(href, target.innerText || target.title || '');
                                                } else {
                                                    location.href = href;
                                                }
                                                return false;
                                            }
                                        }
                                    }, true);
                                })();
                                """.trimIndent(),
                                null
                            )
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val targetUrl = request?.url?.toString() ?: return false

                            // Detect .apk or .xapk
                            if (targetUrl.contains(".apk", ignoreCase = true) ||
                                targetUrl.contains(".xapk", ignoreCase = true) ||
                                targetUrl.contains("pub-", ignoreCase = true) && targetUrl.endsWith(".apk")
                            ) {
                                triggerNativeDownload(targetUrl)
                                return true
                            }

                            // Keep other http/https within WebView
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

                    loadUrl(COSMO_STORE_URL)
                    webViewInstance = this
                }
            },
            update = {
                webViewInstance = it
                it.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
            }
        )

        // Web Loading Progress Bar at top
        if (isPageLoading && webProgress < 1f) {
            LinearProgressIndicator(
                progress = { webProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = CosmoCyan,
                trackColor = Color.Transparent
            )
        }

        // Error Retry Overlay if website failed to load
        if (pageError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CosmoBackgroundDark)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CosmoCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Could not connect to Cosmo Store",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pageError ?: "Please check your network connection.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                pageError = null
                                webViewInstance?.loadUrl(COSMO_STORE_URL)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmoCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
