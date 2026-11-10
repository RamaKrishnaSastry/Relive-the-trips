package com.example.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.auth.UserProfile
import com.example.ui.home.tabs.TripsTab
import com.example.ui.home.tabs.YouTab
import com.example.ui.map.NativeSatelliteMap
import com.example.ui.viewmodel.TempleViewModel
import com.example.ui.visits.CreateVisitSheet
import com.example.ui.visits.VisitDetailScreen

object Routes {
    const val MAP = "map"
    const val TRIPS = "trips"
    const val YOU = "you"
    const val VISIT_DETAIL = "visit_detail/{visitId}"
    const val ADD_VISIT = "add_visit?lat={lat}&lng={lng}&name={name}"

    fun visitDetail(visitId: String) = "visit_detail/$visitId"
    fun addVisit(lat: Double? = null, lng: Double? = null, name: String = ""): String {
        return "add_visit?lat=${lat ?: ""}&lng=${lng ?: ""}&name=$name"
    }
}

@Composable
fun AuthenticatedHomeScreen(
    user: UserProfile,
    templeViewModel: TempleViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(user.id) {
        templeViewModel.setOwnerId(user.id)
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf(Routes.MAP, Routes.TRIPS, Routes.YOU)
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.MAP,
                        onClick = {
                            navController.navigate(Routes.MAP) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Map, contentDescription = stringResource(R.string.tab_map), modifier = Modifier.size(24.dp)) },
                        label = { Text(stringResource(R.string.tab_map)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.TRIPS,
                        onClick = {
                            navController.navigate(Routes.TRIPS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Luggage, contentDescription = stringResource(R.string.tab_trips), modifier = Modifier.size(24.dp)) },
                        label = { Text(stringResource(R.string.tab_trips)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.YOU,
                        onClick = {
                            navController.navigate(Routes.YOU) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.tab_you), modifier = Modifier.size(24.dp)) },
                        label = { Text(stringResource(R.string.tab_you)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MAP,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Routes.MAP) {
                val visits by templeViewModel.allVisits.collectAsStateWithLifecycle()
                val places by templeViewModel.allPlaces.collectAsStateWithLifecycle()
                val categories by templeViewModel.allCategories.collectAsStateWithLifecycle()
                val defaultMapLayer by templeViewModel.defaultMapLayer.collectAsStateWithLifecycle()

                NativeSatelliteMap(
                    visits = visits,
                    places = places,
                    categories = categories,
                    defaultMapLayer = defaultMapLayer,
                    onRequestAddVisitAtCoordinates = { lat, lng, name ->
                        navController.navigate(Routes.addVisit(lat, lng, name))
                    },
                    onOpenVisitDetail = { visitId ->
                        navController.navigate(Routes.visitDetail(visitId))
                    }
                )
            }

            composable(Routes.TRIPS) {
                TripsTab(
                    viewModel = templeViewModel,
                    onOpenAddTemple = {
                        navController.navigate(Routes.addVisit())
                    },
                    onOpenVisitDetail = { visitId ->
                        navController.navigate(Routes.visitDetail(visitId))
                    }
                )
            }

            composable(Routes.YOU) {
                YouTab(
                    user = user,
                    viewModel = templeViewModel,
                    onRequestSignOut = onSignOut
                )
            }

            composable(
                route = Routes.VISIT_DETAIL,
                arguments = listOf(navArgument("visitId") { type = NavType.StringType })
            ) { backStackEntry ->
                val visitId = backStackEntry.arguments?.getString("visitId") ?: ""
                VisitDetailScreen(
                    visitId = visitId,
                    viewModel = templeViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ADD_VISIT,
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("lng") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = "" }
                )
            ) { backStackEntry ->
                val latStr = backStackEntry.arguments?.getString("lat")
                val lngStr = backStackEntry.arguments?.getString("lng")
                val name = backStackEntry.arguments?.getString("name") ?: ""
                val categories by templeViewModel.allCategories.collectAsStateWithLifecycle()

                CreateVisitSheet(
                    initialLatitude = latStr?.toDoubleOrNull(),
                    initialLongitude = lngStr?.toDoubleOrNull(),
                    initialName = name,
                    categories = categories,
                    onDismiss = { navController.popBackStack() },
                    onSaveVisit = { vName, lat, lng, start, end, catIds, deity, arch, prayers, food, notes, isWishlist ->
                        templeViewModel.addFullTempleVisit(
                            name = vName,
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
                        navController.popBackStack()
                    },
                    onOpenAddCustomCategory = {}
                )
            }
        }
    }
}
