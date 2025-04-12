package com.example.flyskai.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import model.Coordinates
import model.LocationState
import model.FlightPhase

class HomeViewModel : ViewModel() {

    private val _locationState = MutableLiveData<LocationState>()
    val locationState: LiveData<LocationState> = _locationState
    private var lastAltitude = 0.0
    private var lastTime = System.currentTimeMillis()

    // VSI moving AVG
    private val vsiBuffer = mutableListOf<Float>()
    private val maxBufferSize = 5

    private val _heading = MutableLiveData<Float>().apply {
        value = 0f
    }

    private val _pitch = MutableLiveData<Float>().apply {
        value = 0f
    }
    private val _bank = MutableLiveData<Float>().apply {
        value = 0f
    }
    val heading: LiveData<Float> = _heading

    fun updateDeviceHeading(pitch: Float, bank: Float, azimuth: Float) {
        _heading.value = azimuth
        _pitch.value = pitch
        _bank.value = bank
    }

    fun updateLocationData(lat: Double, lon: Double, speed: Float, altitude: Double, bearing: Float, trueHeading: Float, magneticHeading: Float, courseToDest: Float) {
        val currentTime = System.currentTimeMillis()
        val deltaTimeSec = (currentTime - lastTime) / 1000.0
        val deltaAlt = altitude - lastAltitude
        val rawVerticalSpeed = if (deltaTimeSec > 0) (deltaAlt / deltaTimeSec).toFloat() else 0f

        // Smooth VSI
        vsiBuffer.add(rawVerticalSpeed)
        if (vsiBuffer.size > maxBufferSize) {
            vsiBuffer.removeAt(0)
        }
        val smoothedVerticalSpeed = vsiBuffer.average().toFloat()

        _locationState.value = LocationState(
            coordinates = Coordinates(lat, lon),
            speed = speed,
            altitude = altitude,
            bearing = bearing,
            verticalSpeed=smoothedVerticalSpeed,
            flightPhase = FlightPhase.IDLE,
            trueHeading = trueHeading,
            magneticHeading = magneticHeading,
            courseToDest = courseToDest
        )
        lastAltitude = altitude
        lastTime = currentTime
    }
}