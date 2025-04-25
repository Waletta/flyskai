package com.example.flyskai.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.asFlow
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flyskai.R
import com.example.flyskai.ui.shared.AppSettings
import com.example.flyskai.ui.shared.CockPitViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.locationcomponent.location
import model.knownAirports
import model.knownLLMConfigurations
import model.Coordinates
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

@Composable
fun MainScreen(
    viewModel: CockPitViewModel,
    isLandscape: Boolean,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var showDestinationDialog by remember { mutableStateOf(false) }
    var triggerCenterMap by remember { mutableStateOf(false) }

    if (isLandscape) {
        val llmConfig = knownLLMConfigurations.find { it.modelId == "PHI3" }
        if (llmConfig == null) {
            Text(
                text = "Phi-3 configuration not found",
                modifier = Modifier.fillMaxSize(),
                textAlign = TextAlign.Center
            )
        } else {
            ChatScreen(
                llmConfig = llmConfig,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            )
        }
    } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        viewModel = viewModel,
                        navController = navController,
                        currentDestination = currentDestination,
                        onDestinationClick = { showDestinationDialog = true },
                        onCenterClick = { triggerCenterMap = true }
                    )
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "onboard",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("onboard") {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            CockPit(
                                cpVm = viewModel,
                            )
                            MapboxMap(
                                viewModel = viewModel,
                                triggerCenterMap = triggerCenterMap,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }
                    composable("center") {
                        Text(
                            text = "Center Screen",
                            modifier = Modifier.fillMaxSize(),
                            textAlign = TextAlign.Center
                        )
                    }
                    composable("unit") {
                        Text(
                            text = "Unit Screen",
                            modifier = Modifier.fillMaxSize(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Destination selection dialog
        if (showDestinationDialog) {
            DestinationDialog(
                viewModel = viewModel,
                onDismiss = { showDestinationDialog = false }
            )
        }
    }


@Composable
private fun NavigationBar(
    viewModel: CockPitViewModel,
    navController: NavController,
    currentDestination: NavDestination?,
    onDestinationClick: () -> Unit,
    onCenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val destination by viewModel.destination.asFlow().collectAsStateWithLifecycle("")

    NavigationBar(modifier = modifier) {
        listOf(
            Triple("onboard", R.drawable.baseline_connecting_airports_24, "Onboard"),
            Triple("center", R.drawable.baseline_my_location_24, "Center"),
            Triple("unit", R.drawable.baseline_conveyor_belt_24, "Unit")
        ).forEach { (route, iconRes, _) -> // Note: Ignore the label parameter
            val selected = currentDestination?.hierarchy?.any { it.route == route } == true
            val icon = if (route == "onboard" && destination?.isNotBlank() == true) {
                R.drawable.baseline_airline_seat_recline_extra_24
            } else {
                iconRes
            }

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = route // Use route for accessibility
                    )
                },
                selected = selected,
                onClick = {
                    when (route) {
                        "onboard" -> {
                            onDestinationClick()
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        "center" -> {
                            onCenterClick()
                        }
                        "unit" -> {
                            AppSettings.toggleUnitSystem()
                            Toast.makeText(context, "Unit system toggled", Toast.LENGTH_SHORT).show()
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            )
        }
    }
}

/**
 * Mapbox map displaying aircraft location and destination annotations.
 */
@Composable
fun MapboxMap(
    viewModel: CockPitViewModel,
    triggerCenterMap: Boolean,
    modifier: Modifier = Modifier
) {
    val acState by viewModel.acState.asFlow().collectAsStateWithLifecycle(initialValue = null)
    var cameraCentered by remember { mutableStateOf(false) }
    val destination by viewModel.destination.asFlow().collectAsStateWithLifecycle("")
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
                    location.updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }
                    val annotationApi = annotations
                    val pointAnnotationManager = annotationApi.createPointAnnotationManager(AnnotationConfig())
                    updateDestinationAnnotation(pointAnnotationManager, destination.toString(), ctx)

                    location.addOnIndicatorPositionChangedListener { point ->
                        if (!cameraCentered) {
                            mapboxMap.easeTo(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(
                                        acState!!.coordinates.longitude,
                                        acState!!.coordinates.latitude
                                    ))
                                    .zoom(14.0)
                                    .build()
                            )
                            cameraCentered = true
                        }
                    }
                }
            }
        },
        update = { mapView ->
            val pointAnnotationManager = mapView.annotations.createPointAnnotationManager(AnnotationConfig())
            updateDestinationAnnotation(pointAnnotationManager, destination.toString(), context)
            if (triggerCenterMap && acState?.coordinates != null) {
                    mapView.mapboxMap.easeTo(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(
                                acState!!.coordinates.longitude,
                                acState!!.coordinates.latitude
                            ))
                            .zoom(14.0)
                            .build()
                    )
            }
        },
        modifier = modifier
    )

}

private fun updateDestinationAnnotation(
    pointAnnotationManager: PointAnnotationManager,
    icao: String,
    context: android.content.Context
) {
    pointAnnotationManager.deleteAll()
    if (icao.isNotBlank()) {
        val airport = knownAirports.find { it.icao.equals(icao, ignoreCase = true) } ?: return
        val point = Point.fromLngLat(airport.lon, airport.lat)
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_airport)
        val bitmap = drawable?.toBitmap() ?: return

        val options = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(bitmap)
            .withTextField(airport.name)
            .withTextOffset(listOf(0.0, 1.5))
            .withTextSize(12.0)
        pointAnnotationManager.create(options)
    }
}

/**
 * Dialog for selecting or clearing a destination airport.
 */
@Composable
fun DestinationDialog(
    viewModel: CockPitViewModel,
    onDismiss: () -> Unit
) {
    val destination by viewModel.destination.asFlow().collectAsStateWithLifecycle("")
    val airportNames = knownAirports.map { it.name }.toTypedArray()
    val options = if (destination?.isNotBlank() == true) {
        arrayOf("Clear Destination") + airportNames
    } else {
        airportNames
    }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Destination") },
            text = {
                Column {
                    options.forEachIndexed { index, option ->
                        TextButton(
                            onClick = {
                                if (destination?.isNotBlank() ==true && index == 0) {
                                    viewModel.setDestination("")
                                    Toast.makeText(context, "Destination cleared.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val selectedIndex = if (destination?.isNotBlank()==true) index - 1 else index
                                    val selectedAirport = knownAirports[selectedIndex]
                                    viewModel.setDestination(selectedAirport.icao)
                                    Toast.makeText(context, "Destination set: ${selectedAirport.name}", Toast.LENGTH_LONG).show()
                                }
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option, textAlign = TextAlign.Start)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}