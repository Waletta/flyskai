package com.example.flyskai.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import model.Coordinates
import model.LocationState
import model.FlightPhase
//import kotlin.math.abs

class HomeViewModel : ViewModel() {

    private val _locationState = MutableLiveData<LocationState>()
    val locationState: LiveData<LocationState> = _locationState
    private var lastAltitude = 0.0
    private var lastTime = System.currentTimeMillis()


    private var lastHeading = 0f
    private var lastHeadingCheckTime = System.currentTimeMillis()
    private var lastPhase = FlightPhase.IDLE

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


    fun updateLocationData(lat: Double, lon: Double, speed: Float, altitude: Double, bearing: Float) {
        val speedKmh = speed
        val currentTime = System.currentTimeMillis()
        val deltaTimeSec = (currentTime - lastTime) / 1000.0 // seconds
        val deltaAlt = altitude - lastAltitude
        val rawVerticalSpeed = if (deltaTimeSec > 0) (deltaAlt / deltaTimeSec).toFloat() else 0f

        // Smooth VSI
        vsiBuffer.add(rawVerticalSpeed)
        if (vsiBuffer.size > maxBufferSize) {
            vsiBuffer.removeAt(0)
        }
        val smoothedVerticalSpeed = vsiBuffer.average().toFloat()

        //val pitch = _pitch.value ?: 0f

        //val heading = _heading.value ?: 0f
//        val isPushback = detectPushback(speedKmh / 3.6f, heading)
//        val currentPhase = when {
//            isPushback -> TakeoffPhase.PUSHBACK
//            speedKmh in 8f..40f -> TakeoffPhase.TAXI
//            speedKmh > 40f && abs(pitch) < 5f -> TakeoffPhase.TAKEOFF_ROLL
//            else -> TakeoffPhase.IDLE
//        }
//        lastPhase = currentPhase

        _locationState.value = LocationState(
            coordinates = Coordinates(lat, lon),
            speed = speedKmh,
            altitude = altitude,
            bearing = bearing,
            verticalSpeed=smoothedVerticalSpeed,
            flightPhase = FlightPhase.IDLE
        )
        lastAltitude = altitude
        lastTime = currentTime
    }
}