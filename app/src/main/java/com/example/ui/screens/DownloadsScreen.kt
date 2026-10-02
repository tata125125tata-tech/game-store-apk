package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import com.example.ui.theme.CosmoAmber
import com.example.ui.theme.CosmoBackgroundDark
import com.example.ui.theme.CosmoCardBorder
import com.example.ui.theme.CosmoCardDark
import com.example.ui.theme.CosmoCyan
import com.example.ui.theme.CosmoGreen
import com.example.ui.theme.CosmoPurple
import com.example.ui.theme.CosmoRed

@Composable
fun DownloadsScreen(
    onNavigateToLibrary: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val downloadManager = remember { CosmoDownloadManager.getInstance(context) }
    val downloads by downloadManager.downloads.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var itemToDelete by remember { mutableStateOf<DownloadItem?>(null) }

    BackHandler { onBack() }

    val filteredDownloads = remember(downloads, selectedFilter) {
        when (selectedFilter) {
            "ACTIVE" -> downloads.filter {
                it.status == DownloadStatus.DOWNLOADING ||
                it.status == DownloadStatus.PENDING ||
                it.status == DownloadStatus.PAUSED
            }
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
            // Header Bar with Back and Go to Library
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Download Manager",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp
                        )
                    )
                }

                OutlinedButton(
                    onClick = onNavigateToLibrary,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = CosmoCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("My Library", fontSize = 12.sp, color = CosmoCyan)
                }
            }

            // 3 Tabs: All / Active / Completed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val activeCount = downloads.count {
                    it.status == DownloadStatus.DOWNLOADING ||
                    it.status == DownloadStatus.PENDING ||
                    it.status == DownloadStatus.PAUSED
                }
                val completedCount = downloads.count { it.status == DownloadStatus.COMPLETED }

                listOf(
                    "ALL" to "All (${downloads.size})",
                    "ACTIVE" to "Active ($activeCount)",
                    "COMPLETED" to "Completed ($completedCount)"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
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

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredDownloads.isEmpty()) {
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
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CosmoCardDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Downloads Found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Downloads started from the Cosmo Web Store will appear here with live speed and progress.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDownloads, key = { it.id }) { item ->
                        DownloadStatusCard(
                            item = item,
                            onPause = { downloadManager.pauseDownload(item.id) },
                            onResume = { downloadManager.resumeDownload(item.id) },
                            onCancel = { downloadManager.cancelDownload(item.id, deleteFile = false) },
                            onRetry = { downloadManager.resumeDownload(item.id) },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }

        // Delete Dialog
        if (itemToDelete != null) {
            val item = itemToDelete!!
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Delete Download", fontWeight = FontWeight.Bold) },
                text = { Text("Delete ${item.title} and remove its file from storage?") },
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
    }
}

@Composable
fun DownloadStatusCard(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CosmoCardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CosmoCardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: App icon, Title, APK/XAPK chip, Status Text, Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmoBackgroundDark),
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
                        // APK or XAPK label
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (item.fileType == FileType.XAPK) CosmoPurple.copy(alpha = 0.2f)
                                    else CosmoCyan.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (item.fileType == FileType.XAPK) "XAPK" else "APK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.fileType == FileType.XAPK) CosmoPurple else CosmoCyan
                            )
                        }

                        // Status & Size
                        Text(
                            text = when (item.status) {
                                DownloadStatus.DOWNLOADING -> "Downloading · ${item.formattedDownloaded} / ${item.formattedTotal}"
                                DownloadStatus.COMPLETED -> "Completed · ${item.formattedTotal}"
                                DownloadStatus.PAUSED -> "Paused · ${item.formattedDownloaded} / ${item.formattedTotal}"
                                DownloadStatus.FAILED -> "Failed · ${item.formattedDownloaded}"
                                DownloadStatus.CANCELLED -> "Cancelled"
                                DownloadStatus.PENDING -> "Connecting..."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = when (item.status) {
                                    DownloadStatus.FAILED -> CosmoRed
                                    DownloadStatus.COMPLETED -> CosmoGreen
                                    DownloadStatus.PAUSED -> CosmoAmber
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 11.sp
                            )
                        )
                    }
                }

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

            // Progress bar for active downloads
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.PENDING) {
                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
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

                    // Action buttons: Pause / Resume / Cancel
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

            // Retry row if failed
            if (item.status == DownloadStatus.FAILED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmoRed.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CosmoRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.errorMessage ?: "Download failed",
                            fontSize = 11.sp,
                            color = CosmoRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onRetry, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = CosmoCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
