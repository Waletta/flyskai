package com.example.flyskai.ui.home

import android.app.Fragment
import android.content.Context
import com.mapbox.common.TileStore
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.flyskai.databinding.FragmentHomeBinding
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.locationcomponent.location
import androidx.core.content.ContentProviderCompat.requireContext
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.animation.easeTo
import com.example.flyskai.R
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.CameraOptions


import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.OfflineManager


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var cameraCentered = false
    private var lastKnownLocation: Point? = null
    private lateinit var mapView: MapView

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
            _binding = FragmentHomeBinding.inflate(inflater, container, false)

            val mapInitOptions = MapInitOptions(requireContext())
            mapView = MapView(requireContext(), mapInitOptions)
            binding.mapContainer.addView(mapView)

            val mapboxMap = mapView.mapboxMap


            mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
                    mapView.location.updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }

            mapView.location.addOnIndicatorPositionChangedListener { point ->
            lastKnownLocation = point

            if (!cameraCentered) {
                mapboxMap.easeTo(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(14.0)
                        .build()
                )
                cameraCentered = true
                }
            }

            binding.bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_center -> {
                        lastKnownLocation?.let { point ->
                            mapView.mapboxMap.easeTo(
                                CameraOptions.Builder()
                                    .center(point)
                                    .zoom(14.0)
                                    .build()
                            )
                        }
                        true
                    }
                    R.id.navigation_download  -> {
                        preloadTileRegion()
                        true
                    }
                    else -> false
                }
            }

        }

        return binding.root
    }



    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun preloadTileRegion() {

        val mapboxMap = mapView.mapboxMap
        val cameraState = mapboxMap.cameraState
        val styleUri = mapboxMap.style?.styleURI ?: Style.MAPBOX_STREETS

        val tileStore = TileStore.create()

        val offlineManager = OfflineManager()
        val cameraOptions = CameraOptions.Builder()
            .center(cameraState.center)
            .zoom(cameraState.zoom)
            .bearing(cameraState.bearing)
            .pitch(cameraState.pitch)
            .padding(cameraState.padding)
            .build()


        val bounds = mapboxMap.coordinateBoundsForCamera(cameraOptions)
        val geometry = createBoundsPolygon(bounds)


        val minZoom = (cameraState.zoom - 2).coerceIn(0.0, 24.0).toInt().toByte()
        val maxZoom = (cameraState.zoom + 2).coerceIn(0.0, 24.0).toInt().toByte()

        val tileRegionId  = "offline-region-${System.currentTimeMillis()}"

        val tilesetDescriptor = offlineManager.createTilesetDescriptor(
            TilesetDescriptorOptions.Builder()
                .styleURI(styleUri)
                .minZoom(minZoom)
                .maxZoom(maxZoom)
                .build()
        )

        val loadOptions = TileRegionLoadOptions.Builder()
            .geometry(geometry)
            .descriptors(listOf(tilesetDescriptor))
            .acceptExpired(false)
            .networkRestriction(NetworkRestriction.NONE)
            .build()

        tileStore.loadTileRegion(tileRegionId,loadOptions)
        }
    }

    // Helper: Convert bounds to a polygon
    private fun createBoundsPolygon(bounds: CoordinateBounds): Polygon {
        val sw = bounds.southwest
        val ne = bounds.northeast
        return Polygon.fromLngLats(
            listOf(
                listOf(
                    sw,
                    Point.fromLngLat(ne.longitude(), sw.latitude()),
                    ne,
                    Point.fromLngLat(sw.longitude(), ne.latitude()),
                    sw
                )
            )
        )
}
