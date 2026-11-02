package com.example.flyskai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flyskai.ui.shared.CockPitViewModel

@Composable
fun MainScreen(
    viewModel: CockPitViewModel,
    isLandscape: Boolean,
) {
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
                onCenterClick = viewModel::onRecenterClick,
            )
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            viewModel = viewModel,
            recenterToggle = recenterToggle,
            destIcao = destAirport?.icao,
            isLandscape = isLandscape,
        )
    }

    if (showDestinationDialog) {
        DestinationDialog(
            current = destAirport,
            onSelect = viewModel::onDestinationSelected,
            onClear = viewModel::onClearDestination,
            onDismiss = viewModel::onDismissDestinationDialog,
        )
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: CockPitViewModel,
    recenterToggle: Boolean,
    destIcao: String?,
    isLandscape: Boolean,
) {
    NavHost(
        navController = navController,
        startDestination = "onboard",
        modifier = modifier,
    ) {
        composable("onboard") {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        CockPit(cpVm = viewModel)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        MapboxMap(
                            destIcao = destIcao,
                            recenterRequest = recenterToggle,
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    CockPit(cpVm = viewModel)
                    MapboxMap(
                        destIcao = destIcao,
                        recenterRequest = recenterToggle,
                    )
                }
            }
        }
    }
}
