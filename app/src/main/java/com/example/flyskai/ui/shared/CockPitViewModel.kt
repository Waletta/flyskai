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

fun calculateDistanceNm(
    from: Coordinates,
    toAirport: Airport?
): Float {
    if (toAirport == null) return 0f

    val earthRadiusMeters = 6_371_000.0

    val lat1Rad       = toRadians(from.latitude)
    val lat2Rad       = toRadians(toAirport.lat)
    val deltaLatRad   = toRadians(toAirport.lat - from.latitude)
    val deltaLonRad   = toRadians(toAirport.lon - from.longitude)

    val havA = sin(deltaLatRad / 2).pow(2) +
            cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2).pow(2)
    val angularDistance = 2 * atan2(sqrt(havA), sqrt(1 - havA))

    val distanceMeters = earthRadiusMeters * angularDistance
    val distanceNm     = distanceMeters / 1852.0

    return distanceNm.toFloat()
}

class VsiSmoother(private val maxBufferSize: Int = 10) {
    private val buffer = mutableListOf<Float>()
    fun smooth(rawVerticalSpeed: Float): Float {
        buffer.add(rawVerticalSpeed)
        if (buffer.size > maxBufferSize) buffer.removeAt(0)
        return buffer.average().toFloat()
    }
}

class CockPitViewModel : ViewModel() {

    private val _destinationAirport = MutableStateFlow<Airport?>(null)
    val destinationAirport: StateFlow<Airport?> = _destinationAirport.asStateFlow()

    private val _acState = MutableStateFlow(AircraftStateData())
    val acState: StateFlow<AircraftStateData> = _acState.asStateFlow()
    private val vsiSmoother = VsiSmoother(maxBufferSize = 10)

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
