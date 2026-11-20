package com.poseit.app.pose.detect

import com.poseit.app.pose.Skeleton

/**
 * Removes landmark jitter from live detections with a One Euro filter per joint coordinate
 * (Casiez et al. 2012: low lag when moving fast, strong smoothing when still).
 *
 * People are tracked across frames by nearest hip/shoulder centre; a person that disappears for more than
 * ~500 ms loses its filter state. Visibility is smoothed with a simple exponential filter.
 *
 * @param minCutoff minimum cutoff frequency (Hz) – lower = smoother when still
 * @param beta speed coefficient – higher = less lag when moving
 * @param dCutoff cutoff for the derivative
 */
class LandmarkSmoother(
    val minCutoff: Float = 1.2f,
    val beta: Float = 0.03f,
    val dCutoff: Float = 1.0f,
) {
    /** @param people detections of one frame (scene space); @param timestampMs frame time; returns smoothed copies */
    fun smooth(people: List<Skeleton>, timestampMs: Long): List<Skeleton> {
        TODO("implemented by the engine agent")
    }

    fun reset() {
        TODO("implemented by the engine agent")
    }
}
