package com.example.flyskai

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.atan2
import kotlin.math.sqrt

class SensorHandler(private val sensorManager: SensorManager) {
    private val rotationSensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: throw IllegalStateException("Rotation Vector Sensor not available")

    private val geomagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        ?: throw IllegalStateException("TYPE_MAGNETIC_FIELD Vector Sensor not available")

    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        ?: throw IllegalStateException("Accelerometer Sensor not available")

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var onOrientationChanged: ((x: Float, y: Float, z: Float) -> Unit)? = null
    private var onMagneticFieldChanged: ((x: Float, y: Float, z: Float) -> Unit)? = null
    private var onAccelerometerChanged: ((x: Float, y: Float, z: Float) -> Unit)? = null

    val currentMagneticHeading = AtomicReference<Float>(0f)
    val latestPitch = AtomicReference<Float>(0f)
    val latestRoll = AtomicReference<Float>(0f)

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            when (event?.sensor?.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    val x = orientationAngles[0]
                    val y = orientationAngles[1]
                    val z = orientationAngles[2]
                    val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                    latestRoll.set(roll)
                    onOrientationChanged?.invoke(x, y, z)
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val pitchRadians = atan2(y, sqrt(x * x + z * z))
                    val pitch = Math.toDegrees(pitchRadians.toDouble()).toFloat()
                    latestPitch.set(-pitch)
                    val azimuth = Math.toDegrees(x.toDouble()).toFloat()
                    val trueHeading = (azimuth + 360) % 360
                    currentMagneticHeading.set(trueHeading)
                    onAccelerometerChanged?.invoke(x, y, z)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }

    fun startListening() {
        sensorManager.registerListener(
            sensorListener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            sensorListener,
            accelerometerSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stopListening() {
        sensorManager.unregisterListener(sensorListener)
        onOrientationChanged = null
        onAccelerometerChanged = null
    }
}