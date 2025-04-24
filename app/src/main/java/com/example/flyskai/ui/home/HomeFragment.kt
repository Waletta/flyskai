package com.example.flyskai.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.flyskai.ui.shared.AppSettings
import com.example.flyskai.R
import com.example.flyskai.databinding.FragmentHomeBinding
import com.example.flyskai.ui.shared.CockPitViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.launch
import model.knownAirports
import model.FlightPosition
import kotlin.math.abs


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private var cameraCentered = false
    private var lastKnownLocation: Point? = null
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private val airportNames = knownAirports.map { it.name }.toTypedArray()
    private val cpFlightViewModel: CockPitViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val onboardItemView = binding.bottomNavigation.findViewById<View>(R.id.navigation_onboard)
        onboardItemView.setOnLongClickListener {
            showPopover(it)
            true
        }
        mapView = MapView(requireContext())
        binding.mapContainer.addView(mapView)
        initMap()

        viewLifecycleOwner.lifecycleScope.launch {
            cpFlightViewModel.destination.observe(viewLifecycleOwner) { destination ->
                val icon = if (destination.isNullOrBlank()) {
                    R.drawable.baseline_connecting_airports_24
                } else {
                    R.drawable.baseline_airline_seat_recline_extra_24
                }
                binding.bottomNavigation.menu.findItem(R.id.navigation_onboard)
                    .setIcon(icon)
                if (::pointAnnotationManager.isInitialized) {
                    updateDestinationAnnotation(destination.orEmpty())
                } else {
                    Log.d("HomeFragment", "pointAnnotationManager not initialized, skipping annotation update")
                }
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_ai -> {
                    showAIDownloadDialog()
                    true
                }
                R.id.navigation_onboard -> {
                    showDestinationDialog()
                    true
                }
                R.id.navigation_center -> {
                    centerCameraOnLocation()
                    true
                }
                R.id.navigation_unit -> {
                    AppSettings.toggleUnitSystem()
                    true
                }
//                R.id.navigation_lookaround -> {
//                    fetchNearbyFlights()
//                    true
//                }
                else -> false
            }
        }

        return binding.root
    }

    private fun showDestinationDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Select Destination")

        val currentDestination = cpFlightViewModel.destination.value.orEmpty()
        val options = if (currentDestination.isNotBlank()) {
            arrayOf("Clear Destination") + airportNames
        } else {
            airportNames
        }

        builder.setItems(options) { _, which ->
            if (currentDestination.isNotBlank() && which == 0) {
                cpFlightViewModel.setDestination("")
                binding.bottomNavigation.menu.findItem(R.id.navigation_onboard)
                    .setIcon(R.drawable.baseline_connecting_airports_24)
                Toast.makeText(
                    requireContext(),
                    "Destination cleared.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val selectedIndex = if (currentDestination.isNotBlank()) which - 1 else which
                val selectedAirport = knownAirports[selectedIndex]
                cpFlightViewModel.setDestination(selectedAirport.icao)
                binding.bottomNavigation.menu.findItem(R.id.navigation_onboard)
                    .setIcon(R.drawable.baseline_airline_seat_recline_extra_24)
                Toast.makeText(
                    requireContext(),
                    "Destination set: ${selectedAirport.name}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.setCancelable(true)
        builder.show()
    }

    private fun updateDestinationAnnotation(icao: String) {
        pointAnnotationManager.deleteAll()
        if (icao.isNotBlank()) {
            val airport = knownAirports.find { it.icao.equals(icao, ignoreCase = true) } ?: return
            val point = Point.fromLngLat(airport.lon, airport.lat)
            val drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_airport)
            val bitmap = drawable?.toBitmap() ?: return

            val options = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withTextField(airport.name)
                .withTextOffset(listOf(0.0, 1.5))
                .withTextSize(12.0)
            pointAnnotationManager.create(options)
            
            lastKnownLocation?.let { currentLocation ->
                val minLat = minOf(currentLocation.latitude(), point.latitude())
                val maxLat = maxOf(currentLocation.latitude(), point.latitude())
                val minLon = minOf(currentLocation.longitude(), point.longitude())
                val maxLon = maxOf(currentLocation.longitude(), point.longitude())

                val centerLat = (minLat + maxLat) / 2.0
                val centerLon = (minLon + maxLon) / 2.0
                val centerPoint = Point.fromLngLat(centerLon, centerLat)

                val latDelta = abs(maxLat - minLat)
                val lonDelta = abs(maxLon - minLon)
                val maxDelta = maxOf(latDelta, lonDelta)

                val zoomLevel = when {
                    maxDelta > 20.0 -> 2.0  // Very far (e.g., intercontinental)
                    maxDelta > 10.0 -> 4.0  // Far (e.g., across a country)
                    maxDelta > 5.0 -> 6.0   // Regional
                    maxDelta > 2.0 -> 8.0   // Local
                    maxDelta > 0.5 -> 10.0  // Close
                    else -> 12.0            // Very close
                }

                val paddedZoom = zoomLevel - 0.7
                mapView.mapboxMap.easeTo(
                    CameraOptions.Builder()
                        .center(centerPoint)
                        .zoom(paddedZoom)
                        .build()
                )
            }

        }
    }
    private fun showAIDownloadDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Enable AI Assistant?")
            .setMessage("This feature requires downloading a ~2.7GB model file. Proceed?")
            .setPositiveButton("Download", null)
            .setNegativeButton("Cancel", null)

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
            val bColor = android.graphics.Color.WHITE
            positiveButton.setTextColor(bColor)
            negativeButton.setTextColor(bColor)
            positiveButton.setOnClickListener {
                Toast.makeText(requireContext(), "Downloading AI model…", Toast.LENGTH_SHORT).show()
                //downloadAIModel()
                dialog.dismiss()
            }

            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
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
            val currentDestination = cpFlightViewModel.destination.value.orEmpty()

            if (currentDestination.isNotBlank()) {
                updateDestinationAnnotation(currentDestination)
            }

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
        pointAnnotationManager.deleteAll()

        val drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_aircraft)
        val bitmap = drawable?.toBitmap()

        if (bitmap == null) {
            println("Could not decode flight icon bitmap.")
            return
        }


        flights.forEach { flight ->
            val point = Point.fromLngLat(flight.lon, flight.lat)

            val options = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withTextField(flight.callsign ?: flight.icao24)
                .withTextOffset(listOf(0.0, 1.5))
                .withTextSize(12.0)
            pointAnnotationManager.create(options)
        }
    }

    private fun showPopover(targetView: View) {
        val popup = android.widget.PopupWindow(requireContext())

        val textView = android.widget.TextView(requireContext()).apply {
            text = "Tap when you're seated."
            setPadding(24, 16, 24, 16)
            setBackgroundResource(R.drawable.popover_background)
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
        }

        popup.contentView = textView
        popup.isOutsideTouchable = true
        popup.isFocusable = false
        popup.elevation = 8f

        // Size and position
        textView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val xOffset = targetView.width / 2 - textView.measuredWidth / 2
        val yOffset = -targetView.height - textView.measuredHeight

        popup.showAsDropDown(targetView, xOffset, yOffset)

        // Auto dismiss after 3 seconds
        targetView.postDelayed({ popup.dismiss() }, 3000)
    }
}
