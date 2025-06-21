package com.example.flyskai // Ensure this matches your package structure

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.util.concurrent.atomic.AtomicReference
import com.example.flyskai.ui.shared.CockPitViewModel

/**
 * Handles sensor registration and data processing.
 * This version calculates processed orientation data (heading, pitch, roll) locally
 * and exposes them via AtomicReferences and a callback.
 * It also forwards RAW sensor data (magnetic field, accelerometer, rotation vector)
 * to the CockPitViewModel for other calculations like magnetic dip angle.
 *
 * @param context Application context needed to get SensorManager.
 * @param viewModel The CockPitViewModel instance to which raw sensor data will be forwarded.
 */
class SensorHandler(private val context: Context, private val viewModel: CockPitViewModel) {

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    // --- MODIFIED: Handle sensor availability gracefully (no IllegalStateException) ---
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val geomagneticSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) // ACCELEROMETER SENSOR INITIALIZED

    // --- MODIFIED: rotationMatrix size must be 16 for SensorManager.getRotationMatrixFromVector ---
    private val rotationMatrix = FloatArray(16)
    private val orientationAngles = FloatArray(3)

    // Callbacks for MainActivity. Note: `onOrientationChanged` is for processed data.
    // `onRaw...` callbacks are for raw data.
    private var onOrientationChangedCallback: ((magneticHeading: Float, pitch: Float, roll: Float) -> Unit)? = null
    private var onRawMagneticFieldChangedCallback: ((x: Float, y: Float, z: Float) -> Unit)? = null
    private var onRawAccelerometerChangedCallback: ((x: Float, y: Float, z: Float) -> Unit)? = null
    private var onRawRotationVectorChangedCallback: ((x: Float, y: Float, z: Float, w: Float) -> Unit)? = null // NEW: Callback for raw rotation vector


    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event ?: return // Null check for the event

            // --- CRITICAL FIX: Clone event.values array! ---
            // The underlying array is reused by the system. Not cloning it leads to unstable/incorrect data.
            val values = event.values.clone()

            when (event.sensor?.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    if (values.size >= 4) { // Rotation Vector has x, y, z, w
                        // Local processing of orientation (as per your "old way")
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)

                        // --- NEW: Pass RAW rotation vector data to ViewModel ---
                        viewModel.updateRotationVector(values[0], values[1], values[2], values[3])

                    } else {
                        Log.w("SensorHandler", "Rotation Vector event.values has less than 4 elements: ${values.size}")
                    }
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    if (values.size >= 3) {
                        val x = values[0]
                        val y = values[1]
                        val z = values[2]
                        // --- Pass RAW magnetic field data to ViewModel ---
                        viewModel.updateRawMagneticField(x, y, z)
                        onRawMagneticFieldChangedCallback?.invoke(x, y, z) // Optional local callback
                    }
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    // --- MODIFIED: Properly handle and pass Accelerometer data ---
                    if (values.size >= 3) {
                        val x = values[0]
                        val y = values[1]
                        val z = values[2]
                        // --- Pass RAW accelerometer data to ViewModel ---
                        viewModel.updateRawAccelerometerData(x, y, z)
                        onRawAccelerometerChangedCallback?.invoke(x, y, z) // Optional local callback
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            Log.d("SensorHandler", "Accuracy changed for ${sensor?.name}: $accuracy")
        }
    }

    /**
     * Starts listening for sensor updates.
     * Call this in your Activity/Fragment's onResume().
     *
     * @param onOrientationChanged Callback for processed orientation (magnetic heading, pitch, roll).
     * @param onRawMagneticFieldChangedCallback Callback for raw magnetic field data.
     * @param onRawAccelerometerChangedCallback Callback for raw accelerometer data.
     * @param onRawRotationVectorChangedCallback Callback for raw rotation vector data.
     */
    fun startListening(
        onOrientationChanged: ((magneticHeading: Float, pitch: Float, roll: Float) -> Unit)? = null,
        onRawMagneticFieldChangedCallback: ((x: Float, y: Float, z: Float) -> Unit)? = null,
        onRawAccelerometerChangedCallback: ((x: Float, y: Float, z: Float) -> Unit)? = null,
        onRawRotationVectorChangedCallback: ((x: Float, y: Float, z: Float, w: Float) -> Unit)? = null // NEW parameter
    ) {
        // Store all callbacks
        this.onOrientationChangedCallback = onOrientationChanged
        this.onRawMagneticFieldChangedCallback = onRawMagneticFieldChangedCallback
        this.onRawAccelerometerChangedCallback = onRawAccelerometerChangedCallback
        this.onRawRotationVectorChangedCallback = onRawRotationVectorChangedCallback // Store new callback

        // --- MODIFIED: Register listeners based on sensor availability ---
        rotationVectorSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d("SensorHandler", "Registered ROTATION_VECTOR sensor.")
        } ?: Log.w("SensorHandler", "ROTATION_VECTOR sensor not found, will not be used for primary orientation.")

        geomagneticSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d("SensorHandler", "Registered MAGNETIC_FIELD sensor.")
        } ?: Log.w("SensorHandler", "MAGNETIC_FIELD sensor not found.")

        accelerometerSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d("SensorHandler", "Registered ACCELEROMETER sensor.")
        } ?: Log.w("SensorHandler", "ACCELEROMETER sensor not found.")
    }

    /**
     * Unregisters all sensor listeners. Call this in your Activity/Fragment's onPause().
     */
    fun stopListening() {
        sensorManager.unregisterListener(sensorListener)
        Log.d("SensorHandler", "Unregistered all sensor listeners.")
        // Clear callbacks to prevent memory leaks
        onOrientationChangedCallback = null
        onRawMagneticFieldChangedCallback = null
        onRawAccelerometerChangedCallback = null
        onRawRotationVectorChangedCallback = null
    }
}
