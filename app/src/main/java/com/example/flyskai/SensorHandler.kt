package com.example.flyskai

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

/**
 * Handles aircraft orientation data (azimuth, pitch, roll) using the rotation vector sensor.
 * @param sensorManager The system sensor manager.
 */
class SensorHandler(private val sensorManager: SensorManager) {
    private val rotationSensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: throw IllegalStateException("Rotation Vector Sensor not available")
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var onOrientationChanged: ((azimuth: Float, pitch: Float, roll: Float) -> Unit)? = null

    val currentTrueHeading = AtomicReference<Float>(0f)
    val latestPitch = AtomicReference<Float>(0f)
    val latestRoll = AtomicReference<Float>(0f)

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                val trueHeading = (azimuth + 360) % 360
                currentTrueHeading.set(trueHeading)
                latestPitch.set(pitch)
                latestRoll.set(roll)
                onOrientationChanged?.invoke(trueHeading, pitch, roll)
                Log.d("SensorHandler", "Orientation - azimuth: $trueHeading, pitch: $pitch, roll: $roll")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }

    /**
     * Starts listening for sensor updates.
     * @param onOrientationChanged Callback to receive orientation data.
     */
    fun startListening(onOrientationChanged: (azimuth: Float, pitch: Float, roll: Float) -> Unit) {
        this.onOrientationChanged = onOrientationChanged
        sensorManager.registerListener(
            sensorListener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    /**
     * Stops listening for sensor updates.
     */
    fun stopListening() {
        sensorManager.unregisterListener(sensorListener)
        onOrientationChanged = null
    }
}