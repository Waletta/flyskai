package com.example.flyskai.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.flyskai.databinding.FragmentHomeBinding
import androidx.fragment.app.Fragment
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.animation.easeTo
import com.example.flyskai.R
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.CameraOptions

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
    }
