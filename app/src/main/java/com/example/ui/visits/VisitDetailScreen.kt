package com.example.ui.visits

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.ExifInterface
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.viewmodel.TempleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    val trips by viewModel.allTrips.collectAsStateWithLifecycle()
    val mediaLinks by viewModel.getMediaLinksForVisit(visitId).collectAsStateWithLifecycle(emptyList())

    val visit = visits.firstOrNull { it.id == visitId }
    val place = places.firstOrNull { p -> p.id == visit?.placeId }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAssignTripDialog by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    var notesText by remember(visit?.notes) { mutableStateOf(visit?.notes ?: "") }


// ... (existing imports, but add the new ones above)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {}

                var detectedDate: Long? = null
                var hasGps = false
                var detectedLat: Double? = null
                var detectedLng: Double? = null

                val hasLocationPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_MEDIA_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val exifInterface = ExifInterface(inputStream)
                        val dateStr = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        if (dateStr != null) {
                            try {
                                detectedDate = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(dateStr)?.time
                            } catch (e: Exception) {}
                        }
                    }

                    if (hasLocationPermission && uri.scheme == "content" && uri.authority?.contains("media") == true) {
                        context.contentResolver.openInputStream(MediaStore.setRequireOriginal(uri))?.use { inputStream ->
                            val exifInterface = ExifInterface(inputStream)
                            val latLong = FloatArray(2)
                            if (exifInterface.getLatLong(latLong)) {
                                hasGps = true
                                detectedLat = latLong[0].toDouble()
                                detectedLng = latLong[1].toDouble()
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VisitDetail", "EXIF read error: ${e.message}")
                }

                viewModel.linkGalleryMedia(
                    visitId = visitId,
                    uriString = uri.toString(),
                    dateTaken = detectedDate ?: visit?.startDate ?: System.currentTimeMillis(),
                    hasGps = hasGps,
                    latitude = detectedLat,
                    longitude = detectedLng
                )
            }
            Toast.makeText(context, "${uris.size} photos added", Toast.LENGTH_SHORT).show()
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Visit?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this visit?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteVisit(visitId)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAssignTripDialog) {
        var selectedTripId by remember { mutableStateOf(visit?.tripId) }
        AlertDialog(
            onDismissRequest = { showAssignTripDialog = false },
            title = { Text("Move to Trip", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (trips.isEmpty()) {
                        Text("No trips created yet.")
                    } else {
                        trips.forEach { trip ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedTripId == trip.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTripId = trip.id }
                            ) {
                                Text(trip.name, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAssignTripDialog = false
                        viewModel.assignVisitToTrip(visitId, selectedTripId)
                        Toast.makeText(context, "Trip updated", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAssignTripDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(place?.name ?: "Visit Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { overflowMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = overflowMenuExpanded,
                            onDismissRequest = { overflowMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    overflowMenuExpanded = false
                                    Toast.makeText(context, "Edit via form", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to trip") },
                                onClick = {
                                    overflowMenuExpanded = false
                                    showAssignTripDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.FolderShared, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    showDeleteConfirmDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
            // 1. Photo Carousel at the top
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Photos (${mediaLinks.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    TextButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Photos")
                    }
                }

                if (mediaLinks.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(mediaLinks, key = { it.id }) { link ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Box {
                                    var isError by remember { mutableStateOf(false) }
                                    AsyncImage(
                                        model = link.externalId,
                                        contentDescription = "Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        onError = { isError = true }
                                    )
                                    if (isError) {
                                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                            IconButton(onClick = {
                                                // TODO: Pass the media link ID to be replaced
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }) {
                                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Re-add")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Tap to add photos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 2. Title & Date Chip
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = place?.name ?: "Temple Visit",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                val dateStr = visit?.startDate?.let { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Today"
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // 3. Small Map / Coordinates
            if (place != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TerracottaPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🗺️", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Coordinates", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("%.4f° N, %.4f° E".format(place.latitude, place.longitude), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }

            // 4. Inline-Editable Notes
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Notes", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = {
                        notesText = it
                        if (visit != null) {
                            viewModel.updateVisit(visit.copy(notes = it))
                        }
                    },
                    placeholder = { Text("Write your thoughts or journal notes...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            // 5. Optional Sections (Show only when they have content)
            if (!visit?.deity.isNullOrBlank()) {
                OptionalDetailSection(title = "Presiding Deity", content = visit?.deity!!)
            }
            if (!visit?.architecture.isNullOrBlank()) {
                OptionalDetailSection(title = "Architecture Style", content = visit?.architecture!!)
            }
            if (!visit?.prayers.isNullOrBlank()) {
                OptionalDetailSection(title = "Prayers / Sankalpam", content = visit?.prayers!!)
            }
            if (!visit?.nearbyFood.isNullOrBlank()) {
                OptionalDetailSection(title = "Nearby Food / Prasadam", content = visit?.nearbyFood!!)
            }
        }
    }
}

@Composable
private fun OptionalDetailSection(title: String, content: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(content, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}
