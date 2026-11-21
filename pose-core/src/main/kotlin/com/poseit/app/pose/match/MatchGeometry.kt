package com.poseit.app.pose.match

import com.poseit.app.pose.BodyProportions
import com.poseit.app.pose.Box
import com.poseit.app.pose.Joint
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.angleDiff

/** Which way a person's chest points relative to the camera. */
internal enum class Facing { FRONT, BACK, UNKNOWN }

/** Endpoints and reference length of a [Segment]; an endpoint with two joints is their midpoint. */
internal val Segment.fromJoints: List<Joint>
    get() = when (this) {
        Segment.TORSO -> listOf(Joint.LEFT_HIP, Joint.RIGHT_HIP)
        Segment.HEAD -> listOf(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
        Segment.SHOULDER_LINE -> listOf(Joint.RIGHT_SHOULDER)
        Segment.HIP_LINE -> listOf(Joint.RIGHT_HIP)
        Segment.LEFT_UPPER_ARM -> listOf(Joint.LEFT_SHOULDER)
        Segment.LEFT_FOREARM -> listOf(Joint.LEFT_ELBOW)
        Segment.RIGHT_UPPER_ARM -> listOf(Joint.RIGHT_SHOULDER)
        Segment.RIGHT_FOREARM -> listOf(Joint.RIGHT_ELBOW)
        Segment.LEFT_THIGH -> listOf(Joint.LEFT_HIP)
        Segment.LEFT_SHIN -> listOf(Joint.LEFT_KNEE)
        Segment.RIGHT_THIGH -> listOf(Joint.RIGHT_HIP)
        Segment.RIGHT_SHIN -> listOf(Joint.RIGHT_KNEE)
    }

internal val Segment.toJoints: List<Joint>
    get() = when (this) {
        Segment.TORSO -> listOf(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
        Segment.HEAD -> listOf(Joint.NOSE)
        Segment.SHOULDER_LINE -> listOf(Joint.LEFT_SHOULDER)
        Segment.HIP_LINE -> listOf(Joint.LEFT_HIP)
        Segment.LEFT_UPPER_ARM -> listOf(Joint.LEFT_ELBOW)
        Segment.LEFT_FOREARM -> listOf(Joint.LEFT_WRIST)
        Segment.RIGHT_UPPER_ARM -> listOf(Joint.RIGHT_ELBOW)
        Segment.RIGHT_FOREARM -> listOf(Joint.RIGHT_WRIST)
        Segment.LEFT_THIGH -> listOf(Joint.LEFT_KNEE)
        Segment.LEFT_SHIN -> listOf(Joint.LEFT_ANKLE)
        Segment.RIGHT_THIGH -> listOf(Joint.RIGHT_KNEE)
        Segment.RIGHT_SHIN -> listOf(Joint.RIGHT_ANKLE)
    }

/** Typical length of the segment in body units (see [BodyProportions]). */
internal val Segment.normalLength: Float
    get() = when (this) {
        Segment.TORSO -> BodyProportions.TORSO
        Segment.HEAD -> BodyProportions.NECK + BodyProportions.HEAD_RADIUS * 0.82f
        Segment.SHOULDER_LINE -> 2f * BodyProportions.SHOULDER_HALF
        Segment.HIP_LINE -> 2f * BodyProportions.HIP_HALF
        Segment.LEFT_UPPER_ARM, Segment.RIGHT_UPPER_ARM -> BodyProportions.UPPER_ARM
        Segment.LEFT_FOREARM, Segment.RIGHT_FOREARM -> BodyProportions.FOREARM
        Segment.LEFT_THIGH, Segment.RIGHT_THIGH -> BodyProportions.THIGH
        Segment.LEFT_SHIN, Segment.RIGHT_SHIN -> BodyProportions.SHIN
    }

/** Shoulder / hip lines: their direction is undefined in profile views. */
internal val Segment.isLine: Boolean get() = this == Segment.SHOULDER_LINE || this == Segment.HIP_LINE

/** Segments whose length does not depend on facing or head pitch – used to compare body sizes. */
internal val Segment.isSizeReference: Boolean get() = !isLine && this != Segment.HEAD

/** Anatomical side of a limb segment: true = left, false = right, null = central. */
internal val Segment.isLeft: Boolean?
    get() = when (this) {
        Segment.LEFT_UPPER_ARM, Segment.LEFT_FOREARM, Segment.LEFT_THIGH, Segment.LEFT_SHIN -> true
        Segment.RIGHT_UPPER_ARM, Segment.RIGHT_FOREARM, Segment.RIGHT_THIGH, Segment.RIGHT_SHIN -> false
        else -> null
    }

internal object MatchGeometry {
    /** Tolerance around the scene edges when deciding whether a joint is "in frame". */
    const val SCENE_MARGIN = 0.02f
    private val SCENE = Box(0f, 0f, 1f, 1f)

    /** Template lines shorter than this fraction of their normal length are skipped (profile views). */
    const val MIN_LINE_FRACTION = 0.25f

    /** Template limbs shorter than this fraction of their normal length have no usable direction. */
    const val MIN_LIMB_FRACTION = 0.12f

    fun isFinite(p: Vec2): Boolean = p.x.isFinite() && p.y.isFinite()

    fun inScene(p: Vec2, margin: Float = SCENE_MARGIN): Boolean = SCENE.contains(p, margin)

    fun sanitizeAspect(aspect: Float): Float = if (aspect.isFinite() && aspect > 0.05f && aspect < 20f) aspect else 0.75f

    /** Drops non-finite points; non-finite visibilities become 0, others are clamped to 0..1. */
    fun sanitize(s: Skeleton): Skeleton {
        val cleanPoints = s.points.filterValues { isFinite(it) }
        val cleanVis = s.visibility.filterKeys { it in cleanPoints }
            .mapValues { (_, v) -> if (v.isFinite()) v.coerceIn(0f, 1f) else 0f }
        val cleanUnit = if (s.unit.isFinite() && s.unit > 0f) s.unit else 0f
        return if (cleanPoints.size == s.points.size && cleanVis == s.visibility && cleanUnit == s.unit) s
        else Skeleton(cleanPoints, cleanVis, cleanUnit)
    }

    /** Hermite smoothstep of x from 0 at [edge0] to 1 at [edge1]. */
    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        if (!(edge1 > edge0)) return if (x >= edge0) 1f else 0f
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** 1 within [perfect] degrees, 0 at >= [zero] degrees, smooth (Hermite) in between. */
    fun segmentScore(absErrorDeg: Float, perfect: Float, zero: Float): Float {
        if (!absErrorDeg.isFinite()) return 0f
        return 1f - smoothstep(perfect, zero, absErrorDeg)
    }

    /** Lean of an "upward" segment (limb angle near ±180°): positive = top toward the image right. */
    fun leanOf(limbAngle: Float): Float = -angleDiff(limbAngle, 180f)

    fun mean(points: List<Vec2>): Vec2? {
        if (points.isEmpty()) return null
        var x = 0f
        var y = 0f
        for (p in points) {
            x += p.x
            y += p.y
        }
        return Vec2(x / points.size, y / points.size)
    }

    fun median(values: List<Float>): Float? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2f
    }

    /**
     * Facing from the shoulders (then hips): FRONT if the subject's LEFT side is on the image right.
     * UNKNOWN when the pair is missing or too narrow to tell (profile).
     * @param iso isotropic positions of usable joints
     * @param unit body unit in the same units
     */
    fun facingOf(iso: (Joint) -> Vec2?, unit: Float): Facing {
        val minWidth = 0.12f * unit
        for ((l, r) in listOf(Joint.LEFT_SHOULDER to Joint.RIGHT_SHOULDER, Joint.LEFT_HIP to Joint.RIGHT_HIP)) {
            val pl = iso(l) ?: continue
            val pr = iso(r) ?: continue
            val dx = pl.x - pr.x
            if (dx > minWidth) return Facing.FRONT
            if (dx < -minWidth) return Facing.BACK
        }
        return Facing.UNKNOWN
    }
}
