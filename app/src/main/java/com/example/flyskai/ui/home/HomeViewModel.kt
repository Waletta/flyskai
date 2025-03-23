package com.example.flyskai.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _speed = MutableLiveData<Float>()   // in m/s
    private val _altitude = MutableLiveData<Double>() // in meters

    private val _text = MutableLiveData<String>().apply {
        value = "Waiting for GPS..."
    }
    val text: LiveData<String> = _text

    val speed: LiveData<Float> = _speed
    val altitude: LiveData<Double> = _altitude

    fun updateSpeedAndAltitude(speed: Float, altitude: Double) {
        _speed.value = speed
        _altitude.value = altitude
    }

    fun updateLocation(lat: Double, lon: Double) {
        _text.value = "📍 Lat: $lat\n🧭 Lon: $lon"
    }
}