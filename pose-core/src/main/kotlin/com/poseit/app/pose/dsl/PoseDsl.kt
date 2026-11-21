package com.poseit.app.pose.dsl

import com.poseit.app.pose.BodyProportions as B
import com.poseit.app.pose.Box
import com.poseit.app.pose.CameraAdvice
import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.toRad
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * ===================================================================================================
 *  Pose authoring DSL (forward kinematics)
 * ===================================================================================================
 *
 *  Built-in poses are written as joint ANGLES instead of raw coordinates, so every pose automatically
 *  has realistic human proportions. Example:
 *
 *    pose("standing_hand_hip", "Hand on Hip", PoseCategory.STANDING) {
 *        description = "A confident classic."
 *        tips("Put your weight on your back leg", "Push the hand-side hip slightly out")
 *        tags(PoseTag.CONFIDENT, PoseTag.SLIMMING)
 *        camera(CameraHeight.WAIST, ShotType.FULL_BODY)
 *        figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
 *            facing = 20f
 *            leftArm(upper = 40f, fore = -50f)       // elbow out, hand back on the hip
 *            rightArm(upper = -6f, fore = -4f)
 *            leftLeg(thigh = 2f, shin = 0f)
 *            rightLeg(thigh = -12f, shin = -4f)
 *        }
 *    }
 *
 *  ANGLE CONVENTION (all angles in degrees, measured in the IMAGE, absolute – not relative to the parent):
 *        0° = pointing straight DOWN
 *      +90° = pointing to the image RIGHT
 *      -90° = pointing to the image LEFT
 *     ±180° = pointing straight UP
 *     e.g. +135° = up and to the right, -45° = down and to the left.
 *
 *  LEFT/RIGHT are ANATOMICAL (the subject's own). With facing = 0 (looking at the camera) the subject's
 *  LEFT shoulder is on the image RIGHT, so an arm raised sideways is leftArm(upper = 90f) / rightArm(upper = -90f).
 *  With facing = 180 (back to camera) it is the other way round.
 *
 *  Segment lengths can be shortened with the *Len parameters (0..1) to fake limbs pointing toward or away
 *  from the camera (foreshortening), e.g. thighs of someone sitting facing the camera: thighLen = 0.45f.
 */

@DslMarker
annotation class PoseDslMarker

fun pose(id: String, name: String, category: PoseCategory, block: PoseBuilder.() -> Unit): PoseTemplate =
    PoseBuilder(id, name, category).apply(block).build()

@PoseDslMarker
class PoseBuilder internal constructor(
    private val id: String,
    private val name: String,
    private val category: PoseCategory,
) {
    var description: String = ""
    var difficulty: Int = 1
    /** width / height of the frame the figures are laid out in. 3:4 portrait by default. */
    var frameAspect: Float = 3f / 4f

    private val tips = mutableListOf<String>()
    private val tags = mutableSetOf<PoseTag>()
    private var camera = CameraAdvice()
    private val figures = mutableListOf<Skeleton>()

    fun tips(vararg t: String) { tips += t }
    fun tags(vararg t: PoseTag) { tags += t }
    fun camera(
        height: CameraHeight = CameraHeight.CHEST,
        shot: ShotType = ShotType.FULL_BODY,
        landscape: Boolean = false,
        note: String? = null,
    ) {
        camera = CameraAdvice(height, shot, landscape, note)
        if (landscape && frameAspect < 1f) frameAspect = 4f / 3f
    }

    /**
     * Adds one person.
     *
     * The figure's BOUNDING BOX (head top .. feet) is positioned in the frame:
     *  - [cx]: horizontal centre of the bounding box (0..1 of frame width)
     *  - exactly one of [bottom] (lowest point, e.g. feet), [top] (top of head) or [cy] (box centre) in 0..1
     *    of frame height. Defaults to bottom = 0.95. Values > 1 / < 0 are fine (cropped portraits).
     *  - [bodyHeight]: the person's STANDING height as a fraction of the frame height (sets the scale; a sitting
     *    person with bodyHeight 0.85 is still "an 0.85-tall person", just folded).
     */
    fun figure(
        cx: Float = 0.5f,
        bottom: Float? = null,
        top: Float? = null,
        cy: Float? = null,
        bodyHeight: Float = 0.82f,
        block: FigureBuilder.() -> Unit,
    ) {
        val fb = FigureBuilder().apply(block)
        figures += fb.build(frameAspect, cx, bottom, top, cy, bodyHeight)
    }

    internal fun build(): PoseTemplate {
        require(figures.isNotEmpty()) { "Pose $id has no figures" }
        return PoseTemplate(
            id = id,
            name = name,
            category = category,
            figures = figures.toList(),
            frameAspect = frameAspect,
            description = description,
            tips = tips.toList(),
            camera = camera,
            tags = tags.toSet(),
            difficulty = difficulty,
        )
    }
}

@PoseDslMarker
class FigureBuilder internal constructor() {
    /** Body yaw. 0 = facing the camera, +90 = profile facing image right, -90 = facing image left, 180 = back. */
    var facing: Float = 0f
    /** Torso lean from vertical; positive = top of the torso toward the image right. */
    var torsoLean: Float = 0f
    /** Apparent torso length (0..1) – shorten when bending toward/away from the camera. */
    var torsoLength: Float = 1f
    /** Rotation of the shoulder line; positive = image-right shoulder LOWER. */
    var shoulderTilt: Float = 0f
    /** Rotation of the hip line; positive = image-right hip LOWER. */
    var hipTilt: Float = 0f
    /** Head roll relative to the torso; positive = toward the image right. */
    var headTilt: Float = 0f
    /** Head yaw (same convention as [facing]); defaults to [facing]. */
    var headYaw: Float? = null
    /** Head pitch; positive = chin up, negative = looking down. Degrees. */
    var headPitch: Float = 0f

    private var leftArm: Limb? = null
    private var rightArm: Limb? = null
    private var leftLeg: Limb? = null
    private var rightLeg: Limb? = null

    private data class Limb(val a: Float, val b: Float, val c: Float?, val aLen: Float, val bLen: Float)

    fun leftArm(upper: Float, fore: Float = upper, hand: Float? = null, upperLen: Float = 1f, foreLen: Float = 1f) {
        leftArm = Limb(upper, fore, hand, upperLen, foreLen)
    }

    fun rightArm(upper: Float, fore: Float = upper, hand: Float? = null, upperLen: Float = 1f, foreLen: Float = 1f) {
        rightArm = Limb(upper, fore, hand, upperLen, foreLen)
    }

    fun leftLeg(thigh: Float, shin: Float = thigh, foot: Float? = null, thighLen: Float = 1f, shinLen: Float = 1f) {
        leftLeg = Limb(thigh, shin, foot, thighLen, shinLen)
    }

    fun rightLeg(thigh: Float, shin: Float = thigh, foot: Float? = null, thighLen: Float = 1f, shinLen: Float = 1f) {
        rightLeg = Limb(thigh, shin, foot, thighLen, shinLen)
    }

    /** Computes joint positions in body units (hip centre at origin), then fits them into the frame. */
    internal fun build(
        frameAspect: Float,
        cx: Float,
        bottom: Float?,
        top: Float?,
        cy: Float?,
        bodyHeight: Float,
    ): Skeleton {
        val pts = LinkedHashMap<Joint, Vec2>()
        val yawRad = facing.toRad()
        val cosYaw = cos(yawRad)
        val sinYaw = sin(yawRad)
        // Which image side the subject's LEFT side is on (+1 = image right).
        val leftSide = if (cosYaw >= 0f) 1f else -1f
        val widthScale = maxOf(abs(cosYaw), 0.14f) * leftSide

        // --- torso -------------------------------------------------------------------------------
        val hipCenter = Vec2.ZERO
        val up = Vec2(sin(torsoLean.toRad()), -cos(torsoLean.toRad()))
        val neck = hipCenter + up * (B.TORSO * torsoLength)
        val perp = Vec2(-up.y, up.x) // image-right perpendicular when upright
        val shoulderAxis = perp.rotated(shoulderTilt)
        val hipAxis = perp.rotated(hipTilt)
        // Twisting the body shifts the shoulders slightly toward where the chest points.
        val shoulderShift = Vec2(sinYaw * 0.06f, 0f)
        pts[Joint.LEFT_SHOULDER] = neck + shoulderShift + shoulderAxis * (B.SHOULDER_HALF * widthScale)
        pts[Joint.RIGHT_SHOULDER] = neck + shoulderShift - shoulderAxis * (B.SHOULDER_HALF * widthScale)
        pts[Joint.LEFT_HIP] = hipCenter + hipAxis * (B.HIP_HALF * widthScale)
        pts[Joint.RIGHT_HIP] = hipCenter - hipAxis * (B.HIP_HALF * widthScale)

        // --- head (a sphere rotated by yaw/pitch, projected, then rolled) --------------------------
        val headAbsTilt = torsoLean + headTilt
        val headUp = Vec2(sin(headAbsTilt.toRad()), -cos(headAbsTilt.toRad()))
        val neckBase = neck + shoulderShift
        val headCenter = neckBase + headUp * (B.NECK + B.HEAD_RADIUS)
        val hy = (headYaw ?: facing).toRad()
        val hp = headPitch.toRad()
        val vis = LinkedHashMap<Joint, Float>()
        fun headPoint(j: Joint, lx: Float, ly: Float, lz: Float) {
            // local: x = subject's left (image right when facing camera), y = down, z = toward camera
            val x1 = lx * cos(hy) + lz * sin(hy)
            val z1 = -lx * sin(hy) + lz * cos(hy)
            val y2 = ly * cos(hp) - z1 * sin(hp)
            val z2 = ly * sin(hp) + z1 * cos(hp)
            val local = Vec2(x1, y2) * B.HEAD_RADIUS
            pts[j] = headCenter + local.rotated(headAbsTilt)
            // Points on the far side of the head would be hidden in a real photo.
            if (z2 < -0.25f) vis[j] = 0.2f
        }
        headPoint(Joint.NOSE, 0f, 0.18f, 1f)
        headPoint(Joint.LEFT_EYE, 0.36f, -0.12f, 0.88f)
        headPoint(Joint.RIGHT_EYE, -0.36f, -0.12f, 0.88f)
        headPoint(Joint.LEFT_EAR, 0.96f, 0.02f, -0.12f)
        headPoint(Joint.RIGHT_EAR, -0.96f, 0.02f, -0.12f)

        // --- limbs ---------------------------------------------------------------------------------
        val outL = leftSide
        val outR = -leftSide
        val la = leftArm ?: Limb(6f * outL, 3f * outL, null, 1f, 1f)
        val ra = rightArm ?: Limb(6f * outR, 3f * outR, null, 1f, 1f)
        val ll = leftLeg ?: Limb(3f * outL * abs(cosYaw), 1f * outL * abs(cosYaw), null, 1f, 1f)
        val rl = rightLeg ?: Limb(3f * outR * abs(cosYaw), 1f * outR * abs(cosYaw), null, 1f, 1f)

        fun arm(shoulder: Joint, elbow: Joint, wrist: Joint, hand: Joint, l: Limb) {
            val s = pts.getValue(shoulder)
            val e = s + Vec2.fromLimbAngle(l.a) * (B.UPPER_ARM * l.aLen)
            val w = e + Vec2.fromLimbAngle(l.b) * (B.FOREARM * l.bLen)
            val h = w + Vec2.fromLimbAngle(l.c ?: l.b) * B.HAND
            pts[elbow] = e; pts[wrist] = w; pts[hand] = h
        }
        arm(Joint.LEFT_SHOULDER, Joint.LEFT_ELBOW, Joint.LEFT_WRIST, Joint.LEFT_HAND, la)
        arm(Joint.RIGHT_SHOULDER, Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST, Joint.RIGHT_HAND, ra)

        fun leg(hip: Joint, knee: Joint, ankle: Joint, foot: Joint, l: Limb, out: Float) {
            val h = pts.getValue(hip)
            val k = h + Vec2.fromLimbAngle(l.a) * (B.THIGH * l.aLen)
            val a = k + Vec2.fromLimbAngle(l.b) * (B.SHIN * l.bLen)
            val f = if (l.c != null) {
                a + Vec2.fromLimbAngle(l.c) * B.FOOT
            } else {
                // Feet rest on the ground pointing where the body faces (foreshortened when facing camera).
                val dir = Vec2(sinYaw + out * 0.3f * abs(cosYaw), 0.45f).normalized()
                a + dir * (B.FOOT * (0.45f + 0.55f * abs(sinYaw)))
            }
            pts[knee] = k; pts[ankle] = a; pts[foot] = f
        }
        leg(Joint.LEFT_HIP, Joint.LEFT_KNEE, Joint.LEFT_ANKLE, Joint.LEFT_FOOT, ll, outL)
        leg(Joint.RIGHT_HIP, Joint.RIGHT_KNEE, Joint.RIGHT_ANKLE, Joint.RIGHT_FOOT, rl, outR)

        // --- fit into frame ----------------------------------------------------------------------
        val extents = pts.values.toMutableList()
        extents += headCenter + Vec2(0f, -B.HEAD_RADIUS)
        extents += headCenter + Vec2(-B.HEAD_RADIUS, 0f)
        extents += headCenter + Vec2(B.HEAD_RADIUS, 0f)
        extents += pts.getValue(Joint.LEFT_ANKLE) + Vec2(0f, 0.06f)
        extents += pts.getValue(Joint.RIGHT_ANKLE) + Vec2(0f, 0.06f)
        val box = Box.around(extents)!!
        val s = bodyHeight / B.STANDING_HEIGHT // frame heights per body unit
        val anchorY: (Float) -> Float = when {
            bottom != null -> { y -> (y - box.bottom) * s + bottom }
            top != null -> { y -> (y - box.top) * s + top }
            cy != null -> { y -> (y - box.center.y) * s + cy }
            else -> { y -> (y - box.bottom) * s + 0.95f }
        }
        val boxCx = box.center.x
        val normalized = pts.mapValues { (_, p) ->
            val xH = (p.x - boxCx) * s + cx * frameAspect
            Vec2(xH / frameAspect, anchorY(p.y))
        }
        return Skeleton(points = normalized, visibility = vis, unit = s)
    }
}
