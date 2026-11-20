package com.poseit.app.pose

/**
 * The body landmarks Poseit works with. Names are ANATOMICAL (the subject's own left/right),
 * exactly like MediaPipe Pose. [mpIndex] is the index in MediaPipe's 33-landmark BlazePose topology.
 *
 * In a photo of someone facing the camera, the subject's LEFT side appears on the image RIGHT.
 */
enum class Joint(val mpIndex: Int, val isLeft: Boolean?, val label: String) {
    NOSE(0, null, "nose"),
    LEFT_EYE(2, true, "left eye"),
    RIGHT_EYE(5, false, "right eye"),
    LEFT_EAR(7, true, "left ear"),
    RIGHT_EAR(8, false, "right ear"),
    LEFT_SHOULDER(11, true, "left shoulder"),
    RIGHT_SHOULDER(12, false, "right shoulder"),
    LEFT_ELBOW(13, true, "left elbow"),
    RIGHT_ELBOW(14, false, "right elbow"),
    LEFT_WRIST(15, true, "left wrist"),
    RIGHT_WRIST(16, false, "right wrist"),
    /** Tip of the index finger (MediaPipe 19/20) – used for hand direction. */
    LEFT_HAND(19, true, "left hand"),
    RIGHT_HAND(20, false, "right hand"),
    LEFT_HIP(23, true, "left hip"),
    RIGHT_HIP(24, false, "right hip"),
    LEFT_KNEE(25, true, "left knee"),
    RIGHT_KNEE(26, false, "right knee"),
    LEFT_ANKLE(27, true, "left ankle"),
    RIGHT_ANKLE(28, false, "right ankle"),
    /** Tip of the foot (MediaPipe foot index 31/32). */
    LEFT_FOOT(31, true, "left foot"),
    RIGHT_FOOT(32, false, "right foot");

    /** The same landmark on the other side of the body (NOSE maps to itself). */
    val opposite: Joint
        get() = when (this) {
            NOSE -> NOSE
            LEFT_EYE -> RIGHT_EYE; RIGHT_EYE -> LEFT_EYE
            LEFT_EAR -> RIGHT_EAR; RIGHT_EAR -> LEFT_EAR
            LEFT_SHOULDER -> RIGHT_SHOULDER; RIGHT_SHOULDER -> LEFT_SHOULDER
            LEFT_ELBOW -> RIGHT_ELBOW; RIGHT_ELBOW -> LEFT_ELBOW
            LEFT_WRIST -> RIGHT_WRIST; RIGHT_WRIST -> LEFT_WRIST
            LEFT_HAND -> RIGHT_HAND; RIGHT_HAND -> LEFT_HAND
            LEFT_HIP -> RIGHT_HIP; RIGHT_HIP -> LEFT_HIP
            LEFT_KNEE -> RIGHT_KNEE; RIGHT_KNEE -> LEFT_KNEE
            LEFT_ANKLE -> RIGHT_ANKLE; RIGHT_ANKLE -> LEFT_ANKLE
            LEFT_FOOT -> RIGHT_FOOT; RIGHT_FOOT -> LEFT_FOOT
        }

    companion object {
        val FACE = listOf(NOSE, LEFT_EYE, RIGHT_EYE, LEFT_EAR, RIGHT_EAR)
        val byMpIndex: Map<Int, Joint> = entries.associateBy { it.mpIndex }
    }
}

/** Which part of the body a [Bone] belongs to – used for grouping feedback & colouring. */
enum class BodyPart(val label: String) {
    HEAD("head"), TORSO("torso"),
    LEFT_ARM("left arm"), RIGHT_ARM("right arm"),
    LEFT_LEG("left leg"), RIGHT_LEG("right leg"),
}

/** Drawable / comparable body segments between two joints. */
enum class Bone(val from: Joint, val to: Joint, val part: BodyPart) {
    SHOULDERS(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER, BodyPart.TORSO),
    HIPS(Joint.LEFT_HIP, Joint.RIGHT_HIP, BodyPart.TORSO),
    LEFT_FLANK(Joint.LEFT_SHOULDER, Joint.LEFT_HIP, BodyPart.TORSO),
    RIGHT_FLANK(Joint.RIGHT_SHOULDER, Joint.RIGHT_HIP, BodyPart.TORSO),
    LEFT_UPPER_ARM(Joint.LEFT_SHOULDER, Joint.LEFT_ELBOW, BodyPart.LEFT_ARM),
    LEFT_FOREARM(Joint.LEFT_ELBOW, Joint.LEFT_WRIST, BodyPart.LEFT_ARM),
    LEFT_HAND(Joint.LEFT_WRIST, Joint.LEFT_HAND, BodyPart.LEFT_ARM),
    RIGHT_UPPER_ARM(Joint.RIGHT_SHOULDER, Joint.RIGHT_ELBOW, BodyPart.RIGHT_ARM),
    RIGHT_FOREARM(Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST, BodyPart.RIGHT_ARM),
    RIGHT_HAND(Joint.RIGHT_WRIST, Joint.RIGHT_HAND, BodyPart.RIGHT_ARM),
    LEFT_THIGH(Joint.LEFT_HIP, Joint.LEFT_KNEE, BodyPart.LEFT_LEG),
    LEFT_SHIN(Joint.LEFT_KNEE, Joint.LEFT_ANKLE, BodyPart.LEFT_LEG),
    LEFT_FOOT(Joint.LEFT_ANKLE, Joint.LEFT_FOOT, BodyPart.LEFT_LEG),
    RIGHT_THIGH(Joint.RIGHT_HIP, Joint.RIGHT_KNEE, BodyPart.RIGHT_LEG),
    RIGHT_SHIN(Joint.RIGHT_KNEE, Joint.RIGHT_ANKLE, BodyPart.RIGHT_LEG),
    RIGHT_FOOT(Joint.RIGHT_ANKLE, Joint.RIGHT_FOOT, BodyPart.RIGHT_LEG),
}
