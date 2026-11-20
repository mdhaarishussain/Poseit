package com.poseit.app.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import android.view.Surface
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Device orientation for the level indicator.
 * @param rollDegrees rotation around the screen normal; 0 = phone perfectly upright (portrait), positive = rotated clockwise
 * @param pitchDegrees forward/back tilt; 0 = phone vertical, positive = top tilted away from the user (camera looks down)
 */
data class DeviceTilt(val rollDegrees: Float, val pitchDegrees: Float)

class LevelSensor(private val context: Context) {

    /** Emits ~15-30 times per second while collected (low-pass filtered). Empty flow if no sensor. */
    val tilt: Flow<DeviceTilt> by lazy { createTiltFlow() }

    private fun createTiltFlow(): Flow<DeviceTilt> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return emptyFlow()
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val sensor = gravity ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return emptyFlow()
        // TYPE_GRAVITY is already fused/filtered: smooth lightly. Raw accelerometer data includes hand shake.
        val smoothing = if (gravity != null) GRAVITY_SMOOTHING else ACCELEROMETER_SMOOTHING

        return callbackFlow {
            val filter = LowPassFilter(smoothing)
            val displayRotation = currentDisplayRotation()
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val values = event.values
                    if (values.size < 3) return
                    val (x, y) = TiltMath.toDisplayAxes(values[0], values[1], displayRotation)
                    val filtered = filter.apply(x, y, values[2])
                    TiltMath.fromGravity(filtered[0], filtered[1], filtered[2])?.let { trySend(it) }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            if (!sensorManager.registerListener(listener, sensor, SAMPLING_PERIOD_US)) {
                Log.w(TAG, "Couldn't register the ${sensor.name} listener")
                close()
                return@callbackFlow
            }
            awaitClose { sensorManager.unregisterListener(listener) }
        }.buffer(Channel.CONFLATED)
    }

    /** Rotation of the default display (always ROTATION_0 for a portrait-locked phone; differs on some tablets). */
    private fun currentDisplayRotation(): Int = try {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
    } catch (e: Exception) {
        Surface.ROTATION_0
    }

    /** Exponential moving average of a 3-vector; the first sample initialises it. */
    private class LowPassFilter(private val alpha: Float) {
        private val state = FloatArray(3)
        private var initialised = false

        fun apply(x: Float, y: Float, z: Float): FloatArray {
            if (!initialised) {
                state[0] = x
                state[1] = y
                state[2] = z
                initialised = true
            } else {
                state[0] += alpha * (x - state[0])
                state[1] += alpha * (y - state[1])
                state[2] += alpha * (z - state[2])
            }
            return state
        }
    }

    private companion object {
        const val TAG = "LevelSensor"

        /** 40 ms = 25 Hz: smooth enough for the level, close to SENSOR_DELAY_UI in cost. */
        const val SAMPLING_PERIOD_US = 40_000
        const val GRAVITY_SMOOTHING = 0.5f
        const val ACCELEROMETER_SMOOTHING = 0.15f
    }
}

/**
 * Converts a gravity reading into roll and pitch.
 *
 * Android sensor axes (for the device's natural portrait orientation): +x to the right of the screen, +y up
 * along the screen, +z out of the screen towards the user. A gravity/accelerometer reading points UP (it measures
 * the reaction to gravity), so an upright phone reads (0, +g, 0) and a phone lying on its back reads (0, 0, +g).
 *
 * Roll: turn the phone clockwise by θ (as the user sees it). The world "up" vector, seen in the device frame, turns
 * counter-clockwise by θ: reading = g·(-sin θ, cos θ, 0). Hence θ = atan2(-x, y): 0 upright, positive clockwise
 * (note the minus sign: plain atan2(x, y) would make clockwise negative, contradicting [DeviceTilt]).
 *
 * Pitch: tilt the top of the phone away from the user by φ. The screen starts facing the sky, so the reading
 * gains a +z component: reading = g·(0, cos φ, sin φ). Hence φ = atan2(z, √(x² + y²)): 0 vertical, positive when
 * the top leans away (rear camera looking down), independent of the roll.
 */
internal object TiltMath {

    private const val RAD_TO_DEG = (180.0 / Math.PI).toFloat()

    /** Null for a (near-)zero vector, e.g. free fall or a broken sensor. */
    fun fromGravity(x: Float, y: Float, z: Float): DeviceTilt? {
        val norm = sqrt(x * x + y * y + z * z)
        if (norm < MIN_NORM || norm.isNaN()) return null
        val roll = atan2(-x, y) * RAD_TO_DEG
        val pitch = atan2(z, sqrt(x * x + y * y)) * RAD_TO_DEG
        return DeviceTilt(rollDegrees = roll, pitchDegrees = pitch)
    }

    /**
     * Remaps sensor x/y (natural-orientation axes) to the axes of a display rotated by [rotation]
     * (a Surface.ROTATION_* constant); z is unaffected. Identity for ROTATION_0.
     */
    fun toDisplayAxes(x: Float, y: Float, rotation: Int): Pair<Float, Float> = when (rotation) {
        Surface.ROTATION_90 -> -y to x
        Surface.ROTATION_180 -> -x to -y
        Surface.ROTATION_270 -> y to -x
        else -> x to y
    }

    private const val MIN_NORM = 0.5f
}
