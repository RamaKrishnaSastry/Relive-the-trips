package com.example.ui.map

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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.model.CategoryEntity
import com.example.data.model.PlaceEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import com.example.data.places.TemplePlacesCatalog
import com.example.map.MapClusterItem
import com.example.map.MapClusterer
import com.example.map.MapPin
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.MarigoldGold
import com.example.ui.theme.TerracottaDark
import com.example.ui.theme.TerracottaLight
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.WishlistOrchid
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

enum class MapTileLayer(val title: String, val icon: String) {
    SATELLITE("Satellite Earth", "🛰️"),
    STREET("Street / Topo", "🗺️"),
    DARK("Night Globe", "🌌")
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

    // Smooth floating point coordinates for 60/120fps fluid gestures
    var centerLat by remember { mutableDoubleStateOf(18.5) }
    var centerLon by remember { mutableDoubleStateOf(78.5) }
    var zoomLevel by remember { mutableFloatStateOf(5.0f) }

    val initialMapLayer = remember(defaultMapLayer) {
        try {
            MapTileLayer.valueOf(defaultMapLayer)
        } catch (e: Exception) {
            MapTileLayer.SATELLITE
        }
    }
    var activeLayer by remember(initialMapLayer) { mutableStateOf(initialMapLayer) }
    var showLayerMenu by remember { mutableStateOf(false) }

    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedPin by remember { mutableStateOf<MapPin?>(null) }
    var droppedCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var showSearchSuggestions by remember { mutableStateOf(false) }

    val placesMap = remember(places) { places.associateBy { it.id } }

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

    val filteredPins = remember(allPins, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) allPins
        else allPins.filter { it.categoryIds.contains(selectedCategoryFilter) }
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
            .background(Color(0xFF090D18))
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

        val centerWorldX = ((centerLon + 180.0) / 360.0) * worldScale
        val centerLatRad = Math.toRadians(centerLat.coerceIn(-85.0511, 85.0511))
        val centerWorldY = ((1.0 - ln(tan(Math.PI / 4.0 + centerLatRad / 2.0)) / Math.PI) / 2.0) * worldScale

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

        // Interactive Fluid Gesture Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val currentWorldScale = 2.0.pow(zoomLevel.toDouble()) * 256.0
                        val dLon = (pan.x / currentWorldScale) * 360.0
                        val latRad = Math.toRadians(centerLat.coerceIn(-80.0, 80.0))
                        val dLat = (pan.y / currentWorldScale) * 360.0 * cos(latRad)

                        var newLon = centerLon - dLon
                        while (newLon > 180.0) newLon -= 360.0
                        while (newLon < -180.0) newLon += 360.0
                        centerLon = newLon

                        centerLat = (centerLat + dLat).coerceIn(-80.0, 80.0)

                        if (zoom != 1.0f) {
                            zoomLevel = (zoomLevel * zoom).coerceIn(2.5f, 16.0f)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val currentWorldScale = 2.0.pow(zoomLevel.toDouble()) * 256.0
                        val dX = tapOffset.x - centerX
                        val dY = tapOffset.y - centerY
                        val tappedLon = centerLon + (dX / currentWorldScale) * 360.0
                        val latRad = Math.toRadians(centerLat.coerceIn(-80.0, 80.0))
                        val tappedLat = centerLat - (dY / currentWorldScale) * 360.0 / cos(latRad)

                        droppedCoordinates = tappedLat.coerceIn(-80.0, 80.0) to tappedLon
                        selectedPin = null
                    }
                }
        ) {
            // Render Visible Map Tiles
            for (ty in minTy..maxTy) {
                for (rawTx in minTx..maxTx) {
                    val tx = ((rawTx % maxTiles) + maxTiles) % maxTiles

                    val tileUrl = when (activeLayer) {
                        MapTileLayer.SATELLITE ->
                            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$intZoom/$ty/$tx"
                        MapTileLayer.STREET ->
                            "https://tile.openstreetmap.org/$intZoom/$tx/$ty.png"
                        MapTileLayer.DARK ->
                            "https://basemaps.cartocdn.com/rastertiles/dark_all/$intZoom/$tx/$ty.png"
                    }

                    val tileScreenX = centerX + (rawTx * 256.0 - centerWorldX) * tileScale
                    val tileScreenY = centerY + (ty * 256.0 - centerWorldY) * tileScale

                    key("$activeLayer-$intZoom-$rawTx-$ty") {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(tileUrl)
                                .crossfade(100)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .size(tileDisplayDp)
                                .offset {
                                    IntOffset(tileScreenX.roundToInt(), tileScreenY.roundToInt())
                                }
                        )
                    }
                }
            }

            // Dropped Location Marker (if user tapped map)
            droppedCoordinates?.let { (lat, lng) ->
                val markerWorldX = ((lng + 180.0) / 360.0) * worldScale
                val mLatRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
                val markerWorldY = ((1.0 - ln(tan(Math.PI / 4.0 + mLatRad / 2.0)) / Math.PI) / 2.0) * worldScale
                val screenX = centerX + (markerWorldX - centerWorldX) * tileScale
                val screenY = centerY + (markerWorldY - centerWorldY) * tileScale

                if (screenX in -40.0..(widthPx + 40.0) && screenY in -40.0..(heightPx + 40.0)) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(screenX.roundToInt() - 16, screenY.roundToInt() - 16) }
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📍", fontSize = 16.sp)
                    }
                }
            }

            // Render Map Clusters and Temple Markers
            clusterItems.forEach { item ->
                when (item) {
                    is MapClusterItem.Cluster -> {
                        val itemWorldX = ((item.longitude + 180.0) / 360.0) * worldScale
                        val iLatRad = Math.toRadians(item.latitude.coerceIn(-85.0511, 85.0511))
                        val itemWorldY = ((1.0 - ln(tan(Math.PI / 4.0 + iLatRad / 2.0)) / Math.PI) / 2.0) * worldScale
                        val screenX = centerX + (itemWorldX - centerWorldX) * tileScale
                        val screenY = centerY + (itemWorldY - centerWorldY) * tileScale

                        if (screenX in -50.0..(widthPx + 50.0) && screenY in -50.0..(heightPx + 50.0)) {
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(screenX.roundToInt() - 22, screenY.roundToInt() - 22) }
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(MarigoldGold, TerracottaPrimary)))
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable {
                                        centerLat = item.latitude
                                        centerLon = item.longitude
                                        zoomLevel = (zoomLevel + 2.0f).coerceAtMost(16.0f)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.count.toString(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                    is MapClusterItem.SinglePin -> {
                        val pin = item.pin
                        val pinWorldX = ((pin.longitude + 180.0) / 360.0) * worldScale
                        val pLatRad = Math.toRadians(pin.latitude.coerceIn(-85.0511, 85.0511))
                        val pinWorldY = ((1.0 - ln(tan(Math.PI / 4.0 + pLatRad / 2.0)) / Math.PI) / 2.0) * worldScale
                        val screenX = centerX + (pinWorldX - centerWorldX) * tileScale
                        val screenY = centerY + (pinWorldY - centerWorldY) * tileScale

                        if (screenX in -50.0..(widthPx + 50.0) && screenY in -50.0..(heightPx + 50.0)) {
                            val isWishlist = pin.isWishlist
                            val isSelected = selectedPin?.id == pin.id

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(screenX.roundToInt() - 20, screenY.roundToInt() - 20) }
                                    .size(40.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            if (isWishlist) listOf(Color(0xFFC084FC), WishlistOrchid)
                                            else listOf(MarigoldGold, TerracottaPrimary)
                                        )
                                    )
                                    .border(if (isSelected) 3.dp else 2.dp, Color.White, CircleShape)
                                    .clickable {
                                        selectedPin = pin
                                        droppedCoordinates = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isWishlist) "★" else "🛕",
                                    fontSize = if (isWishlist) 15.sp else 18.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Search Bar & Category Filter Carousel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF131826).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, MarigoldGold.copy(alpha = 0.5f)),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MarigoldGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            showSearchSuggestions = it.isNotBlank()
                        },
                        placeholder = { Text("Search temple or city (Madurai, Kashi...)", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = ""; showSearchSuggestions = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showSearchSuggestions && searchResults.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF131826).copy(alpha = 0.96f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    searchResults.take(4).forEach { temple ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E283E),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    centerLat = temple.latitude
                                    centerLon = temple.longitude
                                    zoomLevel = 12.0f
                                    searchQuery = temple.name
                                    showSearchSuggestions = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(temple.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                    Text("${temple.cityState} · ${temple.deity}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Categories horizontal filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SatelliteChip(
                    name = "All (${allPins.size})",
                    isSelected = selectedCategoryFilter == null,
                    colorHex = "#C2410C",
                    onClick = { selectedCategoryFilter = null }
                )
                categories.forEach { cat ->
                    val count = allPins.count { it.categoryIds.contains(cat.id) }
                    SatelliteChip(
                        name = "${cat.name} ($count)",
                        isSelected = selectedCategoryFilter == cat.id,
                        colorHex = cat.colorHex,
                        onClick = { selectedCategoryFilter = cat.id }
                    )
                }
            }
        }

        // Floating Controls: Zoom In/Out, Re-Center, Layer Style
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Layer switcher
            Surface(
                shape = CircleShape,
                color = Color(0xFF131826).copy(alpha = 0.94f),
                border = BorderStroke(1.dp, MarigoldGold.copy(alpha = 0.5f)),
                modifier = Modifier.size(42.dp)
            ) {
                IconButton(onClick = { showLayerMenu = !showLayerMenu }) {
                    Icon(Icons.Default.Layers, contentDescription = "Map Style", tint = MarigoldGold)
                }
            }

            AnimatedVisibility(visible = showLayerMenu) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131826).copy(alpha = 0.96f))
                        .border(1.dp, MarigoldGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MapTileLayer.values().forEach { layer ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (activeLayer == layer) TerracottaPrimary else Color.Transparent,
                            modifier = Modifier.clickable {
                                activeLayer = layer
                                showLayerMenu = false
                            }
                        ) {
                            Text(
                                text = "${layer.icon} ${layer.title}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Surface(shape = CircleShape, color = Color(0xFF131826).copy(alpha = 0.94f), border = BorderStroke(1.dp, MarigoldGold.copy(alpha = 0.5f)), modifier = Modifier.size(42.dp)) {
                IconButton(onClick = { zoomLevel = (zoomLevel + 1.2f).coerceAtMost(16.0f) }) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = MarigoldGold)
                }
            }
            Surface(shape = CircleShape, color = Color(0xFF131826).copy(alpha = 0.94f), border = BorderStroke(1.dp, MarigoldGold.copy(alpha = 0.5f)), modifier = Modifier.size(42.dp)) {
                IconButton(onClick = { zoomLevel = (zoomLevel - 1.2f).coerceAtLeast(2.5f) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = MarigoldGold)
                }
            }
            Surface(shape = CircleShape, color = Color(0xFF131826).copy(alpha = 0.94f), border = BorderStroke(1.dp, MarigoldGold.copy(alpha = 0.5f)), modifier = Modifier.size(42.dp)) {
                IconButton(onClick = {
                    centerLat = 18.5
                    centerLon = 78.5
                    zoomLevel = 5.0f
                }) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center on India", tint = MarigoldGold)
                }
            }
        }

        // Dropped Coordinates Action Banner
        AnimatedVisibility(
            visible = droppedCoordinates != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            droppedCoordinates?.let { (lat, lng) ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF182032).copy(alpha = 0.96f),
                    border = BorderStroke(1.5.dp, TerracottaPrimary),
                    shadowElevation = 14.dp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Dropped Pin on Earth", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                    Text("%.4f° N, %.4f° E".format(lat, lng), style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                                }
                            }
                            IconButton(onClick = { droppedCoordinates = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFF94A3B8))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onRequestAddVisitAtCoordinates(lat, lng, "")
                                droppedCoordinates = null
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Temple Visit at this Location", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // Selected Pin Detail Bottom Card (Jumps to Step 5 Visit Details)
        AnimatedVisibility(
            visible = selectedPin != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedPin?.let { pin ->
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, if (pin.isWishlist) WishlistOrchid else TerracottaPrimary),
                    shadowElevation = 14.dp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pin.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "%.4f° N, %.4f° E".format(pin.latitude, pin.longitude),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { selectedPin = null }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.outline)
                            }
                        }

                        if (!pin.deity.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = MarigoldGold.copy(alpha = 0.15f)) {
                                Text(
                                    text = "🕉️ ${pin.deity}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = AntiqueGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (!pin.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                                Text(text = "“${pin.notes}”", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Button to view Full Visit Detail & Linked Photos (Step 5)
                        if (pin.visitId != null) {
                            Button(
                                onClick = {
                                    val vid = pin.visitId
                                    selectedPin = null
                                    onOpenVisitDetail(vid)
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                            ) {
                                Text("View Visit Details & Photos")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SatelliteChip(
    name: String,
    isSelected: Boolean,
    colorHex: String,
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
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) pillColor else Color(0xFF131826).copy(alpha = 0.92f),
        border = BorderStroke(1.dp, if (isSelected) pillColor else Color(0xFF334155)),
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) Color.White else Color(0xFFE2E8F0),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
