package com.example.flyskai.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location

@Composable
fun MapboxMap(
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var cameraCentered by remember { mutableStateOf(false) }
    var lastKnownLocation by remember { mutableStateOf<Point?>(null) }


    AndroidView(
        factory = { ctx ->
            MapView(ctx).also { view ->
                mapView = view
                view.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
                    view.location.updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(mapView) {
        val listener = OnIndicatorPositionChangedListener { point ->
            lastKnownLocation = point
            if (!cameraCentered) {
                mapView?.mapboxMap?.easeTo(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(14.0)
                        .build()
                )
                cameraCentered = true
            }
        }

        mapView?.location?.addOnIndicatorPositionChangedListener(listener)

        onDispose {
            mapView?.location?.removeOnIndicatorPositionChangedListener(listener)
        }
    }

}