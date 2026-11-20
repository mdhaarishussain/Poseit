package com.poseit.app.pose.detect

import com.poseit.app.pose.Joint
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2

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
     *
     * Crop maths, with image aspect `a = W / H` and scene aspect `s`:
     *  - `a > s`: the image is wider, so its sides are cropped: `f = s / a`, `x' = (x - (1 - f) / 2) / f`, `y' = y`.
     *  - otherwise the top/bottom are cropped: `f = a / s`, `y' = (y - (1 - f) / 2) / f`, `x' = x`.
     * Landmarks with non-finite coordinates are skipped; non-finite confidences count as 0. Invalid image sizes or
     * aspects fall back to "no crop".
     */
    fun toScene(landmarks: List<RawLandmark>, imageWidth: Int, imageHeight: Int, sceneAspect: Float): Skeleton {
        val imageAspect = if (imageWidth > 0 && imageHeight > 0) imageWidth.toFloat() / imageHeight else Float.NaN
        val a = imageAspect.takeIf { it.isFinite() && it > 0f }
        val s = sceneAspect.takeIf { it.isFinite() && it > 0f }

        // Crop factors per axis (1 = no crop on that axis).
        var fx = 1f
        var fy = 1f
        if (a != null && s != null) {
            if (a > s) fx = s / a else fy = a / s
        }
        val ox = (1f - fx) / 2f
        val oy = (1f - fy) / 2f

        val points = LinkedHashMap<Joint, Vec2>()
        val visibility = LinkedHashMap<Joint, Float>()
        for (joint in Joint.entries) {
            val lm = landmarks.getOrNull(joint.mpIndex) ?: continue
            if (!lm.x.isFinite() || !lm.y.isFinite()) continue
            points[joint] = Vec2((lm.x - ox) / fx, (lm.y - oy) / fy)
            visibility[joint] = minOf(confidence(lm.visibility), confidence(lm.presence))
        }
        return Skeleton(points = points, visibility = visibility)
    }

    private fun confidence(v: Float): Float = if (v.isFinite()) v.coerceIn(0f, 1f) else 0f
}
