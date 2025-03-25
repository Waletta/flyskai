package model

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