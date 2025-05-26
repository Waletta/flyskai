package com.example.flyskai.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.flyskai.R
import com.example.flyskai.ui.shared.AppSettings
import model.knownAirports
import model.Airport


@Composable
internal fun NavBar(
    destAirport: Airport?,
    navController: NavController,
    currentDestination: NavDestination?,
    onDestinationClick: () -> Unit,
    onCenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    NavigationBar(modifier = modifier) {
        listOf(
            Triple("onboard", R.drawable.baseline_connecting_airports_24, "Onboard"),
            Triple("center", R.drawable.baseline_my_location_24, "Center"),
            Triple("unit", R.drawable.baseline_conveyor_belt_24, "Unit")
        ).forEach { (route, iconRes, _) -> // Note: Ignore the label parameter
            val selected = currentDestination?.hierarchy?.any { it.route == route } == true
            val icon = if (route == "onboard" && destAirport != null) {
                R.drawable.baseline_airline_seat_recline_extra_24
            } else {
                iconRes
            }
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = route
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
                        }
                    }
                },

            )
        }
    }
}

@Composable
fun DestinationDialog(
    current: Airport?,
    onSelect: (icao: String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sortedAirports = knownAirports.sortedBy { it.name }
    var searchText by remember { mutableStateOf("") }

    val airportNames = sortedAirports.map { it.name }.toTypedArray()
    val options = if (current != null) {
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
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Destination",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )


                // 🔥 Search field
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search Airport...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f) // important: allow scrolling inside available space
                        .fillMaxWidth()
                ) {
                    itemsIndexed(options) { index, option ->
                        TextButton(
                            onClick = {
                                if (current != null && index == 0) {
                                    onClear()
                                    Toast.makeText(context, "Destination cleared. Cross-Checked", Toast.LENGTH_LONG).show()
                                } else {
                                    val trueIndex = (if (current != null) index - 1 else index)
                                        .coerceIn(0, sortedAirports.lastIndex)
                                    val picked = sortedAirports[trueIndex]
                                    onSelect(picked.icao)
                                    Toast.makeText(context, "Destination set: ${picked.name}. Cross-checked", Toast.LENGTH_LONG).show()
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
