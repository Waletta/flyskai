package model

enum class FlightPhase {
    IDLE,              // Engines off, parked
    PUSHBACK,          // Aircraft pushed from gate
    TAXI,              // Moving on ground
    HOLD_SHORT,        // Holding before runway
    TAKEOFF_ROLL,      // Accelerating on runway
    ROTATION,          // Nose lifts off
    INITIAL_CLIMB,     // First phase after takeoff
    CLIMB,             // Climbing to cruise altitude
    CRUISE,            // Stable level flight
    DESCENT,           // Descending from cruise
    APPROACH,          // Configured for landing
    FINAL_APPROACH,    // Aligned with runway
    LANDING_FLARE,     // Nose up just before touchdown
    TOUCHDOWN,         // Main gear contact
    ROLL_OUT,          // Decelerating on runway
    EXIT_RUNWAY,       // Taxiway transition
    TAXI_TO_GATE,      // Post-landing taxi
    PARKED             // Engines off at gate
}

data class LocationState(
    val coordinates: Coordinates,
    val speed: Float,     // km/h
    val altitude: Double, // meters
    val bearing: Float,    // degrees
    val verticalSpeed: Float = 0f,
    val flightPhase: FlightPhase = FlightPhase.IDLE
)

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)