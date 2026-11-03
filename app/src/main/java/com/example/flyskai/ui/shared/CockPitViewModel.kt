package com.example.flyskai.ui.shared

import android.hardware.GeomagneticField
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
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sqrt
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
    fun reset() {
        smoothedValue = 0f
        initialized = false
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

    private val _acState = MutableStateFlow(AircraftStateData())
    val acState: StateFlow<AircraftStateData> = _acState.asStateFlow()

    private val _destinationAirport = MutableStateFlow<Airport?>(null)
    val destinationAirport: StateFlow<Airport?> = _destinationAirport.asStateFlow()

    private val _magneticMagnitude = MutableStateFlow(0f)


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

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()


    fun onDestinationClick() {
        _showDestinationDialog.value = true
    }

    fun onRecenterClick() {
        _recenterToggle.value = !_recenterToggle.value
    }

    fun onDestinationSelected(icao: String) {
        setDestinationByIcao(icao)
        pitchReference = _acState.value.pitch
        bankAngleReference = _acState.value.bank
        _showDestinationDialog.value = false
        startCalibrationIndication()
    }

    fun onClearDestination() {
        setDestinationByIcao("")
        pitchReference = 0f
        bankAngleReference = 0f
        _showDestinationDialog.value = false
    }

    fun onDismissDestinationDialog() {
        _showDestinationDialog.value = false
    }

    private fun startCalibrationIndication() {
        pitchSmoother.reset()
        bankSmoother.reset()

        // Set calibration flags to true
        _isCalibrating.value = true

        viewModelScope.launch {
            delay(5000L)
            _isCalibrating.value = false
        }
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
        magneticFieldX: Float,
        magneticFieldY: Float,
        magneticFieldZ: Float
    ) {
        require(speed >= 0f) { "Speed cannot be negative" }
        require(courseOverGround in 0f..360f) { "Course must be between 0 and 360 degrees" }
        require(magneticHeadingFromSensor in 0f..360f) { "Magnetic heading must be between 0 and 360 degrees" }
        require(magneticFieldX.isFinite() && magneticFieldY.isFinite() && magneticFieldZ.isFinite()) { "Magnetic field components must be finite" }

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

        val magneticFieldMagnitude = sqrt(magneticFieldX * magneticFieldX + magneticFieldY * magneticFieldY + magneticFieldZ * magneticFieldZ)

        val computedCourseToDest = calculateCourseToDestination(
            coordinates.latitude,
            coordinates.longitude,
        )

        val etaTimestamp = calculateETA(
            coordinates,
            speed,
            currentTime,
        )

        val distanceMetricToDestination = calculateDistanceToDestination(coordinates)

        if (-smoothedPitch >= 20f || -smoothedPitch <= -10f) {
            playSeatbeltChime()
        }

        _acState.value = AircraftStateData(
            coordinates = coordinates,
            altitude = altitude,
            speed = speed,
            courseOverGround = courseOverGround,
            verticalSpeed = smoothedVerticalSpeed,
            pitch = smoothedPitch.coerceIn(-50f, 50f),
            bank = smoothedBank.coerceIn(-90f, 90f),
            trueHeading = actualTrueHeading,
            magneticHeading = actualMagneticHeading,
            courseToDest = computedCourseToDest,
            destinationAirport = dest,
            destinationDistance = distanceMetricToDestination,
            eta = etaTimestamp,
            magneticFieldMagnitude = magneticFieldMagnitude,
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
            repeat(1) {
                try {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
                    delay(500)
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

        val actualDestAirport = destAirport ?: _destinationAirport.value ?: return 0f

        if (currentLat !in -90.0..90.0 || currentLon !in -180.0..180.0 ||
            actualDestAirport.lat !in -90.0..90.0 || actualDestAirport.lon !in -180.0..180.0) {
            return 0f
        }


        val lat1Rad = Math.toRadians(currentLat)
        val lon1Rad = Math.toRadians(currentLon)
        val lat2Rad = Math.toRadians(actualDestAirport.lat)
        val lon2Rad = Math.toRadians(actualDestAirport.lon)

        var deltaLon = lon2Rad - lon1Rad
        if (deltaLon > PI) deltaLon -= 2 * PI
        if (deltaLon < -PI) deltaLon += 2 * PI

        val deltaPsi = ln(tan(PI / 4 + lat2Rad / 2) / tan(PI / 4 + lat1Rad / 2))

        if (kotlin.math.abs(deltaPsi) < 1e-9) {
            if (deltaLon > 0) return 90f
            if (deltaLon < 0) return 270f
            return 0f
        }

        var bearingRad = atan2(deltaLon, deltaPsi)
        var bearingDeg = Math.toDegrees(bearingRad)
        bearingDeg = (bearingDeg + 360) % 360
        return bearingDeg.toFloat()
    }

    private fun calculateDistanceToDestination(
        coordinates: Coordinates,
    ): Double {
        val dest = _destinationAirport.value ?: return 0.0
        val distanceKm = GeoUtils.smartDistance(
            coordinates.latitude,
            coordinates.longitude,
            dest.lat,
            dest.lon,
            UnitSystem.METRIC
        )
        return distanceKm
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