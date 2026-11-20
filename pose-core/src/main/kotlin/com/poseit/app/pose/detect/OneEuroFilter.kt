package com.poseit.app.pose.detect

import kotlin.math.PI
import kotlin.math.abs

/**
 * One Euro filter for a single scalar signal: an adaptive low-pass filter whose cutoff frequency rises with the
 * signal's speed (Casiez, Roussel & Vogel, "1€ Filter: A Simple Speed-based Low-pass Filter for Noisy Input in
 * Interactive Systems", CHI 2012). Written from the published algorithm:
 *
 * ```
 * alpha(fc, dt) = 1 / (1 + tau / dt)          with tau = 1 / (2 * PI * fc)
 * dx      = (x - xPrev) / dt                   raw speed
 * edx     = lowpass(dx, alpha(dCutoff, dt))    smoothed speed
 * fc      = minCutoff + beta * |edx|           adaptive cutoff
 * xHat    = lowpass(x, alpha(fc, dt))
 * ```
 *
 * @param speedScale multiplies the smoothed speed before it is weighted by [beta], so [beta] can be expressed in
 *   "virtual pixels" even though the filtered values are normalized coordinates.
 */
internal class OneEuroFilter(
    private val minCutoff: Float,
    private val beta: Float,
    private val dCutoff: Float,
    private val speedScale: Float = 1f,
) {
    private var initialized = false
    private var prevRaw = 0f
    private var prevFiltered = 0f
    private var prevSpeed = 0f

    /** True once the filter has seen its first sample. */
    val isInitialized: Boolean get() = initialized

    /** The last filtered value (meaningless before the first sample). */
    val value: Float get() = prevFiltered

    fun reset() {
        initialized = false
        prevSpeed = 0f
    }

    /**
     * Feeds one sample taken [dtSeconds] after the previous one and returns the filtered value. The first sample is
     * returned unchanged. A non-positive (or non-finite) dt returns the previous output without updating the state,
     * since no time has passed.
     */
    fun filter(x: Float, dtSeconds: Float): Float {
        if (!x.isFinite()) return if (initialized) prevFiltered else x
        if (!initialized) {
            initialized = true
            prevRaw = x
            prevFiltered = x
            prevSpeed = 0f
            return x
        }
        if (!(dtSeconds > 0f) || !dtSeconds.isFinite()) return prevFiltered

        val rawSpeed = (x - prevRaw) / dtSeconds
        val speed = lerp(prevSpeed, rawSpeed, alpha(dCutoff, dtSeconds))
        val cutoff = minCutoff + beta * abs(speed) * speedScale
        val filtered = lerp(prevFiltered, x, alpha(cutoff, dtSeconds))

        prevRaw = x
        prevSpeed = speed
        prevFiltered = filtered
        return filtered
    }

    private fun alpha(cutoffHz: Float, dtSeconds: Float): Float {
        val fc = if (cutoffHz.isFinite() && cutoffHz > 1e-4f) cutoffHz else 1e-4f
        val tau = 1f / (2f * PI.toFloat() * fc)
        return 1f / (1f + tau / dtSeconds)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
