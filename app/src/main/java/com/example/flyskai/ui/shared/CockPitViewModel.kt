package com.example.flyskai.ui.shared

import android.hardware.GeomagneticField
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import model.AircraftStateData
import model.Airport
import model.Coordinates
import model.knownAirports
import model.GeoUtils
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import android.opengl.Matrix
import kotlin.math.roundToInt
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.tan

private object Constants {
    const val KNOTS_TO_MPS = 0.514444f
    const val MIN_SPEED_MPS = 10.0f
    const val CRUISE_SPEED_KPH = 830.0
    const val TOTAL_FLIGHT_PHASE_PENALTY_MS = 25 * 60 * 1000L
    const val LANDING_PHASE_EXTRA_TIME_MS = 15 * 60 * 1000L
}



class ValueSmoother(
    private val alpha: Float = 0.1f,
    private val deadband: Float = 0.2f
) {
    private var smoothedValue: Float = 0f
    private var initialized = false

    fun smooth(input: Float): Float {
        val filtered = if (kotlin.math.abs(input - smoothedValue) < deadband) {
            smoothedValue
        } else {
            input
        }
        smoothedValue = if (initialized) {
            alpha * filtered + (1 - alpha) * smoothedValue
        } else {
            initialized = true
            filtered
        }
        return smoothedValue
    }
}

class VsiSmoother(
    private val alpha: Float = 0.05f,
    private val warmupMillis: Long = 6000L,
    private val deadband: Float = 0.5f
) {
    private var ema: Float = 0f
    private var startTime: Long = System.currentTimeMillis()
    private val buffer = ArrayDeque<Float>(5)

    fun smooth(rawVerticalSpeed: Float): Float {
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - startTime
        if (elapsed < warmupMillis) return 0f

        if (buffer.size == 5) buffer.removeFirst()
        buffer.addLast(rawVerticalSpeed)
        val median = buffer.sorted()[buffer.size / 2]
        val filtered = if (kotlin.math.abs(median) < deadband) 0f else median
        ema = alpha * filtered + (1 - alpha) * ema
        return ema
    }
}

class CockPitViewModel : ViewModel() {

    private val _destinationAirport = MutableStateFlow<Airport?>(null)
    val destinationAirport: StateFlow<Airport?> = _destinationAirport.asStateFlow()

    private val _rawMagneticField = MutableStateFlow(FloatArray(3) { 0f })
    private val _rawAccelerometerData = MutableStateFlow(FloatArray(3) { 0f })

    private val _magneticMagnitude = MutableStateFlow(0f)

    private val _magneticDipAngle = MutableStateFlow(0f)


    private val _acState = MutableStateFlow(AircraftStateData())
    val acState: StateFlow<AircraftStateData> = _acState.asStateFlow()

    private val _eta = MutableStateFlow<Long?>(null)
    val eta: StateFlow<Long?> = _eta.asStateFlow()

    private val vsiSmoother = VsiSmoother(alpha = 0.2f)
    private val pitchSmoother = ValueSmoother(alpha = 0.1f, deadband = 0.2f)
    private val bankSmoother = ValueSmoother(alpha = 0.1f, deadband = 0.2f)

    private val _showDestinationDialog = MutableStateFlow(false)
    val showDestinationDialog: StateFlow<Boolean> = _showDestinationDialog.asStateFlow()

    private val _recenterToggle = MutableStateFlow(false)
    val recenterToggle: StateFlow<Boolean> = _recenterToggle.asStateFlow()

    private var lastAltitude = 0.0
    private var lastTime = System.currentTimeMillis()

    private var destLatRad: Double = 0.0
    private var destLonRad: Double = 0.0
    private var destLatCos: Double = 1.0

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100) // Initialize beep
    private var pitchReference: Float = 0f
    private var bankAngleReference: Float = 0f

    private var prevAcc: IntArray? = null
    private var prevMag: IntArray? = null


    fun updateRawMagneticField(x: Float, y: Float, z: Float) {
        _rawMagneticField.value = floatArrayOf(x, y, z).copyOf()
        _magneticMagnitude.value = sqrt(x * x + y * y + z * z)
        calculateOrientationAndDip()
    }

    fun updateRawAccelerometerData(x: Float, y: Float, z: Float) {
        _rawAccelerometerData.value = floatArrayOf(x, y, z).copyOf()
        calculateOrientationAndDip()
    }

    private fun calculateOrientationAndDip() {
        val acc = _rawAccelerometerData.value.map { it.roundToInt() }.toIntArray()
        val mag = _rawMagneticField.value.map { it.roundToInt() }.toIntArray()


        if (acc.contentEquals(prevAcc) && mag.contentEquals(prevMag)) {
            return
        }

        // Cache previous values
        prevAcc = acc.copyOf()
        prevMag = mag.copyOf()


        val roTation = FloatArray(16) // From device to Earth's coordinates (North, East, Down)
        val inTernal = FloatArray(16) // Primarily for internal calculations in getRotationMatrix


        if (acc.size < 3 || mag.size < 3 || acc.all { it == 0 } || mag.all { it == 0 }) { // Ensure arrays have enough data
            _magneticDipAngle.value = 0.0f
            return
        }

        val accFloat = acc.map { it.toFloat() }.toFloatArray()
        val magFloat = mag.map { it.toFloat() }.toFloatArray()

        val success = SensorManager.getRotationMatrix(
            roTation,
            inTernal,
            accFloat,
            magFloat
        )

        if (success) {
            val worldMagneticField = FloatArray(4)
            val magVec = floatArrayOf(magFloat[0], magFloat[1], magFloat[2], 0f)

            Matrix.multiplyMV(worldMagneticField, 0, roTation, 0, magVec, 0)

            val bxwWorldEast = worldMagneticField[0] // East component
            val byWorldNorth = worldMagneticField[1] // North component
            val bzWorldUp = worldMagneticField[2] // Up component

            val horizontalMagnitude = sqrt(bxwWorldEast.pow(2) + byWorldNorth.pow(2))
            val bzWorldDown = -bzWorldUp

            if (horizontalMagnitude != 0.0f || bzWorldDown != 0.0f) {
                val dipAngleInRadians = atan2(bzWorldDown.toDouble(), horizontalMagnitude.toDouble())
                _magneticDipAngle.value = Math.toDegrees(dipAngleInRadians).toFloat()
            } else {
                _magneticDipAngle.value = 0.0f // Magnetic field is zero or undefined
            }
        } else {
            _magneticDipAngle.value = 0.0f // Or keep last valid value
        }
    }


    fun onDestinationClick() {
        _showDestinationDialog.value = true
    }

    fun onRecenterClick() {
        _recenterToggle.value = !_recenterToggle.value
    }

    fun onDestinationSelected(icao: String) {
        setDestinationByIcao(icao)
        playSeatbeltChime()
        pitchReference = _acState.value.pitch
        bankAngleReference = _acState.value.bank
        _showDestinationDialog.value = false
    }

    fun onClearDestination() {
        setDestinationByIcao("")
        pitchReference = 0f
        bankAngleReference = 0f
        playSeatbeltChime()
        _showDestinationDialog.value = false
    }

    fun onDismissDestinationDialog() {
        _showDestinationDialog.value = false
    }

    fun setDestinationByIcao(icao: String) {
        val airport = knownAirports
            .find { it.icao.equals(icao.trim(), ignoreCase = true) }
        _destinationAirport.value = airport

        if (airport != null) {
            destLatRad = Math.toRadians(airport.lat)
            destLonRad = Math.toRadians(airport.lon)
            destLatCos = cos(destLatRad)
        } else {
            destLatRad = 0.0
            destLonRad = 0.0
            destLatCos = 1.0
        }
    }

    fun updateACData(
        coordinates: Coordinates,
        altitude: Double,
        speed: Float,
        courseOverGround: Float,
        magneticHeadingFromSensor: Float,
        pitch: Float,
        bank: Float,
    ) {
        require(speed >= 0f) { "Speed cannot be negative" }
        require(courseOverGround in 0f..360f) { "Course must be between 0 and 360 degrees" }
        require(magneticHeadingFromSensor in 0f..360f) { "Magnetic heading must be between 0 and 360 degrees" }

        val currentTime = System.currentTimeMillis()
        val deltaTimeSec = (currentTime - lastTime) / 1000.0
        val deltaAlt = altitude - lastAltitude
        val rawVerticalSpeed = if (deltaTimeSec > 0.001) (deltaAlt / deltaTimeSec).toFloat() else 0f
        val smoothedVerticalSpeed = vsiSmoother.smooth(rawVerticalSpeed)
        val smoothedPitch = pitchSmoother.smooth(pitch.coerceIn(-90f, 90f)) - pitchReference
        val smoothedBank = bankSmoother.smooth(bank.coerceIn(-180f, 180f)) - bankAngleReference
        val geoField = GeomagneticField(
            coordinates.latitude.toFloat(),
            coordinates.longitude.toFloat(),
            altitude.toFloat(),
            currentTime
        )
        val actualMagneticHeading = magneticHeadingFromSensor
        val actualTrueHeading = (magneticHeadingFromSensor - geoField.declination + 360) % 360
        val dest: Airport? = _destinationAirport.value

        val computedCourseToDest = calculateRhumbLineBearing(
            coordinates.latitude,
            coordinates.longitude,
        )

        val etaTimestamp = calculateETA(
            coordinates,
            speed,
            currentTime,
        )

        _acState.value = AircraftStateData(
            coordinates = coordinates,
            altitude = altitude,
            speed = speed,
            courseOverGround = courseOverGround,
            verticalSpeed = smoothedVerticalSpeed,
            pitch = smoothedPitch.coerceIn(-30f, 30f),
            bank = smoothedBank.coerceIn(-90f, 90f),
            trueHeading = actualTrueHeading,
            magneticHeading = actualMagneticHeading,
            courseToDest = computedCourseToDest,
            destinationAirport = dest,
            eta = etaTimestamp,
            magneticFieldMagnitude = _magneticMagnitude.value, // This is still the magnitude from raw sensor
            magneticDipAngle = _magneticDipAngle.value,
        )
        _eta.value = etaTimestamp

        lastAltitude = altitude
        lastTime = currentTime
    }

    override fun onCleared() {
        toneGenerator.release() // Clean up
        super.onCleared()
    }

    fun playSeatbeltChime() {
        viewModelScope.launch {
            repeat(3) {
                try {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 800)
                    delay(1500)
                } catch (e: Exception) {
                    // Log error (e.g., using Timber or Log)
                }
            }
        }
    }


    fun calculateCourseToDestination(
        currentLat: Double,
        currentLon: Double,
        destAirport: Airport? = null
    ): Float {
        val actualDestAirport = destAirport ?: _destinationAirport.value ?: return 0f.also {
            android.util.Log.w("CockPitViewModel", "No destination airport set")
        }

        if (currentLat !in -90.0..90.0 || currentLon !in -180.0..180.0 ||
            actualDestAirport.lat !in -90.0..90.0 || actualDestAirport.lon !in -180.0..180.0) {
            android.util.Log.w("CockPitViewModel", "Invalid coordinates: current=($currentLat, $currentLon), dest=(${actualDestAirport.lat}, ${actualDestAirport.lon})")
            return 0f
        }
        android.util.Log.d("CockPitViewModel", "Calculating course from ($currentLat, $currentLon) to ${actualDestAirport.name} (${actualDestAirport.icao}, ${actualDestAirport.lat}, ${actualDestAirport.lon})")

        // Assuming GeoUtils.smartDistance is correctly implemented and available
        val distance = GeoUtils.smartDistance(currentLat, currentLon, actualDestAirport.lat, actualDestAirport.lon, UnitSystem.METRIC)
        android.util.Log.d("CockPitViewModel", "Distance to ${actualDestAirport.name}: $distance km")
        if (distance < 0.01) return 0f.also {
            android.util.Log.d("CockPitViewModel", "At destination (<10m)")
        }

        val lat1 = Math.toRadians(currentLat)
        val lat2 = Math.toRadians(actualDestAirport.lat)
        val lon1 = Math.toRadians(currentLon)
        val lon2 = Math.toRadians(actualDestAirport.lon)

        // *** THE CRITICAL CORRECTION IS HERE ***
        // Delta longitude should be (destination longitude - current longitude)
        val deltaLonRad = lon2 - lon1

        val y = sin(deltaLonRad) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLonRad)

        val initialBearingRad = atan2(y, x)
        var bearingDeg = Math.toDegrees(initialBearingRad)

        // Normalize to 0-360 degrees
        bearingDeg = (bearingDeg + 360) % 360

        android.util.Log.d("CockPitViewModel", "Raw course to ${actualDestAirport.name}: $bearingDeg°")
        if (bearingDeg > 360 || bearingDeg < 0) {
            android.util.Log.e("CockPitViewModel", "Invalid bearing calculated: $bearingDeg°")
        }
        return bearingDeg.toFloat().also {
            android.util.Log.d("CockPitViewModel", "Final course returned to ${actualDestAirport.name}: $it°")
        }
    }

    fun calculateRhumbLineBearing(
        currentLat: Double,
        currentLon: Double,
        destAirport: Airport? = null
    ): Float {

        val actualDestAirport = destAirport ?: _destinationAirport.value ?: return 0f.also {
            android.util.Log.w("CockPitViewModel", "No destination airport set")
        }

        if (currentLat !in -90.0..90.0 || currentLon !in -180.0..180.0 ||
            actualDestAirport.lat !in -90.0..90.0 || actualDestAirport.lon !in -180.0..180.0) {
            android.util.Log.w("CockPitViewModel", "Invalid coordinates: current=($currentLat, $currentLon), dest=(${actualDestAirport.lat}, ${actualDestAirport.lon})")
            return 0f
        }

        android.util.Log.d("CockPitViewModel", "Calculating rhumb line course from ($currentLat, $currentLon) to ${actualDestAirport.name} (${actualDestAirport.icao}, ${actualDestAirport.lat}, ${actualDestAirport.lon})")

        val lat1Rad = Math.toRadians(currentLat)
        val lon1Rad = Math.toRadians(currentLon)
        val lat2Rad = Math.toRadians(actualDestAirport.lat)
        val lon2Rad = Math.toRadians(actualDestAirport.lon)

        var deltaLon = lon2Rad - lon1Rad

        // Adjust deltaLon for crossing the anti-meridian
        if (deltaLon > PI) deltaLon -= 2 * PI
        if (deltaLon < -PI) deltaLon += 2 * PI

        val deltaPsi = ln(tan(PI / 4 + lat2Rad / 2) / tan(PI / 4 + lat1Rad / 2))

        // Handle the case where deltaPsi is effectively zero (e.g., traveling East/West along a parallel)
        if (kotlin.math.abs(deltaPsi) < 1e-9) { // Using a small epsilon to check for near-zero
            if (deltaLon > 0) return 90f // East
            if (deltaLon < 0) return 270f // West
            return 0f // Same longitude, effectively no movement or at same point
        }

        var bearingRad = atan2(deltaLon, deltaPsi)
        var bearingDeg = Math.toDegrees(bearingRad)

        // Normalize to 0-360 degrees
        bearingDeg = (bearingDeg + 360) % 360

        android.util.Log.d("CockPitViewModel", "Raw rhumb line course to ${actualDestAirport.name}: $bearingDeg°")
        return bearingDeg.toFloat().also {
            android.util.Log.d("CockPitViewModel", "Final rhumb line course returned to ${actualDestAirport.name}: $it°")
        }
    }

    private fun calculateETA(
        coordinates: Coordinates,
        speedMps: Float,
        currentTime: Long
    ): Long? {
        val dest = _destinationAirport.value ?: return null
        val distanceKm = GeoUtils.smartDistance(
            coordinates.latitude,
            coordinates.longitude,
            dest.lat,
            dest.lon,
            UnitSystem.METRIC
        )

        val speedMpsScaled = speedMps * Constants.KNOTS_TO_MPS
        val remainingCruiseTimeMs = (distanceKm / Constants.CRUISE_SPEED_KPH * 3_600_000).toLong()

        val penalty = if (speedMpsScaled < Constants.MIN_SPEED_MPS) {
            Constants.TOTAL_FLIGHT_PHASE_PENALTY_MS // Use full 25-min penalty for the whole trip
        } else {
            Constants.LANDING_PHASE_EXTRA_TIME_MS // Use 15-min penalty for landing only
        }

        val etaUtc = currentTime + remainingCruiseTimeMs + penalty
        return etaUtc
    }
}