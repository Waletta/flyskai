package model

enum class TakeoffPhase {
    IDLE,
    PUSHBACK,
    TAXI,
    TAKEOFF_ROLL,
    ROTATION,
    CLIMB_OUT
}

data class LocationState(
    val coordinates: Coordinates,
    val speed: Float,     // km/h
    val altitude: Double, // meters
    val bearing: Float,    // degrees
    val verticalSpeed: Float = 0f,
    val takeoffPhase: TakeoffPhase = TakeoffPhase.IDLE
)

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)