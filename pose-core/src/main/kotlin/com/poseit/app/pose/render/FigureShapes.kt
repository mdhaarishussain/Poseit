package com.poseit.app.pose.render

import com.poseit.app.pose.BodyPart
import com.poseit.app.pose.BodyProportions
import com.poseit.app.pose.Bone
import com.poseit.app.pose.Joint
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2

/**
 * Platform-independent description of how to draw a "mannequin" figure for a [Skeleton].
 * Both the Compose renderer (app) and the Java2D renderer (JVM preview tests) consume this, so they look
 * identical.
 *
 * All coordinates are in ISOTROPIC HEIGHT UNITS of the frame: x in 0..aspect, y in 0..1 (multiply both by
 * the frame's pixel height to get pixels, then add the frame's pixel offset).
 */
sealed interface FigureShape {
    val part: BodyPart
    val bone: Bone?

    /** A rounded stick from [a] to [b] whose width tapers from [widthA] to [widthB]. */
    data class Capsule(
        val a: Vec2, val b: Vec2, val widthA: Float, val widthB: Float,
        override val part: BodyPart, override val bone: Bone?,
    ) : FigureShape

    data class Circle(val center: Vec2, val radius: Float, override val part: BodyPart, override val bone: Bone? = null) :
        FigureShape

    /** A filled closed polygon (torso). */
    data class Polygon(val points: List<Vec2>, override val part: BodyPart, override val bone: Bone? = null) : FigureShape
}

object FigureShapes {

    /** Draw order: back limbs first is not knowable in 2D, so torso first, then legs, arms, head. */
    fun build(skeleton: Skeleton, aspect: Float): List<FigureShape> {
        val p = skeleton.toIsotropic(aspect)
        val u = skeleton.estimatedUnit(aspect)
        val out = mutableListOf<FigureShape>()

        val ls = p[Joint.LEFT_SHOULDER]
        val rs = p[Joint.RIGHT_SHOULDER]
        val lh = p[Joint.LEFT_HIP]
        val rh = p[Joint.RIGHT_HIP]
        val midShoulder = if (ls != null && rs != null) Vec2.mid(ls, rs) else ls ?: rs
        val midHip = if (lh != null && rh != null) Vec2.mid(lh, rh) else lh ?: rh

        // Torso: quad + a thick spine so profile views still have depth.
        if (ls != null && rs != null && lh != null && rh != null) {
            out += FigureShape.Polygon(listOf(ls, rs, rh, lh), BodyPart.TORSO)
        }
        if (midShoulder != null && midHip != null) {
            out += FigureShape.Capsule(midShoulder, midHip, 0.44f * u, 0.40f * u, BodyPart.TORSO, null)
        }
        if (ls != null && rs != null) out += FigureShape.Capsule(ls, rs, 0.20f * u, 0.20f * u, BodyPart.TORSO, Bone.SHOULDERS)
        if (lh != null && rh != null) out += FigureShape.Capsule(lh, rh, 0.26f * u, 0.26f * u, BodyPart.TORSO, Bone.HIPS)
        if (ls != null && lh != null) out += FigureShape.Capsule(ls, lh, 0.20f * u, 0.24f * u, BodyPart.TORSO, Bone.LEFT_FLANK)
        if (rs != null && rh != null) out += FigureShape.Capsule(rs, rh, 0.20f * u, 0.24f * u, BodyPart.TORSO, Bone.RIGHT_FLANK)

        fun cap(bone: Bone, wa: Float, wb: Float) {
            val a = p[bone.from] ?: return
            val b = p[bone.to] ?: return
            out += FigureShape.Capsule(a, b, wa * u, wb * u, bone.part, bone)
        }
        // legs
        cap(Bone.LEFT_THIGH, 0.27f, 0.19f); cap(Bone.LEFT_SHIN, 0.18f, 0.12f); cap(Bone.LEFT_FOOT, 0.12f, 0.09f)
        cap(Bone.RIGHT_THIGH, 0.27f, 0.19f); cap(Bone.RIGHT_SHIN, 0.18f, 0.12f); cap(Bone.RIGHT_FOOT, 0.12f, 0.09f)
        // arms
        cap(Bone.LEFT_UPPER_ARM, 0.17f, 0.14f); cap(Bone.LEFT_FOREARM, 0.14f, 0.11f); cap(Bone.LEFT_HAND, 0.11f, 0.08f)
        cap(Bone.RIGHT_UPPER_ARM, 0.17f, 0.14f); cap(Bone.RIGHT_FOREARM, 0.14f, 0.11f); cap(Bone.RIGHT_HAND, 0.11f, 0.08f)

        // head + neck
        val head = headCenter(p, u, midShoulder)
        if (head != null) {
            if (midShoulder != null) {
                val towardHead = (head - midShoulder)
                val neckEnd = midShoulder + towardHead * 0.55f
                out += FigureShape.Capsule(midShoulder, neckEnd, 0.15f * u, 0.13f * u, BodyPart.HEAD, null)
            }
            out += FigureShape.Circle(head, BodyProportions.HEAD_RADIUS * u, BodyPart.HEAD)
        }
        return out
    }

    /** Small marker showing which way the face points (drawn on top of the head), or null. */
    fun noseMarker(skeleton: Skeleton, aspect: Float): FigureShape.Circle? {
        if (!skeleton.isVisible(Joint.NOSE)) return null
        val p = skeleton.toIsotropic(aspect)
        val nose = p[Joint.NOSE] ?: return null
        val u = skeleton.estimatedUnit(aspect)
        return FigureShape.Circle(nose, 0.045f * u, BodyPart.HEAD)
    }

    fun headCenter(p: Map<Joint, Vec2>, u: Float, midShoulder: Vec2?): Vec2? {
        val le = p[Joint.LEFT_EAR]
        val re = p[Joint.RIGHT_EAR]
        val nose = p[Joint.NOSE]
        val leye = p[Joint.LEFT_EYE]
        val reye = p[Joint.RIGHT_EYE]
        return when {
            le != null && re != null -> {
                val c = Vec2.mid(le, re)
                // Ears sit slightly below the centre of the skull.
                c + Vec2(0f, -0.02f * u)
            }
            leye != null && reye != null && nose != null -> Vec2.mid(leye, reye) * 0.5f + nose * 0.5f
            nose != null -> nose
            midShoulder != null -> midShoulder + Vec2(0f, -(BodyProportions.NECK + BodyProportions.HEAD_RADIUS) * u)
            else -> null
        }
    }
}
