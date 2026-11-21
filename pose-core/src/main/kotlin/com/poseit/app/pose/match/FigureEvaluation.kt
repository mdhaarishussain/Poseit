package com.poseit.app.pose.match

import com.poseit.app.pose.BodyPart
import com.poseit.app.pose.BodyProportions
import com.poseit.app.pose.Joint
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.angleDiff
import com.poseit.app.pose.limbAngle
import java.util.EnumMap
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/** A template segment that must be matched, with the template joints usable for each endpoint. */
internal class RequiredSegment(val segment: Segment, val from: List<Joint>, val to: List<Joint>, val angle: Float)

/** Pre-computed facts about one template figure (scene space). */
internal class TemplateInfo(val index: Int, val skeleton: Skeleton, val aspect: Float, private val minVisibility: Float) {

    /** Body unit (torso length) in scene heights. */
    val unit: Float = skeleton.estimatedUnit(aspect).takeIf { it.isFinite() && it > 1e-4f } ?: 0.15f

    /** Reference size for normalising framing offsets: the standing height, capped to the scene height. */
    val refHeight: Float = (unit * BodyProportions.STANDING_HEIGHT).coerceIn(0.15f, 1f)

    fun iso(j: Joint): Vec2? = skeleton[j]?.let { Vec2(it.x * aspect, it.y) }

    /**
     * Present, confidently visible and inside the scene. No margin here: a template joint just outside the frame
     * (e.g. the phone hand of a selfie) can't be seen in the image, so MediaPipe reports it as not visible and
     * requiring it would cap the score with an unfixable "keep your hand visible" hint.
     */
    fun usable(j: Joint): Boolean {
        val p = skeleton[j] ?: return false
        return skeleton.isVisible(j, minVisibility) && MatchGeometry.inScene(p, margin = 0f)
    }

    val required: Map<Segment, RequiredSegment>

    /** Limb angles of segments the template has joints for but that are not required (e.g. legs of a portrait). */
    val optional: Map<Segment, Float>

    init {
        val req = EnumMap<Segment, RequiredSegment>(Segment::class.java)
        val opt = EnumMap<Segment, Float>(Segment::class.java)
        for (seg in Segment.entries) {
            val from = seg.fromJoints.filter(::usable)
            val to = seg.toJoints.filter(::usable)
            val a = MatchGeometry.mean(from.mapNotNull(::iso))
            val b = MatchGeometry.mean(to.mapNotNull(::iso))
            if (a != null && b != null) {
                val minFraction = if (seg.isLine) MatchGeometry.MIN_LINE_FRACTION else MatchGeometry.MIN_LIMB_FRACTION
                if (a.distanceTo(b) >= minFraction * seg.normalLength * unit) {
                    req[seg] = RequiredSegment(seg, from, to, limbAngle(a, b))
                    continue
                }
            }
            // Not required: still report the template's angle if it has the joints at all.
            val ra = MatchGeometry.mean(seg.fromJoints.mapNotNull(::iso))
            val rb = MatchGeometry.mean(seg.toJoints.mapNotNull(::iso))
            if (ra != null && rb != null) opt[seg] = limbAngle(ra, rb)
        }
        required = req
        optional = opt
    }

    val requiredWeight: Float = required.keys.sumOf { it.weight.toDouble() }.toFloat()

    /** Rough isotropic centre (torso, else all points) used for person assignment and numbering. */
    val centre: Vec2 = roughCentre(skeleton, aspect) { true }

    val facing: Facing = MatchGeometry.facingOf({ j -> if (skeleton.isVisible(j, minVisibility)) iso(j) else null }, unit)

    /** The match of this figure when no detected person is assigned to it. */
    fun unassignedMatch(): FigureMatch = FigureMatch(
        score = 0f,
        segments = Segment.entries.mapNotNull { seg ->
            required[seg]?.let { SegmentMatch(seg, it.angle, null, 0f, 0f, required = true) }
                ?: optional[seg]?.let { SegmentMatch(seg, it, null, 0f, 0f, required = false) }
        },
        partScores = emptyMap(),
        coverage = 0f,
        detectedIndex = null,
    )
}

/** Pre-computed facts about one detected person (scene space). */
internal class DetectionInfo(val originalIndex: Int, val skeleton: Skeleton, val aspect: Float, private val minVisibility: Float) {
    fun iso(j: Joint): Vec2? = skeleton[j]?.let { Vec2(it.x * aspect, it.y) }

    /** Confidently detected, wherever it is. */
    fun visible(j: Joint): Boolean = skeleton.isVisible(j, minVisibility)

    /** Confidently detected and inside the scene. */
    fun usable(j: Joint): Boolean {
        val p = skeleton[j] ?: return false
        return visible(j) && MatchGeometry.inScene(p)
    }

    val hasUsableJoint: Boolean = Joint.entries.any(::usable)

    val centre: Vec2 = roughCentre(skeleton, aspect, ::visible)
}

private val TORSO_JOINTS = listOf(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER, Joint.LEFT_HIP, Joint.RIGHT_HIP)

private fun roughCentre(s: Skeleton, aspect: Float, accept: (Joint) -> Boolean): Vec2 {
    fun isoOf(joints: Collection<Joint>) = joints.filter(accept).mapNotNull { j -> s[j]?.let { Vec2(it.x * aspect, it.y) } }
    return MatchGeometry.mean(isoOf(TORSO_JOINTS))
        ?: MatchGeometry.mean(isoOf(s.points.keys))
        ?: MatchGeometry.mean(s.points.values.map { Vec2(it.x * aspect, it.y) })
        ?: Vec2(0.5f * aspect, 0.5f)
}

/** Isotropic endpoints of a matched segment in the template (t*) and in the detection (a*). */
internal class SegmentGeometry(val tFrom: Vec2, val tTo: Vec2, val aFrom: Vec2, val aTo: Vec2)

/**
 * Framing of a detected person relative to the template figure.
 * @param nx horizontal offset of the detection (+ = too far toward the image right), in template body heights
 * @param ny vertical offset (+ = too low in the image), in template body heights
 * @param sizeRatio detected size / template size (1 = same size)
 * @param sizeKnown false when no segment was available to compare sizes
 * @param score 0..1 framing quality
 */
internal class Framing(val nx: Float, val ny: Float, val sizeRatio: Float, val sizeKnown: Boolean, val score: Float)

/**
 * Compares one template figure with one detected person. See [PoseMatcher.match] for the scoring formula.
 */
internal class PairEval(val template: TemplateInfo, val detection: DetectionInfo, cfg: MatchConfig) {

    val geometry: Map<Segment, SegmentGeometry>
    val segments: List<SegmentMatch>

    /** Template joints of required segments that the detection does not show (hidden or out of frame). */
    val missingJoints: Set<Joint>

    val sizeRatio: Float

    /** Body unit of the detected person, in scene heights. */
    val detUnit: Float

    /** Weighted mean of the visible required segment scores. */
    val shape: Float

    /** 1 − 0.5 · worst weighted segment badness: one badly wrong limb can't hide behind many good segments. */
    val worstFactor: Float
    val coverage: Float
    val coverageFactor: Float

    /** Penalty when the template needs the head or the feet and the person's are missing (ruined photo). */
    val criticalFactor: Float
    val framing: Framing?
    val score: Float

    /** Pose similarity without framing: shape · worstFactor · coverageFactor (used for person assignment). */
    val poseQuality: Float
    val partScores: Map<BodyPart, Float>

    /** Facing of the detected person (UNKNOWN when ambiguous). */
    val detectedFacing: Facing

    /** Facing used to translate image directions: detection, else template, else front. */
    val facing: Facing

    /** For a detected profile view: the image direction (+1 right / -1 left) the person faces, else 0. */
    val profileDirection: Int

    init {
        val t = template
        val d = detection
        val geo = EnumMap<Segment, SegmentGeometry>(Segment::class.java)
        val missing = mutableSetOf<Joint>()

        // 1. Geometry of every required segment, using the joints usable in BOTH poses.
        for ((seg, rs) in t.required) {
            val from = rs.from.filter(d::usable)
            val to = rs.to.filter(d::usable)
            if (from.isEmpty()) missing += rs.from
            if (to.isEmpty()) missing += rs.to
            if (from.isEmpty() || to.isEmpty()) continue
            val tFrom = MatchGeometry.mean(from.mapNotNull(t::iso)) ?: continue
            val tTo = MatchGeometry.mean(to.mapNotNull(t::iso)) ?: continue
            val aFrom = MatchGeometry.mean(from.mapNotNull(d::iso)) ?: continue
            val aTo = MatchGeometry.mean(to.mapNotNull(d::iso)) ?: continue
            geo[seg] = SegmentGeometry(tFrom, tTo, aFrom, aTo)
        }
        geometry = geo
        missingJoints = missing

        // 2. Relative size from segment lengths (robust median; foreshortened outliers don't dominate).
        fun ratios(filter: (Segment) -> Boolean) = geo.filterKeys(filter).values.mapNotNull { g ->
            val tl = g.tFrom.distanceTo(g.tTo)
            if (tl > 1e-5f) g.aFrom.distanceTo(g.aTo) / tl else null
        }
        val sizeRatios = ratios { it.isSizeReference }.ifEmpty { ratios { true } }
        val sizeKnown = sizeRatios.isNotEmpty()
        sizeRatio = (MatchGeometry.median(sizeRatios) ?: 1f).coerceIn(0.05f, 20f)
        detUnit = t.unit * sizeRatio

        // 3. Per-segment scores.
        val matches = mutableListOf<SegmentMatch>()
        var visibleWeight = 0f
        var weightedScore = 0f
        var worst = 0f
        val partWeights = EnumMap<BodyPart, Float>(BodyPart::class.java)
        val partSums = EnumMap<BodyPart, Float>(BodyPart::class.java)
        for (seg in Segment.entries) {
            val rs = t.required[seg]
            if (rs == null) {
                t.optional[seg]?.let { matches += SegmentMatch(seg, it, null, 0f, 0f, required = false) }
                continue
            }
            val g = geo[seg]
            if (g == null) {
                matches += SegmentMatch(seg, rs.angle, null, 0f, 0f, required = true)
                continue
            }
            val tAngle = limbAngle(g.tFrom, g.tTo)
            val aAngle = limbAngle(g.aFrom, g.aTo)
            val error = angleDiff(aAngle, tAngle)
            var s = MatchGeometry.segmentScore(abs(error), cfg.perfectWithinDeg, cfg.zeroAtDeg)
            if (seg == Segment.HEAD) {
                // Chin up/down barely changes the head's direction but moves the nose along it.
                val lenT = g.tFrom.distanceTo(g.tTo) / t.unit
                val lenA = g.aFrom.distanceTo(g.aTo) / detUnit
                s = minOf(s, 1f - MatchGeometry.smoothstep(HEAD_LENGTH_OK, HEAD_LENGTH_ZERO, abs(lenA - lenT)))
            }
            matches += SegmentMatch(seg, tAngle, aAngle, error, s, required = true)
            worst = maxOf(worst, (seg.weight / Segment.TORSO.weight).coerceAtMost(1f) * (1f - s))
            visibleWeight += seg.weight
            weightedScore += seg.weight * s
            partWeights[seg.part] = (partWeights[seg.part] ?: 0f) + seg.weight
            partSums[seg.part] = (partSums[seg.part] ?: 0f) + seg.weight * s
        }
        segments = matches
        shape = if (visibleWeight > 0f) weightedScore / visibleWeight else 0f
        worstFactor = 1f - WORST_SEGMENT_PENALTY * worst
        coverage = if (t.requiredWeight > 0f) (visibleWeight / t.requiredWeight).coerceIn(0f, 1f) else 0f
        coverageFactor = ((coverage - 0.4f) / 0.5f).coerceIn(0f, 1f).pow(0.7f)
        criticalFactor = (if (Joint.NOSE in missing) MISSING_HEAD_FACTOR else 1f) *
            (if (Joint.LEFT_ANKLE in missing || Joint.RIGHT_ANKLE in missing) MISSING_FEET_FACTOR else 1f)
        partScores = partWeights.mapValues { (part, w) -> (partSums[part] ?: 0f) / w }
        poseQuality = shape * worstFactor * coverageFactor

        // 4. Framing: anchor on the torso joints both poses show, compare position and size.
        framing = computeFraming(sizeKnown)
        val framingScore = framing?.score ?: 1f
        score = (poseQuality * criticalFactor * (0.75f + 0.25f * framingScore)).coerceIn(0f, 1f)

        // 5. Facing, for turning image directions into the subject's own left/right.
        detectedFacing = MatchGeometry.facingOf({ j -> if (d.visible(j)) d.iso(j) else null }, detUnit)
        facing = when {
            detectedFacing != Facing.UNKNOWN -> detectedFacing
            t.facing != Facing.UNKNOWN -> t.facing
            else -> Facing.FRONT
        }
        profileDirection = computeProfileDirection()
    }

    private fun computeFraming(sizeKnown: Boolean): Framing? {
        val t = template
        val d = detection
        val common = Joint.entries.filter { t.usable(it) && d.usable(it) }.toSet()
        val anchorJoints = when {
            common.containsAll(TORSO_JOINTS) -> TORSO_JOINTS
            Joint.LEFT_SHOULDER in common && Joint.RIGHT_SHOULDER in common -> listOf(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)
            Joint.LEFT_HIP in common && Joint.RIGHT_HIP in common -> listOf(Joint.LEFT_HIP, Joint.RIGHT_HIP)
            else -> common.toList()
        }
        val tAnchor = MatchGeometry.mean(anchorJoints.mapNotNull(t::iso)) ?: return null
        val dAnchor = MatchGeometry.mean(anchorJoints.mapNotNull(d::iso)) ?: return null
        val nx = (dAnchor.x - tAnchor.x) / t.refHeight
        val ny = (dAnchor.y - tAnchor.y) / t.refHeight
        val offsetScore = 1f - MatchGeometry.smoothstep(OFFSET_OK, OFFSET_ZERO, sqrt(nx * nx + ny * ny))
        val sizeScore = if (sizeKnown) 1f - MatchGeometry.smoothstep(SIZE_LOG_OK, SIZE_LOG_ZERO, abs(ln(sizeRatio))) else 1f
        return Framing(nx, ny, sizeRatio, sizeKnown, offsetScore * sizeScore)
    }

    private fun computeProfileDirection(): Int {
        if (detectedFacing != Facing.UNKNOWN) return 0
        val d = detection
        if (!d.visible(Joint.LEFT_SHOULDER) || !d.visible(Joint.RIGHT_SHOULDER) || !d.visible(Joint.NOSE)) return 0
        val ls = d.iso(Joint.LEFT_SHOULDER) ?: return 0
        val rs = d.iso(Joint.RIGHT_SHOULDER) ?: return 0
        val nose = d.iso(Joint.NOSE) ?: return 0
        val dx = nose.x - (ls.x + rs.x) / 2f
        return when {
            dx > 0.05f * detUnit -> 1
            dx < -0.05f * detUnit -> -1
            else -> 0
        }
    }

    fun toFigureMatch(): FigureMatch = FigureMatch(
        score = score,
        segments = segments,
        partScores = partScores,
        coverage = coverage,
        detectedIndex = detection.originalIndex,
    )

    private companion object {
        /** How strongly the single worst segment pulls the score down (0 = plain weighted mean). */
        const val WORST_SEGMENT_PENALTY = 0.5f

        /** Score multipliers when the template needs the head / the feet but they are missing. */
        const val MISSING_HEAD_FACTOR = 0.7f
        const val MISSING_FEET_FACTOR = 0.8f

        /** Head length difference (body units) still considered perfect / scoring zero. */
        const val HEAD_LENGTH_OK = 0.10f
        const val HEAD_LENGTH_ZERO = 0.35f

        /** Centre offset (template body heights) still considered perfect / scoring zero. */
        const val OFFSET_OK = 0.06f
        const val OFFSET_ZERO = 0.5f

        /** |ln(size ratio)| still considered perfect (6 %) / scoring zero (60 %). */
        val SIZE_LOG_OK = ln(1.06f)
        val SIZE_LOG_ZERO = ln(1.6f)
    }
}
