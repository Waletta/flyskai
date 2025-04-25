package com.example.flyskai

import android.Manifest
import com.example.flyskai.ui.components.MainScreen
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flyskai.databinding.ActivityMainBinding
import com.example.flyskai.ui.shared.CockPitViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.Coordinates
import model.knownAirports

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val locationPermissionRequest = 100
    private lateinit var acCallback: LocationCallback
    private lateinit var acRequest: LocationRequest
    private lateinit var sensorManager: SensorManager
    private lateinit var rotationSensor: Sensor
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val cpViewModel: CockPitViewModel by viewModels()
    private var selectedDestinationLatLon: Pair<Double, Double>? = null
    private var selectedIcao: String? = null
    private var currentTrueHeading: Float = 0f
    private var skipFirstFix = false

    private var deviceRollAngle: Float = 0f // Track device roll for orientation detection
    private val _orientationState = MutableStateFlow(false)
    val orientationState: StateFlow<Boolean> get() = _orientationState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: throw IllegalStateException("Rotation Vector Sensor not available")


        acRequest = LocationRequest.Builder(1000L)
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
                    CoroutineScope(Dispatchers.Default).launch {
                        val lat = location.latitude
                        val lon = location.longitude
                        val speedMs = location.speed
                        val altitude = location.altitude
                        val bearing = location.bearing

                        val trueHeading = currentTrueHeading
                        withContext(Dispatchers.Main) {
                            cpViewModel.updateACData(Coordinates(lat, lon), altitude, speedMs, bearing, trueHeading)
                        }
                    }
                }
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mainComposeView.setContent {
            val isLandscapeState = orientationState.collectAsStateWithLifecycle(initialValue = false)
            val isLandscape = isLandscapeState.value
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
        savedInstanceState?.let {
            deviceRollAngle = it.getFloat("deviceRollAngle", 0f)
            _orientationState.value = isLandscape()
        }
    }

    private fun isLandscape(): Boolean {
        return deviceRollAngle in 60f..120f || deviceRollAngle in -120f..-60f
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(sensorListener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        skipFirstFix = true
        if (checkPermission()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(acCallback)
        sensorManager.unregisterListener(sensorListener)
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

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                Log.d("MainActivity", "Sensor update - pitch: $pitch, roll: $roll")
                deviceRollAngle = roll
                currentTrueHeading = (azimuth + 360) % 360
                val newLandscape = isLandscape()
                Log.d("MainActivity", "Updating orientationState: $newLandscape")
                _orientationState.value = newLandscape
            }
        }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun startLocationUpdates() {
        try {
            if (checkPermission()) {
                fusedLocationClient.requestLocationUpdates(
                    acRequest,
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