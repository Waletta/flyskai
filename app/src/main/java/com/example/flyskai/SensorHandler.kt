package com.example.flyskai

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

class SensorHandler(private val sensorManager: SensorManager) {
    private val rotationSensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: throw IllegalStateException("Rotation Vector Sensor not available")

    private val geomagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    ?: throw IllegalStateException("TYPE_MAGNETIC_FIELD Vector Sensor not available")

    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        ?: throw IllegalStateException("Accelerometer Sensor not available")

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var onOrientationChanged: ((azimuth: Float, pitch: Float, roll: Float) -> Unit)? = null
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
                    val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                    val trueHeading = (azimuth + 360) % 360
                    currentMagneticHeading.set(trueHeading)
                    latestPitch.set(pitch)
                    latestRoll.set(roll)
                    onOrientationChanged?.invoke(trueHeading, pitch, roll)
                    Log.d("SensorHandler", "Orientation - azimuth: $trueHeading, pitch: $pitch, roll: $roll")
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    onMagneticFieldChanged?.invoke(x, y, z)
                    // Optional: Log for debugging
                    // Log.d("SensorHandler", "Magnetic Field - X: $x µT, Y: $y µT, Z: $z µT")
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    onAccelerometerChanged?.invoke(x, y, z)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }

    /**
     * Starts listening for sensor updates.
     * @param onOrientationChanged Callback to receive orientation data.
     * @param onMagneticFieldChangedCallback Callback to receive magnetic field data (x, y, z in µT).
     */
    // --- START: Modified startListening signature and body ---
    fun startListening(
        onOrientationChanged: (azimuth: Float, pitch: Float, roll: Float) -> Unit,
        onMagneticFieldChangedCallback: (x: Float, y: Float, z: Float) -> Unit,
        onAccelerometerChangedCallback: (x: Float, y: Float, z: Float) -> Unit
    ) {
        this.onOrientationChanged = onOrientationChanged
        this.onMagneticFieldChanged = onMagneticFieldChangedCallback
        this.onAccelerometerChanged = onAccelerometerChangedCallback

        sensorManager.registerListener(
            sensorListener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        // Register listener for the geomagnetic sensor
        sensorManager.registerListener(
            sensorListener,
            geomagneticSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        sensorManager.registerListener( // NEW registration for Accelerometer
            sensorListener,
            accelerometerSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stopListening() {
        sensorManager.unregisterListener(sensorListener)
        onOrientationChanged = null
        onMagneticFieldChanged = null
        onAccelerometerChanged = null
    }
}