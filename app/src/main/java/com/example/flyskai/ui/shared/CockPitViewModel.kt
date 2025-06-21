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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import android.opengl.Matrix

private object Constants {
    const val EARTH_RADIUS_METERS = 6_371_000.0
    const val KNOTS_TO_MPS = 0.514444f
    const val MIN_SPEED_MPS = 10.0f
    const val CRUISE_SPEED_KPH = 830.0
    const val TOTAL_FLIGHT_PHASE_PENALTY_MS = 25 * 60 * 1000L
    const val LANDING_PHASE_EXTRA_TIME_MS = 15 * 60 * 1000L
}


private fun calculateFlatDistance(x: Double, y: Double, unit: UnitSystem): Double {
    val distanceMeters = sqrt(x.pow(2) + y.pow(2)) * Constants.EARTH_RADIUS_METERS
    return when (unit) {
        UnitSystem.METRIC -> distanceMeters / 1000.0
        UnitSystem.IMPERIAL -> distanceMeters / 1852.0
    }
}


private fun calculateHaversineDistance(
    fromLat: Double,
    fromLon: Double,
    toLat: Double,
    toLon: Double,
    unit: UnitSystem
): Double {
    val lat1 = Math.toRadians(fromLat)
    val lat2 = Math.toRadians(toLat)
    val deltaLon = Math.toRadians(toLon - fromLon)

    val havA = sin((lat2 - lat1) / 2).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
    val angularDistance = 2 * atan2(sqrt(havA), sqrt(1 - havA))

    val distanceMeters = angularDistance * Constants.EARTH_RADIUS_METERS
    return when (unit) {
        UnitSystem.METRIC -> distanceMeters / 1000.0
        UnitSystem.IMPERIAL -> distanceMeters / 1852.0
    }
}

@Suppress("SameParameterValue")
private fun smartDistance(
    fromLat: Double,
    fromLon: Double,
    toLat: Double,
    toLon: Double,
    unit: UnitSystem = UnitSystem.METRIC
): Double {
    val deltaLat = Math.toRadians(toLat - fromLat)
    val deltaLon = Math.toRadians(toLon - fromLon)

    return if (sqrt(deltaLat.pow(2) + deltaLon.pow(2)) < 0.01) {
        val x = deltaLon * cos(Math.toRadians((fromLat + toLat) / 2))
        val y = deltaLat
        calculateFlatDistance(x, y, unit)
    } else {
        calculateHaversineDistance(fromLat, fromLon, toLat, toLon, unit)
    }
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

    fun updateRawMagneticField(x: Float, y: Float, z: Float) {
        _rawMagneticField.value = floatArrayOf(x, y, z)
        _magneticMagnitude.value = sqrt(x * x + y * y + z * z)
        calculateOrientationAndDip()
    }

    fun updateRawAccelerometerData(x: Float, y: Float, z: Float) {
        _rawAccelerometerData.value = floatArrayOf(x, y, z)
        calculateOrientationAndDip() // Recalculate dip when acc data changes
    }


    private fun calculateOrientationAndDip() {
        val R = FloatArray(16) // From device to Earth's coordinates (North, East, Down)
        val I = FloatArray(16) // Primarily for internal calculations in getRotationMatrix

        val acc = _rawAccelerometerData.value
        val mag = _rawMagneticField.value

        if (acc.size < 3 || mag.size < 3 || acc.all { it == 0f } || mag.all { it == 0f }) { // Ensure arrays have enough data
            _magneticDipAngle.value = 0.0f // Or keep previous value / NaN
            return
        }

        val success = SensorManager.getRotationMatrix(
            R,
            I,
            acc,
            mag
        )

        if (success) {
            val worldMagneticField = FloatArray(4)
            val magVec = floatArrayOf(mag[0], mag[1], mag[2], 0f)


            Matrix.multiplyMV(worldMagneticField, 0, R, 0, magVec, 0)

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
        _showDestinationDialog.value = false
    }

    fun onClearDestination() {
        setDestinationByIcao("")
        pitchReference = 0f
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
        val currentTime = System.currentTimeMillis()
        val deltaTimeSec = (currentTime - lastTime) / 1000.0
        val deltaAlt = altitude - lastAltitude
        val rawVerticalSpeed = if (deltaTimeSec > 0.001) (deltaAlt / deltaTimeSec).toFloat() else 0f
        val smoothedVerticalSpeed = vsiSmoother.smooth(rawVerticalSpeed)
        val smoothedPitch = pitchSmoother.smooth(pitch.coerceIn(-90f, 90f)) - pitchReference
        val smoothedBank = bankSmoother.smooth(bank.coerceIn(-180f, 180f))
        val geoField = GeomagneticField(
            coordinates.latitude.toFloat(),
            coordinates.longitude.toFloat(),
            altitude.toFloat(),
            currentTime
        )
        val actualMagneticHeading = magneticHeadingFromSensor
        val actualTrueHeading = (magneticHeadingFromSensor - geoField.declination + 360) % 360
        val dest: Airport? = _destinationAirport.value

        val computedCourseToDest = calculateCourseToDestination(
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
            repeat(3) { // 3 beeps
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 800)
                delay(1500) // 200ms beep + 100ms pause
            }
        }
    }


    fun calculateCourseToDestination(
        currentLat: Double,
        currentLon: Double,
    ): Float {
        val destAirport = _destinationAirport.value ?: return 0f
        val lat1 = Math.toRadians(currentLat)
        val lat2 = Math.toRadians(destAirport.lat)
        val deltaLonRad = Math.toRadians(destAirport.lon - currentLon)
        val y = sin(deltaLonRad) * cos(lat2)
        val x = cos(lat1) * sin(lat2) -
                sin(lat1) * cos(lat2) * cos(deltaLonRad)
        val initialBearingRad = atan2(y, x)
        return ((Math.toDegrees(initialBearingRad) + 360) % 360).toFloat()
    }

    private fun calculateETA(
        coordinates: Coordinates,
        speedMps: Float,
        currentTime: Long
    ): Long? {
        val dest = _destinationAirport.value ?: return null
        val distanceKm = smartDistance(
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
        // --- TEMPORARY FIX: Manually subtract 2 hours ---
        // The original code had a manual 2-hour subtraction. Re-evaluate if this is still needed
        // based on your exact time zone requirements and how `eta` is displayed.
        // val twoHourFixMs = 2 * 60 * 60 * 1000L // 2 hours
        // return etaUtc - twoHourFixMs
        return etaUtc
    }
}