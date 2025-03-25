package com.example.flyskai.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.maps.plugin.PuckBearing

class HomeViewModel : ViewModel() {

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
    val text: LiveData<String> = _text
    val speed: LiveData<Float> = _speed
    val altitude: LiveData<Double> = _altitude


    fun updateLocationData(lat: Double, lon: Double, speed: Float, altitude: Double, bearing: Float) {
        _text.value = "📍 Lat: $lat\n🧭 Lon: $lon"
        _speed.value = speed
        _altitude.value = altitude
        _bearing.value = bearing
    }
}