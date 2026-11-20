package com.poseit.app.pose.detect

import com.poseit.app.pose.Skeleton

/**
 * One landmark as produced by MediaPipe Pose Landmarker, normalized (0..1) to the UPRIGHT input image
 * (the analysis bitmap after rotating by ImageProxy.imageInfo.rotationDegrees, NOT mirrored).
 */
data class RawLandmark(val x: Float, val y: Float, val visibility: Float = 1f, val presence: Float = 1f)

object LandmarkMapping {

    /**
     * Converts one person's 33 MediaPipe landmarks into a [Skeleton] in SCENE space.
     *
     * The scene is the centred crop of the upright image with aspect [sceneAspect] (this is what CameraX's
     * FILL_CENTER ViewPort shows and saves). Only the joints in [com.poseit.app.pose.Joint] are kept; visibility
     * = min(visibility, presence). If [landmarks] has fewer than 33 entries, missing indices are skipped.
     *
     * Pass sceneAspect = imageWidth / imageHeight to get plain image-normalized coordinates (still photos).
     */
    fun toScene(landmarks: List<RawLandmark>, imageWidth: Int, imageHeight: Int, sceneAspect: Float): Skeleton {
        TODO("implemented by the engine agent")
    }
}
