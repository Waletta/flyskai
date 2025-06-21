package com.example.flyskai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flyskai.ui.shared.CockPitViewModel
import model.knownLLMConfigurations

@Composable
fun MainScreen(
    viewModel: CockPitViewModel,
    isLandscape: Boolean,
) {
    if (isLandscape) {
        LandscapeChatScreen()
    } else {
        PortraitMainScreen(viewModel = viewModel)
    }
}

@Composable
private fun PortraitMainScreen(viewModel: CockPitViewModel) {
    val destAirport by viewModel.destinationAirport.collectAsState()
    val showDestinationDialog by viewModel.showDestinationDialog.collectAsState()
    val recenterToggle by viewModel.recenterToggle.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavBar(
                destAirport = destAirport,
                navController = navController,
                currentDestination = currentDestination,
                onDestinationClick = viewModel::onDestinationClick,
                onCenterClick = viewModel::onRecenterClick
            )
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            viewModel = viewModel,
            recenterToggle = recenterToggle,
            destIcao = destAirport?.icao // Pass the ICAO string directly
        )
    }

    if (showDestinationDialog) {
        DestinationDialog(
            current = destAirport,
            onSelect = viewModel::onDestinationSelected,
            onClear = viewModel::onClearDestination,
            onDismiss = viewModel::onDismissDestinationDialog
        )
    }
}


@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: CockPitViewModel,
    recenterToggle: Boolean,
    destIcao: String?
) {
    NavHost(
        navController = navController,
        startDestination = "onboard",
        modifier = modifier
    ) {
        composable("onboard") {
            Column(modifier = Modifier.fillMaxSize()) {
                CockPit(
                    cpVm = viewModel,
                )
                MapboxMap(
                    destIcao = destIcao,
                    recenterRequest = recenterToggle,
                )
            }
        }
    }
}

@Composable
private fun LandscapeChatScreen() {
    val llmConfig = knownLLMConfigurations.find { it.modelId == "PHI3" }
    if (llmConfig == null) {
        Text(
            text = "Phi-3 configuration not found",
            modifier = Modifier.fillMaxSize(),
            textAlign = TextAlign.Center
        )
    } else {
        // Assuming ChatScreen is a composable you have defined elsewhere
        // ChatScreen(llmConfig = llmConfig)
    }
}