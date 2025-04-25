package com.example.flyskai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import com.example.flyskai.ui.shared.CockPitViewModel

@Composable
fun DefaultScreen(
    viewModel: CockPitViewModel,
    navHostId: Int, // ID of NavHostFragment
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Cockpit at the top
        CockPit(
            cpVm = viewModel,
        )

        // NavHostFragment (Map + Bottom Navigation)
        AndroidView(
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = navHostId // Reuse existing NavHostFragment ID
                }
            },
            modifier = Modifier
                .weight(1f) // Take remaining space
                .fillMaxWidth()
        )
    }
}