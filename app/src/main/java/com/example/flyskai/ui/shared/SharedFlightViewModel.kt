package com.example.flyskai.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedFlightViewModel : ViewModel() {
    private val _destination = MutableLiveData<String?>()
    val destination: LiveData<String?> = _destination

    fun setDestination(icao: String) {
        _destination.value = icao
    }
}
