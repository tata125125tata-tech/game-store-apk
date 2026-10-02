package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.downloader.CosmoDownloadManager
import com.example.installer.PackageInstallerManager
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import com.example.model.XapkInfo
import com.example.ui.theme.CosmoAmber
import com.example.ui.theme.CosmoBackgroundDark
import com.example.ui.theme.CosmoCardBorder
import com.example.ui.theme.CosmoCardDark
import com.example.ui.theme.CosmoCyan
import com.example.ui.theme.CosmoGreen
import com.example.ui.theme.CosmoPurple
import com.example.ui.theme.CosmoRed
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateToBrowser: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val downloadManager = remember { CosmoDownloadManager.getInstance(context) }
    val downloads by downloadManager.downloads.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var inspectingXapk by remember { mutableStateOf<Pair<DownloadItem, XapkInfo?>?>(null) }
    var installingXapkStatus by remember { mutableStateOf<String?>(null) }
    var itemToDelete by remember { mutableStateOf<DownloadItem?>(null) }

    BackHandler { onBack() }

    val filteredDownloads = remember(downloads, selectedFilter) {
        when (selectedFilter) {
            "DOWNLOADING" -> downloads.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING || it.status == DownloadStatus.PAUSED }
            "COMPLETED" -> downloads.filter { it.status == DownloadStatus.COMPLETED }
            else -> downloads
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmoBackgroundDark)
            .testTag("downloads_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter Header Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "All (${downloads.size})",
                    "DOWNLOADING" to "Active (${downloads.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }})",
                    "COMPLETED" to "Completed (${downloads.count { it.status == DownloadStatus.COMPLETED }})"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CosmoCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CosmoCyan,
                            containerColor = CosmoCardDark,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selectedFilter == key) CosmoCyan else CosmoCardBorder,
                            selectedBorderColor = CosmoCyan,
                            enabled = true,
                            selected = selectedFilter == key
                        )
                    )
                }
            }

            if (filteredDownloads.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CosmoCyan.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = CosmoCyan,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Downloads Found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap on any game download link in the Cosmo Web Store to start downloading APKs or XAPKs directly.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToBrowser,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmoCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Browse Cosmo Store", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDownloads, key = { it.id }) { item ->
                        DownloadItemCard(
                            item = item,
                            onPause = { downloadManager.pauseDownload(item.id) },
                            onResume = { downloadManager.resumeDownload(item.id) },
                            onCancel = { itemToDelete = item },
                            onInstall = {
                                val file = File(item.localFilePath)
                                if (!file.exists()) {
                                    Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
                                    return@DownloadItemCard
                                }

                                if (item.fileType == FileType.XAPK) {
                                    scope.launch {
                                        installingXapkStatus = "Preparing XAPK..."
                                        PackageInstallerManager.installXapk(
                                            context = context,
                                            xapkFile = file,
                                            onProgress = { status -> installingXapkStatus = status }
                                        ).fold(
                                            onSuccess = {
                                                installingXapkStatus = null
                                                Toast.makeText(context, "XAPK sent to official installer", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = { err ->
                                                installingXapkStatus = null
                                                Toast.makeText(context, "Install failed: ${err.message}", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                } else {
                                    PackageInstallerManager.installApk(context, file).fold(
                                        onSuccess = {
                                            Toast.makeText(context, "Opening official installer...", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { err ->
                                            Toast.makeText(context, "Failed: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            onOpen = {
                                val launched = PackageInstallerManager.openApp(context, item.packageName)
                                if (!launched) {
                                    Toast.makeText(context, "Could not open app", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onInspectXapk = {
                                scope.launch {
                                    val info = PackageInstallerManager.parseXapk(File(item.localFilePath)).getOrNull()
                                    inspectingXapk = item to info
                                }
                            },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }

        // XAPK Installation Progress Dialog
        if (installingXapkStatus != null) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Installing XAPK", fontWeight = FontWeight.Bold) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CosmoCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = installingXapkStatus ?: "Processing...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = { }
            )
        }

        // Delete confirmation dialog
        if (itemToDelete != null) {
            val item = itemToDelete!!
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Delete Download?") },
                text = { Text("This will permanently remove ${item.title} and its downloaded package from storage.") },
                confirmButton = {
                    Button(
                        onClick = {
                            downloadManager.deleteDownload(item.id)
                            itemToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoRed)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // XAPK Info Modal BottomSheet
        if (inspectingXapk != null) {
            val (item, info) = inspectingXapk!!
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { inspectingXapk = null },
                sheetState = sheetState,
                containerColor = CosmoCardDark,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmoPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, tint = CosmoPurple)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("XAPK Package Analysis", color = CosmoPurple, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (info != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoRow("Package Name", info.packageName ?: "Embedded in APKs")
                            InfoRow("Version Name", info.versionName ?: "—")
                            InfoRow("Version Code", info.versionCode?.toString() ?: "—")
                            InfoRow("Min SDK", info.minSdkVersion?.toString() ?: "—")
                            InfoRow("Total APK Files", "${info.totalApks} APKs (Base + Splits)")
                            InfoRow("Total OBB Files", "${info.totalObbs} OBBs")
                            InfoRow("Uncompressed Size", DownloadItem.formatFileSize(info.totalUncompressedSize))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Archive Components:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        info.components.forEach { comp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CosmoBackgroundDark, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = comp.entryName.substringAfterLast("/"),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = DownloadItem.formatFileSize(comp.sizeBytes),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text("Could not parse XAPK details.")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { inspectingXapk = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmoCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
fun DownloadItemCard(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    onInspectXapk: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CosmoCardBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CosmoCardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Icon, Title, Status & FileType Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App or File Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (item.fileType == FileType.XAPK) CosmoPurple.copy(alpha = 0.2f)
                            else CosmoCyan.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.iconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.iconUrl,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (item.fileType == FileType.XAPK) Icons.Default.Archive else Icons.Default.InstallMobile,
                            contentDescription = null,
                            tint = if (item.fileType == FileType.XAPK) CosmoPurple else CosmoCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Type Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (item.fileType == FileType.XAPK) CosmoPurple.copy(alpha = 0.25f)
                                    else CosmoCyan.copy(alpha = 0.25f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (item.fileType == FileType.XAPK) "XAPK" else "APK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.fileType == FileType.XAPK) CosmoPurple else CosmoCyan
                            )
                        }

                        // Size or status info
                        Text(
                            text = when (item.status) {
                                DownloadStatus.DOWNLOADING -> "${item.formattedDownloaded} / ${item.formattedTotal}"
                                DownloadStatus.COMPLETED -> item.formattedTotal
                                DownloadStatus.PAUSED -> "Paused · ${item.formattedDownloaded}"
                                DownloadStatus.FAILED -> "Failed"
                                DownloadStatus.CANCELLED -> "Cancelled"
                                DownloadStatus.PENDING -> "Starting..."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Delete or Cancel icon
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Downloading Progress Section
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CosmoCyan,
                    trackColor = CosmoCardBorder
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.progressPercent}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmoCyan
                    )

                    if (item.formattedSpeed.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.formattedSpeed,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (item.status == DownloadStatus.DOWNLOADING) {
                            IconButton(onClick = onPause, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = CosmoAmber, modifier = Modifier.size(18.dp))
                            }
                        } else if (item.status == DownloadStatus.PAUSED) {
                            IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = CosmoGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = CosmoRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Error notice
            if (item.status == DownloadStatus.FAILED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmoRed.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CosmoRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.errorMessage ?: "Download encountered an error.",
                        color = CosmoRed,
                        fontSize = 11.sp,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onResume, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = CosmoCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Completed Actions: Install / Open / Inspect XAPK
            if (item.status == DownloadStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.fileType == FileType.XAPK) {
                        OutlinedButton(
                            onClick = onInspectXapk,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = CosmoPurple)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inspect", fontSize = 12.sp, color = CosmoPurple)
                        }
                    }

                    if (item.isInstalled) {
                        // Open Action
                        Button(
                            onClick = onOpen,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmoGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        // Install Action
                        Button(
                            onClick = onInstall,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (item.fileType == FileType.XAPK) CosmoPurple else CosmoCyan,
                                contentColor = if (item.fileType == FileType.XAPK) Color.White else Color.Black
                            )
                        ) {
                            Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Install ${if (item.fileType == FileType.XAPK) "XAPK" else "APK"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
