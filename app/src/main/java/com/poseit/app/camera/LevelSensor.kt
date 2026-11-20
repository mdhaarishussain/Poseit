package com.poseit.app.camera

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Device orientation for the level indicator.
 * @param rollDegrees rotation around the screen normal; 0 = phone perfectly upright (portrait), positive = rotated clockwise
 * @param pitchDegrees forward/back tilt; 0 = phone vertical, positive = top tilted away from the user (camera looks down)
 */
data class DeviceTilt(val rollDegrees: Float, val pitchDegrees: Float)

class LevelSensor(private val context: Context) {
    /** Emits ~15-30 times per second while collected (low-pass filtered). Empty flow if no sensor. */
    val tilt: Flow<DeviceTilt> get() = TODO("implemented by the camera-infra agent")
}
