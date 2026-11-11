package com.example.ui.visits

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CategoryEntity
import com.example.data.places.TemplePlacesCatalog
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.WishlistOrchid
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateVisitSheet(
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    initialName: String = "",
    categories: List<CategoryEntity>,
    templeViewModel: com.example.ui.viewmodel.TempleViewModel,
    onDismiss: () -> Unit,
    onSaveVisit: (
        name: String,
        latitude: Double,
        longitude: Double,
        startDate: Long,
        endDate: Long?,
        categoryIds: List<String>,
        deity: String,
        architecture: String,
        prayers: String,
        nearbyFood: String,
        notes: String,
        isWishlist: Boolean
    ) -> Unit,
    onOpenAddCustomCategory: () -> Unit
) {
    val context = LocalContext.current

    val searchQuery by templeViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResultsState by templeViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by templeViewModel.isSearching.collectAsStateWithLifecycle()
    val isOnline by templeViewModel.isOnline.collectAsStateWithLifecycle()

    var showSearchResults by remember { mutableStateOf(false) }

    LaunchedEffect(initialName) {
        if (initialName.isNotBlank()) {
            templeViewModel.updateSearchQuery(initialName)
        }
    }

    var placeName by remember { mutableStateOf(initialName) }
    var latStr by remember { mutableStateOf(initialLatitude?.toString() ?: "") }
    var lngStr by remember { mutableStateOf(initialLongitude?.toString() ?: "") }

    // Default to today
    var startDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showStartDatePicker by remember { mutableStateOf(false) }

    // Photo picker (selected photo uri)
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            Toast.makeText(context, "Photo attached", Toast.LENGTH_SHORT).show()
        }
    }

    val selectedCategoryIds = remember {
        mutableStateListOf<String>().apply {
            val defaultCat = categories.firstOrNull { it.id == "cat_temples" } ?: categories.firstOrNull()
            if (defaultCat != null) add(defaultCat.id)
        }
    }

    var isWishlist by remember { mutableStateOf(false) }
    var showMoreDetails by remember { mutableStateOf(false) }

    var deity by remember { mutableStateOf("") }
    var architecture by remember { mutableStateOf("") }
    var prayers by remember { mutableStateOf("") }
    var nearbyFood by remember { mutableStateOf("") }
    var generalNotes by remember { mutableStateOf("") }

    val dateFormatter = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDateMillis = it }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Temple Visit",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // 1. Place Search Field
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            templeViewModel.updateSearchQuery(it)
                            showSearchResults = it.isNotBlank()
                        },
                        label = { Text("Search Place / Temple *") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("place_search_input")
                    )

                    if (showSearchResults) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp)
                                .heightIn(max = 300.dp)
                        ) {
                            when (val state = searchResultsState) {
                                is com.example.data.network.NetworkResult.Success -> {
                                    val results = state.data.take(8)
                                    Column(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val saved = results.filter { it.type == com.example.data.places.SearchResultType.SAVED }
                                        val temples = results.filter { it.type == com.example.data.places.SearchResultType.TEMPLE }
                                        val others = results.filter { it.type != com.example.data.places.SearchResultType.SAVED && it.type != com.example.data.places.SearchResultType.TEMPLE }

                                        if (saved.isNotEmpty()) {
                                            SearchSectionLabel("Saved")
                                            for (item in saved) {
                                                SearchResultRow(item) {
                                                    placeName = item.name
                                                    templeViewModel.updateSearchQuery(item.name)
                                                    latStr = item.latitude.toString()
                                                    lngStr = item.longitude.toString()
                                                    showSearchResults = false
                                                }
                                            }
                                        }
                                        if (temples.isNotEmpty()) {
                                            SearchSectionLabel("Temples")
                                            for (item in temples) {
                                                SearchResultRow(item) {
                                                    placeName = item.name
                                                    templeViewModel.updateSearchQuery(item.name)
                                                    latStr = item.latitude.toString()
                                                    lngStr = item.longitude.toString()
                                                    if (item.cityState != null) nearbyFood = "Try food in ${item.cityState}"
                                                    showSearchResults = false
                                                }
                                            }
                                        }
                                        if (others.isNotEmpty()) {
                                            SearchSectionLabel("More places")
                                            for (item in others) {
                                                SearchResultRow(item) {
                                                    placeName = item.name
                                                    templeViewModel.updateSearchQuery(item.name)
                                                    latStr = item.latitude.toString()
                                                    lngStr = item.longitude.toString()
                                                    showSearchResults = false
                                                }
                                            }
                                        }
                                    }
                                }
                                is com.example.data.network.NetworkResult.Empty -> {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("No places found", style = MaterialTheme.typography.bodyMedium)
                                        Text("Try a different name or location", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                is com.example.data.network.NetworkResult.Offline -> {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("You're offline, showing saved places only", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                else -> {
                                    if (isSearching) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            repeat(3) {
                                                Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Date Picker (Default Today)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Visit Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFormatter.format(Date(startDateMillis)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }

                // 3. Photo Picker
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedPhotoUri != null) "Photo Attached ✓" else "Pick from Google Photos / Gallery")
                }

                // Wishlist Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Save as Wishlist", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Switch(
                        checked = isWishlist,
                        onCheckedChange = { isWishlist = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = WishlistOrchid)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Collapsed "More details" Section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMoreDetails = !showMoreDetails }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("More details (notes, categories & temple fields)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Icon(
                            imageVector = if (showMoreDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }

                AnimatedVisibility(visible = showMoreDetails) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Categories
                        Text("Categories", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = selectedCategoryIds.contains(cat.id)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedCategoryIds.remove(cat.id)
                                        else selectedCategoryIds.add(cat.id)
                                    },
                                    label = { Text(cat.name) },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Optional Temple fields
                        OutlinedTextField(
                            value = deity,
                            onValueChange = { deity = it },
                            label = { Text("Presiding Deity (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = architecture,
                            onValueChange = { architecture = it },
                            label = { Text("Architecture (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = prayers,
                            onValueChange = { prayers = it },
                            label = { Text("Prayers / Sankalpam (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = nearbyFood,
                            onValueChange = { nearbyFood = it },
                            label = { Text("Nearby Food / Prasadam (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = generalNotes,
                            onValueChange = { generalNotes = it },
                            label = { Text("Notes (optional)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Save Button & Snackbar Toast
                Button(
                    onClick = {
                        val lat = latStr.toDoubleOrNull() ?: return@Button
                        val lng = lngStr.toDoubleOrNull() ?: return@Button
                        val finalNotes = buildString {
                            if (generalNotes.isNotBlank()) append(generalNotes)
                            if (selectedPhotoUri != null) {
                                if (isNotEmpty()) append(" · ")
                                append("PhotoUri: $selectedPhotoUri")
                            }
                        }

                        onSaveVisit(
                            placeName.ifBlank { "Temple" },
                            lat,
                            lng,
                            startDateMillis,
                            null,
                            selectedCategoryIds.toList(),
                            deity,
                            architecture,
                            prayers,
                            nearbyFood,
                            finalNotes,
                            isWishlist
                        )
                        Toast.makeText(context, "Visit saved successfully", Toast.LENGTH_SHORT).show()
                    },
                    enabled = placeName.isNotBlank() && latStr.toDoubleOrNull() != null && lngStr.toDoubleOrNull() != null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_visit_confirm_button")
                ) {
                    Text("Save", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SearchSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun SearchResultRow(result: com.example.data.places.UnifiedSearchResult, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (result.type) {
                com.example.data.places.SearchResultType.SAVED -> Icons.Default.Place
                com.example.data.places.SearchResultType.TEMPLE -> Icons.Default.Place
                com.example.data.places.SearchResultType.CITY -> Icons.Default.Place
                else -> Icons.Default.Place
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (result.type == com.example.data.places.SearchResultType.SAVED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                val locationStr = listOfNotNull(result.cityState, result.country).joinToString(", ")
                if (locationStr.isNotBlank()) {
                    Text(
                        text = locationStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
