package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.example.downloader.CosmoDownloadManager
import com.example.installer.PackageInstallerManager
import com.example.library.LibraryManager
import com.example.library.LibraryStore
import com.example.model.InstalledGame
import com.example.model.LibraryItem
import com.example.settings.SettingsManager
import com.example.ui.theme.CosmoGreen
import com.example.ui.theme.CosmoPurple
import com.example.ui.theme.CosmoRed
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MyLibraryScreen(
    onNavigateToBrowser: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val libraryStore = remember { LibraryStore.getInstance(context) }
    val downloadManager = remember { CosmoDownloadManager.getInstance(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }

    val libraryItems by libraryStore.items.collectAsState()
    val activeDownloadsCount by downloadManager.activeDownloadsCount.collectAsState()

    var selectedTab by remember { mutableStateOf("DOWNLOADS") } // DOWNLOADS or INSTALLED
    var installedGames by remember { mutableStateOf<List<InstalledGame>>(emptyList()) }
    var isLoadingInstalled by remember { mutableStateOf(false) }

    var itemToDelete by remember { mutableStateOf<LibraryItem?>(null) }
    var installingItemStatus by remember { mutableStateOf<String?>(null) }

    fun refreshInstalled() {
        scope.launch {
            isLoadingInstalled = true
            installedGames = LibraryManager.loadInstalledGames(context)
            isLoadingInstalled = false
        }
    }

    LaunchedEffect(Unit) {
        refreshInstalled()
        downloadManager.refreshInstalledStatus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("my_library_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Active Downloads Banner if any running
            if (activeDownloadsCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onNavigateToDownloads() }
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "$activeDownloadsCount download in progress",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "View Downloads",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Tab Selector: Downloaded Packages vs All Installed Apps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == "DOWNLOADS",
                    onClick = { selectedTab = "DOWNLOADS" },
                    label = {
                        Text(
                            text = "Downloaded Packages (${libraryItems.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                FilterChip(
                    selected = selectedTab == "INSTALLED",
                    onClick = {
                        selectedTab = "INSTALLED"
                        refreshInstalled()
                    },
                    label = {
                        Text(
                            text = "Device Apps (${installedGames.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Content List
            if (selectedTab == "DOWNLOADS") {
                if (libraryItems.isEmpty()) {
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
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Library is Empty",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Downloaded APK and XAPK packages automatically appear in My Library for instant installation.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onNavigateToBrowser,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Browse Web Store", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(libraryItems, key = { it.id }) { item ->
                            val isInstalled = PackageInstallerManager.isPackageInstalled(context, item.packageName)
                            LibraryPackageCard(
                                item = item,
                                isInstalled = isInstalled,
                                onInstall = {
                                    val file = File(item.localFilePath)
                                    if (!file.exists()) {
                                        Toast.makeText(context, "Package file not found on device", Toast.LENGTH_SHORT).show()
                                        return@LibraryPackageCard
                                    }

                                    if (item.isXapk) {
                                        scope.launch {
                                            installingItemStatus = "Staging XAPK package..."
                                            PackageInstallerManager.installXapk(
                                                context = context,
                                                xapkFile = file,
                                                onProgress = { status -> installingItemStatus = status }
                                            ).fold(
                                                onSuccess = {
                                                    installingItemStatus = null
                                                    Toast.makeText(context, "Opening package installer...", Toast.LENGTH_SHORT).show()
                                                },
                                                onFailure = { err ->
                                                    installingItemStatus = null
                                                    Toast.makeText(context, "Installation error: ${err.message}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    } else {
                                        PackageInstallerManager.installApk(context, file).fold(
                                            onSuccess = {
                                                Toast.makeText(context, "Opening package installer...", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = { err ->
                                                Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                },
                                onOpen = {
                                    val launched = PackageInstallerManager.openApp(context, item.packageName)
                                    if (!launched) {
                                        Toast.makeText(context, "Unable to launch application", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDelete = { itemToDelete = item }
                            )
                        }
                    }
                }
            } else {
                // Device Installed Apps Tab
                if (isLoadingInstalled) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (installedGames.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No installed apps found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(installedGames, key = { it.packageName }) { game ->
                            InstalledDeviceAppCard(
                                game = game,
                                onPlay = {
                                    LibraryManager.launchApp(context, game.packageName)
                                },
                                onAppInfo = {
                                    LibraryManager.openAppDetails(context, game.packageName)
                                },
                                onUninstall = {
                                    LibraryManager.uninstallApp(context, game.packageName)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Non-floating Installation Status Notice
        if (installingItemStatus != null) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Installing Package", fontWeight = FontWeight.Bold) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = installingItemStatus ?: "Preparing installation...")
                    }
                },
                confirmButton = { }
            )
        }

        // Delete Dialog
        if (itemToDelete != null) {
            val item = itemToDelete!!
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Delete Library Item", fontWeight = FontWeight.Bold) },
                text = { Text("Delete ${item.title} and remove the downloaded ${item.typeLabel} file from device storage?") },
                confirmButton = {
                    Button(
                        onClick = {
                            libraryStore.deleteItemAndFile(item.id)
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
fun LibraryPackageCard(
    item: LibraryItem,
    isInstalled: Boolean,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fileExists = item.fileExists

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: App icon, Title, Version, Delete action
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                            imageVector = if (item.isXapk) Icons.Default.Archive else Icons.Default.InstallMobile,
                            contentDescription = null,
                            tint = if (item.isXapk) CosmoPurple else MaterialTheme.colorScheme.primary,
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
                        // Badge: APK or XAPK
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (item.isXapk) CosmoPurple.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = item.typeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isXapk) CosmoPurple else MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!item.versionName.isNullOrBlank()) {
                            Text(
                                text = "v${item.versionName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }

                        Text(
                            text = item.formattedSize,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Spacer(modifier = Modifier.height(8.dp))

            // XAPK Details indicator if applicable
            if (item.isXapk) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CosmoPurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.packageDetailsLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Status and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!fileExists) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CosmoRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "File deleted from storage",
                            fontSize = 11.sp,
                            color = CosmoRed
                        )
                    } else if (isInstalled) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CosmoGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Installed on device",
                            fontSize = 11.sp,
                            color = CosmoGreen,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Downloaded ${item.formattedDate}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Natural, in-place button action (NO FLOATING BUTTON)
                if (!fileExists) {
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Remove", fontSize = 11.sp, color = CosmoRed)
                    }
                } else if (isInstalled) {
                    Button(
                        onClick = onOpen,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CosmoGreen,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onInstall,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (item.isXapk) CosmoPurple else MaterialTheme.colorScheme.primary,
                            contentColor = if (item.isXapk) Color.White else MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.InstallMobile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Install", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun InstalledDeviceAppCard(
    game: InstalledGame,
    onPlay: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(game.iconDrawable) {
        try {
            game.iconDrawable?.toBitmap(128, 128)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = game.appName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.appName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = game.packageName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmoGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}
