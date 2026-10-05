package com.example.ui.visits

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CategoryEntity
import com.example.data.model.MediaLinkEntity
import com.example.data.model.MediaLinkType
import com.example.data.model.PlaceEntity
import com.example.data.model.TripEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.MarigoldGold
import com.example.ui.theme.SageForest
import com.example.ui.theme.TerracottaDark
import com.example.ui.theme.TerracottaLight
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.WishlistOrchid
import com.example.ui.viewmodel.TempleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VisitDetailScreen(
    visitId: String,
    viewModel: TempleViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val visits by viewModel.allVisits.collectAsStateWithLifecycle()
    val places by viewModel.allPlaces.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val trips by viewModel.allTrips.collectAsStateWithLifecycle()
    val mediaLinks by viewModel.getMediaLinksForVisit(visitId).collectAsStateWithLifecycle(emptyList())

    val visit = visits.firstOrNull { it.id == visitId }
    val place = places.firstOrNull { it.id == visit?.placeId }
    val currentTrip = trips.firstOrNull { it.id == visit?.tripId }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showGooglePhotosLinkDialog by remember { mutableStateOf(false) }
    var showAssignTripDialog by remember { mutableStateOf(false) }
    var showEditVisitDialog by remember { mutableStateOf(false) }
    var showDateSuggestionsDialog by remember { mutableStateOf(false) }

    // System Photo Picker Contract (Zero Permission, Google Play Compliant!)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Handled gracefully for transient URIs
                }
                viewModel.linkGalleryMedia(
                    visitId = visitId,
                    uriString = uri.toString(),
                    dateTaken = visit?.startDate,
                    hasGps = false
                )
            }
            Toast.makeText(context, "${uris.size} photos linked to visit", Toast.LENGTH_SHORT).show()
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete This Visit?", fontWeight = FontWeight.Bold) },
            text = { Text("This will remove this temple visit record and un-link media. Your original photos in your device gallery will NOT be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteVisit(visitId)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Visit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Google Photos Link Dialog
    if (showGooglePhotosLinkDialog) {
        var googlePhotosUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGooglePhotosLinkDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TerracottaPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link Google Photos", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste a Google Photos share link or album URL (e.g. photos.app.goo.gl/... or photos.google.com/album/...).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = googlePhotosUrl,
                        onValueChange = { googlePhotosUrl = it },
                        placeholder = { Text("https://photos.app.goo.gl/...", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Zero-Copy Guarantee: We store only the reference URL. Photos remain securely in your Google Photos account.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SageForest
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (googlePhotosUrl.isNotBlank()) {
                            viewModel.linkGooglePhotosMedia(visitId, googlePhotosUrl.trim(), visit?.startDate)
                            Toast.makeText(context, "Google Photos reference linked!", Toast.LENGTH_SHORT).show()
                            showGooglePhotosLinkDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                ) {
                    Text("Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGooglePhotosLinkDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Assign Trip Dialog
    if (showAssignTripDialog && visit != null) {
        var newTripName by remember { mutableStateOf("") }
        var isCreatingNewTrip by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAssignTripDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderShared, contentDescription = null, tint = MarigoldGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Assign to Pilgrimage Trip", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Group this visit into an organized pilgrimage trip (e.g., 'South India Temples 2025').",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (trips.isNotEmpty() && !isCreatingNewTrip) {
                        Text("Select Existing Trip:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Option for No Trip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (visit.tripId == null) TerracottaLight else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.assignVisitToTrip(visitId, null)
                                        showAssignTripDialog = false
                                    }
                            ) {
                                Text(
                                    text = "— None (Independent Visit) —",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            trips.forEach { trip ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (visit.tripId == trip.id) TerracottaLight else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.assignVisitToTrip(visitId, trip.id)
                                            showAssignTripDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.FolderShared, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(trip.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(trip.startDate)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        TextButton(
                            onClick = { isCreatingNewTrip = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Create New Trip")
                        }
                    } else {
                        // Create New Trip Input
                        OutlinedTextField(
                            value = newTripName,
                            onValueChange = { newTripName = it },
                            label = { Text("Trip Name") },
                            placeholder = { Text("e.g. South India March 2025") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (trips.isNotEmpty()) {
                            TextButton(onClick = { isCreatingNewTrip = false }) {
                                Text("Choose from existing trips")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isCreatingNewTrip || trips.isEmpty()) {
                    Button(
                        onClick = {
                            if (newTripName.isNotBlank()) {
                                viewModel.createTrip(
                                    name = newTripName.trim(),
                                    startDate = visit.startDate,
                                    endDate = visit.endDate,
                                    onCreated = { newTripId ->
                                        viewModel.assignVisitToTrip(visitId, newTripId)
                                        showAssignTripDialog = false
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                    ) {
                        Text("Create & Assign")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignTripDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Photo Suggestions Dialog (Matching Visit Date Window)
    if (showDateSuggestionsDialog && visit != null) {
        val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        AlertDialog(
            onDismissRequest = { showDateSuggestionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MarigoldGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Suggested Photos for this Visit", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Photos taken around ${dateFormatter.format(Date(visit.startDate))} that lack GPS data can be linked to this visit.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Automatic Date Matcher", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "To select non-GPS photos from this date window, use the Device Gallery picker.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDateSuggestionsDialog = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                ) {
                    Text("Open Photo Picker")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateSuggestionsDialog = false }) { Text("Close") }
            }
        )
    }

    if (visit == null || place == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Visit record not found")
        }
        return
    }

    val visitCategories = categories.filter { visit.categoryIds.contains(it.id) }
    val dateFormatter = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Temple Visit Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Visit", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Hero Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, if (visit.state == VisitState.VISITED) TerracottaPrimary else WishlistOrchid),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = place.name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.4f° N, %.4f° E".format(place.latitude, place.longitude),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // State Badge with Toggle
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (visit.state == VisitState.VISITED) TerracottaLight else WishlistOrchid.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (visit.state == VisitState.VISITED) TerracottaPrimary else WishlistOrchid),
                            modifier = Modifier.clickable { viewModel.toggleVisitState(visit) }
                        ) {
                            Text(
                                text = if (visit.state == VisitState.VISITED) "✓ VISITED" else "★ WISHLIST",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (visit.state == VisitState.VISITED) TerracottaDark else WishlistOrchid,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AntiqueGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (visit.endDate != null) {
                                "${dateFormatter.format(Date(visit.startDate))} — ${dateFormatter.format(Date(visit.endDate))}"
                            } else {
                                dateFormatter.format(Date(visit.startDate))
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Tags
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visitCategories.forEach { cat ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Trip Grouping Card (Step 5 Requirement)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MarigoldGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MarigoldGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderShared, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pilgrimage Trip Group",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentTrip?.name ?: "No Trip Assigned (Independent)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (currentTrip != null) TerracottaDark else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showAssignTripDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TerracottaPrimary)
                    ) {
                        Text(if (currentTrip == null) "Assign" else "Change", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Sacred Heritage & Journal Details Section
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
                    Text(
                        text = "Sacred Heritage & Journal Notes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    if (!visit.deity.isNullOrBlank()) {
                        DetailItemRow(label = "Presiding Deity", value = visit.deity, icon = "🕉️")
                    }
                    if (!visit.architecture.isNullOrBlank()) {
                        DetailItemRow(label = "Architecture Style", value = visit.architecture, icon = "🏛️")
                    }
                    if (!visit.prayers.isNullOrBlank()) {
                        DetailItemRow(label = "What was prayed for / Sankalpam", value = visit.prayers, icon = "🙏")
                    }
                    if (!visit.notes.isNullOrBlank()) {
                        DetailItemRow(label = "Pilgrimage Notes", value = visit.notes, icon = "📜")
                    }
                }
            }

            // Step 5: Linked Media Section (Zero-Copy)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Linked Photos (${mediaLinks.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "Zero-copy links only · Tap thumbnail to open original",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Media Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("link_photos_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Device Photos", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showGooglePhotosLinkDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, TerracottaPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = TerracottaPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Google Photos", fontSize = 12.sp, color = TerracottaPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showDateSuggestionsDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MarigoldGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = MarigoldGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Suggest Non-GPS Photos from Trip Dates", fontSize = 12.sp, color = AntiqueGold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (mediaLinks.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Photo, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No photos linked to this visit yet",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Tap 'Device Photos' or 'Google Photos' above to link images from your trips. We store only identifiers—no media is ever re-uploaded or copied.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        // Grid of Photo Thumbnails
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            mediaLinks.chunked(3).forEach { rowLinks ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowLinks.forEach { media ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    // Tap thumbnail opens photo in original app (Gallery / Google Photos)
                                                    try {
                                                        if (media.type == MediaLinkType.GOOGLE_PHOTOS || media.externalId.startsWith("http")) {
                                                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(media.externalId))
                                                            context.startActivity(webIntent)
                                                        } else {
                                                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                                                setDataAndType(Uri.parse(media.externalId), "image/*")
                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            context.startActivity(viewIntent)
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Opening original media...", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                        ) {
                                            if (media.type == MediaLinkType.GOOGLE_PHOTOS && !media.externalId.startsWith("content:")) {
                                                // Web / Google Photos Link
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(0xFF1E283E)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MarigoldGold, modifier = Modifier.size(32.dp))
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Google Photos", fontSize = 10.sp, color = Color.White)
                                                    }
                                                }
                                            } else {
                                                AsyncImage(
                                                    model = media.externalId,
                                                    contentDescription = "Temple photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            // Open external indicator badge
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.6f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Launch, contentDescription = "Open", tint = Color.White, modifier = Modifier.size(12.dp))
                                            }

                                            // Unlink photo button
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                                    .clickable { viewModel.unlinkMedia(media.id) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Unlink", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    for (i in rowLinks.size until 3) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Zero-Copy Guarantee Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SageForest.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, SageForest.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = SageForest, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Zero-Copy Guarantee: Photos & videos are never copied or re-hosted. The app stores only links and identifiers to device gallery or Google Photos originals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailItemRow(label: String, value: String, icon: String) {
    Column {
        Text(
            text = "$icon $label",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
