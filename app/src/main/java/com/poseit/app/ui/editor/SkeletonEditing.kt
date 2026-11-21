package com.poseit.app.ui.editor

import com.poseit.app.pose.BodyProportions
import com.poseit.app.pose.Bone
import com.poseit.app.pose.Box
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.dsl.pose
import com.poseit.app.pose.render.FigureShapes
import com.poseit.app.pose.toDeg
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** What a drag in the editor manipulates. */
sealed interface DragTarget {
    /**
     * A body joint. Limb joints rotate their bone around the parent joint (forward kinematics, descendants follow
     * rigidly). Shoulders tilt the shoulder line around the neck (both arms follow), hips tilt the hip line around
     * the hip centre (both legs follow).
     */
    data class JointTarget(val joint: Joint) : DragTarget

    /** The head: face points rotate around the neck (shoulder centre). */
    data object Head : DragTarget

    /** The "hips" handle: the upper body (shoulders, arms, head) rotates around the hip centre. */
    data object Lean : DragTarget

    /** The torso: the whole figure moves. */
    data object Move : DragTarget
}

/** A visible handle of the selected figure, in ISOTROPIC frame units (x * aspect, y). */
data class EditorHandle(val target: DragTarget, val position: Vec2)

/**
 * Pure editing operations on a [Skeleton] in frame-normalised coordinates. All geometry is done in isotropic space
 * (x multiplied by the frame aspect) so rotations preserve the real bone lengths; results are converted back.
 * [Skeleton.unit] and [Skeleton.visibility] are always preserved.
 */
object SkeletonEditing {

    private val ARM_LEFT = listOf(Joint.LEFT_SHOULDER, Joint.LEFT_ELBOW, Joint.LEFT_WRIST, Joint.LEFT_HAND)
    private val ARM_RIGHT = listOf(Joint.RIGHT_SHOULDER, Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST, Joint.RIGHT_HAND)
    private val LEG_LEFT = listOf(Joint.LEFT_HIP, Joint.LEFT_KNEE, Joint.LEFT_ANKLE, Joint.LEFT_FOOT)
    private val LEG_RIGHT = listOf(Joint.RIGHT_HIP, Joint.RIGHT_KNEE, Joint.RIGHT_ANKLE, Joint.RIGHT_FOOT)
    private val CHAINS = listOf(ARM_LEFT, ARM_RIGHT, LEG_LEFT, LEG_RIGHT)

    /** Distance of the hips (lean) handle below the hip centre, in body units. */
    private const val LEAN_HANDLE_OFFSET = 0.42f

    /** Shoulders + arms + face: everything above the pelvis. */
    private val UPPER_BODY: Set<Joint> = (ARM_LEFT + ARM_RIGHT + Joint.FACE).toSet()
    private val ARMS: Set<Joint> = (ARM_LEFT + ARM_RIGHT).toSet()
    private val LEGS: Set<Joint> = (LEG_LEFT + LEG_RIGHT).toSet()

    /** Joints further down the chain than [joint] (elbow -> wrist, hand), empty for chain ends and the face. */
    fun descendants(joint: Joint): List<Joint> {
        for (chain in CHAINS) {
            val i = chain.indexOf(joint)
            if (i >= 0) return chain.subList(i + 1, chain.size)
        }
        return emptyList()
    }

    /** The joint a limb joint rotates around, or null for chain roots (shoulders, hips) and the face. */
    fun parentOf(joint: Joint): Joint? {
        for (chain in CHAINS) {
            val i = chain.indexOf(joint)
            if (i > 0) return chain[i - 1]
        }
        return null
    }

    // ---- handles / hit testing -------------------------------------------------------------------------------

    /** Handles to draw for [skeleton]: every body joint, one head handle and the hips (lean) handle. */
    fun handles(skeleton: Skeleton, aspect: Float): List<EditorHandle> {
        val iso = skeleton.toIsotropic(aspect)
        val out = mutableListOf<EditorHandle>()
        for ((joint, p) in iso) {
            if (joint in Joint.FACE) continue
            out += EditorHandle(DragTarget.JointTarget(joint), p)
        }
        headCenter(skeleton, aspect)?.let { out += EditorHandle(DragTarget.Head, it) }
        val hip = hipCenterIso(iso)
        val neck = neckIso(iso)
        if (hip != null && neck != null) {
            // Sits a little below the pelvis, along the spine, so it doesn't cover the two hip joints.
            val down = (hip - neck).normalized().takeIf { it.length > 0.5f } ?: Vec2(0f, 1f)
            out += EditorHandle(DragTarget.Lean, hip + down * (skeleton.estimatedUnit(aspect) * LEAN_HANDLE_OFFSET))
        }
        return out
    }

    /** Centre of the drawn head (isotropic), matching the renderer. */
    fun headCenter(skeleton: Skeleton, aspect: Float): Vec2? {
        val iso = skeleton.toIsotropic(aspect)
        if (Joint.FACE.none { it in iso }) return null
        return FigureShapes.headCenter(iso, skeleton.estimatedUnit(aspect), neckIso(iso))
    }

    /**
     * What a touch at [touch] (isotropic) grabs on [skeleton], given a handle radius [handleRadius] (isotropic).
     * Handles win (closest first); otherwise the body itself: a limb bone grabs its far joint, the torso moves the
     * figure, the head rotates the head. Null when the touch misses the figure.
     */
    fun hitTest(skeleton: Skeleton, aspect: Float, touch: Vec2, handleRadius: Float): DragTarget? {
        handles(skeleton, aspect)
            .map { it to it.position.distanceTo(touch) }
            .filter { it.second <= handleRadius }
            .minByOrNull { it.second }
            ?.let { return it.first.target }
        return bodyHit(skeleton, aspect, touch, slop = handleRadius * 0.35f)
    }

    /** Distance-based body hit test (no handles). */
    fun bodyHit(skeleton: Skeleton, aspect: Float, touch: Vec2, slop: Float): DragTarget? {
        val iso = skeleton.toIsotropic(aspect)
        val u = skeleton.estimatedUnit(aspect)
        // Head
        headCenter(skeleton, aspect)?.let { c ->
            if (c.distanceTo(touch) <= BodyProportions.HEAD_RADIUS * u + slop) return DragTarget.Head
        }
        // Torso polygon / spine
        if (insideTorso(iso, touch, u * 0.22f + slop)) return DragTarget.Move
        // Limbs: closest bone within half its thickness + slop
        var best: Pair<Joint, Float>? = null
        for (bone in Bone.entries) {
            // Only limb bones (shoulder->elbow ... ankle->foot): grabbing one rotates it around its upper joint.
            if (parentOf(bone.to) != bone.from) continue
            val a = iso[bone.from] ?: continue
            val b = iso[bone.to] ?: continue
            val d = distanceToSegment(touch, a, b)
            if (d <= u * 0.14f + slop && (best == null || d < best.second)) best = bone.to to d
        }
        return best?.let { DragTarget.JointTarget(it.first) }
    }

    /** Minimum distance from [touch] to the figure (isotropic), used to pick which figure was tapped. */
    fun distanceToFigure(skeleton: Skeleton, aspect: Float, touch: Vec2): Float {
        val iso = skeleton.toIsotropic(aspect)
        val u = skeleton.estimatedUnit(aspect)
        var best = Float.MAX_VALUE
        if (insideTorso(iso, touch, 0f)) return 0f
        for (bone in Bone.entries) {
            val a = iso[bone.from] ?: continue
            val b = iso[bone.to] ?: continue
            best = min(best, distanceToSegment(touch, a, b))
        }
        headCenter(skeleton, aspect)?.let { best = min(best, max(0f, it.distanceTo(touch) - BodyProportions.HEAD_RADIUS * u)) }
        if (best == Float.MAX_VALUE) iso.values.forEach { best = min(best, it.distanceTo(touch)) }
        return best
    }

    private fun insideTorso(iso: Map<Joint, Vec2>, p: Vec2, spineRadius: Float): Boolean {
        val ls = iso[Joint.LEFT_SHOULDER]
        val rs = iso[Joint.RIGHT_SHOULDER]
        val lh = iso[Joint.LEFT_HIP]
        val rh = iso[Joint.RIGHT_HIP]
        if (ls != null && rs != null && lh != null && rh != null && pointInPolygon(p, listOf(ls, rs, rh, lh))) return true
        val neck = neckIso(iso)
        val hip = hipCenterIso(iso)
        return neck != null && hip != null && spineRadius > 0f && distanceToSegment(p, neck, hip) <= spineRadius
    }

    // ---- drag ----------------------------------------------------------------------------------------------

    /**
     * Applies a drag of [target] to [start] (the skeleton as it was when the finger went down). The finger moved
     * from [fingerStart] to [fingerNow] (isotropic). Computing from the start state avoids drift.
     */
    fun drag(start: Skeleton, aspect: Float, target: DragTarget, fingerStart: Vec2, fingerNow: Vec2): Skeleton {
        if (target == DragTarget.Move) {
            val d = fingerNow - fingerStart
            return translateClamped(start, Vec2(d.x / aspect, d.y))
        }
        val iso = start.toIsotropic(aspect)
        val moved: Map<Joint, Vec2> = when (target) {
            DragTarget.Move -> iso
            DragTarget.Head -> {
                val pivot = neckIso(iso)
                val face = Joint.FACE.filter { it in iso }
                if (pivot == null) translate(iso, fingerNow - fingerStart, face) else rotate(iso, face, pivot, fingerStart, fingerNow)
            }
            DragTarget.Lean -> {
                val pivot = hipCenterIso(iso)
                val neck = neckIso(iso)
                if (pivot == null || neck == null) {
                    iso
                } else {
                    // The handle sits on the pivot, so steer the neck by the finger's movement (a virtual lever).
                    rotate(iso, UPPER_BODY.filter { it in iso }, pivot, neck, neck + (fingerNow - fingerStart))
                }
            }
            is DragTarget.JointTarget -> dragJoint(iso, target.joint, fingerStart, fingerNow)
        }
        return fromIsotropic(start, moved, aspect)
    }

    private fun dragJoint(iso: Map<Joint, Vec2>, joint: Joint, fingerStart: Vec2, fingerNow: Vec2): Map<Joint, Vec2> {
        val delta = fingerNow - fingerStart
        when (joint) {
            Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER -> {
                // Tilting needs both shoulders; with one missing, the shoulder simply moves with its arm.
                val pivot = if (Joint.LEFT_SHOULDER in iso && Joint.RIGHT_SHOULDER in iso) neckIso(iso) else null
                val joints = ARMS.filter { it in iso }
                return if (pivot == null) {
                    translate(iso, delta, listOf(joint) + descendants(joint))
                } else {
                    rotate(iso, joints, pivot, fingerStart, fingerNow)
                }
            }
            Joint.LEFT_HIP, Joint.RIGHT_HIP -> {
                val pivot = if (Joint.LEFT_HIP in iso && Joint.RIGHT_HIP in iso) hipCenterIso(iso) else null
                val joints = LEGS.filter { it in iso }
                return if (pivot == null) {
                    translate(iso, delta, listOf(joint) + descendants(joint))
                } else {
                    rotate(iso, joints, pivot, fingerStart, fingerNow)
                }
            }
            else -> Unit
        }
        val subtree = listOf(joint) + descendants(joint).filter { it in iso }
        // Rotate around the nearest joint up the chain that exists (detections may miss some).
        var parent = parentOf(joint)
        while (parent != null && parent !in iso) parent = parentOf(parent)
        val pivot = parent?.let { iso[it] } ?: return translate(iso, delta, subtree)
        return rotate(iso, subtree, pivot, fingerStart, fingerNow)
    }

    /** Rotates [joints] around [pivot] by the angle the finger swept around it. */
    private fun rotate(iso: Map<Joint, Vec2>, joints: Collection<Joint>, pivot: Vec2, fingerStart: Vec2, fingerNow: Vec2): Map<Joint, Vec2> {
        val a = fingerStart - pivot
        val b = fingerNow - pivot
        // Too close to the pivot to define an angle: keep things as they are.
        if (a.length < 1e-4f || b.length < 1e-4f) return iso
        val degrees = (atan2(b.y, b.x) - atan2(a.y, a.x)).toDeg()
        return rotateBy(iso, joints, pivot, degrees)
    }

    private fun rotateBy(iso: Map<Joint, Vec2>, joints: Collection<Joint>, pivot: Vec2, degrees: Float): Map<Joint, Vec2> {
        val set = joints.toSet()
        return iso.mapValues { (j, p) -> if (j in set) pivot + (p - pivot).rotated(degrees) else p }
    }

    private fun translate(iso: Map<Joint, Vec2>, delta: Vec2, joints: Collection<Joint>? = null): Map<Joint, Vec2> {
        val set = joints?.toSet()
        return iso.mapValues { (j, p) -> if (set == null || j in set) p + delta else p }
    }

    // ---- whole-figure operations ---------------------------------------------------------------------------

    /**
     * Moves the whole figure by [delta] (frame-normalised), limited so at least [keepVisible] of the frame still
     * overlaps the figure's bounding box (it can never be lost off-screen).
     */
    fun translateClamped(skeleton: Skeleton, delta: Vec2, keepVisible: Float = 0.08f): Skeleton {
        val box = Box.around(skeleton.points.values) ?: return skeleton
        // Lower bound <= upper bound always holds because box.left <= box.right.
        val dx = delta.x.coerceIn(keepVisible - box.right, 1f - keepVisible - box.left)
        val dy = delta.y.coerceIn(keepVisible - box.bottom, 1f - keepVisible - box.top)
        return skeleton.mapPoints { Vec2(it.x + dx, it.y + dy) }
    }

    /** Mirrors the figure left/right in place (around its own centre) with anatomically correct labels. */
    fun mirrorInPlace(skeleton: Skeleton): Skeleton {
        val box = Box.around(skeleton.points.values) ?: return skeleton
        val cx = box.center.x
        val m = skeleton.mirrored()
        val shift = 2f * cx - 1f
        return m.mapPoints { Vec2(it.x + shift, it.y) }
    }

    /**
     * A relaxed standing mannequin authored with the pose DSL.
     * @param unit body unit (frame heights) to match other figures; 0 = default size
     * @param bottom where the feet stand (0..1 of the frame height)
     */
    fun neutralFigure(aspect: Float, cx: Float = 0.5f, bottom: Float = 0.94f, unit: Float = 0f): Skeleton {
        val bodyHeight = if (unit > 0f) (unit * BodyProportions.STANDING_HEIGHT).coerceIn(0.2f, 1.2f) else 0.82f
        return pose("editor_neutral", "Neutral", PoseCategory.CUSTOM) {
            frameAspect = aspect
            figure(cx = cx, bottom = bottom, bodyHeight = bodyHeight) {
                leftArm(upper = 14f, fore = 8f)
                rightArm(upper = -14f, fore = -8f)
                leftLeg(thigh = 4f, shin = 2f)
                rightLeg(thigh = -4f, shin = -2f)
            }
        }.figures.first()
    }

    /**
     * Where to put an extra person: the widest horizontal gap between existing figures (frame-normalised x), and
     * their common floor line.
     */
    fun placementForNewFigure(figures: List<Skeleton>, aspect: Float): Skeleton {
        val reference = figures.firstOrNull()
        val unit = reference?.estimatedUnit(aspect) ?: 0f
        val centres = figures.mapNotNull { f -> Box.around(f.points.values)?.center?.x }.sorted()
        val edges = listOf(0.08f) + centres + listOf(0.92f)
        var bestX = 0.5f
        var bestGap = -1f
        for (i in 0 until edges.size - 1) {
            val gap = edges[i + 1] - edges[i]
            if (gap > bestGap) {
                bestGap = gap
                bestX = (edges[i] + edges[i + 1]) / 2f
            }
        }
        val floor = figures
            .mapNotNull { f -> listOfNotNull(f[Joint.LEFT_ANKLE], f[Joint.RIGHT_ANKLE], f[Joint.LEFT_FOOT], f[Joint.RIGHT_FOOT]).maxOfOrNull { it.y } }
            .maxOrNull()
            ?.let { it + 0.01f }
            ?.coerceIn(0.5f, 1.1f)
            ?: 0.94f
        return neutralFigure(aspect, cx = bestX.coerceIn(0.1f, 0.9f), bottom = floor, unit = unit)
    }

    // ---- helpers -------------------------------------------------------------------------------------------

    private fun fromIsotropic(original: Skeleton, iso: Map<Joint, Vec2>, aspect: Float): Skeleton =
        original.copy(points = iso.mapValues { (_, p) -> Vec2(p.x / aspect, p.y) })

    private fun neckIso(iso: Map<Joint, Vec2>): Vec2? {
        val l = iso[Joint.LEFT_SHOULDER]
        val r = iso[Joint.RIGHT_SHOULDER]
        return when {
            l != null && r != null -> Vec2.mid(l, r)
            else -> l ?: r
        }
    }

    private fun hipCenterIso(iso: Map<Joint, Vec2>): Vec2? {
        val l = iso[Joint.LEFT_HIP]
        val r = iso[Joint.RIGHT_HIP]
        return when {
            l != null && r != null -> Vec2.mid(l, r)
            else -> l ?: r
        }
    }

    fun distanceToSegment(p: Vec2, a: Vec2, b: Vec2): Float {
        val ab = b - a
        val len2 = ab.dot(ab)
        if (len2 < 1e-12f) return p.distanceTo(a)
        val t = ((p - a).dot(ab) / len2).coerceIn(0f, 1f)
        val proj = a + ab * t
        val dx = p.x - proj.x
        val dy = p.y - proj.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun pointInPolygon(p: Vec2, poly: List<Vec2>): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[j]
            if ((a.y > p.y) != (b.y > p.y)) {
                val x = (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x
                if (p.x < x) inside = !inside
            }
            j = i
        }
        return inside
    }

    /** Largest change of any bone length between two skeletons (isotropic), for tests / sanity checks. */
    internal fun maxBoneLengthChange(a: Skeleton, b: Skeleton, aspect: Float): Float {
        val ia = a.toIsotropic(aspect)
        val ib = b.toIsotropic(aspect)
        var worst = 0f
        for (bone in Bone.entries) {
            val la = ia[bone.from]?.let { f -> ia[bone.to]?.distanceTo(f) } ?: continue
            val lb = ib[bone.from]?.let { f -> ib[bone.to]?.distanceTo(f) } ?: continue
            worst = max(worst, abs(la - lb))
        }
        return worst
    }
}
