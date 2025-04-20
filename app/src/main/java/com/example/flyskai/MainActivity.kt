package com.example.flyskai
import com.example.flyskai.ui.components.ChatScreen
import com.example.flyskai.ui.components.CockPit
import com.example.flyskai.ui.home.HomeViewModel


import android.Manifest
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.flyskai.databinding.ActivityMainBinding
import com.example.flyskai.ui.shared.SharedFlightViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import model.knownAirports
// import model.ChatViewModel
import model.PhiModel
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
//    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequest = 100
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var sensorManager: SensorManager
    private lateinit var rotationSensor: Sensor
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val sharedFlightViewModel: SharedFlightViewModel by viewModels()
    private var selectedDestinationLatLon: Pair<Double, Double>? = null
    private var selectedIcao: String? = null


    private var skipFirstFix = false
    private var lastRotationVector: FloatArray = FloatArray(3)
//    private val _northUpMode = MutableStateFlow(true)
//    val northUpMode: StateFlow<Boolean> = _northUpMode

//    fun toggleNorthUp() {
//        _northUpMode.value = !_northUpMode.value
//    }

    private lateinit var tokenizer: Any

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
 //       sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
 //       rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
 //           ?: throw IllegalStateException("Rotation Vector Sensor not available")

 //       fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//        sharedFlightViewModel.destination.observe(this) { icao ->
//            if (!icao.isNullOrBlank()) {
//                val airport = knownAirports.find { it.icao == icao }
//                if (airport != null) {
//                    selectedDestinationLatLon = Pair(airport.lat, airport.lon)
//                    selectedIcao = icao
//                }
//            }
//        }

//        locationRequest = LocationRequest.Builder(2000L)
//            .setMinUpdateIntervalMillis(4000L)
//            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
//            .setMaxUpdateDelayMillis(8000L)
//            .build()

//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult) {
//                if (skipFirstFix) {
//                    skipFirstFix = false
//                    return
//                }
//                locationResult.lastLocation?.let { location ->
//                    CoroutineScope(Dispatchers.Default).launch {
//                        val lat = location.latitude
//                        val lon = location.longitude
//                        val speedMs = location.speed
//                        val altitude = location.altitude
//                        val bearing = location.bearing
//
//                        // 1. Compute rotation matrix and true heading
//                        val rotationMatrix = FloatArray(9)
//                        val orientation = FloatArray(3)
//                        var trueHeading = 0f
//
//                        rotationSensor.let { sensor ->
//                            SensorManager.getRotationMatrixFromVector(rotationMatrix, lastRotationVector)
//                            SensorManager.getOrientation(rotationMatrix, orientation)
//                            trueHeading = Math.toDegrees(orientation[0].toDouble()).toFloat()
//                            trueHeading = (trueHeading + 360) % 360 // normalize
//                        }
//
//
//                        // 2. Compute geomagnetic field and magnetic heading
//                        val geoField = GeomagneticField(
//                            lat.toFloat(),
//                            lon.toFloat(),
//                            altitude.toFloat(),
//                            System.currentTimeMillis()
//                        )
//                        val magneticHeading = (trueHeading - geoField.declination + 360) % 360
//
//
//                        // 3. Compute courseToDest if destination is set
//                        val courseToDest = selectedDestinationLatLon?.let { (destLat, destLon) ->
//                            computeCourseToDestination(
//                                currentLat = lat,
//                                currentLon = lon,
//                                destLat = destLat,
//                                destLon = destLon
//                            )
//                        } ?: 0f // fallback to 0 if no destination selected


//                        withContext(Dispatchers.Main) {
//                            homeViewModel.updateLocationData(lat, lon, speedMs, altitude, bearing, trueHeading, magneticHeading, courseToDest)
//                        }
//                    }
//                }
//            }
//        }


        // ✅ INIT MODEL EARLY
        Log.i("MAINEVENT", "LLM model path:")

        val model = PhiModel.instance()
        val path = File(filesDir, "llms_data/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf").absolutePath
        Log.i("MAINEVENT", "LLM model path: $path")
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Log.i("MAINEVENT", "✅ Model loading")
                model.load(path)
                Log.i("MAINEVENT", "✅ Model loaded successfully")
            } catch (e: Exception) {
                Log.e("MAINEVENT", "❌ Model load failed", e)
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setupToolbarObservers()
//        val chatViewModel = ChatViewModel(applicationContext)
//
//        binding.appBarMain.composeCockpitView.setContent {
//            ChatScreen(viewModel = chatViewModel)
//        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )

//        if (checkPermission()) {
//            startLocationUpdates()
//        } else {
//            requestPermission()
//        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
//        sensorManager.registerListener(sensorListener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        skipFirstFix = true
//        if (checkPermission()) {
//            startLocationUpdates()
//        }
    }

    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Exit") { _, _ -> finish() }
            .show()
    }

    override fun onPause() {
        super.onPause()
 //       fusedLocationClient.removeLocationUpdates(locationCallback)
//        sensorManager.unregisterListener(sensorListener)
    }

//    private fun SetupToolbarObservers() {
//        ChatScreen()
//        homeViewModel.locationState.observe(this) { state ->
//            val track = state.bearing
//            //val heading = homeViewModel.heading.value ?: 0f
//             binding.appBarMain.composeCockpitView.setContent {
//                CockPit(track = track, trueHeading = state.trueHeading, magneticHeading = state.magneticHeading,
//                    speed = state.speed,
//                    altitude = state.altitude.toFloat(),
//                    verticalSpeed = state.verticalSpeed,
//                    flightPhase = state.flightPhase,
//                    northUpMode= false, course = state.courseToDest,
//                    destination = sharedFlightViewModel.destination.value ?: "",
//                    currentLat = state.coordinates.latitude, currentLon = state.coordinates.longitude)
////                 PhiChatScreen()
//
//             }
//        }
//    }
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
//
//    private val sensorListener = object : SensorEventListener {
//        override fun onSensorChanged(event: SensorEvent?) {
//            if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
//                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
//                SensorManager.getOrientation(rotationMatrix, orientationAngles)
//                val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
//                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()   // pitch (nose up/down)
//                val bank = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()    // roll (bank)
//                homeViewModel.updateDeviceHeading(pitch, bank, (azimuth + 360) % 360)
//            }
//        }
//
//        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
//    }

//    private fun startLocationUpdates() {
//        try {
//            if (checkPermission()) {
//                fusedLocationClient.requestLocationUpdates(
//                    locationRequest,
//                    locationCallback,
//                    Looper.getMainLooper()
//                )
//            } else {
//                requestPermission()
//            }
//        } catch (e: SecurityException) {
//            Log.e("FlyskAI", "SecurityException: ${e.message}")
//        }
//    }

    private fun computeCourseToDestination(
        currentLat: Double,
        currentLon: Double,
        destLat: Double,
        destLon: Double
    ): Float {
        val lat1Rad = Math.toRadians(currentLat)
        val lat2Rad = Math.toRadians(destLat)
        val deltaLonRad = Math.toRadians(destLon - currentLon)

        val y = sin(deltaLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) -
                sin(lat1Rad) * cos(lat2Rad) * cos(deltaLonRad)
        val initialBearingRad = atan2(y, x)

        val bearingDeg = (Math.toDegrees(initialBearingRad) + 360) % 360
        return bearingDeg.toFloat()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequest && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()

            //startLocationUpdates()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}