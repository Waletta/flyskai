package com.example.flyskai.ui.home

import com.mapbox.common.TileStore
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.flyskai.databinding.FragmentHomeBinding
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.locationcomponent.location
import java.util.Locale
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

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


import android.widget.Toast
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.OfflineManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var cameraCentered = false
    private var lastKnownLocation: Point? = null
    private lateinit var mapView: MapView



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
            _binding = FragmentHomeBinding.inflate(inflater, container, false)
            val homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

            homeViewModel.speed.observe(viewLifecycleOwner) { speed ->
                binding.textSpeed.text = String.format(Locale.US, "Speed: %.1f m/s", speed)
            }

            homeViewModel.altitude.observe(viewLifecycleOwner) { alt ->
                binding.textAltitude.text = String.format(Locale.US, "Altitude: %.1f m", alt)
            }

            val mapInitOptions = MapInitOptions(requireContext())
            mapView = MapView(requireContext(), mapInitOptions)
            binding.mapContainer.addView(mapView)

            val mapboxMap = mapView.getMapboxMap()


            mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
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
                                mapView.getMapboxMap().easeTo(
                                    CameraOptions.Builder()
                                        .center(point)
                                        .zoom(14.0)
                                        .build()
                                )
                            }
                            true
                        }
                        R.id.navigation_download  -> {
//                            preloadTileRegion()
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

//    private fun preloadTileRegion() {
//        val mapboxMap = mapView.getMapboxMap()
//        val cameraState = mapboxMap.cameraState
//        val styleUri = mapboxMap.getStyle()?.styleURI ?: Style.MAPBOX_STREETS
//
//        val tileStore = TileStore.create()
//        val offlineManager = OfflineManager()
//
//
//        val cameraOptions = com.mapbox.maps.CameraOptions.Builder()
//            .center(cameraState.center)
//            .zoom(cameraState.zoom)
//            .bearing(cameraState.bearing)
//            .pitch(cameraState.pitch)
//            .padding(cameraState.padding)
//            .build()
//
//
//        val visibleBounds = mapboxMap.coordinateBoundsForCamera(cameraOptions)
//        val geometry = createBoundsPolygon(visibleBounds)
//
//        // 2. Calculate min/max zoom levels (clamped between 0-22)
//        val minZoom = (cameraState.zoom - 2).coerceIn(0.0, 22.0).toInt()
//        val maxZoom = (cameraState.zoom + 2).coerceIn(0.0, 22.0).toInt()
//
//
//        // 3. Configure offline download
//        val regionId = "offline-region-${System.currentTimeMillis()}"
//
//
//
//        // 4. Create TilesetDescriptor CORRECTLY (FIXED)
//        val tilesetDescriptor = offlineManager.createTilesetDescriptor(
//            TilesetDescriptorOptions.Builder()
//                .styleURI(styleUri)
//                .minZoom(minZoom)
//                .maxZoom(maxZoom)
//                .build()
//        )
//
//        // Configure download options
//        val loadOptions = TileRegionLoadOptions.Builder()
//            .geometry(geometry)
//            .descriptors(listOf(tilesetDescriptor))
//            .acceptExpired(false) // Changed to false for fresh data
//            .networkRestriction(NetworkRestriction.NONE)
//            .metadata(ByteArray(0)) // Can add custom metadata if needed
//            .build()
//
//        // Show loading indicator
//        Toast.makeText(requireContext(), "Downloading map area...", Toast.LENGTH_SHORT).show()
//
//        // Start download
//        TileStore.getInstance().loadTileRegion(regionId, loadOptions) { result ->
//            activity?.runOnUiThread {
//                result.fold(
//                    onSuccess = {
//                        Log.d("FlyskAI", "Offline region downloaded successfully")
//                        Toast.makeText(
//                            requireContext(),
//                            "Map area saved for offline use",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    },
//                    onFailure = { error ->
//                        Log.e("FlyskAI", "Tile download failed", error)
//                        Toast.makeText(
//                            requireContext(),
//                            "Download failed: ${error.localizedMessage}",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                )
//            }
//        }
//    }
//
//    // Helper: Convert bounds to a polygon
//    private fun createBoundsPolygon(bounds: CoordinateBounds): Polygon {
//        val sw = bounds.southwest
//        val ne = bounds.northeast
//        return Polygon.fromLngLats(
//            listOf(
//                listOf(
//                    sw,
//                    Point.fromLngLat(ne.longitude(), sw.latitude()),
//                    ne,
//                    Point.fromLngLat(sw.longitude(), ne.latitude()),
//                    sw
//                )
//            )
//        )
//    }
}
