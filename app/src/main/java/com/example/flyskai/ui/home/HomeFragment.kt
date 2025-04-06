package com.example.flyskai.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.example.flyskai.AppSettings
import com.example.flyskai.R
import com.example.flyskai.databinding.FragmentHomeBinding
import com.mapbox.geojson.Point

import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.locationcomponent.location


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private var cameraCentered = false
    private var lastKnownLocation: Point? = null

    private lateinit var pointAnnotationManager: PointAnnotationManager


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        mapView = MapView(requireContext())
        binding.mapContainer.addView(mapView)

        initMap()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_center -> {
                    centerCameraOnLocation()
                    true
                }
                R.id.navigation_unit -> {
                    AppSettings.toggleUnitSystem()
                    true
                }
                R.id.navigation_lookaround -> {
                    fetchNearbyFlights()
                    true
                }
                else -> false
            }
        }

        return binding.root
    }

    private fun initMap() {
        val mapboxMap = mapView.mapboxMap
        mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
            mapView.location.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
            val annotationApi = mapView.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager(AnnotationConfig())


            mapView.location.addOnIndicatorPositionChangedListener { point ->
                lastKnownLocation = point
                if (!cameraCentered) {
                    centerCameraOnLocation()
                    cameraCentered = true
                }
            }
        }
    }

    private fun centerCameraOnLocation() {
        lastKnownLocation?.let { point ->
            mapView.mapboxMap.easeTo(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(14.0)
                    .build()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // OpenSky Basic Starts here

    private fun fetchNearbyFlights() {
        val location = lastKnownLocation ?: return

        val minLat = location.latitude() - 0.5
        val maxLat = location.latitude() + 0.5
        val minLon = location.longitude() - 0.5
        val maxLon = location.longitude() + 0.5

        val url = "https://opensky-network.org/api/states/all?" +
                "lamin=$minLat&lamax=$maxLat&lomin=$minLon&lomax=$maxLon"

        Thread {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }

                    val flights = parseOpenSkyFlights(response)

                    requireActivity().runOnUiThread {
                        showFlightsOnMap(flights)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }


    data class FlightPosition(val icao24: String, val callsign: String?, val lat: Double, val lon: Double)

    private fun parseOpenSkyFlights(json: String): List<FlightPosition> {
        val result = mutableListOf<FlightPosition>()
        val jsonObject = org.json.JSONObject(json)
        val states = jsonObject.optJSONArray("states") ?: return result

        for (i in 0 until states.length()) {
            val state = states.getJSONArray(i)
            val icao24 = state.optString(0)
            val callsign = state.optString(1)?.trim()
            val lat = state.optDouble(6)
            val lon = state.optDouble(5)

            if (!lat.isNaN() && !lon.isNaN()) {
                result.add(FlightPosition(icao24, callsign, lat, lon))
            }
        }

        return result
    }

    private fun showFlightsOnMap(flights: List<FlightPosition>) {
        pointAnnotationManager.deleteAll() // Clear old markers

        val drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_aircraft)
        val bitmap = drawable?.toBitmap()

        if (bitmap == null) {
            println("❌ Could not decode flight icon bitmap.")
            return
        }


        flights.forEach { flight ->
            val point = Point.fromLngLat(flight.lon, flight.lat)

            val options = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withTextField(flight.callsign ?: flight.icao24)
                .withTextOffset(listOf(0.0, 1.5))
                .withTextSize(12.0) // 🔽 Smaller text (default is around 16–18)


            pointAnnotationManager.create(options)
        }
    }

    ////

}
