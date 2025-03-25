package com.example.flyskai.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _locationState = MutableLiveData<LocationState>()
    val locationState: LiveData<LocationState> = _locationState

    private val _speed = MutableLiveData<Float>().apply {
        value = 0f
    }   // in m/s
    private val _altitude = MutableLiveData<Double>().apply {
        value = 0.00
    }

    private val _bearing = MutableLiveData<Float>().apply {
        value = 0f
    }

    private val _text = MutableLiveData<String>().apply {
        value = "Waiting for GPS..."
    }

    // private val _coordinates = MutableLiveData<String>("Waiting for GPS...")


    fun updateLocationData(lat: Double, lon: Double, speed: Float, altitude: Double, bearing: Float) {
        // Update consolidated state
        val speedKmh = speed * 3.6f
        _locationState.value = LocationState(
            coordinates = Coordinates(lat, lon),
            speed = speedKmh,
            altitude = altitude,
            bearing = bearing
        )

        // Update individual properties
        _speed.value = speed
        _altitude.value = altitude
//        _coordinates.value = "📍 Lat: ${"%.6f".format(lat)}\n🧭 Lon: ${"%.6f".format(lon)}"
    }


}

// Data classes for structured state
data class LocationState(
    val coordinates: Coordinates,
    val speed: Float,     // km/h
    val altitude: Double, // meters
    val bearing: Float    // degrees
)

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)