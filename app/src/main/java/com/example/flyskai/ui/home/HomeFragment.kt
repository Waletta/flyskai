package com.example.flyskai.ui.home

import android.os.Bundle
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

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var cameraCentered = false
    private var lastKnownLocation: Point? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
            _binding = FragmentHomeBinding.inflate(inflater, container, false)

            val homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

            // Observe LiveData and update TextViews
            homeViewModel.speed.observe(viewLifecycleOwner) { speed ->
                binding.textSpeed.text = String.format(Locale.US, "Speed: %.1f m/s", speed)
            }

            homeViewModel.altitude.observe(viewLifecycleOwner) { alt ->
                binding.textAltitude.text = String.format(Locale.US, "Altitude: %.1f m", alt)
            }

            val mapView = binding.mapView
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
                        R.id.navigation_notifications -> {
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
                        R.id.navigation_home -> {
                            // TODO: Handle home button action
                            true
                        }
                        R.id.navigation_dashboard -> {
                            // TODO: Handle dashboard action
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
