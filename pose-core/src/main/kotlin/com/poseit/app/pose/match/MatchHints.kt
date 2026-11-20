package com.poseit.app.pose.match

import com.poseit.app.pose.BodyPart
import com.poseit.app.pose.Joint
import com.poseit.app.pose.interiorAngle
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

/** Wording shared by every coaching message. */
internal object Phrasing {
    private val YOUR = Regex("\\b[Yy]our\\b")
    private val YOU = Regex("\\b[Yy]ou\\b")

    /**
     * Produces both phrasings from ONE subject-facing imperative: the subject hears it as is ("Raise your left arm"),
     * the photographer is asked to relay it ("Ask them to raise their left arm").
     */
    fun instruct(imperative: String, audience: FramingAudience): String = when (audience) {
        FramingAudience.SUBJECT -> imperative
        FramingAudience.PHOTOGRAPHER -> "Ask them to " + thirdPerson(imperative)
    }

    fun thirdPerson(imperative: String): String =
        imperative.replaceFirstChar { it.lowercaseChar() }.replace(YOUR, "their").replace(YOU, "them")

    /** Text for the voice coach: dashes and colons become pauses. */
    fun speech(text: String): String = text.replace(" – ", ", ").replace(": ", ", ")

    fun side(left: Boolean): String = if (left) "left" else "right"

    /** The subject's own side that lies toward the image right ([towardImageRight]) or the image left. */
    fun subjectSide(towardImageRight: Boolean, facing: Facing): String {
        val subjectLeftIsImageRight = facing != Facing.BACK
        return side(towardImageRight == subjectLeftIsImageRight)
    }
}

/** Builds the prioritised coaching hints for one [PoseMatcher.match] call. */
internal class HintBuilder(private val audience: FramingAudience, private val templates: List<TemplateInfo>) {

    private val multi = templates.size > 1

    /** 1-based person numbers, assigned left to right in the template. */
    private val numbers: IntArray = IntArray(templates.size).also { arr ->
        templates.indices.sortedBy { templates[it].centre.x }.forEachIndexed { rank, idx -> arr[idx] = rank + 1 }
    }

    private fun prefix(figure: Int): String = if (multi) "Person ${numbers[figure]}: " else ""

    private fun hint(text: String, kind: HintKind, severity: Float, part: BodyPart? = null, figure: Int? = null) = CoachHint(
        text = text,
        kind = kind,
        severity = severity.coerceIn(0f, 1f),
        part = part,
        figureIndex = figure,
        speech = Phrasing.speech(text),
    )

    fun noPerson(): CoachHint = hint(
        when (audience) {
            FramingAudience.SUBJECT -> "Step into the frame"
            FramingAudience.PHOTOGRAPHER -> if (multi) "Point the camera at the people" else "Point the camera at the person"
        },
        HintKind.NO_PERSON,
        1f,
    )

    fun noPose(): CoachHint = hint("Pick a pose to get started", HintKind.POSE, 0f)

    fun perfect(): CoachHint = hint("Perfect – hold it!", HintKind.PERFECT, 0f)

    private fun almostThere(): CoachHint = hint(
        when (audience) {
            FramingAudience.SUBJECT -> "Almost there – match the outline closely"
            FramingAudience.PHOTOGRAPHER -> "Almost there – help them match the outline"
        },
        HintKind.POSE,
        0.1f,
    )

    /**
     * @param assigned per template figure, its evaluation against the assigned person (null = nobody assigned)
     * @return at most [MAX_HINTS] hints, most severe first, never empty
     */
    fun build(assigned: List<PairEval?>): List<CoachHint> {
        val all = mutableListOf<CoachHint>()
        val evals = assigned.filterNotNull()
        if (evals.size < templates.size) all += missingPeople(templates.size, evals.size)
        val steppedBack = mutableSetOf<Int>()
        for (e in evals) {
            val vis = visibility(e)
            if (vis.any { it.second }) steppedBack += e.template.index
            all += vis.map { it.first }
            all += pose(e)
        }
        all += framing(evals, steppedBack)
        return all.sortedByDescending { it.severity }
            .distinctBy { it.text }
            .take(MAX_HINTS)
            .ifEmpty { listOf(almostThere()) }
    }

    private fun missingPeople(expected: Int, present: Int): CoachHint {
        val text = when (audience) {
            FramingAudience.SUBJECT -> "Waiting for $expected people – $present in frame"
            FramingAudience.PHOTOGRAPHER ->
                (if (expected == 2) "Get both people in the frame" else "Get all $expected people in the frame") + " – $present visible"
        }
        return hint(text, HintKind.MISSING_PERSON, 0.97f)
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Visibility
    // ---------------------------------------------------------------------------------------------------------------

    /** Hints about required joints the camera doesn't see; the flag marks "cut off – step back" style hints. */
    private fun visibility(e: PairEval): List<Pair<CoachHint, Boolean>> {
        val missing = e.missingJoints
        if (missing.isEmpty()) return emptyList()
        val f = e.template.index
        val p = prefix(f)
        val subject = audience == FramingAudience.SUBJECT
        fun cutOff(joints: Collection<Joint>) = joints.any { j ->
            val pt = e.detection.skeleton[j]
            pt == null || !MatchGeometry.inScene(pt)
        }
        val out = mutableListOf<Pair<CoachHint, Boolean>>()

        if (Joint.NOSE in missing) {
            val cut = cutOff(listOf(Joint.NOSE))
            val nose = e.detection.skeleton[Joint.NOSE]
            val text = when {
                !cut -> Phrasing.instruct("Turn your face toward the camera", audience)
                subject -> "Your head is out of frame"
                nose == null -> "Their head is cut off – step back or tilt up"
                nose.y < 0f -> "Their head is cut off – tilt the phone up"
                // Off the image left: bring it toward the image right by moving the phone LEFT (and vice versa).
                nose.x < 0f -> "Their head is cut off – move the phone left"
                nose.x > 1f -> "Their head is cut off – move the phone right"
                else -> "Their head is cut off – tilt the phone down"
            }
            out += hint(p + text, HintKind.VISIBILITY, if (cut) 0.8f else 0.5f, BodyPart.HEAD, f) to cut
        }

        val legMissing = missing.filter { it in LEG_JOINTS }
        if (legMissing.isNotEmpty()) {
            val feet = legMissing.any { it == Joint.LEFT_ANKLE || it == Joint.RIGHT_ANKLE }
            val cut = cutOff(legMissing)
            val text = when {
                !cut -> Phrasing.instruct(if (feet) "Keep your feet visible" else "Keep your legs visible", audience)
                feet && subject -> "Your feet are cut off – step back"
                feet -> "Feet are cut off – step back or tilt down"
                subject -> "Your legs are out of frame – step back"
                else -> "Legs are cut off – step back or tilt down"
            }
            val part = when {
                legMissing.all { it.isLeft == true } -> BodyPart.LEFT_LEG
                legMissing.all { it.isLeft == false } -> BodyPart.RIGHT_LEG
                else -> null
            }
            val severity = if (!cut) 0.45f else if (feet) 0.62f else 0.66f
            out += hint(p + text, HintKind.VISIBILITY, severity, part, f) to cut
        }

        for (left in listOf(true, false)) {
            val wrist = if (left) Joint.LEFT_WRIST else Joint.RIGHT_WRIST
            val armMissing = missing.filter { it in ARM_JOINTS && it.isLeft == left }
            if (armMissing.isEmpty()) continue
            val side = Phrasing.side(left)
            val limb = if (armMissing == listOf(wrist)) "hand" else "arm"
            val cut = cutOff(armMissing)
            val text = when {
                !cut -> Phrasing.instruct("Keep your $side $limb visible", audience)
                subject -> "Your $side $limb is out of frame"
                else -> "Their $side $limb is cut off"
            }
            val part = if (left) BodyPart.LEFT_ARM else BodyPart.RIGHT_ARM
            out += hint(p + text, HintKind.VISIBILITY, if (cut) 0.5f else 0.4f, part, f) to false
        }
        return out
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Pose
    // ---------------------------------------------------------------------------------------------------------------

    private fun pose(e: PairEval): List<CoachHint> {
        val f = e.template.index
        val p = prefix(f)
        val out = mutableListOf<CoachHint>()

        val templateFacing = e.template.facing
        val facingMismatch = templateFacing != Facing.UNKNOWN && e.detectedFacing != Facing.UNKNOWN &&
            templateFacing != e.detectedFacing
        if (facingMismatch) {
            val imperative = if (templateFacing == Facing.FRONT) "Turn around to face the camera" else "Turn your back to the camera"
            out += hint(p + Phrasing.instruct(imperative, audience), HintKind.POSE, 0.95f, BodyPart.TORSO, f)
        }

        val candidates = e.segments
            .filter { it.required && it.actualAngle != null && it.score < POSE_HINT_BELOW }
            .filterNot { facingMismatch && it.segment.isLine }
            .map { it to poseSeverity(it) }
            .sortedByDescending { it.second }
        val usedParts = mutableSetOf<BodyPart>()
        for ((m, severity) in candidates) {
            if (m.segment.part in usedParts) continue
            val imperative = poseInstruction(e, m) ?: continue
            usedParts += m.segment.part
            out += hint(p + Phrasing.instruct(imperative, audience), HintKind.POSE, severity, m.segment.part, f)
            if (usedParts.size >= MAX_HINTS) break
        }
        return out
    }

    /** Weighted badness of a segment: big, central segments first; the head only when nothing else is off. */
    private fun poseSeverity(m: SegmentMatch): Float {
        val weight = (m.segment.weight / Segment.TORSO.weight).coerceAtMost(1f)
        val headFactor = if (m.segment == Segment.HEAD) 0.6f else 1f
        return (1f - m.score) * (0.6f + 0.4f * weight) * headFactor
    }

    /**
     * Turns the worst segments into a concrete, anatomical instruction (subject phrasing). Positions are compared in
     * BODY-NORMALISED coordinates: each segment's end relative to its start, divided by the person's body unit.
     */
    private fun poseInstruction(e: PairEval, m: SegmentMatch): String? {
        val seg = m.segment
        val g = e.geometry[seg] ?: return null
        val tRel = (g.tTo - g.tFrom) / e.template.unit
        val aRel = (g.aTo - g.aFrom) / e.detUnit
        val dx = aRel.x - tRel.x // + = actual end further toward the image right than wanted
        val dy = aRel.y - tRel.y // + = actual end lower than wanted
        val left = seg.isLeft
        // Image direction that points away from the body for this limb (+1 = image right).
        val leftOutward = if (e.facing == Facing.BACK) -1f else 1f
        val outward = when (left) {
            true -> leftOutward
            false -> -leftOutward
            null -> 0f
        }
        val dOut = dx * outward // + = actual end further from the body than wanted
        val vertical = abs(dy) >= abs(dOut)

        return when (seg) {
            Segment.LEFT_UPPER_ARM, Segment.RIGHT_UPPER_ARM -> {
                val side = Phrasing.side(left == true)
                when {
                    vertical -> if (dy > 0) "Raise your $side arm" else "Lower your $side arm"
                    dOut > 0 -> "Bring your $side arm closer to your body"
                    else -> "Move your $side arm away from your body"
                }
            }
            Segment.LEFT_FOREARM, Segment.RIGHT_FOREARM -> {
                val side = Phrasing.side(left == true)
                val upper = e.geometry[if (left == true) Segment.LEFT_UPPER_ARM else Segment.RIGHT_UPPER_ARM]
                val bend = upper?.let { u ->
                    interiorAngle(u.aFrom, g.aFrom, g.aTo) - interiorAngle(u.tFrom, g.tFrom, g.tTo)
                }
                when {
                    bend != null && bend >= BEND_HINT_DEG -> "Bend your $side elbow more"
                    bend != null && bend <= -BEND_HINT_DEG -> "Straighten your $side arm"
                    vertical -> if (dy > 0) "Raise your $side hand" else "Lower your $side hand"
                    dOut > 0 -> "Bring your $side hand closer to your body"
                    else -> "Move your $side hand away from your body"
                }
            }
            Segment.LEFT_THIGH, Segment.RIGHT_THIGH -> {
                val side = Phrasing.side(left == true)
                when {
                    vertical -> if (dy > 0) "Lift your $side knee" else "Lower your $side knee"
                    dOut > 0 -> "Bring your $side foot in"
                    else -> "Step your $side foot out"
                }
            }
            Segment.LEFT_SHIN, Segment.RIGHT_SHIN -> {
                val side = Phrasing.side(left == true)
                val thigh = e.geometry[if (left == true) Segment.LEFT_THIGH else Segment.RIGHT_THIGH]
                val bend = thigh?.let { th ->
                    interiorAngle(th.aFrom, g.aFrom, g.aTo) - interiorAngle(th.tFrom, g.tFrom, g.tTo)
                }
                when {
                    bend != null && bend >= BEND_HINT_DEG -> "Bend your $side knee more"
                    bend != null && bend <= -BEND_HINT_DEG -> "Straighten your $side leg"
                    vertical -> if (dy > 0) "Lift your $side foot" else "Lower your $side foot"
                    dOut > 0 -> "Bring your $side foot in"
                    else -> "Step your $side foot out"
                }
            }
            Segment.TORSO -> {
                val actual = m.actualAngle ?: return null
                val leanT = MatchGeometry.leanOf(m.templateAngle)
                val leanA = MatchGeometry.leanOf(actual)
                val towardImageRight = leanT - leanA > 0f
                when {
                    abs(leanT) < UPRIGHT_DEG && abs(leanA) >= UPRIGHT_DEG -> "Stand up straight"
                    e.profileDirection != 0 ->
                        if (towardImageRight == (e.profileDirection > 0)) "Lean forward" else "Lean back"
                    else -> "Lean to your ${Phrasing.subjectSide(towardImageRight, e.facing)}"
                }
            }
            Segment.SHOULDER_LINE, Segment.HIP_LINE -> {
                val what = if (seg == Segment.SHOULDER_LINE) "shoulder" else "hip"
                if (aRel.length < MatchGeometry.MIN_LINE_FRACTION * seg.normalLength) {
                    // The template shows the body square-on but the person stands sideways.
                    if (e.template.facing == Facing.BACK) "Turn your back to the camera" else "Turn to face the camera"
                } else {
                    // Lines run right -> left, so rel.y > 0 means the LEFT side is lower.
                    if (tRel.y > aRel.y) "Drop your left $what" else "Drop your right $what"
                }
            }
            Segment.HEAD -> when {
                abs(dx) >= abs(dy) -> "Tilt your head to your ${Phrasing.subjectSide(dx < 0f, e.facing)}"
                dy > 0 -> "Lift your chin"
                else -> "Lower your chin"
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Framing
    // ---------------------------------------------------------------------------------------------------------------

    private fun framing(evals: List<PairEval>, steppedBack: Set<Int>): List<CoachHint> {
        val framed = evals.filter { it.framing != null }
        if (framed.isEmpty()) return emptyList()
        return when (audience) {
            FramingAudience.SUBJECT -> framed.flatMap { subjectFraming(it, it.template.index in steppedBack) }
            FramingAudience.PHOTOGRAPHER -> photographerFraming(framed, steppedBack)
        }
    }

    private fun subjectFraming(e: PairEval, alreadySteppingBack: Boolean): List<CoachHint> {
        val fr = e.framing ?: return emptyList()
        val f = e.template.index
        val p = prefix(f)
        val out = mutableListOf<CoachHint>()
        if (abs(fr.nx) > OFFSET_HINT) {
            val side = Phrasing.subjectSide(towardImageRight = fr.nx < 0f, facing = e.facing)
            out += hint(p + "Step to your $side", HintKind.FRAMING, offsetSeverity(fr.nx), figure = f)
        }
        if (fr.sizeKnown) {
            if (fr.sizeRatio < TOO_SMALL) {
                out += hint(p + "Step closer", HintKind.FRAMING, sizeSeverity(fr.sizeRatio), figure = f)
            } else if (fr.sizeRatio > TOO_BIG && !alreadySteppingBack) {
                out += hint(p + "Step back", HintKind.FRAMING, sizeSeverity(fr.sizeRatio), figure = f)
            }
        }
        return out
    }

    private fun photographerFraming(evals: List<PairEval>, steppedBack: Set<Int>): List<CoachHint> {
        val out = mutableListOf<CoachHint>()
        val frames = evals.mapNotNull { it.framing }
        val nx = frames.map { it.nx }.average().toFloat()
        val ny = frames.map { it.ny }.average().toFloat()
        // To move the subject toward the image right, the phone moves LEFT (and vice versa).
        if (abs(nx) > OFFSET_HINT) {
            out += hint(if (nx < 0f) "Move the phone left" else "Move the phone right", HintKind.FRAMING, offsetSeverity(nx))
        }
        // Subject too low in the picture -> tilt down (content moves up), and vice versa.
        if (abs(ny) > OFFSET_HINT) {
            out += hint(if (ny > 0f) "Tilt the phone down" else "Tilt the phone up", HintKind.FRAMING, offsetSeverity(ny))
        }
        val sized = frames.filter { it.sizeKnown }
        if (sized.isNotEmpty()) {
            val ratio = exp(sized.map { ln(it.sizeRatio.toDouble()) }.average()).toFloat()
            if (ratio < TOO_SMALL) {
                out += hint("Move closer or zoom in", HintKind.FRAMING, sizeSeverity(ratio))
            } else if (ratio > TOO_BIG && steppedBack.isEmpty()) {
                out += hint("Step back or zoom out", HintKind.FRAMING, sizeSeverity(ratio))
            }
        }
        if (evals.size > 1) {
            for (e in evals) {
                val fr = e.framing ?: continue
                val residual = fr.nx - nx
                if (abs(residual) <= RESIDUAL_HINT) continue
                val side = Phrasing.subjectSide(towardImageRight = residual < 0f, facing = e.facing)
                val text = prefix(e.template.index) + Phrasing.instruct("Step to your $side", audience)
                out += hint(text, HintKind.FRAMING, offsetSeverity(residual) - 0.05f, figure = e.template.index)
            }
        }
        if (steppedBack.isEmpty() && evals.any(::cropsAtJoint)) {
            out += hint("Don't crop at the joints – show the feet or frame above the knees", HintKind.FRAMING, 0.3f)
        }
        return out
    }

    /** True when the bottom edge of the picture runs through a knee or ankle the template doesn't put there. */
    private fun cropsAtJoint(e: PairEval): Boolean = CROP_JOINTS.any { j ->
        val pt = e.detection.skeleton[j] ?: return@any false
        if (pt.y !in CROP_BAND || pt.x < -MatchGeometry.SCENE_MARGIN || pt.x > 1f + MatchGeometry.SCENE_MARGIN) return@any false
        val intended = e.template.skeleton[j]?.let { it.y in INTENDED_CROP_BAND } ?: false
        !intended
    }

    private fun offsetSeverity(offset: Float) = 0.25f + 0.35f * MatchGeometry.smoothstep(OFFSET_HINT, 0.5f, abs(offset))

    private fun sizeSeverity(ratio: Float): Float {
        val lr = abs(ln(ratio))
        return 0.25f + 0.35f * MatchGeometry.smoothstep(ln(TOO_BIG), ln(1.8f), lr)
    }

    companion object {
        const val MAX_HINTS = 3

        /** Segments scoring below this produce a pose hint (≈ 20° off with the default config). */
        const val POSE_HINT_BELOW = 0.9f

        /** Elbow/knee interior-angle difference that triggers a bend/straighten hint. */
        const val BEND_HINT_DEG = 20f

        /** Torso lean below which a figure counts as upright. */
        const val UPRIGHT_DEG = 8f

        /** Centre offset (template body heights) that triggers a framing hint. */
        const val OFFSET_HINT = 0.12f

        /** Offset of one person relative to the group that triggers a per-person hint. */
        const val RESIDUAL_HINT = 0.15f

        const val TOO_SMALL = 0.85f
        const val TOO_BIG = 1.18f

        private val CROP_BAND = 0.955f..1.045f
        private val INTENDED_CROP_BAND = 0.93f..1.07f

        private val LEG_JOINTS = setOf(
            Joint.LEFT_HIP, Joint.RIGHT_HIP, Joint.LEFT_KNEE, Joint.RIGHT_KNEE, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE,
        )
        private val ARM_JOINTS = setOf(
            Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER, Joint.LEFT_ELBOW, Joint.RIGHT_ELBOW, Joint.LEFT_WRIST, Joint.RIGHT_WRIST,
        )
        private val CROP_JOINTS = listOf(Joint.LEFT_KNEE, Joint.RIGHT_KNEE, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE)
    }
}
