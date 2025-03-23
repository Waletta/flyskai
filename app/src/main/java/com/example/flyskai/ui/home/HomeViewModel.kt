package com.example.flyskai.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Waiting for location..."
    }
    val text: LiveData<String> = _text

    fun updateLocation(lat: Double, lon: Double) {
        _text.value = "📍 Lat: $lat\n🧭 Lon: $lon"
    }
}