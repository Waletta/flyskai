package com.example.flyskai.ui.shared

import android.hardware.GeomagneticField
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import model.Coordinates
import model.AircraftStateData
import model.Airport
import model.knownAirports
import java.lang.Math.toRadians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun calculateCourseToDestination(
    currentLat: Double,
    currentLon: Double,
    destAirport: Airport?
): Float {
    if (destAirport == null) return 0f
    val lat1 = toRadians(currentLat)
    val lat2 = toRadians(destAirport.lat)
    val deltaLonRad = toRadians(destAirport.lon - currentLon)
    val y = sin(deltaLonRad) * cos(lat2)
    val x = cos(lat1) * sin(lat2) -
            sin(lat1) * cos(lat2) * cos(deltaLonRad)
    val initialBearingRad = atan2(y, x)
    return ((Math.toDegrees(initialBearingRad) + 360) % 360).toFloat()
}

fun smartDistance(
    fromLat: Double,
    fromLon: Double,
    toLat: Double,
    toLon: Double,
    unit: UnitSystem = UnitSystem.METRIC
): Double {
    val earthRadiusMeters = 6_371_000.0

    val deltaLat = toRadians(toLat - fromLat)
    val deltaLon = toRadians(toLon - fromLon)

    val avgLat = toRadians((fromLat + toLat) / 2.0)
    val cosAvgLat = cos(avgLat)

    val x = deltaLon * cosAvgLat
    val y = deltaLat
    val flatDistanceRad = sqrt(x * x + y * y)

    return if (flatDistanceRad < 0.01) { // 0.01 rad ≈ 60 km
        val distanceMeters = flatDistanceRad * earthRadiusMeters
        convertDistance(distanceMeters, unit)
    } else {
        val lat1Rad = toRadians(fromLat)
        val lat2Rad = toRadians(toLat)

        val havA = sin(deltaLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLon / 2).pow(2)
        val angularDistance = 2 * atan2(sqrt(havA), sqrt(1 - havA))

        val distanceMeters = earthRadiusMeters * angularDistance
        convertDistance(distanceMeters, unit)
    }
}


private fun convertDistance(distanceMeters: Double, unit: UnitSystem): Double {
    return when (unit) {
        UnitSystem.METRIC -> distanceMeters / 1000.0
        UnitSystem.IMPERIAL -> distanceMeters / 1852.0
    }
}

// Warmup up 6 secs before calc of EMA
class VsiSmoother(private val alpha: Float = 0.2f, private val warmupMillis: Long = 6000L) {
    private var ema: Float = 0f
    private var startTime: Long = System.currentTimeMillis()

    fun smooth(rawVerticalSpeed: Float): Float {
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - startTime

        return if (elapsed < warmupMillis) {
            0f // During warm-up period: always return 0
        } else {
            ema = alpha * rawVerticalSpeed + (1 - alpha) * ema
            ema
        }
    }
}

class CockPitViewModel : ViewModel() {
    private val _destinationAirport = MutableStateFlow<Airport?>(null)
    val destinationAirport: StateFlow<Airport?> = _destinationAirport.asStateFlow()
    private val _acState = MutableStateFlow(AircraftStateData())
    val acState: StateFlow<AircraftStateData> = _acState.asStateFlow()
    private val vsiSmoother = VsiSmoother(alpha = 0.2f)
    private var lastAltitude = 0.0
    private var lastTime = System.currentTimeMillis()


    // --- Cached trig values for the destination ---
    private var destLatRad: Double = 0.0
    private var destLonRad: Double = 0.0
    private var destCosLat: Double = 1.0

    /** Call this when the user selects a new ICAO. */
    fun setDestinationByIcao(icao: String) {
        val airport = knownAirports
            .find { it.icao.equals(icao.trim(), ignoreCase = true) }
        _destinationAirport.value = airport

        if (airport != null) {
            destLatRad  = toRadians(airport.lat)
            destLonRad  = toRadians(airport.lon)
            destCosLat  = cos(destLatRad)
        } else {
            destLatRad  = 0.0
            destLonRad  = 0.0
            destCosLat  = 1.0
        }
    }

    /**
     * Updates the aircraft's location and derived data.
     * @param coordinates The current GPS coordinates (latitude, longitude).
     * @param speed The ground speed in knots.
     * @param altitude The altitude in feet.
     * @param bearing The bearing (direction of movement) in degrees.
     * @param trueHeading The true heading in degrees relative to true north.
     */
    fun updateACData(coordinates: Coordinates, altitude: Double, speed: Float, bearing: Float,
                           trueHeading: Float,
                           ) {
        val currentTime = System.currentTimeMillis()
        val deltaTimeSec = (currentTime - lastTime) / 1000.0
        val deltaAlt = altitude - lastAltitude
        val rawVerticalSpeed = if (deltaTimeSec > 0.001) (deltaAlt / deltaTimeSec).toFloat() else 0f
        val smoothedVerticalSpeed = vsiSmoother.smooth(rawVerticalSpeed)

        val geoField = GeomagneticField(
            coordinates.latitude.toFloat(),
            coordinates.longitude.toFloat(),
            altitude.toFloat(),
            currentTime
        )
        val magneticHeading = (trueHeading - geoField.declination + 360) % 360

        val dest: Airport? = _destinationAirport.value
        val computedCourseToDest = calculateCourseToDestination(
            coordinates.latitude,
            coordinates.longitude,
            dest
        )

        _acState.value = AircraftStateData(
            coordinates = coordinates,
            altitude = altitude,
            speed = speed,
            bearing = bearing,
            verticalSpeed=smoothedVerticalSpeed,
            pitch= 0f,
            bank= 0f,
            trueHeading = trueHeading,
            magneticHeading = magneticHeading,
            courseToDest = computedCourseToDest,
            destinationAirport = dest
        )
        lastAltitude = altitude
        lastTime = currentTime
    }

}
