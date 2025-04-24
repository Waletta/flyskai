package com.example.flyskai.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import model.Coordinates
import model.LocationState
import model.FlightPhase

class VsiSmoother(private val maxBufferSize: Int = 10) {
    private val buffer = mutableListOf<Float>()

    fun smooth(rawVerticalSpeed: Float): Float {
        buffer.add(rawVerticalSpeed)
        if (buffer.size > maxBufferSize) buffer.removeAt(0)
        return buffer.average().toFloat()
    }
}

class CockPitViewModel : ViewModel() {

    private val _locationState = MutableLiveData<LocationState>()
    val locationState: LiveData<LocationState> = _locationState

    private val _heading = MutableLiveData<Float>().apply {value = 0f}
    val heading: LiveData<Float> = _heading

    private val _pitch = MutableLiveData<Float>().apply {value = 0f}
    val pitch: LiveData<Float> = _pitch

    private val _bank = MutableLiveData<Float>().apply {value = 0f}
    val bank: LiveData<Float> = _bank

    private val _destination = MutableLiveData<String>().apply { value = "" }
    val destination: LiveData<String?> = _destination

    private val vsiSmoother = VsiSmoother(maxBufferSize = 10)

    fun setDestination(icao: String) {
        _destination.value = icao
    }

    fun updateDeviceHeading(pitch: Float, bank: Float, azimuth: Float) {
        _heading.value = azimuth
        _pitch.value = pitch
        _bank.value = bank
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
    fun updateLocationData(coordinates: Coordinates, speed: Float, altitude: Double, bearing: Float,
                           trueHeading: Float, magneticHeading: Float, courseToDest: Float) {
        val currentTime = System.currentTimeMillis()
        val deltaTimeSec = (currentTime - lastTime) / 1000.0
        val deltaAlt = altitude - lastAltitude
        val rawVerticalSpeed = if (deltaTimeSec > 0.001) (deltaAlt / deltaTimeSec).toFloat() else 0f
        val smoothedVerticalSpeed = vsiSmoother.smooth(rawVerticalSpeed)

        _locationState.value = LocationState(
            coordinates = coordinates,
            speed = speed,
            altitude = altitude,
            bearing = bearing,
            verticalSpeed=smoothedVerticalSpeed,
            flightPhase = FlightPhase.IDLE,
            trueHeading = trueHeading,
            magneticHeading = magneticHeading,
            courseToDest = courseToDest,
        )
        lastAltitude = altitude
        lastTime = currentTime
    }

}
