package com.poseit.app.input

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Volume-button shutter support. MainActivity forwards volume key presses here ONLY while a screen has
 * registered interest (e.g. the camera screen while it is visible), otherwise the keys change the volume as usual.
 */
object HardwareKeys {
    private val _shutter = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    /** Emits once per volume-key press while [captureVolumeKeys] is true. */
    val shutter: SharedFlow<Unit> = _shutter.asSharedFlow()

    /** Set to true while the camera screen is resumed; false otherwise. */
    @Volatile
    var captureVolumeKeys: Boolean = false

    /** Called by MainActivity. Returns true if the key press was consumed. */
    fun onVolumeKey(): Boolean {
        if (!captureVolumeKeys) return false
        _shutter.tryEmit(Unit)
        return true
    }
}
