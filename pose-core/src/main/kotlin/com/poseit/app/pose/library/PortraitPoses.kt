package com.poseit.app.pose.library

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.dsl.pose

/**
 * Waist-up and head-and-shoulders portraits. The figures are deliberately cropped (legs below the frame), so
 * matching concentrates on the head, shoulders and arms.
 *
 * Authoring reminders (see the DSL header): angles are absolute image angles (0 = down, +90 = image right,
 * 180 = up). With a positive `facing` the subject's LEFT side is the far side (image right); with a negative
 * `facing` their RIGHT side is the far side (image left).
 */
object PortraitPoses {
    val all: List<PoseTemplate> = listOf(
        pose("port_chin_rest", "Thoughtful chin rest", PoseCategory.PORTRAIT) {
            description = "A thoughtful classic: one arm folded across the waist props up the other elbow, and the " +
                "knuckles rest lightly under the chin. It frames the face and gives the hands a natural job."
            tips(
                "Turn your shoulders about 30° away from the camera",
                "Fold one arm across your waist and rest the other elbow on that hand",
                "Let your chin touch your knuckles lightly, without pushing your face up",
                "Look into the lens with a soft, closed-mouth smile",
            )
            tags(PoseTag.CONFIDENT, PoseTag.ELEGANT, PoseTag.CAFE, PoseTag.OFFICE, PoseTag.CITY)
            camera(CameraHeight.EYE, ShotType.HALF_BODY, note = "Shoot at eye level and leave a little headroom above the hair.")
            difficulty = 1
            figure(cx = 0.515f, top = 0.1f, bodyHeight = 1.7f) {
                facing = 25f
                headYaw = 10f
                headTilt = -6f
                headPitch = -3f
                leftArm(upper = -13f, fore = -160f, hand = -172f, upperLen = 0.86f)
                rightArm(upper = -4f, fore = 92f, hand = 96f, upperLen = 0.92f, foreLen = 0.94f)
            }
        },
        pose("port_face_frame", "Face frame", PoseCategory.PORTRAIT) {
            description = "Both hands softly frame the jawline, drawing every eye straight to the face. Great for " +
                "beauty shots, new hairstyles and close-ups with a playful head tilt."
            tips(
                "Rest your palms lightly along your jaw, fingers relaxed, without squashing your cheeks",
                "Bring your elbows in front of your body, close together",
                "Tilt your head slightly toward one shoulder",
                "Keep your shoulders down and relaxed",
            )
            tags(PoseTag.PLAYFUL, PoseTag.ELEGANT, PoseTag.HOME, PoseTag.NATURE)
            camera(CameraHeight.EYE, ShotType.HALF_BODY, note = "Focus on the eyes and fill the frame with the face and hands.")
            difficulty = 1
            figure(cx = 0.492f, top = 0.1f, bodyHeight = 1.7f) {
                facing = -12f
                headTilt = 8f
                headPitch = -2f
                leftArm(upper = 21f, fore = -151f, hand = -172f, upperLen = 0.66f)
                rightArm(upper = -10f, fore = 149f, hand = 172f, upperLen = 0.61f)
            }
        },
        pose("port_hand_in_hair", "Fingers through hair", PoseCategory.PORTRAIT) {
            description = "Lifting one arm to run the fingers through the hair opens up the body and adds movement " +
                "and attitude. A favourite for fashion and outdoor portraits."
            tips(
                "Lift your near arm and slide your fingers into your hair at the crown",
                "Keep the raised elbow pointing out to the side, not at the camera",
                "Let the other arm hang loosely",
                "Lift your chin just a touch and relax your mouth",
            )
            tags(PoseTag.COOL, PoseTag.ELEGANT, PoseTag.NATURE, PoseTag.BEACH, PoseTag.CITY)
            camera(CameraHeight.EYE, ShotType.HALF_BODY, note = "Keep the raised elbow inside the frame. Don't crop it off.")
            difficulty = 2
            figure(cx = 0.463f, top = 0.1f, bodyHeight = 1.6f) {
                facing = -25f
                headYaw = -10f
                headTilt = 6f
                headPitch = 2f
                leftArm(upper = 150f, fore = -100f, hand = -125f, foreLen = 0.9f)
                rightArm(upper = -8f, fore = 4f)
            }
        },
        pose("port_over_shoulder", "Shoulder glance", PoseCategory.PORTRAIT) {
            description = "Turning your back to the camera and glancing over your shoulder creates a graceful neck " +
                "line and an inviting, candid feel. Works anywhere with a nice background."
            tips(
                "Turn your back to the camera, then turn your head over your near shoulder",
                "Lift that shoulder a little toward your chin",
                "Look into the lens with your chin slightly down",
                "Keep your arms soft and relaxed",
            )
            tags(PoseTag.ELEGANT, PoseTag.CANDID, PoseTag.TRAVEL, PoseTag.CITY, PoseTag.NATURE)
            camera(CameraHeight.EYE, ShotType.HALF_BODY, note = "Focus on the eye nearest to the camera.")
            difficulty = 2
            figure(cx = 0.469f, top = 0.1f, bodyHeight = 1.7f) {
                facing = 150f
                headYaw = 65f
                headTilt = 6f
                headPitch = -4f
                shoulderTilt = -5f
                leftArm(upper = -8f, fore = -4f)
                rightArm(upper = 9f, fore = 2f)
            }
        },
        pose("port_arms_crossed_smile", "Crossed-arm smile", PoseCategory.PORTRAIT) {
            description = "Folded arms with a warm smile read as confident and approachable at the same time. The " +
                "smile keeps it friendly, the angled body keeps it slim."
            tips(
                "Turn your body slightly away and fold your arms loosely",
                "Tuck one hand under the opposite arm instead of gripping",
                "Keep your elbows a little away from your waist",
                "Turn your face back to the camera and smile with your eyes",
            )
            tags(PoseTag.CONFIDENT, PoseTag.CASUAL, PoseTag.OFFICE, PoseTag.CITY, PoseTag.WALL)
            camera(CameraHeight.EYE, ShotType.HALF_BODY, note = "Crop just below the elbows, never through them.")
            difficulty = 1
            figure(cx = 0.491f, top = 0.1f, bodyHeight = 1.7f) {
                facing = -20f
                headYaw = -5f
                headTilt = 5f
                headPitch = -3f
                leftArm(upper = 10f, fore = -100f, hand = -105f, upperLen = 0.9f)
                rightArm(upper = -6f, fore = 94f, hand = 100f, upperLen = 0.91f)
            }
        },
        pose("port_chin_on_hand", "Café chin rest", PoseCategory.PORTRAIT) {
            description = "Leaning forward over a table with the chin resting on the knuckles feels relaxed and " +
                "intimate. Perfect for café, restaurant and desk shots."
            tips(
                "Sit at a table and lean your upper body slightly toward the camera",
                "Put one elbow on the table and rest your chin on your knuckles",
                "Lay the other forearm flat on the table",
                "Tilt your head a little toward the supporting hand",
            )
            tags(PoseTag.CASUAL, PoseTag.CANDID, PoseTag.CAFE, PoseTag.HOME)
            camera(CameraHeight.EYE, ShotType.HALF_BODY, note = "Shoot from across the table at eye level.")
            difficulty = 1
            figure(cx = 0.424f, top = 0.1f, bodyHeight = 1.7f) {
                facing = -20f
                torsoLength = 0.88f
                headYaw = -5f
                headTilt = 10f
                leftArm(upper = -15f, fore = -162f, hand = -172f, upperLen = 0.9f)
                rightArm(upper = -11f, fore = 83f, hand = 88f, upperLen = 0.88f)
            }
        },
        pose("port_dreamy_gaze", "Dreamy gaze", PoseCategory.PORTRAIT) {
            description = "Holding a warm cup and gazing off into the distance gives a calm, story-telling mood. " +
                "The candid look hides any camera shyness."
            tips(
                "Hold a cup with both hands at chest height",
                "Turn your head away and look into the distance, slightly upward",
                "Relax your face and let a tiny smile happen",
                "Keep your elbows soft and close to your body",
            )
            tags(PoseTag.CANDID, PoseTag.CASUAL, PoseTag.CAFE, PoseTag.HOME, PoseTag.NATURE)
            camera(CameraHeight.EYE, ShotType.HALF_BODY, note = "Leave empty space in the direction the subject is looking.")
            difficulty = 1
            figure(cx = 0.467f, top = 0.1f, bodyHeight = 1.7f) {
                facing = 30f
                headYaw = 75f
                headPitch = 8f
                headTilt = -3f
                leftArm(upper = 6f, fore = -135f, hand = -110f, upperLen = 0.97f, foreLen = 0.82f)
                rightArm(upper = -5f, fore = 129f, hand = 110f, upperLen = 0.92f, foreLen = 0.82f)
            }
        },
        pose("port_collar_adjust", "Collar adjust", PoseCategory.PORTRAIT) {
            description = "Pinching the collar as if fixing it adds a sharp, self-assured touch. A favourite for " +
                "shirts, jackets and coats."
            tips(
                "Turn your shoulders a little away from the camera",
                "Pinch the edge of your collar with your far hand, elbow out",
                "Slide the other hand into your pocket",
                "Drop your chin slightly and look straight into the lens",
            )
            tags(PoseTag.COOL, PoseTag.CONFIDENT, PoseTag.CITY, PoseTag.OFFICE, PoseTag.PARTY)
            camera(CameraHeight.EYE, ShotType.HALF_BODY, note = "A slightly lower angle than eye level makes it look stronger.")
            difficulty = 1
            figure(cx = 0.509f, top = 0.1f, bodyHeight = 1.7f) {
                facing = 20f
                headYaw = 5f
                headPitch = -5f
                headTilt = -3f
                leftArm(upper = 20f, fore = -130f, hand = -160f, upperLen = 0.75f)
                rightArm(upper = -16f, fore = 22f)
            }
        },
        pose("port_shoulder_touch", "Shoulder touch", PoseCategory.PORTRAIT) {
            description = "One hand resting on the opposite shoulder with the head tilted toward it creates soft " +
                "lines across the body. Elegant and gentle."
            tips(
                "Turn your body about 30° away from the camera",
                "Reach your near hand across and rest it on your far shoulder",
                "Tilt your head toward that hand",
                "Look back at the camera with your chin slightly down",
            )
            tags(PoseTag.ELEGANT, PoseTag.ROMANTIC, PoseTag.HOME, PoseTag.NATURE)
            camera(CameraHeight.EYE, ShotType.HALF_BODY, note = "Soft side light from a window flatters this pose.")
            difficulty = 1
            figure(cx = 0.493f, top = 0.1f, bodyHeight = 1.7f) {
                facing = 30f
                headYaw = 10f
                headTilt = 10f
                headPitch = -5f
                rightArm(upper = 26f, fore = 146f, hand = 120f, upperLen = 0.93f)
                leftArm(upper = 8f, fore = 2f)
            }
        },
    )
}
