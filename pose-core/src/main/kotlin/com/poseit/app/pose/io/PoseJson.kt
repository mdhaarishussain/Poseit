package com.poseit.app.pose.io

import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton

/** JSON (de)serialization of user-created poses. Must be lenient: ignore unknown keys, never crash on bad input. */
object PoseJson {
    fun encode(poses: List<PoseTemplate>): String {
        TODO("implemented by the engine agent")
    }

    /** Returns an empty list for blank/corrupt input. */
    fun decode(json: String): List<PoseTemplate> {
        TODO("implemented by the engine agent")
    }
}

object CustomPoseFactory {
    /**
     * Builds a custom template from people detected in a still photo.
     * @param people skeletons in IMAGE-normalized coordinates (LandmarkMapping.toScene with sceneAspect = image aspect)
     * @param imageAspect image width / height; becomes the template frameAspect
     * Joints with low visibility (< 0.5) are dropped so the template only contains what was actually seen.
     * The body [Skeleton.unit] is estimated from the points.
     */
    fun fromDetection(
        id: String,
        name: String,
        category: PoseCategory,
        people: List<Skeleton>,
        imageAspect: Float,
        referenceImage: String?,
        createdAt: Long,
        source: PoseSource = PoseSource.FROM_PHOTO,
    ): PoseTemplate {
        TODO("implemented by the engine agent")
    }
}
