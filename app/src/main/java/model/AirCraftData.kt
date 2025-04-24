package model

data class Airport(val icao: String, val name: String, val lat: Double, val lon: Double)

val knownAirports = listOf(
    Airport("LIBR", "Brindisi", 40.65696, 17.94532),
    Airport("LIBD", "Bari", 41.13618, 16.76188),
    Airport("LIRN", "Naples", 40.87588, 14.28410),
    Airport("LATI", "Tirana", 41.41392, 19.71940),
    Airport("LIBP", "Pescara", 42.43125, 14.18273),
    Airport("LICH", "Charleroi", 50.46339, 4.46206),
    Airport("LIPA", "Paris CDG", 49.00858, 2.55070),
    Airport("LIMA", "Malaga", 36.67763, -4.49375),
    Airport("LITA", "Tallin", 59.41463, 24.83349),
)

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

data class AircraftStateData(
    val coordinates: Coordinates,           // Latitude and longitude
    val altitude: Double,                   // Height above sea level
    val speed: Float,                       // Ground or airspeed
    val bearing: Float,                     // Direction of movement relative to ground
    val verticalSpeed: Float = 0f,          // Rate of climb or descent
    val pitch: Float = 0f,                  // Nose angle relative to horizon
    val bank: Float = 0f,                   // Wing angle relative to horizon (degrees, positive for right wing down)
    val trueHeading: Float = 0f,            // Direction aircraft is pointing (true north)
    val magneticHeading: Float = 0f,        // Direction aircraft is pointing (magnetic north)
    val courseToDest: Float = 0f,           // Direction to destination
    val destinationAirport: Airport? = null, // Destination airport, nullable for no destination
) {
    companion object {
        fun Coordinates(latitude: Double, longitude: Double): Coordinates {
            return Coordinates(latitude, longitude)
        }
    }
}

data class Coordinates(
    val latitude: Double = 0.00,
    val longitude: Double = 0.00,
)