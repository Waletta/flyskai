package model

data class AircraftState(
    val icao24: String,
    val callsign: String?,
    val originCountry: String,
    val longitude: Double?,
    val latitude: Double?,
    val baroAltitude: Double?,
    val velocity: Double?,
    val heading: Double?
)