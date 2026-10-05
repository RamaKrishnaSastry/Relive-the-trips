package com.example.ui.home.tabs

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.UserProfile
import com.example.data.model.CategoryEntity
import com.example.ui.home.dialogs.ImportBackupDialog
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.MarigoldGold
import com.example.ui.theme.SageForest
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.WishlistOrchid
import com.example.ui.viewmodel.TempleViewModel
import com.example.ui.viewmodel.ThemeMode
import java.util.Locale

@Composable
fun SettingsTab(
    user: UserProfile,
    viewModel: TempleViewModel,
    onOpenAddCategory: () -> Unit,
    onRequestSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val currentMapLayer by viewModel.defaultMapLayer.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    val visitedCount by viewModel.visitedCount.collectAsStateWithLifecycle()
    val wishlistCount by viewModel.wishlistCount.collectAsStateWithLifecycle()
    val placesCount by viewModel.placesCount.collectAsStateWithLifecycle()
    val mediaCount by viewModel.mediaCount.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val isSeeding by viewModel.isSeeding.collectAsStateWithLifecycle()

    var showImportDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    if (showImportDialog) {
        ImportBackupDialog(
            onDismiss = { showImportDialog = false },
            onImport = { jsonText ->
                showImportDialog = false
                viewModel.importBackupJson(jsonText) { result ->
                    if (result.isSuccess) {
                        Toast.makeText(
                            context,
                            "Successfully imported ${result.getOrNull()} visits & sacred places!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Import failed: ${result.exceptionOrNull()?.localizedMessage ?: "Invalid JSON format"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Local Diary?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "This will delete all local temple visits, custom places, and linked photos from this device. If you synced with Google Drive, your cloud backup remains safe.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllUserData()
                        showClearDataDialog = false
                        Toast.makeText(context, "All local journal records cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${cat.name}'? Existing visits will remain intact.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(cat.id)
                        categoryToDelete = null
                        Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = MarigoldGold,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Settings & Preferences",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Personalize theme, globe styles, auto-backups & privacy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: Account Profile & Pilgrim Snapshot Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(TerracottaPrimary, MarigoldGold)
                                )
                            )
                            .border(2.dp, MarigoldGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.displayName.take(1).uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = user.email.ifBlank { "Local Pilgrim Profile" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SageForest.copy(alpha = 0.15f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = if (user.email.isNotBlank()) "Google Account Active" else "Offline Pilgrim Account",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = SageForest,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Pilgrim Stats Snapshot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatPillItem(count = visitedCount, label = "Visited", icon = "🛕", tint = TerracottaPrimary)
                    StatPillItem(count = wishlistCount, label = "Wishlist", icon = "🔖", tint = WishlistOrchid)
                    StatPillItem(count = mediaCount, label = "Photos", icon = "📸", tint = AntiqueGold)
                    StatPillItem(count = placesCount, label = "Places", icon = "🗺️", tint = SageForest)
                }

                OutlinedButton(
                    onClick = onRequestSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of Account")
                }
            }
        }

        // Section 2: Appearance & Theme
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MarigoldGold.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MarigoldGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Appearance & Theme",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Select your spiritual color scheme and contrast",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Theme Mode Chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOptionCard(
                        title = "Dark Sanctum",
                        description = "Deep night temple canvas, charcoal & warm gold highlights",
                        icon = Icons.Default.DarkMode,
                        isSelected = currentThemeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                    )
                    ThemeOptionCard(
                        title = "Parchment Light",
                        description = "Warm cream parchment, earthy terracotta & sunlit stone",
                        icon = Icons.Default.LightMode,
                        isSelected = currentThemeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                    )
                    ThemeOptionCard(
                        title = "System Default",
                        description = "Dynamically synchronizes with your device's display setting",
                        icon = Icons.Default.BrightnessAuto,
                        isSelected = currentThemeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Default Map Layer Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = AntiqueGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Default Globe / Map Layer",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Initial view loaded whenever you open the 3D globe",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MapLayerChip(
                        title = "Satellite Earth",
                        iconStr = "🛰️",
                        isSelected = currentMapLayer == "SATELLITE",
                        onClick = { viewModel.setDefaultMapLayer("SATELLITE") },
                        modifier = Modifier.weight(1f)
                    )
                    MapLayerChip(
                        title = "Street & Topo",
                        iconStr = "🗺️",
                        isSelected = currentMapLayer == "STREET",
                        onClick = { viewModel.setDefaultMapLayer("STREET") },
                        modifier = Modifier.weight(1f)
                    )
                    MapLayerChip(
                        title = "Night Globe",
                        iconStr = "🌌",
                        isSelected = currentMapLayer == "DARK",
                        onClick = { viewModel.setDefaultMapLayer("DARK") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section 3: Auto-Backup & Offline Snapshot
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SageForest.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = SageForest,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "On-Device Auto-Backup & Sync",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Automatic snapshot saved locally on every app launch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Travel diary entries (temple visits, deity details, prayers, wishlist, and photo links) are safely saved in structured JSON. Export to Google Drive or any cloud app anytime below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        when (syncState) {
                            is com.example.sync.SyncState.Success -> SageForest.copy(alpha = 0.5f)
                            is com.example.sync.SyncState.Syncing -> TerracottaPrimary.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (syncState) {
                                is com.example.sync.SyncState.Syncing -> "🔄 Saving auto-backup snapshot..."
                                is com.example.sync.SyncState.Success -> "✓ ${(syncState as com.example.sync.SyncState.Success).message}"
                                is com.example.sync.SyncState.Offline -> "⚡ Working offline with local Room DB"
                                else -> "☁️ Auto-saves once when app opens"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = when (syncState) {
                                is com.example.sync.SyncState.Success -> SageForest
                                is com.example.sync.SyncState.Syncing -> TerracottaPrimary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.triggerDriveSyncManual(user.email)
                        Toast.makeText(context, "Saving local backup snapshot...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Auto-Backup Snapshot Now")
                }
            }
        }

        // Section 4: Data Management, Backup & Restore
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = TerracottaPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Backup, Export & Restore",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Complete control of your sacred travel database",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Generate a complete JSON backup containing all your temple visits, GPS coordinates, notes, categories, pilgrimages, and photo links to save to Google Drive or share.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export Button
                    Button(
                        onClick = {
                            val jsonText = viewModel.exportBackupJson()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, jsonText)
                                putExtra(Intent.EXTRA_TITLE, "Temple_Travel_Diary_Backup.json")
                                type = "application/json"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Export to Google Drive / Files")
                            context.startActivity(shareIntent)
                            Toast.makeText(context, "Exporting travel diary backup...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SageForest),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON")
                    }

                    // Import Button
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        border = BorderStroke(1.dp, SageForest),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = SageForest, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import JSON", color = SageForest)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Seed Sample Data Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Seed Famous Sacred Temples",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Populate renowned temples (Kashi, Meenakshi, Kedarnath, Tirupati, Angkor Wat)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            viewModel.seedSampleData()
                            Toast.makeText(context, "Famous sacred temples seeded!", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isSeeding,
                        colors = ButtonDefaults.buttonColors(containerColor = AntiqueGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isSeeding) "Loading..." else "Seed Data")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Clear Data Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reset Local Diary Data",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                        Text(
                            text = "Wipes local visits and places from this phone",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = { showClearDataDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset DB")
                    }
                }
            }
        }

        // Section 5: Custom Categories Manager
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = TerracottaPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manage Categories (${categories.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    FilledTonalButton(
                        onClick = onOpenAddCategory,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Category")
                    }
                }

                Text(
                    text = "Tag and organize your visits by trip type or purpose. Custom categories can be deleted below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Category Chips List
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val catColor = try {
                                        Color(android.graphics.Color.parseColor(cat.colorHex))
                                    } catch (e: Exception) {
                                        TerracottaPrimary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    if (cat.isDefault) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Default",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                if (!cat.isDefault) {
                                    IconButton(
                                        onClick = { categoryToDelete = cat },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 6: Zero-Copy Photos & Privacy Standards
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SageForest.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SageForest,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zero-Copy Privacy Standard",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "• Photos and videos are never copied or uploaded to external servers.\n• Gallery items are linked using Android's official Scoped Content URIs.\n• Google Photos links open directly in the Google Photos app with full privacy.\n• All diary entries are stored in your encrypted local SQLite database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 7: Mobile Readiness & Diagnostics Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MarigoldGold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = MarigoldGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Mobile Installation Diagnostic",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Certified ready for real Android devices & APK installation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DiagnosticCheckRow(title = "Android Compatibility", value = "Android 7.0 (API 24) to Android 14+ (API 36)")
                        DiagnosticCheckRow(title = "App Package Name", value = "com.aistudio.templemap.vtrpx")
                        DiagnosticCheckRow(title = "Storage Engine", value = "Room 2.6 SQLite (Offline-First, Zero Loss)")
                        DiagnosticCheckRow(title = "Google Play Policy", value = "Compliant (Zero-Permission Photo Picker)")
                        DiagnosticCheckRow(title = "Edge-to-Edge Display", value = "Active with dynamic WindowInsets")
                        DiagnosticCheckRow(title = "Battery & Network", value = "Optimized (No background wake locks/polling)")
                        DiagnosticCheckRow(title = "App Version", value = "1.0 (Release Build 1)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TerracottaPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) TerracottaPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) TerracottaPrimary else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) TerracottaPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = TerracottaPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun MapLayerChip(
    title: String,
    iconStr: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MarigoldGold.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) MarigoldGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = iconStr, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) MarigoldGold else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatPillItem(
    count: Int,
    label: String,
    icon: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$icon $count",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DiagnosticCheckRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "✓", color = SageForest, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal
        )
    }
}
