package com.example.flyskai.ui.shared

import android.hardware.GeomagneticField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import model.Coordinates
import model.AircraftStateData
import model.Airport
import model.knownAirports
import java.lang.Math.toRadians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


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

class VsiSmoother(private val maxBufferSize: Int = 10) {
    private val buffer = mutableListOf<Float>()

    fun smooth(rawVerticalSpeed: Float): Float {
        buffer.add(rawVerticalSpeed)
        if (buffer.size > maxBufferSize) buffer.removeAt(0)
        return buffer.average().toFloat()
    }
}

class CockPitViewModel : ViewModel() {

    private val _acState = MutableLiveData<AircraftStateData>()
    val acState: LiveData<AircraftStateData> = _acState

    private val _destination = MutableLiveData<String>().apply { value = "" }
    val destination: LiveData<String?> = _destination

    private val vsiSmoother = VsiSmoother(maxBufferSize = 10)

    fun setDestination(icao: String) {
        if (knownAirports.any { it.icao.equals(icao, ignoreCase = true) }) {
            _destination.value = icao.uppercase()
        } else {
            _destination.value = ""
        }
    }

    private var lastAltitude = 0.0
    private var lastTime = System.currentTimeMillis()

    /**
     * Updates the aircraft's location and derived data.
     * @param coordinates The current GPS coordinates (latitude, longitude).
     * @param speed The ground speed in knots.
     * @param altitude The altitude in feet.
     * @param bearing The bearing (direction of movement) in degrees.
     * @param trueHeading The true heading in degrees relative to true north.
     * @param magneticHeading The magnetic heading in degrees relative to magnetic north.
     * @param courseToDest The course to the destination in degrees.
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

        val destIcao = _destination.value
        val destAirport = knownAirports.find { it.icao.equals(destIcao, ignoreCase = true) }
        val computedCourseToDest = calculateCourseToDestination(
            coordinates.latitude,
            coordinates.longitude,
            destAirport
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
            destinationAirport = destAirport
        )
        lastAltitude = altitude
        lastTime = currentTime
    }

}
