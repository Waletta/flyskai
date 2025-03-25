package com.example.flyskai.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun HomeScreen(viewModel: HomeViewModel, context: Context = LocalContext.current) {
    val locationState by viewModel.locationState.observeAsState()
    val mapView = rememberMapViewWithLifecycle(context)

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            locationState?.let {
                CockpitPFD(speed = it.speed.toInt(), altitude = it.altitude.toInt())
            } ?: Text("Waiting for GPS...", color = Color.White)
        }
    }
}
