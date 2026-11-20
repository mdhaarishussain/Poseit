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
    ): MatchResult {
        TODO("implemented by the engine agent")
    }
}
