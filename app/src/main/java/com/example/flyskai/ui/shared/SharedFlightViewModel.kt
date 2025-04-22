package com.example.flyskai.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedFlightViewModel : ViewModel() {
    private val _destination = MutableLiveData<String?>()
    private val _disttodestination = MutableLiveData<Float?>()

    val destination: LiveData<String?> = _destination
    val disttodestination: LiveData<Float?> = _disttodestination

    fun setDestination(icao: String, dist: Float) {
        _destination.value = icao
        _disttodestination.value = dist

    }

}
