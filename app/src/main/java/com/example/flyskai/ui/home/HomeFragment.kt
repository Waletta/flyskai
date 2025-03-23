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

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
                mapboxMap.setCamera(
                    com.mapbox.maps.CameraOptions.Builder()
                        .center(point)
                        .zoom(14.0)
                        .build()
                )
            }
        }
        return binding.root // ✅ now properly outside loadStyleUri block
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
