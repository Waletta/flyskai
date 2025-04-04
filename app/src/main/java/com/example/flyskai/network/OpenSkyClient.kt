// OpenSkyApiService.kt
package com.example.flyskai.network

import retrofit2.http.GET
import retrofit2.http.Query

data class OpenSkyResponse(
    val time: Long,
    val states: List<List<Any>>?
)

interface OpenSkyApiService {

    @GET("states/all")
    suspend fun getAllStates(
        @Query("lamin") minLat: Double,
        @Query("lamax") maxLat: Double,
        @Query("lomin") minLon: Double,
        @Query("lomax") maxLon: Double
    ): OpenSkyResponse
}
