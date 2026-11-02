package com.example.flyskai

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority

/**
 * Manages location updates for aircraft data.
 * Combines native GNSS hardware (LocationManager.GPS_PROVIDER) for reliable offline/flight tracking
 * with FusedLocationProviderClient for optimal ground positioning.
 */
class LocationHandler(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private var fusedCallback: LocationCallback? = null
    private var gpsListener: LocationListener? = null
    private var gnssStatusCallback: GnssStatus.Callback? = null

    private var onLocationUpdate: ((Double, Double, Float, Double, Float) -> Unit)? = null
    private var lastLocationTime: Long = 0L

    private val fusedRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
        .setMinUpdateIntervalMillis(1000L)
        .setMaxUpdateDelayMillis(2000L)
        .build()

    fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }

    fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startUpdates(onUpdate: (lat: Double, lon: Double, speedMs: Float, altitudeM: Double, bearingDeg: Float) -> Unit): Boolean {
        if (!hasLocationPermission()) {
            Log.w("LocationHandler", "Location permission not granted")
            return false
        }

        this.onLocationUpdate = onUpdate

        // 1. Start native GNSS GPS_PROVIDER (crucial for in-flight / airplane mode)
        startNativeGpsUpdates()

        // 2. Start FusedLocationProviderClient
        startFusedUpdates()

        // 3. Monitor satellite status
        registerGnssStatus()

        return true
    }

    @SuppressLint("MissingPermission")
    private fun startNativeGpsUpdates() {
        if (locationManager == null) return
        if (!hasFineLocationPermission()) return

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                    handleNewLocation(it, "GPS_CACHE")
                }

                gpsListener = LocationListener { location ->
                    handleNewLocation(location, "GPS_HARDWARE")
                }

                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    gpsListener!!,
                    Looper.getMainLooper()
                )
                Log.i("LocationHandler", "Registered native GPS_PROVIDER listener")
            } else {
                Log.w("LocationHandler", "GPS_PROVIDER is disabled on device")
            }
        } catch (e: Exception) {
            Log.e("LocationHandler", "Error starting native GPS updates: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startFusedUpdates() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { handleNewLocation(it, "FUSED_CACHE") }
            }

            fusedCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { handleNewLocation(it, "FUSED") }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                fusedRequest,
                fusedCallback!!,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                Log.e("LocationHandler", "Failed to start Fused updates: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("LocationHandler", "Error starting Fused updates: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerGnssStatus() {
        if ((locationManager == null) || !hasFineLocationPermission()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                gnssStatusCallback = object : GnssStatus.Callback() {
                    override fun onSatelliteStatusChanged(status: GnssStatus) {
                        val count = status.satelliteCount
                        var usedInFix = 0
                        for (i in 0 until count) {
                            if (status.usedInFix(i)) usedInFix++
                        }
                        Log.d("LocationHandler", "GNSS Satellites: $count in view, $usedInFix used in fix")
                    }
                }
                locationManager.registerGnssStatusCallback(gnssStatusCallback!!, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                Log.e("LocationHandler", "Error registering GnssStatus: ${e.message}")
            }
        }
    }

    private fun handleNewLocation(location: Location, source: String) {
        val time = location.time
        if (time != 0L && time <= lastLocationTime && (System.currentTimeMillis() - lastLocationTime < 300)) {
            return
        }
        lastLocationTime = if (time != 0L) time else System.currentTimeMillis()

        val lat = location.latitude
        val lon = location.longitude
        val speed = if (location.hasSpeed()) location.speed else 0f
        val altitude = if (location.hasAltitude()) location.altitude else 0.0
        val bearing = if (location.hasBearing()) location.bearing else 0f

        Log.d("LocationHandler", "Location from $source: lat=$lat, lon=$lon, alt=$altitude, speed=$speed")
        onLocationUpdate?.invoke(lat, lon, speed, altitude, bearing)
    }

    fun stopUpdates() {
        fusedCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            fusedCallback = null
        }
        gpsListener?.let {
            locationManager?.removeUpdates(it)
            gpsListener = null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssStatusCallback != null) {
            locationManager?.unregisterGnssStatusCallback(gnssStatusCallback!!)
            gnssStatusCallback = null
        }
        onLocationUpdate = null
    }
}
