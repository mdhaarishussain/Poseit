package com.poseit.app.pose.library

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.dsl.pose

/**
 * Single-person sitting poses on chairs, benches, steps, kerbs and stools. The seat itself is not drawn, so
 * most figures are seen from a three-quarter angle where the horizontal thighs make the sitting posture obvious.
 */
object SittingPoses {
    val all: List<PoseTemplate> = listOf(
        pose("sit_crossed_legs", "Crossed legs", PoseCategory.SITTING) {
            description = "A timeless chair pose: crossing the legs at the knee and angling them away from the " +
                "camera lengthens the legs and looks polished."
            tips(
                "Sit on the front half of the chair with your back straight",
                "Cross your near leg over the other at the knee and point your toes",
                "Angle your knees away from the camera",
                "Rest both hands lightly on your top knee",
            )
            tags(PoseTag.ELEGANT, PoseTag.SLIMMING, PoseTag.CAFE, PoseTag.OFFICE)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Shoot from the side the knees point toward so the legs look long.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.92f, bodyHeight = 0.9f) {
                facing = 45f
                torsoLean = 12f
                headTilt = -10f
                headYaw = 20f
                leftLeg(thigh = 80f, shin = 3f, thighLen = 0.7f)
                rightLeg(thigh = 95f, shin = -22f, foot = 10f)
                rightArm(upper = 32f, fore = 48f, hand = 62f)
                leftArm(upper = 30f, fore = -9f, upperLen = 0.6f, foreLen = 0.8f)
            }
        },
        pose("sit_elbows_on_knees", "Elbows on knees", PoseCategory.SITTING) {
            description = "Leaning forward with the elbows on the knees feels open and engaged, and brings the " +
                "face closer to the camera. Great on a bench or low wall."
            tips(
                "Sit on the edge of the bench with your feet flat and knees apart",
                "Lean forward and rest your elbows just above your knees",
                "Loosely clasp your hands in front of you",
                "Lift your chin and look at the camera",
            )
            tags(PoseTag.CASUAL, PoseTag.COOL, PoseTag.CITY, PoseTag.NATURE)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Get down to the subject's eye level instead of shooting from above.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.92f, bodyHeight = 1.1f) {
                facing = 80f
                torsoLean = 45f
                headTilt = -35f
                headYaw = 30f
                rightLeg(thigh = 92f, shin = -3f)
                leftLeg(thigh = 90f, shin = 4f)
                rightArm(upper = 12f, fore = 60f, hand = 30f)
                leftArm(upper = 9f, fore = 64f, hand = 30f, foreLen = 0.85f)
            }
        },
        pose("sit_one_knee_up", "One knee up", PoseCategory.SITTING) {
            description = "Sitting on steps with one foot up a step and an arm draped over the knee makes a " +
                "relaxed, effortless triangle shape."
            tips(
                "Sit on a step and place your near foot one step higher",
                "Drape your near forearm over the raised knee",
                "Put your other hand on the step behind you",
                "Turn your head toward the camera",
            )
            tags(PoseTag.CASUAL, PoseTag.COOL, PoseTag.STAIRS, PoseTag.CITY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot from slightly below the step so the raised knee doesn't hide the body.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.93f, bodyHeight = 0.9f) {
                facing = 60f
                headYaw = 20f
                torsoLean = 4f
                headTilt = -4f
                rightLeg(thigh = 128f, shin = 8f)
                leftLeg(thigh = 75f, shin = 6f)
                rightArm(upper = 28f, fore = 70f, hand = 30f)
                leftArm(upper = -22f, fore = -18f)
            }
        },
        pose("sit_side_saddle", "Side-saddle", PoseCategory.SITTING) {
            description = "Knees together and angled to one side with the torso facing the camera: the most " +
                "elegant way to sit on a bench in a skirt or dress."
            tips(
                "Keep your knees and ankles together",
                "Swing both legs to one side and tuck your feet back",
                "Turn your shoulders toward the camera and sit up tall",
                "Stack your hands on your lap",
            )
            tags(PoseTag.ELEGANT, PoseTag.ROMANTIC, PoseTag.NATURE, PoseTag.CAFE)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Shoot from the side the knees point to so the legs look long.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.92f, bodyHeight = 0.92f) {
                facing = 22f
                torsoLean = 6f
                headTilt = 2f
                headPitch = -4f
                leftLeg(thigh = 72f, shin = -18f, foot = 40f, thighLen = 0.65f)
                rightLeg(thigh = 74f, shin = -16f, foot = 40f, thighLen = 0.85f)
                rightArm(upper = -2f, fore = 62f, hand = 80f)
                leftArm(upper = 15f, fore = -32f, hand = -60f, foreLen = 0.85f)
            }
        },
        pose("sit_steps_sideways", "Sideways on steps", PoseCategory.SITTING) {
            description = "Sitting across a step with your back to the wall turns a staircase into a lounge. " +
                "One leg long and one knee up keeps the shape interesting."
            tips(
                "Sit sideways on a wide step with your back against the wall",
                "Stretch your far leg along the step and bend your near knee",
                "Rest your near forearm on the raised knee",
                "Turn your face toward the camera",
            )
            tags(PoseTag.CASUAL, PoseTag.CANDID, PoseTag.STAIRS, PoseTag.HOME)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot from a few steps below to include the staircase lines.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.85f, bodyHeight = 0.86f) {
                facing = 88f
                headYaw = 30f
                torsoLean = -8f
                headTilt = 6f
                leftLeg(thigh = 84f, shin = 86f, foot = 160f)
                rightLeg(thigh = 132f, shin = 14f)
                rightArm(upper = 25f, fore = 80f, hand = 40f)
                leftArm(upper = -25f, fore = -20f)
            }
        },
        pose("sit_hug_knees", "Hugging knees", PoseCategory.SITTING) {
            description = "Knees pulled in and arms wrapped around them: a cosy, dreamy pose for the beach, a " +
                "park lawn or a windowsill."
            tips(
                "Pull your knees up to your chest with your feet flat",
                "Wrap your arms around your shins",
                "Lift your chin over your knees and look off into the distance",
            )
            tags(PoseTag.CANDID, PoseTag.ROMANTIC, PoseTag.BEACH, PoseTag.NATURE)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Get low and shoot from the side for a clean, compact shape.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.88f, bodyHeight = 0.95f) {
                facing = 70f
                torsoLean = 6f
                headTilt = -4f
                headYaw = 25f
                leftLeg(thigh = 128f, shin = -14f)
                rightLeg(thigh = 132f, shin = -10f)
                rightArm(upper = 28f, fore = 80f, hand = 120f, foreLen = 0.7f)
                leftArm(upper = 20f, fore = 85f, hand = 120f, foreLen = 0.75f)
            }
        },
        pose("sit_armchair_lounge", "Armchair lounge", PoseCategory.SITTING) {
            description = "Sideways in an armchair with the legs draped over the armrest and a hand behind the " +
                "head: the ultimate cosy, at-home lounging shot."
            tips(
                "Sit sideways in an armchair with your back against one armrest",
                "Drape your legs over the other armrest and let your feet hang",
                "Tuck your near hand behind your head",
                "Rest your other hand on your stomach and look at the camera",
            )
            tags(PoseTag.CASUAL, PoseTag.PLAYFUL, PoseTag.HOME, PoseTag.CAFE)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Shoot from the front of the chair so the whole body lies across the frame.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.8f, bodyHeight = 0.88f) {
                facing = 70f
                torsoLean = -35f
                headTilt = 20f
                headYaw = 30f
                rightLeg(thigh = 115f, shin = 8f, foot = 40f)
                leftLeg(thigh = 112f, shin = 14f, foot = 40f)
                rightArm(upper = 175f, fore = -50f, hand = -60f, foreLen = 0.58f)
                leftArm(upper = 30f, fore = 80f, hand = 90f)
            }
        },
        pose("sit_chair_backwards", "Backwards chair", PoseCategory.SITTING) {
            description = "Straddling a chair turned backwards, arms folded on the backrest: confident, " +
                "casual and great for creative portraits."
            tips(
                "Turn the chair around and sit astride it",
                "Fold your forearms on top of the backrest",
                "Let your knees point out to the sides with feet flat",
                "Lean in slightly toward the camera",
            )
            tags(PoseTag.COOL, PoseTag.CONFIDENT, PoseTag.HOME, PoseTag.OFFICE)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Shoot straight on at chest height to keep the chair symmetrical.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.92f, bodyHeight = 0.92f) {
                facing = 5f
                headTilt = 5f
                headPitch = -3f
                leftLeg(thigh = 60f, shin = 5f, thighLen = 0.65f)
                rightLeg(thigh = -60f, shin = -5f, thighLen = 0.65f)
                leftArm(upper = 25f, fore = -95f, hand = -95f, upperLen = 0.75f)
                rightArm(upper = -25f, fore = 95f, hand = 95f, upperLen = 0.75f)
            }
        },
        pose("sit_ankle_on_knee", "Ankle on knee", PoseCategory.SITTING) {
            description = "Resting one ankle on the opposite knee with an arm along the bench back takes up " +
                "space and reads relaxed and self-assured."
            tips(
                "Rest one ankle on the opposite knee",
                "Stretch your arm along the back of the bench",
                "Rest your other hand on your raised shin",
                "Sit back and relax your shoulders",
            )
            tags(PoseTag.CONFIDENT, PoseTag.CASUAL, PoseTag.NATURE, PoseTag.CITY)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Shoot slightly off-centre so the raised knee points across the frame.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.92f, bodyHeight = 0.86f) {
                facing = 20f
                headTilt = 3f
                rightLeg(thigh = -8f, shin = 2f, thighLen = 0.4f)
                leftLeg(thigh = 68f, shin = -84f, foot = -95f, thighLen = 0.62f)
                rightArm(upper = -100f, fore = -10f, hand = -5f)
                leftArm(upper = 20f, fore = 12f, hand = 30f)
            }
        },
        pose("sit_high_perch", "High perch", PoseCategory.SITTING) {
            description = "Perched on a bar stool or high wall with one foot on the rung and one leg reaching " +
                "the ground: tall, balanced and leggy."
            tips(
                "Sit on the front edge of a high stool or wall",
                "Put one foot on the rung and stretch the other leg toward the floor",
                "Rest one hand on your knee and the other on the seat",
                "Keep your back straight and shoulders relaxed",
            )
            tags(PoseTag.ELEGANT, PoseTag.CONFIDENT, PoseTag.CAFE, PoseTag.PARTY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot from waist height so the long leg stays long.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.86f) {
                facing = 30f
                headTilt = -3f
                headPitch = -3f
                rightLeg(thigh = 35f, shin = 14f)
                leftLeg(thigh = 75f, shin = -10f, thighLen = 0.8f)
                rightArm(upper = -14f, fore = -4f)
                leftArm(upper = 30f, fore = 15f)
            }
        },
    )
}
