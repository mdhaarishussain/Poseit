package com.poseit.app.pose.match

import com.poseit.app.pose.BodyPart
import com.poseit.app.pose.Skeleton

/** Who should act on framing hints ("step left" vs "move the phone left"). */
enum class FramingAudience { SUBJECT, PHOTOGRAPHER }

data class MatchConfig(
    /** Joints below this visibility are treated as not seen. */
    val minVisibility: Float = 0.5f,
    /** Segment angle error (degrees) that still counts as perfect. */
    val perfectWithinDeg: Float = 12f,
    /** Segment angle error (degrees) at which a segment scores zero. */
    val zeroAtDeg: Float = 55f,
    /** Overall score at/above which the pose counts as matched (the user's strictness setting) -> PERFECT hint. */
    val perfectScore: Float = 0.80f,
)

/**
 * A comparable body segment. Some are "virtual" (built from midpoints), e.g. [TORSO] = mid-hip -> mid-shoulder,
 * [HEAD] = mid-shoulder -> nose. [weight] is the importance in the overall score.
 */
enum class Segment(val label: String, val part: BodyPart, val weight: Float) {
    TORSO("torso", BodyPart.TORSO, 1.6f),
    HEAD("head", BodyPart.HEAD, 0.6f),
    SHOULDER_LINE("shoulders", BodyPart.TORSO, 0.6f),
    HIP_LINE("hips", BodyPart.TORSO, 0.5f),
    LEFT_UPPER_ARM("left upper arm", BodyPart.LEFT_ARM, 1.2f),
    LEFT_FOREARM("left forearm", BodyPart.LEFT_ARM, 1.0f),
    RIGHT_UPPER_ARM("right upper arm", BodyPart.RIGHT_ARM, 1.2f),
    RIGHT_FOREARM("right forearm", BodyPart.RIGHT_ARM, 1.0f),
    LEFT_THIGH("left thigh", BodyPart.LEFT_LEG, 1.2f),
    LEFT_SHIN("left shin", BodyPart.LEFT_LEG, 1.0f),
    RIGHT_THIGH("right thigh", BodyPart.RIGHT_LEG, 1.2f),
    RIGHT_SHIN("right shin", BodyPart.RIGHT_LEG, 1.0f),
}

/**
 * @param templateAngle limb angle (see [com.poseit.app.pose.limbAngle]) of the segment in the template
 * @param actualAngle limb angle in the detected pose, or null if not visible
 * @param error signed angular error actual - template in degrees (0 if not visible)
 * @param score 0..1 (0 when not visible)
 * @param required false when the template itself doesn't have this segment inside the frame (e.g. legs of a portrait)
 */
data class SegmentMatch(
    val segment: Segment,
    val templateAngle: Float,
    val actualAngle: Float?,
    val error: Float,
    val score: Float,
    val required: Boolean = true,
)

/**
 * Match of one template figure.
 * @param score 0..1 similarity, already penalised for low [coverage]
 * @param partScores 0..1 per body part (for colouring the overlay); parts not evaluated are absent
 * @param coverage weighted fraction of the required segments that were visible
 * @param detectedIndex index into the detected-people list that was assigned to this figure, or null
 */
data class FigureMatch(
    val score: Float,
    val segments: List<SegmentMatch>,
    val partScores: Map<BodyPart, Float>,
    val coverage: Float,
    val detectedIndex: Int?,
)

enum class HintKind { NO_PERSON, MISSING_PERSON, FRAMING, VISIBILITY, POSE, PERFECT }

/**
 * A coaching instruction.
 * @param text short on-screen text, e.g. "Raise your left arm"
 * @param speech what the voice coach says (defaults to text)
 * @param severity 0..1, how far off this is (used for sorting / colouring)
 * @param figureIndex which template figure it refers to (multi-person poses), or null
 */
data class CoachHint(
    val text: String,
    val kind: HintKind,
    val severity: Float,
    val part: BodyPart? = null,
    val figureIndex: Int? = null,
    val speech: String = text,
)

/**
 * @param score overall 0..1 (mean of figure scores; 0 when nobody is detected)
 * @param figures one entry per TEMPLATE figure, same order
 * @param hints most important first, at most ~3, never empty (PERFECT hint when matched)
 */
data class MatchResult(
    val score: Float,
    val figures: List<FigureMatch>,
    val hints: List<CoachHint>,
    val peopleDetected: Int,
) {
    companion object {
        val EMPTY = MatchResult(0f, emptyList(), emptyList(), 0)
    }
}

object PoseMatcher {

    /**
     * Compares detected people with the template figures.
     *
     * Never throws: empty lists, missing joints, NaN coordinates and odd configs all produce a valid result.
     *
     * SCORING (per template figure and assigned person, everything in isotropic scene units, x * sceneAspect):
     * - Segments: TORSO = mid-hip -> mid-shoulder, HEAD = mid-shoulder -> nose, SHOULDER_LINE = right -> left
     *   shoulder, HIP_LINE = right -> left hip, limbs from their joint pairs; angles via [com.poseit.app.pose.limbAngle].
     *   Midpoints only use the joints both poses show.
     * - A template segment is REQUIRED when its joints exist, have visibility >= [MatchConfig.minVisibility] and lie in
     *   the scene (0..1 with a 0.02 margin). Shoulder/hip lines shorter than 25 % of their normal length (profile views)
     *   and degenerate limbs are skipped.
     * - `segmentScore` = 1 within [MatchConfig.perfectWithinDeg], 0 at >= [MatchConfig.zeroAtDeg], Hermite smoothstep
     *   in between. The HEAD additionally loses score when the nose-to-shoulders distance differs (chin up/down).
     * - `shape` = Σ(weight · segmentScore) / Σ(weight) over the required segments the person shows.
     * - `worstFactor` = 1 − 0.5 · max over those segments of (weight / torsoWeight) · (1 − segmentScore). Without it a
     *   completely wrong arm would still leave the weighted mean at ~0.82 (a "perfect" match at the default strictness).
     * - `coverage` = visible required weight / required weight; `coverageFactor` = clamp((coverage − 0.4) / 0.5, 0, 1)^0.7.
     * - `criticalFactor` = 0.7 if the template needs the head (nose) and it is missing, × 0.8 if it needs the feet
     *   (ankles) and they are missing, else 1: a photo with the head or feet cut off is never a match.
     * - `framing` (0..1) = offsetScore · sizeScore: the person's torso anchor vs the template's, offset normalised by
     *   the template body height (perfect ≤ 0.06, zero ≥ 0.5), and the median segment-length ratio (perfect within
     *   6 %, zero at 60 %).
     * - `figureScore` = shape · worstFactor · coverageFactor · criticalFactor · (0.75 + 0.25 · framing).
     * - Overall score = mean of the figure scores; figures without a person score 0.
     *
     * MULTI-PERSON: people are assigned to figures by exhaustive search minimising
     * Σ (1 − shape · worstFactor · coverageFactor) + 0.5 · min(centreDistance / templateBodyHeight, 4);
     * every figure gets a person while people remain.
     *
     * HINTS (at most 3, most severe first, never empty): NO_PERSON, MISSING_PERSON, VISIBILITY (cut-off or hidden
     * joints), POSE (worst weighted segments, anatomical side, image directions converted with the person's facing),
     * FRAMING (position / size / cropping), or a single PERFECT when score >= [MatchConfig.perfectScore] and every
     * figure has a person. Multi-person hints are prefixed "Person N: ", numbered left to right in the template.
     *
     * @param templateFigures template figures in SCENE space (see [com.poseit.app.pose.scene.SceneMapping.templateToScene])
     * @param detected detected people in SCENE space (see [com.poseit.app.pose.detect.LandmarkMapping.toScene])
     * @param sceneAspect scene width / height – needed to measure angles correctly
     * @param audience who acts on framing hints
     */
    fun match(
        templateFigures: List<Skeleton>,
        detected: List<Skeleton>,
        sceneAspect: Float,
        audience: FramingAudience = FramingAudience.SUBJECT,
        config: MatchConfig = MatchConfig(),
    ): MatchResult = try {
        compute(templateFigures, detected, sceneAspect, audience, config)
    } catch (e: RuntimeException) {
        // Last line of defence for the camera loop: a bad frame must never crash the app.
        MatchResult(
            score = 0f,
            figures = templateFigures.map { FigureMatch(0f, emptyList(), emptyMap(), 0f, null) },
            hints = listOf(CoachHint(FALLBACK_TEXT, HintKind.POSE, 0f)),
            peopleDetected = 0,
        )
    }

    private const val FALLBACK_TEXT = "Match the outline"

    private fun compute(
        templateFigures: List<Skeleton>,
        detected: List<Skeleton>,
        sceneAspect: Float,
        audience: FramingAudience,
        config: MatchConfig,
    ): MatchResult {
        val aspect = MatchGeometry.sanitizeAspect(sceneAspect)
        val cfg = sanitize(config)
        val templates = templateFigures.mapIndexed { i, s -> TemplateInfo(i, MatchGeometry.sanitize(s), aspect, cfg.minVisibility) }
        val people = detected.mapIndexedNotNull { i, s ->
            DetectionInfo(i, MatchGeometry.sanitize(s), aspect, cfg.minVisibility).takeIf { it.hasUsableJoint }
        }
        val hints = HintBuilder(audience, templates)

        if (templates.isEmpty()) {
            return MatchResult(0f, emptyList(), listOf(if (people.isEmpty()) hints.noPerson() else hints.noPose()), people.size)
        }
        if (people.isEmpty()) {
            return MatchResult(0f, templates.map { it.unassignedMatch() }, listOf(hints.noPerson()), 0)
        }

        val evals = templates.map { t -> people.map { d -> PairEval(t, d, cfg) } }
        val cost = Array(templates.size) { i ->
            val t = templates[i]
            FloatArray(people.size) { j ->
                val e = evals[i][j]
                val distance = (t.centre.distanceTo(people[j].centre) / t.refHeight).coerceAtMost(MAX_ASSIGN_DISTANCE)
                (1f - e.poseQuality) + 0.5f * distance
            }
        }
        val assignment = Assignment.solve(cost, people.size)
        val assigned = templates.indices.map { i -> assignment[i].takeIf { it >= 0 }?.let { evals[i][it] } }

        val figures = assigned.mapIndexed { i, e -> e?.toFigureMatch() ?: templates[i].unassignedMatch() }
        val score = (figures.sumOf { it.score.toDouble() } / figures.size).toFloat().coerceIn(0f, 1f)
        val matched = score >= cfg.perfectScore && assigned.all { it != null }
        val hintList = if (matched) listOf(hints.perfect()) else hints.build(assigned)
        return MatchResult(score, figures, hintList, people.size)
    }

    /** Normalised centre distance cap used by the assignment cost. */
    private const val MAX_ASSIGN_DISTANCE = 4f

    private fun sanitize(c: MatchConfig): MatchConfig {
        val defaults = MatchConfig()
        val minVis = if (c.minVisibility.isFinite()) c.minVisibility.coerceIn(0f, 1f) else defaults.minVisibility
        val perfect = if (c.perfectWithinDeg.isFinite() && c.perfectWithinDeg >= 0f) c.perfectWithinDeg.coerceAtMost(179f)
        else defaults.perfectWithinDeg
        val zero = if (c.zeroAtDeg.isFinite() && c.zeroAtDeg > perfect) c.zeroAtDeg.coerceAtMost(180f)
        else maxOf(defaults.zeroAtDeg, perfect + 1f)
        val perfectScore = if (c.perfectScore.isFinite()) c.perfectScore.coerceIn(0f, 1f) else defaults.perfectScore
        return MatchConfig(minVis, perfect, zero, perfectScore)
    }
}
