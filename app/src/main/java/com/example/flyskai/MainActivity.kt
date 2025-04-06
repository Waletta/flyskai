package com.example.flyskai

import android.Manifest
import com.example.flyskai.ui.components.CockPit
import com.example.flyskai.ui.home.HomeViewModel
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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.flyskai.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequest = 100
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var sensorManager: SensorManager
    private lateinit var rotationSensor: Sensor
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var skipFirstFix = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: throw IllegalStateException("Rotation Vector Sensor not available")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(1000L)
            .setMinUpdateIntervalMillis(2000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateDelayMillis(4000L)
            .build()

        locationCallback = object : LocationCallback() {
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
                        withContext(Dispatchers.Main) {
                            homeViewModel.updateLocationData(lat, lon, speedMs, altitude, bearing)
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
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(sensorListener)
    }

    private fun setupToolbarObservers() {
        homeViewModel.locationState.observe(this) { state ->
            val track = state.bearing
            val heading = homeViewModel.heading.value ?: 0f
             binding.appBarMain.composeCockpitView.setContent {
                CockPit(track = track, heading = heading,
                    speed = state.speed,
                    altitude = state.altitude.toFloat(),
                    verticalSpeed = state.verticalSpeed,
                    takeoffPhase = state.takeoffPhase)
            }
        }
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
                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()   // pitch (nose up/down)
                val bank = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()    // roll (bank)
                homeViewModel.updateDeviceHeading(pitch, bank, (azimuth + 360) % 360)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun startLocationUpdates() {
        try {
            if (checkPermission()) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
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