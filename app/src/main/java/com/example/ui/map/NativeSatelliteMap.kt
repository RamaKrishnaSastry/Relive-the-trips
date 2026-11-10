package com.example.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.example.data.model.CategoryEntity
import com.example.data.model.PlaceEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import com.example.data.places.TemplePlacesCatalog
import com.example.map.MapClusterItem
import com.example.map.MapClusterer
import com.example.map.MapPin
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

enum class MapTileLayer(val title: String, val icon: String, val attribution: String) {
    SATELLITE("Satellite Earth", "🛰️", "Imagery © Esri"),
    STREET("Street / Topo", "🗺️", "© OpenStreetMap contributors"),
    DARK("Night Globe", "🌌", "© CARTO")
}

@Composable
fun NativeSatelliteMap(
    visits: List<VisitEntity>,
    places: List<PlaceEntity>,
    categories: List<CategoryEntity>,
    defaultMapLayer: String = "SATELLITE",
    onRequestAddVisitAtCoordinates: (lat: Double, lng: Double, name: String) -> Unit,
    onOpenVisitDetail: (visitId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var centerLat by remember { mutableDoubleStateOf(18.5) }
    var centerLon by remember { mutableDoubleStateOf(78.5) }
    var zoomLevel by remember { mutableFloatStateOf(5.0f) }

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
    var searchQuery by remember { mutableStateOf("") }
    var showSearchSuggestions by remember { mutableStateOf(false) }

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

    val searchResults = remember(searchQuery) {
        if (searchQuery.isNotBlank()) TemplePlacesCatalog.searchTemples(searchQuery) else emptyList()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("native_satellite_map_container")
    ) {
        val widthPx = with(density) { maxWidth.toPx().toDouble() }
        val heightPx = with(density) { maxHeight.toPx().toDouble() }
        val centerX = widthPx / 2.0
        val centerY = heightPx / 2.0

        val intZoom = zoomLevel.roundToInt().coerceIn(2, 16)
        val worldScale = 2.0.pow(intZoom.toDouble()) * 256.0
        val tileScale = 2.0.pow((zoomLevel - intZoom).toDouble())
        val tileDisplayPx = 256.0 * tileScale
        val tileDisplayDp = with(density) { ceil(tileDisplayPx).toFloat().toDp() }

        val (centerWorldX, centerWorldY) = MapProjection.latLonToWorld(centerLat, centerLon, intZoom.toFloat())

        val halfW = (widthPx / 2.0) / tileScale
        val halfH = (heightPx / 2.0) / tileScale
        val leftWorldX = centerWorldX - halfW
        val rightWorldX = centerWorldX + halfW
        val topWorldY = centerWorldY - halfH
        val bottomWorldY = centerWorldY + halfH

        val maxTiles = 1 shl intZoom
        val minTx = floor(leftWorldX / 256.0).toInt()
        val maxTx = floor(rightWorldX / 256.0).toInt()
        val minTy = floor(topWorldY / 256.0).toInt().coerceIn(0, maxTiles - 1)
        val maxTy = floor(bottomWorldY / 256.0).toInt().coerceIn(0, maxTiles - 1)

        // Full-bleed map tiles & gestures surface
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
                            zoomLevel = (zoomLevel + logZoom).coerceIn(2.5f, 16.0f)
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
                            zoomLevel = (zoomLevel + 1.0f).coerceAtMost(16.0f)
                        },
                        onTap = { tapOffset ->
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
                    )
                }
        ) {
            for (ty in minTy..maxTy) {
                for (rawTx in minTx..maxTx) {
                    val tx = ((rawTx % maxTiles) + maxTiles) % maxTiles
                    val tileUrl = when (activeLayer) {
                        MapTileLayer.SATELLITE -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$intZoom/$ty/$tx"
                        MapTileLayer.STREET -> "https://tile.openstreetmap.org/$intZoom/$tx/$ty.png"
                        MapTileLayer.DARK -> "https://basemaps.cartocdn.com/rastertiles/dark_all/$intZoom/$tx/$ty.png"
                    }

                    val tileScreenX = centerX + (rawTx * 256.0 - centerWorldX) * tileScale
                    val tileScreenY = centerY + (ty * 256.0 - centerWorldY) * tileScale

                    key("$activeLayer-$intZoom-$rawTx-$ty") {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(tileUrl).crossfade(100).build(),
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

            // Pins and clusters
            clusterItems.forEach { item ->
                when (item) {
                    is MapClusterItem.Cluster -> {
                        val (screenX, screenY) = MapProjection.latLonToScreen(
                            item.latitude, item.longitude, centerLat, centerLon, zoomLevel, widthPx, heightPx
                        )
                        val size = 40.dp
                        val halfSizePx = with(density) { (size.toPx() / 2f).roundToInt() }
                        if (screenX in -50.0..(widthPx + 50.0) && screenY in -50.0..(heightPx + 50.0)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                tonalElevation = 4.dp,
                                modifier = Modifier
                                    .offset { IntOffset(screenX.roundToInt() - halfSizePx, screenY.roundToInt() - halfSizePx) }
                                    .size(size)
                                    .clickable {
                                        centerLat = item.latitude
                                        centerLon = item.longitude
                                        zoomLevel = (zoomLevel + 2.0f).coerceAtMost(16.0f)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = item.count.toString(),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
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
                        val size = 36.dp
                        val halfSizePx = with(density) { (size.toPx() / 2f).roundToInt() }
                        if (screenX in -50.0..(widthPx + 50.0) && screenY in -50.0..(heightPx + 50.0)) {
                            Surface(
                                shape = CircleShape,
                                color = if (pin.isWishlist) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                tonalElevation = 6.dp,
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier
                                    .offset { IntOffset(screenX.roundToInt() - halfSizePx, screenY.roundToInt() - halfSizePx) }
                                    .size(size)
                                    .clickable {
                                        selectedPin = pin
                                        droppedCoordinates = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (pin.isWishlist) "★" else "🛕",
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top Overlay: Floating rounded search pill & filter chips row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Floating rounded search pill
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
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
                            searchQuery = it
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
                        IconButton(onClick = { searchQuery = ""; showSearchSuggestions = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Search suggestions dropdown
            AnimatedVisibility(visible = showSearchSuggestions && searchResults.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        searchResults.take(3).forEach { temple ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        centerLat = temple.latitude
                                        centerLon = temple.longitude
                                        zoomLevel = 13.0f
                                        searchQuery = temple.name
                                        showSearchSuggestions = false
                                    }
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(temple.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                }
            }

            // Row of filter chips: All, Visited, Wishlist, plus categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeFilter == "ALL",
                    onClick = { activeFilter = "ALL" },
                    label = { Text("All") },
                    shape = RoundedCornerShape(16.dp)
                )
                FilterChip(
                    selected = activeFilter == "VISITED",
                    onClick = { activeFilter = "VISITED" },
                    label = { Text("Visited") },
                    shape = RoundedCornerShape(16.dp)
                )
                FilterChip(
                    selected = activeFilter == "WISHLIST",
                    onClick = { activeFilter = "WISHLIST" },
                    label = { Text("Wishlist") },
                    shape = RoundedCornerShape(16.dp)
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = activeFilter == cat.id,
                        onClick = { activeFilter = cat.id },
                        label = { Text(cat.name) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        // Single + button at bottom-end floating above map
        FloatingActionButton(
            onClick = { onRequestAddVisitAtCoordinates(centerLat, centerLon, "") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add visit", modifier = Modifier.size(24.dp))
        }

        // Tapping a pin opens a half-height bottom sheet with cover photo, place name and date, and an "Open" button
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
                        // Cover photo placeholder / image
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
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
    }
}
