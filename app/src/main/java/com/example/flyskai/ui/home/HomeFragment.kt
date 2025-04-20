package com.example.flyskai.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.flyskai.AppSettings
import com.example.flyskai.R
import com.example.flyskai.databinding.FragmentHomeBinding
import com.example.flyskai.ui.shared.SharedFlightViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.locationcomponent.location
import model.knownAirports

class HomeFragment : Fragment() {

    //private val sharedViewModel: SharedLlmModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private var cameraCentered = false
    private var lastKnownLocation: Point? = null

    private lateinit var pointAnnotationManager: PointAnnotationManager

    private val airportNames = knownAirports.map { it.name }.toTypedArray()
    private val sharedFlightViewModel: SharedFlightViewModel by activityViewModels()
    private var isSeated = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val onboardItemView = binding.bottomNavigation.findViewById<View>(R.id.navigation_onboard)
        onboardItemView.setOnLongClickListener {
            showPopover(it, "Tap when you're seated.")
            true
        }
        mapView = MapView(requireContext())
        binding.mapContainer.addView(mapView)

        initMap()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_ai -> {
                    showAIDownloadDialog()
                    true
                }
                R.id.navigation_onboard -> {
                    val menuItem = binding.bottomNavigation.menu.findItem(R.id.navigation_onboard)

                    if (isSeated) {
                        unseat()
                    }
                    else {
                    val builder = android.app.AlertDialog.Builder(requireContext())
                    builder.setTitle("Select your destination")
                    builder.setItems(airportNames) { _, which ->
                        val selectedAirport = knownAirports[which]
                        sharedFlightViewModel.setDestination(selectedAirport.icao)
                        Toast.makeText(
                            requireContext(),
                            "Destination set: ${selectedAirport.name}\nLet’s fly this bird!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                        isSeated = true
                        menuItem.setIcon(R.drawable.baseline_airline_seat_recline_extra_24)
                        builder.setCancelable(true)
                        builder.show()
                    }
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



    private fun unseat() {
        val menuItem = binding.bottomNavigation.menu.findItem(R.id.navigation_onboard)
        sharedFlightViewModel.setDestination("")
        isSeated = false
        menuItem.setIcon(R.drawable.baseline_connecting_airports_24)
        Toast.makeText(requireContext(), "You’ve unseated. Ready to reset destination.", Toast.LENGTH_SHORT).show()
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
                .withTextSize(12.0)
            pointAnnotationManager.create(options)
        }
    }

    private fun showPopover(targetView: View, message: String) {
        val popup = android.widget.PopupWindow(requireContext())

        val textView = android.widget.TextView(requireContext()).apply {
            text = message
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
