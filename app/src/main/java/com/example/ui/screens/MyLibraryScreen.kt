package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.downloader.CosmoDownloadManager
import com.example.library.LibraryManager
import com.example.model.InstalledGame
import com.example.ui.theme.CosmoBackgroundDark
import com.example.ui.theme.CosmoCardBorder
import com.example.ui.theme.CosmoCardDark
import com.example.ui.theme.CosmoCyan
import com.example.ui.theme.CosmoGreen
import com.example.ui.theme.CosmoPurple
import com.example.ui.theme.CosmoRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MyLibraryScreen(
    onNavigateToBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadManager = remember { CosmoDownloadManager.getInstance(context) }

    var games by remember { mutableStateOf<List<InstalledGame>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    fun refreshGames() {
        scope.launch {
            isLoading = true
            games = LibraryManager.loadInstalledGames(context)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshGames()
    }

    val filteredGames = remember(games, searchQuery, selectedFilter) {
        games.filter { game ->
            val matchesQuery = searchQuery.isBlank() ||
                game.appName.contains(searchQuery, ignoreCase = true) ||
                game.packageName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "COSMO" -> game.isFromCosmo
                "DEVICE" -> !game.isFromCosmo
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmoBackgroundDark)
            .testTag("my_library_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Stats Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, CosmoCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CosmoCardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GAME LIBRARY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CosmoCyan,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${games.size} Games Ready to Play",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Downloaded via Cosmo Store & Device games",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = { refreshGames() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CosmoCyan.copy(alpha = 0.15f))
                            .testTag("library_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan Installed Games",
                            tint = CosmoCyan
                        )
                    }
                }
            }

            // Search Bar & Filter Chips
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed games...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CosmoCyan, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("library_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmoCyan,
                        unfocusedBorderColor = CosmoCardBorder,
                        focusedContainerColor = CosmoCardDark,
                        unfocusedContainerColor = CosmoCardDark
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "ALL" to "All (${games.size})",
                        "COSMO" to "From Cosmo (${games.count { it.isFromCosmo }})",
                        "DEVICE" to "Device (${games.count { !it.isFromCosmo }})"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 11.sp) },
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Games List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CosmoCyan)
                }
            } else if (filteredGames.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = CosmoCyan,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No Matching Games" else "No Games Installed",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try a different search term" else "Install games from the Cosmo Web Store to see them in your Library.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                            Text("Find Games on Store", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredGames, key = { it.packageName }) { game ->
                        InstalledGameCard(
                            game = game,
                            onPlay = {
                                val launched = LibraryManager.launchApp(context, game.packageName)
                                if (!launched) {
                                    Toast.makeText(context, "Cannot open ${game.appName}", Toast.LENGTH_SHORT).show()
                                }
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
}

@Composable
fun InstalledGameCard(
    game: InstalledGame,
    onPlay: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
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
            .border(1.dp, CosmoCardBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CosmoCardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmoBackgroundDark),
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
                        tint = CosmoCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Game Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = game.appName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (game.isFromCosmo) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CosmoPurple.copy(alpha = 0.25f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "COSMO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmoPurple
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "v${game.versionName ?: "1.0"} · ${game.packageName}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play / Open Button
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmoGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // More Options Dropdown
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(CosmoCardDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("App Details", color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = CosmoCyan) },
                        onClick = {
                            menuExpanded = false
                            onAppInfo()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Uninstall", color = CosmoRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CosmoRed) },
                        onClick = {
                            menuExpanded = false
                            onUninstall()
                        }
                    )
                }
            }
        }
    }
}
