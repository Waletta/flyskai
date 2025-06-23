package model

import com.example.flyskai.ui.shared.UnitSystem
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Airport(val icao: String, val name: String, val lat: Double, val lon: Double)

object GeoUtils {
    fun calculateFlatDistance(x: Double, y: Double, unit: UnitSystem): Double {
        val distanceMeters = sqrt(x.pow(2) + y.pow(2)) * 6_371_000.0
        return when (unit) {
            UnitSystem.METRIC -> distanceMeters / 1000.0
            UnitSystem.IMPERIAL -> distanceMeters / 1852.0
        }
    }
    fun calculateHaversineDistance(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
        unit: UnitSystem
    ): Double {
        val lat1 = Math.toRadians(fromLat)
        val lat2 = Math.toRadians(toLat)
        val deltaLon = Math.toRadians(toLon - fromLon)
        val havA = sin((lat2 - lat1) / 2).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
        val angularDistance = 2 * atan2(sqrt(havA), sqrt(1 - havA))
        val distanceMeters = angularDistance * 6_371_000.0 // Earth Radius
        return when (unit) {
            UnitSystem.METRIC -> distanceMeters / 1000.0
            UnitSystem.IMPERIAL -> distanceMeters / 1852.0
        }
    }

    @Suppress("SameParameterValue")
    fun smartDistance(
        fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, unit: UnitSystem = UnitSystem.METRIC
    ): Double {
        val deltaLat = Math.toRadians(toLat - fromLat)
        val deltaLon = Math.toRadians(toLon - fromLon)
        return if (sqrt(deltaLat.pow(2) + deltaLon.pow(2)) < 0.01) {
            val x = deltaLon * cos(Math.toRadians((fromLat + toLat) / 2))
            val y = deltaLat
            calculateFlatDistance(x, y, unit)
        } else {
            calculateHaversineDistance(fromLat, fromLon, toLat, toLon, unit)
        }
    }


}


val knownAirports = listOf(
    Airport("EHAM", "Amsterdam", 52.3105, 4.7683), // Original
    Airport("KATL", "Atlanta Hartsfield-Jackson", 33.6407, -84.4277), // Added
    Airport("NZAA", "Auckland", -37.0082, 174.7850), // Added
    Airport("VTBS", "Bangkok", 13.6900, 100.7501), // Original
    Airport("LIBRI", "Bari", 41.13618, 16.76188), // Original
    Airport("ZBAA", "Beijing", 40.0799, 116.6031), // Original
    Airport("YBBN", "Brisbane", -27.3842, 153.1175), // Added
    Airport("LIBDS", "Brindisi", 40.65696, 17.94532), // Original
    Airport("LICAG", "Cagliari", 39.259406, 9.046275), // Original
    Airport("HECA", "Cairo", 30.1118, 31.4001), // Added
    Airport("LICRL", "Charleroi", 50.46339, 4.46206), // Original
    Airport("KORD", "Chicago", 41.9742, -87.9073), // Original
    Airport("KDFW", "Dallas/Fort Worth", 32.8972, -97.0403), // Added
    Airport("VIDP", "Delhi", 28.5665, 77.1031), // Added
    Airport("KDEN", "Denver", 39.8561, -104.6737), // Added
    Airport("OMDB", "Dubai", 25.2532, 55.3657), // Original
    Airport("EDDF", "Frankfurt", 50.0379, 8.5622), // Original
    Airport("VHHH", "Hong Kong", 22.3080, 113.9185), // Original
    Airport("FAOR", "Johannesburg", -26.1392, 28.2460), // Original
    Airport("VTSG", "Kuala Lumpur", 2.7456, 101.7019), // Added
    Airport("KLAX", "Los Angeles LAX", 33.9416, -118.4085), // Original
    Airport("EGLL", "London Heathrow", 51.4700, -0.4543), // Original
    Airport("LEMD", "Madrid", 40.4722, -3.5608), // Original
    Airport("LIAGP", "Malaga", 36.67763, -4.49375), // Original
    Airport("RPLL", "Manila", 14.5086, 121.0198), // Added
    Airport("MMMX", "Mexico City", 19.4363, -99.0721), // Added
    Airport("KMIA", "Miami", 25.7959, -80.2870), // Original
    Airport("LIMC", "Milan Malpensa", 45.6306, 8.7231), // Added
    Airport("UUWW", "Moscow", 55.5915, 37.2615), // Original
    Airport("VABB", "Mumbai", 19.0887, 72.8679), // Added
    Airport("EDDM", "Munich", 48.3538, 11.7861), // Added
    Airport("LINAP", "Napoli", 40.87588, 14.28410), // Original
    Airport("KJFK", "New York JFK", 40.6413, -73.7781), // Added (replaces LINY)
    Airport("LFPG", "Paris CDG", 49.0097, 2.5479), // Original
    Airport("LIPSR", "Pescara", 42.43125, 14.18273), // Original
    Airport("OERK", "Riyadh", 24.9576, 46.6988), // Added
    Airport("LIRF", "Rome Fiumicino", 41.8003, 12.2389), // Added
    Airport("SCEL", "Santiago", -33.3930, -70.7858), // Added
    Airport("KSFO", "San Francisco", 37.6213, -122.3790), // Added
    Airport("SBGR", "São Paulo", -23.4356, -46.4731), // Original
    Airport("RKSI", "Seoul Incheon", 37.4602, 126.4407), // Added
    Airport("ZSPD", "Shanghai Pudong", 31.1434, 121.8052), // Added
    Airport("WSSS", "Singapore", 1.3644, 103.9915), // Original
    Airport("YSSY", "Sydney", -33.9399, 151.1753), // Original
    Airport("LITA", "Tallin", 59.41463, 24.83349), // Original
    Airport("LATI", "Tirana", 41.41392, 19.71940), // Original
    Airport("RJTT", "Tokyo", 35.5494, 139.7798), // Original
    Airport("CYYZ", "Toronto", 43.6777, -79.6248), // Original
    Airport("YVR", "Vancouver", 49.1939, -123.1844), // Added
    Airport("LSZH", "Zurich", 47.4647, 8.5492) // Original
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
    val destinationDistance: Double = 0.0,                // Distance to destination
    val eta: Long? = null,
    val magneticFieldMagnitude: Float = 0.0f,
)

data class Coordinates(
    val latitude: Double = 0.00,
    val longitude: Double = 0.00,
)