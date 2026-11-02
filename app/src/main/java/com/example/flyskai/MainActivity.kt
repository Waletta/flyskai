package com.example.flyskai

import android.Manifest
import android.content.res.Configuration
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.LocalConfiguration
import com.example.flyskai.databinding.ActivityMainBinding
import com.example.flyskai.ui.components.MainScreen
import com.example.flyskai.ui.shared.CockPitViewModel
import com.google.android.gms.location.LocationServices
import model.Coordinates

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private lateinit var locationHandler: LocationHandler
    private lateinit var sensorHandler: SensorHandler
    private val cpViewModel: CockPitViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted) {
            startLocationUpdates()
        } else if (coarseGranted) {
            Toast.makeText(
                this,
                "Precise location is recommended for altitude and flight tracking",
                Toast.LENGTH_LONG,
            ).show()
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorHandler = SensorHandler(getSystemService(SENSOR_SERVICE) as SensorManager)
        locationHandler = LocationHandler(this, fusedLocationClient)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mainComposeView.setContent {
            val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            MainScreen(
                viewModel = cpViewModel,
                isLandscape = isLandscape,
            )
        }

        requestLocationPermissions()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        // Sensors start immediately so artificial horizon & compass are live even without GPS lock
        sensorHandler.startListening { pitch, roll, heading, magX, magY, magZ ->
            cpViewModel.updateAttitude(pitch, roll, heading, magX, magY, magZ)
        }

        if (locationHandler.hasLocationPermission()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorHandler.stopListening()
        locationHandler.stopUpdates()
    }

    private fun requestLocationPermissions() {
        if (locationHandler.hasLocationPermission()) {
            startLocationUpdates()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    private fun startLocationUpdates() {
        locationHandler.startUpdates { lat, lon, speed, altitude, bearing ->
            cpViewModel.updateLocation(Coordinates(lat, lon), altitude, speed, bearing)
        }
    }
}
