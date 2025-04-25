package com.example.flyskai.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.flyskai.R
import com.example.flyskai.ui.shared.AppSettings
import com.example.flyskai.ui.shared.CockPitViewModel
import model.knownAirports

@Composable
internal fun NavigationBar(
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
                            Toast.makeText(context, "Unit system toggled", Toast.LENGTH_SHORT)
                                .show()
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Destination",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                options.forEachIndexed { index, option ->
                    TextButton(
                        onClick = {
                            if (destination?.isNotBlank() == true && index == 0) {
                                viewModel.setDestination("")
                                Toast.makeText(context, "Destination cleared.", Toast.LENGTH_SHORT).show()
                            } else {
                                val selectedIndex = if (destination?.isNotBlank() == true) index - 1 else index
                                val selectedAirport = knownAirports[selectedIndex]
                                viewModel.setDestination(selectedAirport.icao)
                                Toast.makeText(context, "Destination set: ${selectedAirport.name}", Toast.LENGTH_LONG).show()
                            }
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = option,
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.White
                    )
                }
            }
        }
    }
}