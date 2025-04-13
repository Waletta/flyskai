package com.example.flyskai.ui.shared

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

