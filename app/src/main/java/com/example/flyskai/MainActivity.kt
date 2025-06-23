package com.example.flyskai

import android.Manifest
import com.example.flyskai.ui.components.MainScreen
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.flyskai.databinding.ActivityMainBinding
import com.example.flyskai.ui.shared.CockPitViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.Coordinates

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val locationPermissionRequest = 100
    private lateinit var acCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var sensorHandler: SensorHandler
    private val cpViewModel: CockPitViewModel by viewModels()
    private var skipFirstFix = false


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorHandler = SensorHandler(getSystemService(SENSOR_SERVICE) as SensorManager)

        locationRequest = LocationRequest.Builder(1000L)
            .setMinUpdateIntervalMillis(2000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateDelayMillis(4000L)
            .build()

        acCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (skipFirstFix) {
                    skipFirstFix = false
                    return
                }
                locationResult.lastLocation?.let { location ->
                    lifecycleScope.launch(Dispatchers.Default) {
                        val lat = location.latitude
                        val lon = location.longitude
                        val speedMs = location.speed
                        val altitude = location.altitude
                        val courseOverGround = location.bearing
                        val magneticHeading = sensorHandler.currentMagneticHeading.get()
                        val pitch = sensorHandler.latestPitch.get()
                        val roll = sensorHandler.latestRoll.get()
                        val magneticField = sensorHandler.latestMagneticField.get()
                        if (magneticField.all { it == 0f }) {
                            Log.w("Sensor", "Magnetometer data not ready")
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            cpViewModel.updateACData(Coordinates(lat, lon), altitude, speedMs, courseOverGround,
                                magneticHeading, pitch, roll,
                                magneticFieldX = magneticField[0],
                                magneticFieldY = magneticField[1],
                                magneticFieldZ = magneticField[2]
                                )
                        }
                    }
                }
            }
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mainComposeView.setContent {
            val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            MainScreen(
                viewModel = cpViewModel,
                isLandscape = isLandscape,
            )
        }

        if (checkPermission()) {
            startLocationUpdates()
        } else {
            requestPermission()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        sensorHandler.startListening()
        skipFirstFix = true
        if (checkPermission()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(acCallback)
        sensorHandler.stopListening()
    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionRequest
        )
    }

    private fun startLocationUpdates() {
        try {
            if (checkPermission()) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    acCallback,
                    Looper.getMainLooper()
                )
            } else {
                requestPermission()
            }
        } catch (e: SecurityException) {
            Log.e("FlyskAI", "SecurityException: ${e.message}")
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequest && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}