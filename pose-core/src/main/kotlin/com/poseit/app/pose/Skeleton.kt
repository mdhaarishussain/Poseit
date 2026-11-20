package com.poseit.app.pose

import kotlinx.serialization.Serializable

/**
 * One person's 2D pose.
 *
 * Coordinates are NORMALIZED to the frame they live in: x = 0..1 across the frame width, y = 0..1 down
 * the frame height (values outside 0..1 are allowed and mean "outside the frame", e.g. the legs of a
 * waist-up portrait). Because width and height differ, ALWAYS convert with the frame aspect ratio
 * ([toIsotropic]) before measuring angles or distances.
 *
 * @param points landmark positions. Missing joints are simply absent.
 * @param visibility optional per-joint confidence 0..1 (detections only; templates omit it = fully visible).
 * @param unit size of one "body unit" (= torso length, hip-centre to shoulder-centre, of a neutral pose)
 *   expressed in frame HEIGHTS. Used by renderers to size the head and limb thickness. 0 = unknown
 *   (renderers then estimate it from the points).
 */
@Serializable
data class Skeleton(
    val points: Map<Joint, Vec2>,
    val visibility: Map<Joint, Float> = emptyMap(),
    val unit: Float = 0f,
) {
    operator fun get(j: Joint): Vec2? = points[j]

    fun isVisible(j: Joint, minVisibility: Float = 0.5f): Boolean =
        points.containsKey(j) && (visibility[j] ?: 1f) >= minVisibility

    fun midShoulder(): Vec2? = mid(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
    fun midHip(): Vec2? = mid(Joint.LEFT_HIP, Joint.RIGHT_HIP)

    private fun mid(a: Joint, b: Joint): Vec2? {
        val pa = points[a] ?: return null
        val pb = points[b] ?: return null
        return Vec2.mid(pa, pb)
    }

    /** Converts to isotropic "height units" (x multiplied by [aspect] = frameWidth / frameHeight). */
    fun toIsotropic(aspect: Float): Map<Joint, Vec2> = points.mapValues { (_, p) -> Vec2(p.x * aspect, p.y) }

    /**
     * Best-effort body unit (torso length in frame heights) estimated from the points when [unit] is 0.
     */
    fun estimatedUnit(aspect: Float): Float {
        if (unit > 0f) return unit
        val iso = toIsotropic(aspect)
        val ms = midOf(iso, Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
        val mh = midOf(iso, Joint.LEFT_HIP, Joint.RIGHT_HIP)
        val candidates = mutableListOf<Float>()
        if (ms != null && mh != null) candidates += ms.distanceTo(mh)
        val ls = iso[Joint.LEFT_SHOULDER]
        val rs = iso[Joint.RIGHT_SHOULDER]
        if (ls != null && rs != null) candidates += ls.distanceTo(rs) / 0.76f
        val le = iso[Joint.LEFT_EAR]
        val re = iso[Joint.RIGHT_EAR]
        if (le != null && re != null) candidates += le.distanceTo(re) / 0.42f
        val lsh = iso[Joint.LEFT_SHOULDER]
        val lel = iso[Joint.LEFT_ELBOW]
        if (lsh != null && lel != null) candidates += lsh.distanceTo(lel) / BodyProportions.UPPER_ARM
        val rsh = iso[Joint.RIGHT_SHOULDER]
        val rel = iso[Joint.RIGHT_ELBOW]
        if (rsh != null && rel != null) candidates += rsh.distanceTo(rel) / BodyProportions.UPPER_ARM
        return candidates.maxOrNull()?.takeIf { it > 1e-4f } ?: 0.15f
    }

    /** Horizontally mirrored copy: x -> 1 - x AND left/right labels swapped, so the result is anatomically valid. */
    fun mirrored(): Skeleton = Skeleton(
        points = points.entries.associate { (j, p) -> j.opposite to Vec2(1f - p.x, p.y) },
        visibility = visibility.entries.associate { (j, v) -> j.opposite to v },
        unit = unit,
    )

    /** Mirrors only the coordinates (x -> 1 - x) WITHOUT swapping labels – for drawing on a mirrored preview. */
    fun flippedForDisplay(): Skeleton = copy(points = points.mapValues { (_, p) -> Vec2(1f - p.x, p.y) })

    fun mapPoints(transform: (Vec2) -> Vec2): Skeleton = copy(points = points.mapValues { (_, p) -> transform(p) })

    companion object {
        private fun midOf(m: Map<Joint, Vec2>, a: Joint, b: Joint): Vec2? {
            val pa = m[a] ?: return null
            val pb = m[b] ?: return null
            return Vec2.mid(pa, pb)
        }
    }
}

/**
 * Average human proportions, in BODY UNITS (1 unit = torso length, hip-centre to shoulder-centre).
 * Roughly a 7.3-heads-tall adult.
 */
object BodyProportions {
    const val TORSO = 1.0f
    const val NECK = 0.14f            // shoulder-centre to base of skull
    const val HEAD_RADIUS = 0.23f     // head drawn as a circle of this radius
    const val SHOULDER_HALF = 0.38f   // half the shoulder width
    const val HIP_HALF = 0.19f        // half the hip-joint width
    const val UPPER_ARM = 0.60f
    const val FOREARM = 0.52f
    const val HAND = 0.20f
    const val THIGH = 0.88f
    const val SHIN = 0.84f
    const val FOOT = 0.26f

    /** Approximate full standing height (feet to top of head) in body units. */
    const val STANDING_HEIGHT = THIGH + SHIN + 0.06f + TORSO + NECK + 2 * HEAD_RADIUS + 0.04f
}
