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
import com.mapbox.common.TilesetDescriptor
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.TilesetDescriptorOptions
import android.widget.Toast

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
                    com.mapbox.maps.CameraOptions.Builder()
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
                                    com.mapbox.maps.CameraOptions.Builder()
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

//    private fun preloadTileRegion(regionId: String = "flyskai-region") {
//        val tileStore = TileStore.getInstance()
//
//        val southwest = Point.fromLngLat(12.0, 42.0)
//        val northeast = Point.fromLngLat(18.0, 45.0)
//
//        val geometry = Polygon.fromLngLats(
//            listOf(
//                listOf(
//                    southwest,
//                    Point.fromLngLat(northeast.longitude(), southwest.latitude()),
//                    northeast,
//                    Point.fromLngLat(southwest.longitude(), northeast.latitude()),
//                    southwest
//                )
//            )
//        )
//
//        val tilesetDescriptor = TilesetDescriptor(
//            TilesetDescriptorOptions.Builder()
//                .styleURI(Style.MAPBOX_STREETS)
//                .minZoom(10)
//                .maxZoom(16)
//                .build()
//        )
//
//        val loadOptions = TileRegionLoadOptions.Builder()
//            .geometry(geometry)
//            .descriptors(listOf(tilesetDescriptor))
//            .acceptExpired(true)
//            .networkRestriction(NetworkRestriction.NONE)
//            .build()
//
//        tileStore.loadTileRegion(regionId, loadOptions) { result ->
//            result.fold(
//                onSuccess = {
//                    Log.d("FlyskAI", "Offline region downloaded ✔️")
//                    Toast.makeText(requireContext(), "Offline map ready 🗺️", Toast.LENGTH_SHORT).show()
//                },
//                onFailure = {
//                    Log.e("FlyskAI", "Tile download failed: ${it.message}")
//                    Toast.makeText(requireContext(), "Download failed ❌", Toast.LENGTH_SHORT).show()
//                }
//            )
//        }
//    }
}
