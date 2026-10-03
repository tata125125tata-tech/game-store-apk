package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DownloadItem
import com.example.settings.SettingsManager
import com.example.settings.ThemeMode

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }

    // State flows
    val currentTheme by settingsManager.themeMode.collectAsState()
    val autoDeleteApk by settingsManager.autoDeleteApk.collectAsState()
    val autoRetry by settingsManager.autoRetry.collectAsState()
    val downloadNotifications by settingsManager.downloadNotificationsEnabled.collectAsState()
    val wifiOnly by settingsManager.downloadWifiOnly.collectAsState()
    val allowMobileData by settingsManager.allowMobileData.collectAsState()
    val jsEnabled by settingsManager.javaScriptEnabled.collectAsState()
    val domStorage by settingsManager.domStorageEnabled.collectAsState()
    val deepLinkEnabled by settingsManager.deepLinkEnabled.collectAsState()
    val autoOpenAfterInstall by settingsManager.autoOpenAfterInstall.collectAsState()

    var cacheBytes by remember { mutableLongStateOf(0L) }
    var downloadBytes by remember { mutableLongStateOf(0L) }
    var availableStorageBytes by remember { mutableLongStateOf(0L) }
    var showClearDownloadsDialog by remember { mutableStateOf(false) }

    fun refreshStorageMetrics() {
        cacheBytes = settingsManager.getAppCacheSize()
        downloadBytes = settingsManager.getDownloadedFilesSize()
        availableStorageBytes = settingsManager.getAvailableStorageBytes()
    }

    LaunchedEffect(Unit) {
        refreshStorageMetrics()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Section 1: Theme
        SettingsSectionCard(
            title = "Theme",
            icon = Icons.Default.BrightnessMedium
        ) {
            Text(
                text = "Select application appearance for native screens:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.values().forEach { mode ->
                    val isSelected = currentTheme == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { settingsManager.setThemeMode(mode) },
                        label = {
                            Text(
                                text = mode.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (mode) {
                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessMedium
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Section 2: Downloads
        SettingsSectionCard(
            title = "Downloads",
            icon = Icons.Default.Download
        ) {
            // Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Download Location",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = settingsManager.getDownloadDir().path,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto Retry
            SettingsSwitchRow(
                title = "Auto Retry on Disconnect",
                subtitle = "Automatically resume interrupted downloads when network recovers",
                checked = autoRetry,
                onCheckedChange = { settingsManager.setAutoRetry(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Auto Delete APK after installation
            SettingsSwitchRow(
                title = "Delete APK After Installation",
                subtitle = "Remove installer package once the game is successfully installed",
                checked = autoDeleteApk,
                onCheckedChange = { settingsManager.setAutoDeleteApk(it) }
            )
        }

        // Section 3: Network
        SettingsSectionCard(
            title = "Network",
            icon = Icons.Default.NetworkCheck
        ) {
            SettingsSwitchRow(
                title = "Download Over Wi-Fi Only",
                subtitle = "Prevent package downloads when using cellular data",
                checked = wifiOnly,
                onCheckedChange = { settingsManager.setDownloadWifiOnly(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSwitchRow(
                title = "Allow Mobile Data Usage",
                subtitle = "Permit store browsing and downloads on cellular connections",
                checked = allowMobileData,
                onCheckedChange = { settingsManager.setAllowMobileData(it) }
            )
        }

        // Section 4: Browser / WebView
        SettingsSectionCard(
            title = "Browser / WebView",
            icon = Icons.Default.Language
        ) {
            SettingsSwitchRow(
                title = "JavaScript",
                subtitle = "Required for Cosmo Game Store catalog navigation and search",
                checked = jsEnabled,
                onCheckedChange = { settingsManager.setJavaScriptEnabled(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSwitchRow(
                title = "DOM Storage",
                subtitle = "Enable HTML5 web application client-side database storage",
                checked = domStorage,
                onCheckedChange = { settingsManager.setDomStorageEnabled(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSwitchRow(
                title = "Deep-Link Handling",
                subtitle = "Open cosmo-game.pages.dev links directly inside the application",
                checked = deepLinkEnabled,
                onCheckedChange = { settingsManager.setDeepLinkEnabled(it) }
            )
        }

        // Section 5: Installation
        SettingsSectionCard(
            title = "Installation",
            icon = Icons.Default.InstallMobile
        ) {
            SettingsSwitchRow(
                title = "Auto-Open After Installation",
                subtitle = "Launch installed game automatically once package installation finishes",
                checked = autoOpenAfterInstall,
                onCheckedChange = { settingsManager.setAutoOpenAfterInstall(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Open system settings manually", Toast.LENGTH_SHORT).show()
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unknown App Installation Permission",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Manage system permission to install downloaded APK/XAPK packages",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Section 6: Storage
        SettingsSectionCard(
            title = "Storage",
            icon = Icons.Default.Storage
        ) {
            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("App Cache", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = DownloadItem.formatFileSize(cacheBytes),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text("Downloads", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = DownloadItem.formatFileSize(downloadBytes),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text("Free Storage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = DownloadItem.formatFileSize(availableStorageBytes),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val freed = settingsManager.clearAppCache()
                        refreshStorageMetrics()
                        Toast.makeText(
                            context,
                            "App cache cleared (${DownloadItem.formatFileSize(freed)} freed)",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Cache", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { showClearDownloadsDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Clear Downloads", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Open System Storage Settings button
            Button(
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(fallback)
                        } catch (err: Exception) {
                            Toast.makeText(context, "Storage settings unavailable", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Android System Storage Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Section 7: Device
        SettingsSectionCard(
            title = "Device",
            icon = Icons.Default.PhoneAndroid
        ) {
            DeviceInfoRow(label = "Device Model", value = settingsManager.deviceModel)
            DeviceInfoRow(label = "Android Version", value = settingsManager.androidVersion)
            DeviceInfoRow(label = "CPU Architecture", value = settingsManager.deviceAbi)
        }

        // Section 8: Notifications
        SettingsSectionCard(
            title = "Notifications",
            icon = Icons.Default.Notifications
        ) {
            SettingsSwitchRow(
                title = "Download Progress Notifications",
                subtitle = "Display real-time download percentage, size, speed, stop and cancel actions",
                checked = downloadNotifications,
                onCheckedChange = { settingsManager.setDownloadNotificationsEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Clear downloads confirmation dialog
    if (showClearDownloadsDialog) {
        AlertDialog(
            onDismissRequest = { showClearDownloadsDialog = false },
            title = { Text("Clear Downloaded Packages", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all downloaded installer packages in storage? Installed games will not be affected.") },
            confirmButton = {
                Button(
                    onClick = {
                        settingsManager.clearDownloadedFiles()
                        refreshStorageMetrics()
                        showClearDownloadsDialog = false
                        Toast.makeText(context, "Downloaded files cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDownloadsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
