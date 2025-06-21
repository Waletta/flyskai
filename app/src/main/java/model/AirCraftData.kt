package model

data class Airport(val icao: String, val name: String, val lat: Double, val lon: Double)

val knownAirports = listOf(
    Airport("LIBDS", "Brindisi", 40.65696, 17.94532),
    Airport("LIBRI", "Bari", 41.13618, 16.76188),
    Airport("LINAP", "Napoli", 40.87588, 14.28410),
    Airport("LATI", "Tirana", 41.41392, 19.71940),
    Airport("LIPSR", "Pescara", 42.43125, 14.18273),
    Airport("LICAG", "Cagliari", 39.259406, 9.046275),
    Airport("LICRL", "Charleroi", 50.46339, 4.46206),
    Airport("LIAGP", "Malaga", 36.67763, -4.49375),
    Airport("LITA", "Tallin", 59.41463, 24.83349),
    Airport("LINY", "NY JFK", 40.64634739968817, -73.78349034554053),

    // Major Airports
    Airport("KLAX", "Los Angeles LAX", 33.9416, -118.4085),
    Airport("EGLL", "London Heathrow", 51.4700, -0.4543),
    Airport("RJTT", "Tokyo", 35.5494, 139.7798),
    Airport("YSSY", "Sydney", -33.9399, 151.1753),
    Airport("OMDB", "Dubai", 25.2532, 55.3657),
    Airport("ZBAA", "Beijing", 40.0799, 116.6031),
    Airport("CYYZ", "Toronto", 43.6777, -79.6248),
    Airport("EDDF", "Frankfurt", 50.0379, 8.5622),
    Airport("EHAM", "Amsterdam", 52.3105, 4.7683),
    Airport("LFPG", "Paris CDG", 49.0097, 2.5479),
    Airport("VHHH", "Hong Kong", 22.3080, 113.9185),

    // Other Important Airports
    Airport("LSZH", "Zurich", 47.4647, 8.5492),
    Airport("WSSS", "Singapore", 1.3644, 103.9915),
    Airport("LEMD", "Madrid", 40.4722, -3.5608),
    Airport("KORD", "Chicago", 41.9742, -87.9073),
    Airport("KMIA", "Miami", 25.7959, -80.2870),
    Airport("SBGR", "São Paulo", -23.4356, -46.4731),
    Airport("FAOR", "Johannesburg", -26.1392, 28.2460),
    Airport("VTBS", "Bangkok", 13.6900, 100.7501),
    Airport("NZAA", "Auckland", -37.0082, 174.7850),
    Airport("UUWW", "Moscow", 55.5915, 37.2615)
)


data class AircraftStateData(
    val coordinates: Coordinates = Coordinates(0.0, 0.0),  // Latitude and longitude
    val altitude: Double = 0.0,                            // Height above sea level
    val speed: Float = 0f,                                 // Ground or airspeed
    val courseOverGround: Float = 0f,                      // Direction of movement relative to ground
    val verticalSpeed: Float = 0f,                         // Rate of climb or descent
    val pitch: Float = 0f,                                 // Nose angle relative to horizon
    val bank: Float = 0f,                                  // Wing angle relative to horizon (degrees, positive for right wing down)
    val trueHeading: Float = 0f,                           // Direction aircraft is pointing (true north)
    val magneticHeading: Float = 0f,                       // Direction aircraft is pointing (magnetic north)
    val courseToDest: Float = 0f,                          // Direction to destination
    val destinationAirport: Airport? = null,               // Optional destination airport data
    val eta: Long? = null,
    val magneticFieldMagnitude: Float = 0.0f,
    val magneticDipAngle: Float = 0.0f
)

data class Coordinates(
    val latitude: Double = 0.00,
    val longitude: Double = 0.00,
)