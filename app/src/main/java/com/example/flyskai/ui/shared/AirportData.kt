package com.example.flyskai.ui.shared

data class Airport(val icao: String, val name: String, val lat: Double, val lon: Double)

val knownAirports = listOf(
    Airport("LIBR", "Brindisi", 40.6576, 17.9470),
    Airport("LIBD", "Bari", 41.1389, 16.7606),
    Airport("LIRN", "Naples", 40.8860, 14.2908),
    Airport("LATI", "Tirana", 41.4147, 19.7206),
    Airport("LIBP", "Pescara", 42.4317, 14.1811)
)

