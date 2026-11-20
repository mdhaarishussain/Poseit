package com.poseit.app.detect

import android.content.Context
import android.graphics.Bitmap
import com.poseit.app.pose.Skeleton
import kotlinx.coroutines.flow.SharedFlow
import java.io.Closeable

/**
 * @param people detected people in SCENE space (see com.poseit.app.pose.scene.SceneMapping docs), at most maxPoses
 * @param frameTimestampMs timestamp passed to [PoseDetector.detectLive]
 * @param inferenceMs how long MediaPipe took
 */
data class DetectionResult(
    val people: List<Skeleton>,
    val frameTimestampMs: Long,
    val inferenceMs: Long,
)

/**
 * Wrapper around MediaPipe Pose Landmarker (models in assets/models/pose_landmarker_{lite,full}.task).
 *
 * - Live mode: feed upright, NON-mirrored camera frames with [detectLive]; results arrive asynchronously on
 *   [results]. Frames arriving while the previous one is still being processed may be dropped.
 * - Still mode: [detectStill] for photos picked from the gallery (blocking; call off the main thread).
 *
 * Creating the landmarker is slow (~100-500 ms); construct off the main thread. Falls back from GPU to CPU
 * delegate if GPU init fails. Never throws from detect calls – errors are logged and reported via [errors].
 *
 * @param accurate true = full model, false = lite model
 * @param maxPoses maximum number of people to detect (2 for couples, 3 for groups)
 */
class PoseDetector(
    context: Context,
    val accurate: Boolean,
    val maxPoses: Int = 2,
) : Closeable {

    val results: SharedFlow<DetectionResult> get() = TODO("implemented by the camera-infra agent")

    val errors: SharedFlow<String> get() = TODO("implemented by the camera-infra agent")

    /**
     * @param uprightBitmap ARGB_8888 frame already rotated upright, NOT mirrored
     * @param timestampMs monotonically increasing frame time
     * @param sceneAspect aspect (w/h) of the visible scene; landmarks are mapped with LandmarkMapping.toScene
     */
    fun detectLive(uprightBitmap: Bitmap, timestampMs: Long, sceneAspect: Float) {
        TODO("implemented by the camera-infra agent")
    }

    /** Returns people in IMAGE-normalized coordinates (sceneAspect = bitmap aspect). Blocking. */
    fun detectStill(bitmap: Bitmap): List<Skeleton> {
        TODO("implemented by the camera-infra agent")
    }

    override fun close() {
        TODO("implemented by the camera-infra agent")
    }
}
