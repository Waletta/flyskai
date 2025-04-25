package model

data class Airport(val icao: String, val name: String, val lat: Double, val lon: Double)

val knownAirports = listOf(
    Airport("LIBR", "Brindisi", 40.65696, 17.94532),
    Airport("LIBD", "Bari", 41.13618, 16.76188),
    Airport("LIRN", "Napoli", 40.87588, 14.28410),
    Airport("LATI", "Tirana", 41.41392, 19.71940),
    Airport("LIBP", "Pescara", 42.43125, 14.18273),
    Airport("LICH", "Charleroi", 50.46339, 4.46206),
    Airport("LIPA", "Paris CDG", 49.00858, 2.55070),
    Airport("LIMA", "Malaga", 36.67763, -4.49375),
    Airport("LITA", "Tallin", 59.41463, 24.83349),
    Airport("LINY", "NY JFK", 40.64634739968817, -73.78349034554053),
)


data class AircraftStateData(
    val coordinates: Coordinates = Coordinates(0.0, 0.0),  // Latitude and longitude
    val altitude: Double = 0.0,                            // Height above sea level
    val speed: Float = 0f,                                 // Ground or airspeed
    val bearing: Float = 0f,                               // Direction of movement relative to ground
    val verticalSpeed: Float = 0f,                         // Rate of climb or descent
    val pitch: Float = 0f,                                 // Nose angle relative to horizon
    val bank: Float = 0f,                                  // Wing angle relative to horizon (degrees, positive for right wing down)
    val trueHeading: Float = 0f,                           // Direction aircraft is pointing (true north)
    val magneticHeading: Float = 0f,                       // Direction aircraft is pointing (magnetic north)
    val courseToDest: Float = 0f,                          // Direction to destination
    val destinationAirport: Airport? = null                // Optional destination airport data
) {
    companion object {
        fun currentCoordinates(latitude: Double, longitude: Double): Coordinates =
            Coordinates(latitude, longitude)

        fun destinationCoordinates(airport: Airport?): Coordinates =
            airport?.let { Coordinates(it.lat, it.lon) }
                ?: Coordinates(0.0, 0.0)

        fun destinationAirportName(airport: Airport?): String =
            airport?.name ?: ""
        }

        val nowCoordinates: Coordinates
            get() = currentCoordinates(coordinates.latitude, coordinates.longitude)

        val destCoordinates: Coordinates
            get() = destinationCoordinates(destinationAirport)
        val destAirportName: String
            get() = destinationAirportName(destinationAirport)
    }

data class Coordinates(
    val latitude: Double = 0.00,
    val longitude: Double = 0.00,
)