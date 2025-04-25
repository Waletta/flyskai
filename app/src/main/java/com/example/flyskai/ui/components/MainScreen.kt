package com.example.flyskai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flyskai.ui.shared.CockPitViewModel
import model.knownLLMConfigurations
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun MainScreen(
    viewModel: CockPitViewModel,
    isLandscape: Boolean,
) {
    val destAirport by viewModel.destinationAirport.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var showDestinationDialog by remember { mutableStateOf(false) }
    var recenterToggle by remember { mutableStateOf(false) }

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
                    NavBar(
                        destAirport = destAirport,
                        navController = navController,
                        currentDestination = currentDestination,
                        onDestinationClick = { showDestinationDialog = true },
                        onCenterClick = { recenterToggle = !recenterToggle }
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
                            MapboxMap( destIcao = destAirport?.icao, recenterRequest = recenterToggle)
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

        if (showDestinationDialog) {
            DestinationDialog(
                current = destAirport,
                onSelect = { airportIcao ->
                    viewModel.setDestinationByIcao(airportIcao)
                    showDestinationDialog = false
                },
                onClear = {
                    viewModel.setDestinationByIcao("")
                    showDestinationDialog = false
                },
                onDismiss = { showDestinationDialog = false }
            )
        }
    }