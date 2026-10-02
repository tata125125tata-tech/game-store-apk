package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.installer.PackageInstallerManager
import com.example.model.DownloadItem
import com.example.settings.SettingsManager
import com.example.ui.theme.CosmoAmber
import com.example.ui.theme.CosmoBackgroundDark
import com.example.ui.theme.CosmoCardBorder
import com.example.ui.theme.CosmoCardDark
import com.example.ui.theme.CosmoCyan
import com.example.ui.theme.CosmoGreen
import com.example.ui.theme.CosmoPurple
import com.example.ui.theme.CosmoRed

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }

    val autoDelete by settingsManager.autoDeleteApk.collectAsState()
    val wifiOnly by settingsManager.downloadWifiOnly.collectAsState()
    val xapkAutoExtract by settingsManager.xapkAutoExtract.collectAsState()

    var canInstallApps by remember { mutableStateOf(PackageInstallerManager.canRequestPackageInstalls(context)) }
    var storageSizeBytes by remember { mutableLongStateOf(0L) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        canInstallApps = PackageInstallerManager.canRequestPackageInstalls(context)
        storageSizeBytes = settingsManager.getDownloadedFilesSize()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmoBackgroundDark)
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Installation & Security
            SettingsSectionHeader(title = "INSTALLATION & SECURITY")

            Card(
                colors = CardDefaults.cardColors(containerColor = CosmoCardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmoCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Install Unknown Apps Permission
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (canInstallApps) CosmoGreen.copy(alpha = 0.15f) else CosmoAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (canInstallApps) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (canInstallApps) CosmoGreen else CosmoAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Install Unknown Apps", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = if (canInstallApps) "Granted (Ready to install APK/XAPK)" else "Permission required to trigger installer",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        if (!canInstallApps) {
                            OutlinedButton(
                                onClick = {
                                    PackageInstallerManager.openUnknownSourcesSettings(context)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Grant", fontSize = 11.sp, color = CosmoCyan)
                            }
                        }
                    }

                    // Auto-delete APK toggle
                    SettingsToggleRow(
                        icon = Icons.Default.CleaningServices,
                        iconTint = CosmoCyan,
                        title = "Auto-Delete APK After Install",
                        subtitle = "Automatically delete downloaded installer file once game installation is verified",
                        checked = autoDelete,
                        onCheckedChange = { settingsManager.setAutoDeleteApk(it) }
                    )

                    // XAPK Extraction toggle
                    SettingsToggleRow(
                        icon = Icons.Default.Archive,
                        iconTint = CosmoPurple,
                        title = "XAPK Multi-Package Engine",
                        subtitle = "Parse split-APKs and stage expansions via Android PackageInstaller sessions",
                        checked = xapkAutoExtract,
                        onCheckedChange = { settingsManager.setXapkAutoExtract(it) }
                    )
                }
            }

            // Section 2: Storage & Network
            SettingsSectionHeader(title = "STORAGE & NETWORK")

            Card(
                colors = CardDefaults.cardColors(containerColor = CosmoCardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmoCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Storage used
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmoCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = CosmoCyan, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Downloaded Packages", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = "Using ${DownloadItem.formatFileSize(storageSizeBytes)} in app storage",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        if (storageSizeBytes > 0) {
                            OutlinedButton(
                                onClick = { showClearDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Clear", fontSize = 11.sp, color = CosmoRed)
                            }
                        }
                    }

                    // Download over Wi-Fi only
                    SettingsToggleRow(
                        icon = Icons.Default.Wifi,
                        iconTint = CosmoCyan,
                        title = "Download on Wi-Fi Only",
                        subtitle = "Conserve mobile data when downloading large game files",
                        checked = wifiOnly,
                        onCheckedChange = { settingsManager.setDownloadWifiOnly(it) }
                    )
                }
            }

            // Section 3: About Cosmo Game Store
            SettingsSectionHeader(title = "ABOUT COSMO GAME STORE")

            Card(
                colors = CardDefaults.cardColors(containerColor = CosmoCardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmoCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmoBackgroundDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = CosmoCyan,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text("Cosmo Game Store", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Version 1.0.0 (Official Build)", color = CosmoCyan, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Cosmo Game Store connects you to curated Android gaming experiences with official package installation, real HTTP downloads, XAPK multi-component support, and a local games launcher.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmoBackgroundDark, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = CosmoCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("https://cosmo-game.pages.dev/", fontSize = 12.sp, color = CosmoCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Clear Storage Dialog
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear Downloaded Packages?") },
                text = { Text("This will delete all cached APK and XAPK installer files (${DownloadItem.formatFileSize(storageSizeBytes)}). Installed games will not be affected.") },
                confirmButton = {
                    Button(
                        onClick = {
                            settingsManager.clearDownloadedFiles()
                            storageSizeBytes = settingsManager.getDownloadedFilesSize()
                            showClearDialog = false
                            Toast.makeText(context, "Storage cleared", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoRed)
                    ) {
                        Text("Clear All", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            color = CosmoCyan,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CosmoCyan,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = CosmoCardBorder
            )
        )
    }
}
