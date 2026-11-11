package com.example.ui.home.tabs

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.auth.SigningCertificateValidator
import com.example.auth.UserProfile
import com.example.ui.viewmodel.BackupPreview
import com.example.ui.viewmodel.TempleViewModel
import com.example.ui.viewmodel.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun YouTab(
    user: UserProfile,
    viewModel: TempleViewModel,
    onUpdateUser: ((String, String, String?) -> Unit)? = null,
    onRequestSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val visitedCount by viewModel.visitedCount.collectAsStateWithLifecycle()
    val placesCount by viewModel.placesCount.collectAsStateWithLifecycle()
    val mediaCount by viewModel.mediaCount.collectAsStateWithLifecycle()

    var showClearDataDialog by remember { mutableStateOf(false) }
    var pendingImportPreview by remember { mutableStateOf<BackupPreview?>(null) }
    var showDebugDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val exportDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(context, uri) { result ->
                if (result.isSuccess) Toast.makeText(context, "Exported successfully", Toast.LENGTH_SHORT).show()
                else Toast.makeText(context, "Export error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = viewModel.parseBackupPreviewFromUri(context, uri)
            if (result.isSuccess) pendingImportPreview = result.getOrNull()
            else Toast.makeText(context, "Invalid backup", Toast.LENGTH_SHORT).show()
        }
    }

    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(user.displayName) }
        var editEmail by remember { mutableStateOf(user.email) }
        var editPhotoUrl by remember { mutableStateOf(user.photoUrl ?: "") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile Details", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhotoUrl,
                        onValueChange = { editPhotoUrl = it },
                        label = { Text("Avatar Photo URL (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateUser?.invoke(editName, editEmail, editPhotoUrl.ifBlank { null })
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    pendingImportPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { pendingImportPreview = null },
            title = { Text("Import Backup", fontWeight = FontWeight.SemiBold) },
            text = { Text("Restore ${preview.visitsCount} visits?") },
            confirmButton = {
                Button(
                    onClick = {
                        val p = preview
                        pendingImportPreview = null
                        viewModel.importFromPreview(p) { result ->
                            if (result.isSuccess) Toast.makeText(context, "Restored ${result.getOrNull()} visits", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportPreview = null }) { Text("Cancel") }
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear data", fontWeight = FontWeight.SemiBold) },
            text = { Text("Delete local data?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllUserData()
                        showClearDataDialog = false
                        Toast.makeText(context, "Data cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDebugDialog && BuildConfig.DEBUG) {
        val signingInfo = remember { SigningCertificateValidator.getSigningInfo(context) }
        val clipboardManager = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showDebugDialog = false },
            title = { Text("About (Debug)", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Package: ${signingInfo.packageName}", style = MaterialTheme.typography.bodySmall)
                    Text("SHA-1: ${signingInfo.currentSha1}", style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(signingInfo.currentSha1))
                        Toast.makeText(context, "SHA-1 copied", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Copy SHA-1") }
            },
            dismissButton = {
                TextButton(onClick = { showDebugDialog = false }) { Text("Close") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Account Chip on the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "You",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Account Chip (Avatar + First Name + Sign Out)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.clickable(onClick = onRequestSignOut)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!user.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(24.dp).clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    Text(
                        text = user.displayName.substringBefore(" "),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sign out",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // User Profile Card: Avatar, Name, and Edit Profile action
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!user.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.email.ifBlank { "Local Profile" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { showEditProfileDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Three Stat Numbers: Visits, Places, Photos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(count = visitedCount, label = "Visits", modifier = Modifier.weight(1f))
            StatCard(count = placesCount, label = "Places", modifier = Modifier.weight(1f))
            StatCard(count = mediaCount, label = "Photos", modifier = Modifier.weight(1f))
        }

        // Grouped list of at most 8 rows with icons
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // 1. Theme
                SettingsRow(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    onClick = {
                        val nextMode = when (currentThemeMode) {
                            ThemeMode.DARK -> ThemeMode.LIGHT
                            ThemeMode.LIGHT -> ThemeMode.SYSTEM
                            ThemeMode.SYSTEM -> ThemeMode.DARK
                        }
                        viewModel.setThemeMode(nextMode)
                        Toast.makeText(context, "Theme: $nextMode", Toast.LENGTH_SHORT).show()
                    }
                )

                // 2. Map style
                SettingsRow(
                    icon = Icons.Default.Layers,
                    title = "Map style",
                    onClick = {
                        val curr = viewModel.defaultMapLayer.value
                        val next = if (curr == "SATELLITE") "STREET" else "SATELLITE"
                        viewModel.setDefaultMapLayer(next)
                        Toast.makeText(context, "Map style: $next", Toast.LENGTH_SHORT).show()
                    }
                )

                // 3. Local backup
                SettingsRow(
                    icon = Icons.Default.Sync,
                    title = "Local backup",
                    onClick = {
                        viewModel.triggerDriveSyncManual(user.email)
                        Toast.makeText(context, "Local backup saved", Toast.LENGTH_SHORT).show()
                    }
                )

                // 4. Export
                SettingsRow(
                    icon = Icons.Default.Upload,
                    title = "Export",
                    onClick = {
                        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportDocLauncher.launch("temple_map_$ts.json")
                    }
                )

                // 5. Import
                SettingsRow(
                    icon = Icons.Default.Download,
                    title = "Import",
                    onClick = {
                        openDocLauncher.launch(arrayOf("application/json"))
                    }
                )

                // 6. Clear data
                SettingsRow(
                    icon = Icons.Default.Delete,
                    title = "Clear data",
                    onClick = { showClearDataDialog = true }
                )

                // 7. About (debug only)
                if (BuildConfig.DEBUG) {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "About (debug only)",
                        onClick = { showDebugDialog = true }
                    )
                }

                // 8. Sign out
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Sign out",
                    onClick = onRequestSignOut
                )
            }
        }
    }
}

@Composable
private fun StatCard(count: Int, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
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
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}
