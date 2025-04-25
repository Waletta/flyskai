package com.example.flyskai

import android.Manifest
import com.example.flyskai.ui.components.CockPit
import com.example.flyskai.ui.components.ChatScreen
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.flyskai.databinding.ActivityMainBinding
import com.example.flyskai.ui.shared.CockPitViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.Coordinates
import model.knownAirports
import model.knownLLMConfigurations

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
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
    private val _orientationState = MutableLiveData<Boolean>(false) // Track landscape/portrait
    val orientationState: LiveData<Boolean> get() = _orientationState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: throw IllegalStateException("Rotation Vector Sensor not available")

        cpViewModel.destination.observe(this) { icao ->
            if (!icao.isNullOrBlank()) {
                val airport = knownAirports.find { it.icao == icao }
                if (airport != null) {
                    selectedDestinationLatLon = Pair(airport.lat, airport.lon)
                    selectedIcao = icao
                }
            }
        }

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
        setupToolbarObservers()

        val drawerLayout: DrawerLayout = binding.drawerLayout
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )

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

    private fun setupToolbarObservers() {
        // Observe both acState and orientationState to ensure UI updates on orientation change
        orientationState.observe(this) { isLandscape ->
            Log.d("MainActivity", "OrientationState: isLandscape=$isLandscape, deviceRollAngle=$deviceRollAngle")
            binding.appBarMain.composeCockpitView.setContent {
                if (isLandscape) {
                    Log.d("MainActivity", "Device in landscape, loading ChatScreen")
                    val llmConfig = knownLLMConfigurations.find { it.modelId == "PHI3" }
                    Log.d("MainActivity", "Phi-3 config: $llmConfig")
                    ChatScreen(
                        llmConfig = llmConfig,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Log.d("MainActivity", "Device in portrait or face-up, loading CockPit")
                    CockPit(cpViewModel)
                }
            }
        }
        cpViewModel.acState.observe(this) { state ->
            Log.d("MainActivity", "ACState: $state")
            // Trigger orientation update to ensure UI reflects current state
            _orientationState.value = isLandscape()
        }
    }

    private fun isLandscape(): Boolean {
        // Device tilted horizontally (landscape) when tilt angle is near ±90 degrees
        return deviceRollAngle in 60f..120f || deviceRollAngle in -120f..-60f
    }
    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
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
                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()                //val bank = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()    // roll (bank)
                currentTrueHeading = (azimuth + 360) % 360
                val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                Log.d("MainActivity", "Sensor update - pitch: $pitch, roll: $roll")
                deviceRollAngle = roll

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