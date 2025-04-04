// OpenSkyMapper.kt
package com.example.flyskai.network

import model.AircraftState

fun mapToAircraftStates(states: List<List<Any>>): List<AircraftState> {
    return states.mapNotNull { state ->
        try {
            AircraftState(
                icao24 = state[0] as String,
                callsign = state[1] as? String,
                originCountry = state[2] as String,
                longitude = state[5] as? Double,
                latitude = state[6] as? Double,
                baroAltitude = state[7] as? Double,
                velocity = state[9] as? Double,
                heading = state[10] as? Double
            )
        } catch (e: Exception) {
            null
        }
    }
}
