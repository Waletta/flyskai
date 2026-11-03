package com.example.flyskai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages location updates for aircraft data.
 * @param fusedLocationClient The FusedLocationProviderClient for location services.
 * @param context The application context.
 */
class LocationHandler(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val context: Context
) {
    private var locationCallback: LocationCallback? = null
    private var skipFirstFix = false
    private val locationRequest = LocationRequest.Builder(1000L)
        .setMinUpdateIntervalMillis(2000L)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setMaxUpdateDelayMillis(4000L)
        .build()

    /**
     * Starts location updates and invokes the callback with location data.
     * @param onLocationUpdate Callback to receive location data (lat, lon, speed, altitude, bearing).
     * @return True if updates started successfully, false if permission is missing.
     */
    fun startUpdates(onLocationUpdate: (Double, Double, Float, Double, Float) -> Unit): Boolean {
        if (!checkPermission()) {
            Log.w("LocationHandler", "Location permission not granted")
            return false
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (skipFirstFix) {
                    skipFirstFix = false
                    return
                }
                locationResult.lastLocation?.let { location ->
                    CoroutineScope(Dispatchers.Default).launch {
                        val lat = location.latitude
                        val lon = location.longitude
                        val speedMs = location.speed
                        val altitude = location.altitude
                        val bearing = location.bearing
                        withContext(Dispatchers.Main) {
                            onLocationUpdate(lat, lon, speedMs, altitude, bearing)
                        }
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                Log.e("LocationHandler", "Failed to start location updates: ${e.message}")
            }
            skipFirstFix = true
            return true
        } catch (e: SecurityException) {
            Log.e("LocationHandler", "SecurityException: ${e.message}")
            return false
        }
    }

    /**
     * Stops location updates.
     */
    fun stopUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}