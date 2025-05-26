package com.example.flyskai.ui.components

import android.graphics.Bitmap
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.example.flyskai.R
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import model.knownAirports
import kotlin.math.abs

@Composable
fun MapboxMap(
    destIcao: String?,
    recenterRequest: Boolean,
) {
    val context = LocalContext.current

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var cameraCentered by remember { mutableStateOf(false) }
    var lastKnownLocation by remember { mutableStateOf<Point?>(null) }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
                    location.updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }
                }
                // create the annotation manager
                pointAnnotationManager = annotations.createPointAnnotationManager()
                location.addOnIndicatorPositionChangedListener { point ->
                    lastKnownLocation = point
                }
                mapView = this

            }
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(mapView) {
        val listener = OnIndicatorPositionChangedListener { point ->
            lastKnownLocation = point
            if (!cameraCentered) {
                mapView?.camera?.easeTo(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(12.0)
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

    LaunchedEffect(recenterRequest) {
        if (recenterRequest) {
            lastKnownLocation?.let { point ->
                mapView?.mapboxMap?.easeTo(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(14.0)
                        .build()
                )
            }
        }
    }

    LaunchedEffect(destIcao) {
        val manager = pointAnnotationManager ?: return@LaunchedEffect
        manager.deleteAll()

        val icao = destIcao ?: return@LaunchedEffect
        if (icao.isBlank()) return@LaunchedEffect

        // find airport
        val airport = knownAirports.find {
            it.icao.equals(icao, ignoreCase = true)
        } ?: return@LaunchedEffect

        val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_airport)
            ?: return@LaunchedEffect
        val iconBmp: Bitmap = drawable.toBitmap()

        val point = Point.fromLngLat(airport.lon, airport.lat)
        val options = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(iconBmp)
            .withTextOffset(listOf(0.0, 1.5))
            .withTextSize(12.0)
        manager.create(options)

        lastKnownLocation?.let { userPt ->
            val minLat = minOf(userPt.latitude(), point.latitude())
            val maxLat = maxOf(userPt.latitude(), point.latitude())
            val minLon = minOf(userPt.longitude(), point.longitude())
            val maxLon = maxOf(userPt.longitude(), point.longitude())

            val centerLat = (minLat + maxLat) / 2.0
            val centerLon = (minLon + maxLon) / 2.0
            val centerPoint = Point.fromLngLat(centerLon, centerLat)

            val latDelta = abs(maxLat - minLat)
            val lonDelta = abs(maxLon - minLon)
            val maxDelta = maxOf(latDelta, lonDelta)

            val zoomLevel = when {
                maxDelta > 20.0 -> 2.0
                maxDelta > 10.0 -> 4.0
                maxDelta > 5.0  -> 6.0
                maxDelta > 2.0  -> 8.0
                maxDelta > 0.5  -> 10.0
                else            -> 12.0
            }
            val paddedZoom = zoomLevel - 0.7

            mapView?.mapboxMap?.easeTo(
                CameraOptions.Builder()
                    .center(centerPoint)
                    .zoom(paddedZoom)
                    .build()
            )
        }
    }

}