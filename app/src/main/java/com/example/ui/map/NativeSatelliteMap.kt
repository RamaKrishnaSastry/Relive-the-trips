package com.example.ui.map

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.example.data.model.CategoryEntity
import com.example.data.model.PlaceEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import com.example.data.network.NetworkResult
import com.example.data.places.DiscoveredPlace
import com.example.data.places.SearchResultType
import com.example.data.places.UnifiedSearchResult
import com.example.map.MapClusterItem
import com.example.map.MapClusterer
import com.example.map.MapPin
import com.example.ui.viewmodel.TempleViewModel
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

enum class MapTileLayer(val title: String) {
    SATELLITE("Satellite Earth"),
    STREET("Street / Topo")
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NativeSatelliteMap(
    visits: List<VisitEntity>,
    places: List<PlaceEntity>,
    categories: List<CategoryEntity>,
    defaultMapLayer: String = "SATELLITE",
    onRequestAddVisitAtCoordinates: (lat: Double, lng: Double, name: String) -> Unit,
    onOpenVisitDetail: (visitId: String) -> Unit,
    templeViewModel: TempleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val isOnline by templeViewModel.isOnline.collectAsStateWithLifecycle()

    var centerLat by remember { mutableDoubleStateOf(templeViewModel.mapLat.value) }
    var centerLon by remember { mutableDoubleStateOf(templeViewModel.mapLon.value) }
    var zoomLevel by remember { mutableFloatStateOf(templeViewModel.zoomLevel.value) }

    LaunchedEffect(centerLat, centerLon, zoomLevel) {
        templeViewModel.saveMapState(centerLat, centerLon, zoomLevel)
    }

    var isPinMode by remember { mutableStateOf(false) }
    var pinReverseName by remember { mutableStateOf("") }

    LaunchedEffect(isPinMode, centerLat, centerLon) {
        if (isPinMode) {
            kotlinx.coroutines.delay(500)
            pinReverseName = templeViewModel.reverseGeocode(centerLat, centerLon)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    centerLat = loc.latitude
                    centerLon = loc.longitude
                    zoomLevel = 15f
                } else {
                    Toast.makeText(context, "Location unavailable", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not obtain location", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    val tileImageLoader = remember(context) {
        val cacheDir = File(context.cacheDir, "map_tile_cache")
        val okHttpClient = OkHttpClient.Builder()
            .cache(Cache(cacheDir, 50L * 1024L * 1024L))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "TempleMap/1.0 (Android)")
                    .build()
                chain.proceed(request)
            }
            .build()

        ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.20).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir).maxSizeBytes(50L * 1024L * 1024L).build() }
            .build()
    }

    val initialMapLayer = remember(defaultMapLayer) {
        try { MapTileLayer.valueOf(defaultMapLayer) } catch (e: Exception) { MapTileLayer.SATELLITE }
    }
    var activeLayer by remember(initialMapLayer) { mutableStateOf(initialMapLayer) }

    var activeFilter by remember { mutableStateOf("ALL") }
    var selectedPin by remember { mutableStateOf<MapPin?>(null) }
    var droppedCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val searchQuery by templeViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResultsState by templeViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by templeViewModel.isSearching.collectAsStateWithLifecycle()

    var selectedSearchResult: UnifiedSearchResult? by remember { mutableStateOf<UnifiedSearchResult?>(null) }
    var showSearchSuggestions by remember { mutableStateOf(false) }

    val discoveredPlaces by templeViewModel.discoveredPlaces.collectAsStateWithLifecycle()
    val religionFilter by templeViewModel.religionFilter.collectAsStateWithLifecycle()
    val isDiscovering by templeViewModel.isDiscovering.collectAsStateWithLifecycle()
    val discoveryResult by templeViewModel.discoveryResult.collectAsStateWithLifecycle()

    var showSearchThisArea by remember { mutableStateOf(false) }
    var lastSearchedLocation by remember { mutableStateOf<Triple<Double, Double, Float>?>(null) }
    var selectedDiscoveredPlace by remember { mutableStateOf<DiscoveredPlace?>(null) }

    LaunchedEffect(centerLat, centerLon, zoomLevel) {
        if (zoomLevel >= 11f && !isPinMode) {
            val moved = lastSearchedLocation?.let { (lat, lon, zoom) ->
                Math.abs(lat - centerLat) > 0.005 || Math.abs(lon - centerLon) > 0.005 || Math.abs(zoom - zoomLevel) > 0.5
            } ?: true
            showSearchThisArea = moved
        } else {
            showSearchThisArea = false
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(discoveryResult) {
        when (discoveryResult) {
            is NetworkResult.Error -> {
                snackbarHostState.showSnackbar((discoveryResult as NetworkResult.Error).message)
                templeViewModel.clearDiscoveryResult()
            }
            is NetworkResult.Offline -> {
                snackbarHostState.showSnackbar("Offline: results may be limited")
                templeViewModel.clearDiscoveryResult()
            }
            else -> {}
        }
    }

    val visibleDiscoveredPlaces = remember(discoveredPlaces, religionFilter) {
        if (religionFilter == null) discoveredPlaces
        else discoveredPlaces.filter { it.religion?.contains(religionFilter!!, ignoreCase = true) == true }
    }

    val placesMap = remember(places) { places.associateBy { it.id } }
    val visitsMap = remember(visits) { visits.associateBy { it.id } }

    val allPins = remember(visits, placesMap) {
        visits.mapNotNull { visit ->
            val place = placesMap[visit.placeId] ?: return@mapNotNull null
            MapPin(
                id = visit.id,
                placeId = place.id,
                visitId = visit.id,
                name = place.name,
                latitude = place.latitude,
                longitude = place.longitude,
                isWishlist = visit.state == VisitState.WISHLIST,
                categoryIds = visit.categoryIds,
                deity = visit.deity,
                architecture = visit.architecture,
                notes = visit.notes,
                startDate = visit.startDate
            )
        }
    }

    val filteredPins = remember(allPins, activeFilter) {
        when (activeFilter) {
            "ALL" -> allPins
            "VISITED" -> allPins.filter { !it.isWishlist }
            "WISHLIST" -> allPins.filter { it.isWishlist }
            else -> allPins.filter { it.categoryIds.contains(activeFilter) }
        }
    }

    val clusterItems = remember(filteredPins, zoomLevel) {
        MapClusterer.clusterPins(filteredPins, zoomLevel)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("native_satellite_map_container")
    ) {
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp))
        val widthPx = with(density) { maxWidth.toPx().toDouble() }
        val heightPx = with(density) { maxHeight.toPx().toDouble() }
        val centerX = widthPx / 2.0
        val centerY = heightPx / 2.0

        val intZoom = zoomLevel.roundToInt().coerceIn(2, 19)
        val tileZoomInt = intZoom.coerceAtMost(18)
        val tileScale = 2.0.pow((zoomLevel - tileZoomInt).toDouble())
        val tileDisplayPx = 256.0 * tileScale
        val tileDisplayDp = with(density) { ceil(tileDisplayPx).toFloat().toDp() }

        val (centerWorldX, centerWorldY) = MapProjection.latLonToWorld(centerLat, centerLon, tileZoomInt.toFloat())

        val halfW = (widthPx / 2.0) / tileScale
        val halfH = (heightPx / 2.0) / tileScale
        val leftWorldX = centerWorldX - halfW
        val rightWorldX = centerWorldX + halfW
        val topWorldY = centerWorldY - halfH
        val bottomWorldY = centerWorldY + halfH

        val maxTiles = 1 shl tileZoomInt
        val minTx = floor(leftWorldX / 256.0).toInt()
        val maxTx = floor(rightWorldX / 256.0).toInt()
        val minTy = floor(topWorldY / 256.0).toInt().coerceIn(0, maxTiles - 1)
        val maxTy = floor(bottomWorldY / 256.0).toInt().coerceIn(0, maxTiles - 1)

        // Map surface with pan, zoom, double tap, and long press gestures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (pan.x != 0f || pan.y != 0f) {
                            val (newLat, newLon) = MapProjection.panCenter(
                                panDeltaX = pan.x.toDouble(),
                                panDeltaY = pan.y.toDouble(),
                                centerLat = centerLat,
                                centerLon = centerLon,
                                zoomLevel = zoomLevel
                            )
                            centerLat = newLat
                            centerLon = newLon
                        }
                        if (zoom != 1.0f) {
                            val logZoom = log2(zoom.toDouble()).toFloat()
                            zoomLevel = (zoomLevel + logZoom).coerceIn(2.5f, 19.0f)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            val targetLatLon = MapProjection.screenToLatLon(
                                screenX = tapOffset.x.toDouble(),
                                screenY = tapOffset.y.toDouble(),
                                centerLat = centerLat,
                                centerLon = centerLon,
                                zoomLevel = zoomLevel,
                                screenWidth = widthPx,
                                screenHeight = heightPx
                            )
                            centerLat = (centerLat + targetLatLon.first) / 2.0
                            centerLon = (centerLon + targetLatLon.second) / 2.0
                            zoomLevel = (zoomLevel + 1.0f).coerceAtMost(19.0f)
                        },
                        onLongPress = { tapOffset ->
                            val (tappedLat, tappedLon) = MapProjection.screenToLatLon(
                                screenX = tapOffset.x.toDouble(),
                                screenY = tapOffset.y.toDouble(),
                                centerLat = centerLat,
                                centerLon = centerLon,
                                zoomLevel = zoomLevel,
                                screenWidth = widthPx,
                                screenHeight = heightPx
                            )
                            droppedCoordinates = tappedLat to tappedLon
                            selectedPin = null
                        },
                        onTap = { tapOffset ->
                            if (!isPinMode) {
                                val (tappedLat, tappedLon) = MapProjection.screenToLatLon(
                                    screenX = tapOffset.x.toDouble(),
                                    screenY = tapOffset.y.toDouble(),
                                    centerLat = centerLat,
                                    centerLon = centerLon,
                                    zoomLevel = zoomLevel,
                                    screenWidth = widthPx,
                                    screenHeight = heightPx
                                )
                                droppedCoordinates = tappedLat to tappedLon
                                selectedPin = null
                            }
                        }
                    )
                }
        ) {
            // Render Map Tiles
            for (ty in minTy..maxTy) {
                for (rawTx in minTx..maxTx) {
                    val tx = ((rawTx % maxTiles) + maxTiles) % maxTiles
                    val tileUrl = when (activeLayer) {
                        MapTileLayer.SATELLITE -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$tileZoomInt/$ty/$tx"
                        MapTileLayer.STREET -> "https://tile.openstreetmap.org/$tileZoomInt/$tx/$ty.png"
                    }

                    val tileScreenX = centerX + (rawTx * 256.0 - centerWorldX) * tileScale
                    val tileScreenY = centerY + (ty * 256.0 - centerWorldY) * tileScale

                    key("$activeLayer-$tileZoomInt-$rawTx-$ty") {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(tileUrl)
                                .apply {
                                    if (!isOnline) {
                                        networkCachePolicy(coil.request.CachePolicy.DISABLED)
                                    }
                                }
                                .crossfade(100)
                                .build(),
                            imageLoader = tileImageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .size(tileDisplayDp)
                                .offset { IntOffset(tileScreenX.roundToInt(), tileScreenY.roundToInt()) }
                        )
                    }
                }
            }

            // Render Pins and Clusters (Anchored Bottom-Center)
            clusterItems.forEach { item ->
                when (item) {
                    is MapClusterItem.Cluster -> {
                        val (screenX, screenY) = MapProjection.latLonToScreen(
                            item.latitude, item.longitude, centerLat, centerLon, zoomLevel, widthPx, heightPx
                        )
                        val size = 42.dp
                        val halfSizePx = with(density) { (size.toPx() / 2f).roundToInt() }
                        if (screenX in -50.0..(widthPx + 50.0) && screenY in -50.0..(heightPx + 50.0)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                tonalElevation = 6.dp,
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier
                                    .offset { IntOffset(screenX.roundToInt() - halfSizePx, screenY.roundToInt() - halfSizePx) }
                                    .size(size)
                                    .clickable {
                                        centerLat = item.latitude
                                        centerLon = item.longitude
                                        zoomLevel = (zoomLevel + 2.5f).coerceAtMost(19.0f)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = item.count.toString(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                }
                            }
                        }
                    }
                    is MapClusterItem.SinglePin -> {
                        val pin = item.pin
                        val (screenX, screenY) = MapProjection.latLonToScreen(
                            pin.latitude, pin.longitude, centerLat, centerLon, zoomLevel, widthPx, heightPx
                        )
                        val pinWidthPx = with(density) { 32.dp.toPx().roundToInt() }
                        val pinHeightPx = with(density) { 42.dp.toPx().roundToInt() }
                        if (screenX in -50.0..(widthPx + 50.0) && screenY in -50.0..(heightPx + 50.0)) {
                            Surface(
                                shape = CircleShape,
                                color = if (pin.isWishlist) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                tonalElevation = 6.dp,
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier
                                    .offset { IntOffset(screenX.roundToInt() - (pinWidthPx / 2), screenY.roundToInt() - pinHeightPx) }
                                    .size(36.dp)
                                    .clickable {
                                        selectedPin = pin
                                        droppedCoordinates = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (pin.isWishlist) Icons.Default.Star else Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Render Dropped Pin if present
            droppedCoordinates?.let { (dLat, dLon) ->
                val (screenX, screenY) = MapProjection.latLonToScreen(
                    dLat, dLon, centerLat, centerLon, zoomLevel, widthPx, heightPx
                )
                val pinWidthPx = with(density) { 36.dp.toPx().roundToInt() }
                val pinHeightPx = with(density) { 44.dp.toPx().roundToInt() }
                if (screenX in -50.0..(widthPx + 50.0) && screenY in -50.0..(heightPx + 50.0)) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Dropped Pin",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .offset { IntOffset(screenX.roundToInt() - (pinWidthPx / 2), screenY.roundToInt() - pinHeightPx) }
                            .size(36.dp)
                    )
                }
            }

            // Render Discovered Places Markers
            visibleDiscoveredPlaces.forEach { dp ->
                val (screenX, screenY) = MapProjection.latLonToScreen(
                    dp.latitude, dp.longitude, centerLat, centerLon, zoomLevel, widthPx, heightPx
                )
                val size = 28.dp
                val halfWidthPx = with(density) { (size.toPx() / 2f).roundToInt() }
                val heightPxInt = with(density) { size.toPx().roundToInt() }
                if (screenX in -50.0..(widthPx + 50.0) && screenY in -50.0..(heightPx + 50.0)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 4.dp,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .offset { IntOffset(screenX.roundToInt() - halfWidthPx, screenY.roundToInt() - heightPxInt) }
                            .size(size)
                            .clickable {
                                selectedDiscoveredPlace = dp
                                selectedSearchResult = null
                                selectedPin = null
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Discovered Place",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Search Result Pin
            selectedSearchResult?.let { result ->
                val (screenX, screenY) = MapProjection.latLonToScreen(
                    result.latitude, result.longitude, centerLat, centerLon, zoomLevel, widthPx, heightPx
                )
                val size = 44.dp
                val halfSizePx = with(density) { (size.toPx() / 2f).roundToInt() }
                if (screenX in -50.0..(widthPx + 50.0) && screenY in -50.0..(heightPx + 50.0)) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Selected Search Result",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .offset { IntOffset(screenX.roundToInt() - halfSizePx, screenY.roundToInt() - halfSizePx - with(density) { 20.dp.toPx().toInt() }) }
                            .size(size)
                    )
                }
            }

            // Fixed Crosshair Pin in Pin Mode (at exact center)
            if (isPinMode) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset(y = (-20).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Pin Mode Center",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // Top Search Pill & Filter Chips (Hidden during Pin Mode)
        if (!isPinMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                templeViewModel.updateSearchQuery(it, centerLat, centerLon)
                                showSearchSuggestions = it.isNotBlank()
                            },
                            placeholder = { Text("Search places...", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { templeViewModel.updateSearchQuery("", centerLat, centerLon); showSearchSuggestions = false }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Suggestions Dropdown
                AnimatedVisibility(visible = showSearchSuggestions) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                    ) {
                        when (val state = searchResultsState) {
                            is NetworkResult.Success -> {
                                val results = state.data.take(8)
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val saved = results.filter { it.type == SearchResultType.SAVED }
                                    val temples = results.filter { it.type == SearchResultType.TEMPLE }
                                    val others = results.filter { it.type != SearchResultType.SAVED && it.type != SearchResultType.TEMPLE }

                                    if (saved.isNotEmpty()) {
                                        SearchSectionLabel("Saved")
                                        for (item in saved) {
                                            SearchResultRow(item) {
                                                selectedSearchResult = item
                                                centerLat = item.latitude
                                                centerLon = item.longitude
                                                zoomLevel = 15f
                                                showSearchSuggestions = false
                                            }
                                        }
                                    }
                                    if (temples.isNotEmpty()) {
                                        SearchSectionLabel("Temples")
                                        for (item in temples) {
                                            SearchResultRow(item) {
                                                selectedSearchResult = item
                                                centerLat = item.latitude
                                                centerLon = item.longitude
                                                zoomLevel = 15f
                                                showSearchSuggestions = false
                                            }
                                        }
                                    }
                                    if (others.isNotEmpty()) {
                                        SearchSectionLabel("More places")
                                        for (item in others) {
                                            SearchResultRow(item) {
                                                selectedSearchResult = item
                                                centerLat = item.latitude
                                                centerLon = item.longitude
                                                zoomLevel = 15f
                                                showSearchSuggestions = false
                                            }
                                        }
                                    }
                                }
                            }
                            is NetworkResult.Empty -> {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("No places found", style = MaterialTheme.typography.bodyMedium)
                                    Text("Try a different name or location", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            is NetworkResult.Offline -> {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("You're offline, showing saved places only", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            else -> {
                                if (isSearching) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        repeat(3) {
                                            Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = (religionFilter == "hindu"),
                        onClick = { templeViewModel.setReligionFilter(if (religionFilter == "hindu") null else "hindu") },
                        label = { Text("Hindu / All") },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = religionFilter == "hindu", borderWidth = 1.dp),
                        modifier = Modifier.padding(bottom = 2.dp).shadow(2.dp, RoundedCornerShape(16.dp))
                    )

                    VerticalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    FilterChip(
                        selected = (activeFilter == "ALL"),
                        onClick = { activeFilter = "ALL" },
                        enabled = true,
                        label = { Text("All") },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = activeFilter == "ALL", borderWidth = 1.dp),
                        modifier = Modifier.padding(bottom = 2.dp).shadow(2.dp, RoundedCornerShape(16.dp))
                    )
                    FilterChip(
                        selected = activeFilter == "VISITED",
                        onClick = { activeFilter = "VISITED" },
                        enabled = true,
                        label = { Text("Visited") },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = activeFilter == "VISITED", borderWidth = 1.dp),
                        modifier = Modifier.padding(bottom = 2.dp).shadow(2.dp, RoundedCornerShape(16.dp))
                    )
                    FilterChip(
                        selected = activeFilter == "WISHLIST",
                        onClick = { activeFilter = "WISHLIST" },
                        enabled = true,
                        label = { Text("Wishlist") },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = activeFilter == "WISHLIST", borderWidth = 1.dp),
                        modifier = Modifier.padding(bottom = 2.dp).shadow(2.dp, RoundedCornerShape(16.dp))
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = activeFilter == cat.id,
                            onClick = { activeFilter = cat.id },
                            enabled = true,
                            label = { Text(cat.name) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = activeFilter == cat.id, borderWidth = 1.dp),
                            modifier = Modifier.padding(bottom = 2.dp).shadow(2.dp, RoundedCornerShape(16.dp))
                        )
                    }
                }

                // Search This Area Button
                AnimatedVisibility(
                    visible = showSearchThisArea,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Button(
                        onClick = {
                            val (swLat, swLon) = MapProjection.worldToLatLon(leftWorldX, bottomWorldY, tileZoomInt.toFloat())
                            val (neLat, neLon) = MapProjection.worldToLatLon(rightWorldX, topWorldY, tileZoomInt.toFloat())
                            templeViewModel.discoverArea(swLat, swLon, neLat, neLon)
                            lastSearchedLocation = Triple(centerLat, centerLon, zoomLevel)
                            showSearchThisArea = false
                        },
                        shape = CircleShape,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        if (isDiscovering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Searching...", style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search this area", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Floating Action Buttons (Map Controls)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Layer Switch Button
            FloatingActionButton(
                onClick = {
                    val next = if (activeLayer == MapTileLayer.SATELLITE) MapTileLayer.STREET else MapTileLayer.SATELLITE
                    activeLayer = next
                    templeViewModel.setDefaultMapLayer(next.name)
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(44.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (activeLayer == MapTileLayer.SATELLITE) Icons.Default.Map else Icons.Default.Satellite,
                    contentDescription = "Map Style",
                    modifier = Modifier.size(20.dp)
                )
            }

            // My Location Button
            FloatingActionButton(
                onClick = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(20.dp))
            }

            // Pin Mode Toggle / Confirm Button
            FloatingActionButton(
                onClick = {
                    if (!isPinMode) {
                        isPinMode = true
                    } else {
                        onRequestAddVisitAtCoordinates(centerLat, centerLon, pinReverseName)
                        isPinMode = false
                    }
                },
                containerColor = if (isPinMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isPinMode) Icons.Default.CenterFocusStrong else Icons.Default.Add,
                    contentDescription = "Pin Mode",
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Pin Mode Bottom Card
        AnimatedVisibility(
            visible = isPinMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pinReverseName.ifBlank { "Selected Location" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "%.6f, %.6f".format(centerLat, centerLon),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Fine Adjustment Nudge Arrow Buttons (~5 meters delta)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(onClick = { centerLat += 0.000045 }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Nudge Up", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { centerLat -= 0.000045 }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Nudge Down", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { centerLon -= 0.000045 }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Nudge Left", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { centerLon += 0.000045 }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Nudge Right", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                onRequestAddVisitAtCoordinates(centerLat, centerLon, pinReverseName)
                                isPinMode = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Confirm")
                        }
                        OutlinedButton(
                            onClick = { isPinMode = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        // Discovered Place Selection Bottom Sheet
        AnimatedVisibility(
            visible = selectedDiscoveredPlace != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedDiscoveredPlace?.let { result ->
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = listOfNotNull(result.religion, result.denomination).joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { selectedDiscoveredPlace = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    onRequestAddVisitAtCoordinates(result.latitude, result.longitude, result.name)
                                    selectedDiscoveredPlace = null
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add visit")
                            }
                            OutlinedButton(
                                onClick = {
                                    templeViewModel.addFullTempleVisit(
                                        name = result.name,
                                        latitude = result.latitude,
                                        longitude = result.longitude,
                                        startDate = System.currentTimeMillis(),
                                        endDate = null,
                                        categoryIds = emptyList(),
                                        deity = result.religion ?: "",
                                        architecture = result.denomination ?: "",
                                        prayers = "",
                                        nearbyFood = "",
                                        notes = "Discovered via Overpass",
                                        isWishlist = true
                                    )
                                    selectedDiscoveredPlace = null
                                    Toast.makeText(context, "Added to wishlist", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Text("Wishlist")
                            }
                        }
                    }
                }
            }
        }

        // Tapping a pin opens a half-height bottom sheet
        AnimatedVisibility(
            visible = selectedPin != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedPin?.let { pin ->
                val visit = visitsMap[pin.visitId]
                val dateStr = remember(visit?.startDate) {
                    if (visit != null) SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(visit.startDate)) else ""
                }

                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pin.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                if (dateStr.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { selectedPin = null },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Button(
                            onClick = {
                                val vid = pin.visitId
                                selectedPin = null
                                if (vid != null) onOpenVisitDetail(vid)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Open", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Search Result Selection Bottom Sheet
        AnimatedVisibility(
            visible = selectedSearchResult != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedSearchResult?.let { result ->
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = result.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { selectedSearchResult = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    onRequestAddVisitAtCoordinates(result.latitude, result.longitude, result.name)
                                    selectedSearchResult = null
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add visit")
                            }
                            OutlinedButton(
                                onClick = {
                                    templeViewModel.addFullTempleVisit(
                                        name = result.name,
                                        latitude = result.latitude,
                                        longitude = result.longitude,
                                        startDate = System.currentTimeMillis(),
                                        endDate = null,
                                        categoryIds = emptyList(),
                                        deity = "",
                                        architecture = "",
                                        prayers = "",
                                        nearbyFood = "",
                                        notes = "Quick saved from search",
                                        isWishlist = true
                                    )
                                    selectedSearchResult = null
                                    Toast.makeText(context, "Added to wishlist", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Text("Wishlist")
                            }
                        }
                    }
                }
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
private fun SearchResultRow(result: UnifiedSearchResult, onClick: () -> Unit) {
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
                SearchResultType.SAVED -> Icons.Default.Place
                SearchResultType.TEMPLE -> Icons.Default.Place
                SearchResultType.CITY -> Icons.Default.Place
                else -> Icons.Default.Place
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (result.type == SearchResultType.SAVED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
