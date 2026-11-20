package com.poseit.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow

enum class Lens { BACK, FRONT }
enum class FlashMode { OFF, AUTO, ON }

/** Aspect ratio of the preview / saved photo (portrait). */
enum class SceneAspect(val ratio: Float, val label: String) {
    RATIO_3_4(3f / 4f, "3:4"),
    RATIO_9_16(9f / 16f, "9:16"),
}

/**
 * CameraX wrapper: Preview + ImageCapture + ImageAnalysis bound together in a UseCaseGroup with the
 * PreviewView's ViewPort, so the analysed frames, the visible preview and the saved photo all show the same
 * area (= SCENE space).
 *
 * Analysis frames are delivered to [onFrame] on a background executor as upright (rotated), NON-mirrored
 * ARGB bitmaps already cropped to the viewport, together with a timestamp in ms. The callback must not keep
 * the bitmap after returning unless it copies it.
 */
class PoseCamera(private val context: Context) {

    val lens: StateFlow<Lens> get() = TODO("implemented by the camera-infra agent")

    /** Max zoom ratio supported by the current camera (1 when unknown). */
    val maxZoom: StateFlow<Float> get() = TODO("implemented by the camera-infra agent")

    val hasFlash: StateFlow<Boolean> get() = TODO("implemented by the camera-infra agent")

    /**
     * (Re)binds all use cases. Safe to call again to switch lens or aspect.
     * @param onFrame receives (uprightBitmap, timestampMs); called on a background thread
     */
    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lens: Lens,
        aspect: SceneAspect,
        onFrame: (Bitmap, Long) -> Unit,
    ) {
        TODO("implemented by the camera-infra agent")
    }

    fun setFlash(mode: FlashMode) {
        TODO("implemented by the camera-infra agent")
    }

    /** @param ratio 1..maxZoom */
    fun setZoom(ratio: Float) {
        TODO("implemented by the camera-infra agent")
    }

    /** Tap-to-focus at view coordinates. */
    fun focusAt(previewView: PreviewView, x: Float, y: Float) {
        TODO("implemented by the camera-infra agent")
    }

    /**
     * Takes a photo and saves it to MediaStore under Pictures/Poseit (JPEG).
     * @param mirror save horizontally mirrored (used for front camera when the user prefers mirrored selfies)
     * @return the content Uri of the saved image
     * @throws Exception if capture fails
     */
    suspend fun takePhoto(mirror: Boolean): Uri {
        TODO("implemented by the camera-infra agent")
    }

    fun unbind() {
        TODO("implemented by the camera-infra agent")
    }
}
