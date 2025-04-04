package com.example.flyskai.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import model.Coordinates
import model.LocationState

class HomeViewModel : ViewModel() {

    private val _locationState = MutableLiveData<LocationState>()
    val locationState: LiveData<LocationState> = _locationState

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
    val pitch: LiveData<Float> = _pitch
    val bank: LiveData<Float> = _bank


    fun updateDeviceHeading(pitch: Float, bank: Float, azimuth: Float) {
        _heading.value = azimuth
        _pitch.value = pitch
        _bank.value = bank
    }

    fun updateLocationData(lat: Double, lon: Double, speed: Float, altitude: Double, bearing: Float) {
        val speedKmh = speed * 3.6f
        _locationState.value = LocationState(
            coordinates = Coordinates(lat, lon),
            speed = speedKmh,
            altitude = altitude,
            bearing = bearing
        )
    }
}