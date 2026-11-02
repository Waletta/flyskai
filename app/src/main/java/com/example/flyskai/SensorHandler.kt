package com.example.flyskai

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.atan2
import kotlin.math.sqrt

class SensorHandler(private val sensorManager: SensorManager) {
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var latestAccel = FloatArray(3) { 0f }
    private var latestMag = FloatArray(3) { 0f }
    private var hasAccel = false
    private var hasMag = false

    val currentMagneticHeading = AtomicReference(0f)
    val latestPitch = AtomicReference(0f)
    val latestRoll = AtomicReference(0f)
    val latestMagneticField = AtomicReference(floatArrayOf(0f, 0f, 0f))

    private var onAttitudeListener: ((pitch: Float, roll: Float, heading: Float, magX: Float, magY: Float, magZ: Float) -> Unit)? = null
    private var lastDispatchTime: Long = 0L
    private val throttleIntervalMs: Long = 33L // ~30 Hz throttle for smooth UI and efficient CPU usage

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return

            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    try {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)

                        val azimuthDeg = ((Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360f) % 360f)
                        // orientationAngles[1] is pitch (-pi/2 to pi/2, negative is nose-up in standard orientation)
                        val pitchDeg = -Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                        // orientationAngles[2] is roll (-pi to pi, positive right wing down)
                        val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                        latestPitch.set(pitchDeg)
                        latestRoll.set(rollDeg)
                        currentMagneticHeading.set(azimuthDeg)
                        notifyListener()
                    } catch (e: Exception) {
                        Log.e("SensorHandler", "Error processing rotation vector: ${e.message}")
                    }
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    latestAccel = event.values.copyOf()
                    hasAccel = true
                    if (rotationSensor == null) {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        val pitchRadians = atan2(y, sqrt((x * x) + (z * z)))
                        val pitch = Math.toDegrees(pitchRadians.toDouble()).toFloat()
                        latestPitch.set(-pitch)
                        updateHeadingFromAccelAndMag()
                        notifyListener()
                    }
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    latestMag = event.values.copyOf()
                    hasMag = true
                    latestMagneticField.set(event.values.copyOf())
                    if (rotationSensor == null) {
                        updateHeadingFromAccelAndMag()
                    }
                    notifyListener()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }

        private fun updateHeadingFromAccelAndMag() {
            if (!hasAccel || !hasMag) return
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, latestAccel, latestMag)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val heading = Math.toDegrees(orientation[0].toDouble()).toFloat()
                currentMagneticHeading.set((heading + 360f) % 360f)
            }
        }

        private fun notifyListener() {
            val now = SystemClock.uptimeMillis()
            if ((now - lastDispatchTime) >= throttleIntervalMs) {
                lastDispatchTime = now
                val mag = latestMagneticField.get()
                onAttitudeListener?.invoke(
                    latestPitch.get(),
                    latestRoll.get(),
                    currentMagneticHeading.get(),
                    mag[0],
                    mag[1],
                    mag[2],
                )
            }
        }
    }

    fun startListening(onAttitudeChanged: ((pitch: Float, roll: Float, heading: Float, magX: Float, magY: Float, magZ: Float) -> Unit)? = null) {
        this.onAttitudeListener = onAttitudeChanged
        rotationSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometerSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometerSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(sensorListener)
        onAttitudeListener = null
    }
}
