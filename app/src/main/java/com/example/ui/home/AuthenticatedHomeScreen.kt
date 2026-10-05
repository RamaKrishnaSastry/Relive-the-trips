package com.example.ui.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.platform.LocalContext
import com.example.ui.viewmodel.ThemeMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.auth.UserProfile
import com.example.data.model.CategoryEntity
import com.example.data.model.PlaceEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.MarigoldGold
import com.example.ui.theme.ParchmentBorder
import com.example.ui.theme.SageForest
import com.example.ui.theme.SpicedCrimson
import com.example.ui.theme.TerracottaDark
import com.example.ui.theme.TerracottaLight
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.WishlistOrchid
import com.example.ui.viewmodel.TempleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedHomeScreen(
    user: UserProfile,
    templeViewModel: TempleViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var selectedVisitId by remember { mutableStateOf<String?>(null) }

    // If user opened a visit detail, show full Step 5 detail screen
    if (selectedVisitId != null) {
        com.example.ui.visits.VisitDetailScreen(
            visitId = selectedVisitId!!,
            viewModel = templeViewModel,
            onBack = { selectedVisitId = null }
        )
        return
    }

    // Step 4: Create Visit Sheet State
    var showCreateVisitSheet by remember { mutableStateOf(false) }
    var createVisitInitialLat by remember { mutableStateOf<Double?>(null) }
    var createVisitInitialLng by remember { mutableStateOf<Double?>(null) }
    var createVisitInitialName by remember { mutableStateOf("") }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out of Journal?", fontWeight = FontWeight.Bold) },
            text = { Text("All your recorded temple visits, prayers, and wishlist markers remain safely preserved in your private on-device Room database.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, colorHex ->
                templeViewModel.addCustomCategory(name = name, colorHex = colorHex)
                showAddCategoryDialog = false
            }
        )
    }

    if (showCreateVisitSheet) {
        val categories by templeViewModel.allCategories.collectAsStateWithLifecycle()
        com.example.ui.visits.CreateVisitSheet(
            initialLatitude = createVisitInitialLat,
            initialLongitude = createVisitInitialLng,
            initialName = createVisitInitialName,
            categories = categories,
            onDismiss = { showCreateVisitSheet = false },
            onSaveVisit = { name, lat, lng, start, end, catIds, deity, arch, prayers, food, notes, isWishlist ->
                templeViewModel.addFullTempleVisit(
                    name = name,
                    latitude = lat,
                    longitude = lng,
                    startDate = start,
                    endDate = end,
                    categoryIds = catIds,
                    deity = deity,
                    architecture = arch,
                    prayers = prayers,
                    nearbyFood = food,
                    notes = notes,
                    isWishlist = isWishlist,
                    userEmail = user.email
                )
                showCreateVisitSheet = false
            },
            onOpenAddCustomCategory = {
                showAddCategoryDialog = true
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MarigoldGold.copy(alpha = 0.2f))
                                .border(1.dp, MarigoldGold, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_temple_app_icon_1791221642669),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Temple Map",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                )
                            )
                            Text(
                                text = "Step 5: Visit Details & Photo Links",
                                style = MaterialTheme.typography.labelSmall,
                                color = MarigoldGold
                            )
                        }
                    }
                },
                actions = {
                    val syncState by templeViewModel.syncState.collectAsStateWithLifecycle()
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when (syncState) {
                            is com.example.sync.SyncState.Syncing -> TerracottaPrimary.copy(alpha = 0.2f)
                            is com.example.sync.SyncState.Success -> SageForest.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            when (syncState) {
                                is com.example.sync.SyncState.Success -> SageForest.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { templeViewModel.triggerDriveSyncManual(user.email) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (syncState) {
                                    is com.example.sync.SyncState.Syncing -> "☁️ Syncing..."
                                    is com.example.sync.SyncState.Success -> "☁️ Drive Synced"
                                    else -> "☁️ Drive Sync"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = when (syncState) {
                                    is com.example.sync.SyncState.Success -> SageForest
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier.testTag("topbar_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = TerracottaPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Journal") },
                    label = { Text("Journal") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TerracottaLight,
                        selectedIconColor = TerracottaDark,
                        selectedTextColor = TerracottaDark
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Map, contentDescription = "3D Globe") },
                    label = { Text("Globe") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TerracottaLight,
                        selectedIconColor = TerracottaDark,
                        selectedTextColor = TerracottaDark
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "Wishlist") },
                    label = { Text("Wishlist") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TerracottaLight,
                        selectedIconColor = TerracottaDark,
                        selectedTextColor = TerracottaDark
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = TerracottaLight,
                        selectedIconColor = TerracottaDark,
                        selectedTextColor = TerracottaDark
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> LivelyJournalTab(
                    user = user,
                    viewModel = templeViewModel,
                    onOpenAddTemple = {
                        createVisitInitialLat = null
                        createVisitInitialLng = null
                        createVisitInitialName = ""
                        showCreateVisitSheet = true
                    },
                    onOpenAddCategory = { showAddCategoryDialog = true },
                    onOpenVisitDetail = { visitId -> selectedVisitId = visitId }
                )
                1 -> {
                    val visits by templeViewModel.allVisits.collectAsStateWithLifecycle()
                    val places by templeViewModel.allPlaces.collectAsStateWithLifecycle()
                    val categories by templeViewModel.allCategories.collectAsStateWithLifecycle()
                    val defaultMapLayer by templeViewModel.defaultMapLayer.collectAsStateWithLifecycle()
                    com.example.ui.map.NativeSatelliteMap(
                        visits = visits,
                        places = places,
                        categories = categories,
                        defaultMapLayer = defaultMapLayer,
                        onRequestAddVisitAtCoordinates = { lat, lng, name ->
                            createVisitInitialLat = lat
                            createVisitInitialLng = lng
                            createVisitInitialName = name
                            showCreateVisitSheet = true
                        },
                        onOpenVisitDetail = { visitId ->
                            selectedVisitId = visitId
                        }
                    )
                }
                2 -> WishlistTab(
                    viewModel = templeViewModel,
                    onOpenAddTemple = {
                        createVisitInitialLat = null
                        createVisitInitialLng = null
                        createVisitInitialName = ""
                        showCreateVisitSheet = true
                    },
                    onOpenVisitDetail = { visitId -> selectedVisitId = visitId }
                )
                3 -> SettingsTab(
                    user = user,
                    viewModel = templeViewModel,
                    onOpenAddCategory = { showAddCategoryDialog = true },
                    onRequestSignOut = { showSignOutDialog = true }
                )
            }
        }
    }
}

@Composable
private fun LivelyJournalTab(
    user: UserProfile,
    viewModel: TempleViewModel,
    onOpenAddTemple: () -> Unit,
    onOpenAddCategory: () -> Unit,
    onOpenVisitDetail: (String) -> Unit
) {
    val visits by viewModel.allVisits.collectAsStateWithLifecycle()
    val places by viewModel.allPlaces.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val trips by viewModel.allTrips.collectAsStateWithLifecycle()
    val visitedCount by viewModel.visitedCount.collectAsStateWithLifecycle()
    val wishlistCount by viewModel.wishlistCount.collectAsStateWithLifecycle()
    val placesCount by viewModel.placesCount.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryId.collectAsStateWithLifecycle()

    var journalViewMode by remember { mutableIntStateOf(0) } // 0 = Visits, 1 = Timeline, 2 = Trips
    var showCreateTripDialog by remember { mutableStateOf(false) }

    if (showCreateTripDialog) {
        var newTripName by remember { mutableStateOf("") }
        var tripNotes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateTripDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderShared, contentDescription = null, tint = MarigoldGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Pilgrimage Trip", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Group your sacred visits into a named pilgrimage (e.g., 'South India Yatra, March 2025').", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = newTripName,
                        onValueChange = { newTripName = it },
                        label = { Text("Trip Name") },
                        placeholder = { Text("e.g. South India Pilgrimage 2025") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tripNotes,
                        onValueChange = { tripNotes = it },
                        label = { Text("Notes / Companions") },
                        placeholder = { Text("e.g. Family trip by car across Tamil Nadu temples") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTripName.isNotBlank()) {
                            viewModel.createTrip(
                                name = newTripName.trim(),
                                startDate = System.currentTimeMillis(),
                                endDate = System.currentTimeMillis() + 7 * 86400000L,
                                notes = tripNotes.ifBlank { null }
                            )
                            showCreateTripDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                ) {
                    Text("Create Trip")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTripDialog = false }) { Text("Cancel") }
            }
        )
    }

    val filteredVisits = remember(visits, selectedCategory) {
        if (selectedCategory == null) {
            visits
        } else {
            visits.filter { it.categoryIds.contains(selectedCategory) }
        }
    }

    val placesMap = remember(places) {
        places.associateBy { it.id }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial User Pilgrim Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with gold border
                if (!user.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(2.dp, MarigoldGold, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(TerracottaPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pilgrim: ${user.displayName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillBadge(
                            text = if (user.isDemoSession) "Explorer Mode" else "Google Verified",
                            color = TerracottaPrimary
                        )
                        PillBadge(
                            text = "Room DB Active",
                            color = SageForest
                        )
                    }
                }
            }
        }

        // Live Database Metrics Ribbon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Visited",
                count = visitedCount,
                icon = Icons.Default.Place,
                accentColor = TerracottaPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Wishlist",
                count = wishlistCount,
                icon = Icons.Default.Star,
                accentColor = WishlistOrchid,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Mapped Sites",
                count = placesCount,
                icon = Icons.Default.LocationOn,
                accentColor = SageForest,
                modifier = Modifier.weight(1f)
            )
        }

        // Action Toolbar: Seed Sample Temples + Add Temple
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.seedSampleData() },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("seed_temples_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MarigoldGold)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Seed 3 Temples", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = onOpenAddTemple,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("add_temple_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, TerracottaPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TerracottaPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Visit", color = TerracottaPrimary, style = MaterialTheme.typography.labelLarge)
            }
        }

        // View Mode Segmented Bar: Visits | Timeline | Trips
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (journalViewMode == 0) TerracottaPrimary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { journalViewMode = 0 }
                ) {
                    Text(
                        text = "📖 Visits",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (journalViewMode == 0) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (journalViewMode == 1) TerracottaPrimary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { journalViewMode = 1 }
                ) {
                    Text(
                        text = "⏳ Timeline",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (journalViewMode == 1) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (journalViewMode == 2) TerracottaPrimary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { journalViewMode = 2 }
                ) {
                    Text(
                        text = "🧳 Trips (${trips.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (journalViewMode == 2) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        when (journalViewMode) {
            0 -> {
                // CATEGORY FILTER + VISITS LIST
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = TerracottaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Categories (${categories.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        TextButton(onClick = onOpenAddCategory) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("New Custom", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Horizontal Pill Scroll
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryFilterPill(
                            name = "All (${visits.size})",
                            isSelected = selectedCategory == null,
                            colorHex = "#C2410C",
                            icon = Icons.Default.Place,
                            onClick = { viewModel.selectCategory(null) }
                        )

                        categories.forEach { cat ->
                            CategoryFilterPill(
                                name = cat.name,
                                isSelected = selectedCategory == cat.id,
                                colorHex = cat.colorHex,
                                icon = getCategoryIcon(cat.iconName),
                                onClick = { viewModel.selectCategory(cat.id) }
                            )
                        }
                    }
                }

                // Visits Items
                if (filteredVisits.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(TerracottaLight.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TravelExplore,
                                    contentDescription = null,
                                    tint = TerracottaPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No visits logged yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap 'Seed 3 Temples' above to populate iconic Indian pilgrimage sites or log your first sacred visit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        filteredVisits.forEach { visit ->
                            val place = placesMap[visit.placeId]
                            JournalVisitCard(
                                visit = visit,
                                place = place,
                                onDelete = { viewModel.deleteVisit(visit.id) },
                                onClick = { onOpenVisitDetail(visit.id) }
                            )
                        }
                    }
                }
            }

            1 -> {
                // CHRONOLOGICAL TIMELINE VIEW (Step 5 Requirement)
                val sortedVisits = remember(visits) { visits.sortedByDescending { it.startDate } }
                if (sortedVisits.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    ) {
                        Text(
                            text = "No visits to display on timeline. Log visits to see your pilgrimage journey unfold.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        sortedVisits.forEachIndexed { index, visit ->
                            val place = placesMap[visit.placeId]
                            val isLast = index == sortedVisits.lastIndex
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Timeline Left Stem & Node
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(36.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (visit.state == VisitState.VISITED) TerracottaPrimary else WishlistOrchid),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(if (visit.state == VisitState.VISITED) "🛕" else "★", fontSize = 11.sp)
                                    }
                                    if (!isLast) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(90.dp)
                                                .background(TerracottaPrimary.copy(alpha = 0.3f))
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Timeline Content Card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = 14.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onOpenVisitDetail(visit.id) }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = place?.name ?: "Sacred Site",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = dateFormatter.format(Date(visit.startDate)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AntiqueGold
                                            )
                                        }
                                        if (!visit.deity.isNullOrBlank()) {
                                            Text("🕉️ ${visit.deity}", style = MaterialTheme.typography.labelSmall, color = TerracottaPrimary, modifier = Modifier.padding(top = 2.dp))
                                        }
                                        if (!visit.notes.isNullOrBlank()) {
                                            Text(visit.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                                        }
                                        Text(
                                            text = "Tap to open details & photos →",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TerracottaPrimary,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // TRIPS VIEW (Step 5 Requirement)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pilgrimage Trips", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Button(
                        onClick = { showCreateTripDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ New Trip")
                    }
                }

                if (trips.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.FolderShared, contentDescription = null, tint = MarigoldGold, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Pilgrimage Trips Created Yet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "Group visits into journeys like 'South India March 2025' or 'Chardham Yatra'. Tap '+ New Trip' to start!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        val dateFormatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                        trips.forEach { trip ->
                            val tripVisits = visits.filter { it.tripId == trip.id }
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, MarigoldGold.copy(alpha = 0.5f)),
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(trip.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                dateFormatter.format(Date(trip.startDate)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = TerracottaLight
                                        ) {
                                            Text(
                                                "${tripVisits.size} Temples",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = TerracottaDark,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    if (!trip.notes.isNullOrBlank()) {
                                        Text(trip.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // List of temples in this trip
                                    if (tripVisits.isEmpty()) {
                                        Text(
                                            "No visits assigned yet. Tap any visit from the Journal or Map to assign it to this trip.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            tripVisits.forEach { tv ->
                                                val p = placesMap[tv.placeId]
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { onOpenVisitDetail(tv.id) }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("🛕 ${p?.name ?: "Temple"}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                                        Text("Details →", style = MaterialTheme.typography.labelSmall, color = TerracottaPrimary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Step Status Card
        StepFiveStatusCard()

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StepFiveStatusCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MarigoldGold.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MarigoldGold.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Step 5: Visit Details, Zero-Copy Photos & Trips",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AntiqueGold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Each visit now features full details (deity, architecture, sankalpam, nearby food, notes), zero-copy photo linking from device gallery & Google Photos, timeline tracking, and pilgrimage trips grouping.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun JournalVisitCard(
    visit: VisitEntity,
    place: PlaceEntity?,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateStr = remember(visit.startDate) {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(visit.startDate))
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("visit_card_${visit.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Temple Name + Visited / Wishlist Stamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place?.name ?: "Sacred Temple",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.2.sp
                        )
                    )
                    if (place != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TerracottaPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "%.4f° N, %.4f° E".format(place.latitude, place.longitude),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Stamp Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (visit.state == VisitState.VISITED) TerracottaLight else WishlistOrchid.copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        if (visit.state == VisitState.VISITED) TerracottaPrimary else WishlistOrchid
                    )
                ) {
                    Text(
                        text = if (visit.state == VisitState.VISITED) "✓ VISITED" else "★ WISHLIST",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = if (visit.state == VisitState.VISITED) TerracottaDark else WishlistOrchid,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Deity & Architecture Tags
            if (!visit.deity.isNullOrBlank() || !visit.architecture.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!visit.deity.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MarigoldGold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "🕉️ ${visit.deity}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = AntiqueGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (!visit.architecture.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "🏛️ ${visit.architecture}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Journal Notes
            if (!visit.notes.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“${visit.notes}”",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Footer: Date, Photo detail hint & Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Logged on $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "📷 View photos & sacred details →",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TerracottaPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryFilterPill(
    name: String,
    isSelected: Boolean,
    colorHex: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val pillColor = remember(colorHex) {
        try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            TerracottaPrimary
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) pillColor else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) pillColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else pillColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PillBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            ),
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun DataModelStatusCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MarigoldGold.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MarigoldGold.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AntiqueGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Step 2: Room Data Model Operational",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AntiqueGold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "The 5 Room entities (Place, Visit, Category, Trip, MediaLink) and relational DAOs are now live with reactive StateFlow streaming.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WishlistTab(
    viewModel: TempleViewModel,
    onOpenAddTemple: () -> Unit,
    onOpenVisitDetail: (String) -> Unit
) {
    val wishlistVisits by viewModel.wishlistVisits.collectAsStateWithLifecycle()
    val places by viewModel.allPlaces.collectAsStateWithLifecycle()
    val placesMap = remember(places) { places.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sacred Wishlist",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${wishlistVisits.size} temples to visit next",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onOpenAddTemple,
                colors = ButtonDefaults.buttonColors(containerColor = WishlistOrchid),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Wish")
            }
        }

        if (wishlistVisits.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = WishlistOrchid,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your Wishlist is Empty",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Save future pilgrimage aspirations, holy rivers, and ancient temples you dream of visiting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(wishlistVisits, key = { it.id }) { visit ->
                    val place = placesMap[visit.placeId]
                    JournalVisitCard(
                        visit = visit,
                        place = place,
                        onDelete = { viewModel.deleteVisit(visit.id) },
                        onClick = { onOpenVisitDetail(visit.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepPreviewTab(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(TerracottaLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TerracottaDark,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stepNumber,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TerracottaPrimary
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
private fun SettingsTab(
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
                    text = "Personalize theme, globe styles, cross-device sync & backups",
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

        // Section 3: Google Drive Cross-Device Sync
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
                            text = "Google Drive Cross-Device Sync",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Seamless sync across phones without photo duplication",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Travel diary entries (temple visits, prayers, deity details, and wishlist markers) are saved in lightweight JSON to your personal Google Drive app folder. Only text metadata syncs, keeping bandwidth low and your cloud storage free.",
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
                                is com.example.sync.SyncState.Syncing -> "🔄 Syncing with Google Drive..."
                                is com.example.sync.SyncState.Success -> "✓ ${(syncState as com.example.sync.SyncState.Success).message}"
                                is com.example.sync.SyncState.Offline -> "⚡ Working offline with local Room DB"
                                else -> "☁️ Auto-syncs once when app opens"
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
                        Toast.makeText(context, "Initiating Google Drive sync...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync with Google Drive Now")
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
                    text = "Generate a complete JSON backup containing all your temple visits, GPS coordinates, notes, categories, and pilgrimages to save to your device or share.",
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
                            val shareIntent = Intent.createChooser(sendIntent, "Export Temple Travel Diary")
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

@Composable
private fun ImportBackupDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Upload, contentDescription = null, tint = SageForest)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Travel Diary Backup", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste previously exported JSON travel diary text below. All records will be imported and merged safely into your local database.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    label = { Text("Paste JSON Backup") },
                    placeholder = { Text("{\"version\":1, \"places\":[...], \"visits\":[...]}") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (jsonText.isNotBlank()) onImport(jsonText.trim()) },
                enabled = jsonText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SageForest)
            ) {
                Text("Restore & Merge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#C2410C") }
    val colors = listOf("#C2410C", "#D97706", "#047857", "#1D4ED8", "#7E22CE", "#B91C1C", "#8B5CF6")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Category", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Select Category Color:", style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    if (selectedColor == hex) 3.dp else 1.dp,
                                    if (selectedColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
            ) {
                Text("Save Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "temple" -> Icons.Default.Place
        "pilgrimage" -> Icons.Default.AutoAwesome
        "family" -> Icons.Default.Home
        "friends" -> Icons.Default.Explore
        "solo" -> Icons.Default.LocationOn
        "food" -> Icons.Default.Restaurant
        "nature" -> Icons.Default.Landscape
        "work" -> Icons.Default.Work
        else -> Icons.Default.Star
    }
}
