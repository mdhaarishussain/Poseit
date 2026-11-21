package com.poseit.app.pose.library

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.dsl.pose

/**
 * Single-person standing classics. All figures are full body in a 3:4 frame.
 *
 * Authoring reminders (see the DSL header): angles are absolute image angles (0 = down, +90 = image right,
 * 180 = up); with a positive [com.poseit.app.pose.dsl.FigureBuilder.facing] the subject's LEFT side is the far
 * (back) side, with a negative facing their RIGHT side is the far side. "Weight on the back leg" therefore means
 * the left leg for positive facing and the right leg for negative facing.
 */
object StandingPoses {
    val all: List<PoseTemplate> = listOf(
        pose("stand_hand_on_hip", "Hand on hip", PoseCategory.STANDING) {
            description = "The go-to confident pose: one hand on the waist creates a flattering triangle of space " +
                "between arm and body, and the angled stance slims the silhouette."
            tips(
                "Turn your body about 30° away from the camera",
                "Shift your weight onto your back leg and pop the front knee inward",
                "Rest your back hand on your waist with the elbow pointing back",
                "Drop your chin slightly and lean it toward the lens",
            )
            tags(PoseTag.CONFIDENT, PoseTag.SLIMMING, PoseTag.CITY, PoseTag.TRAVEL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot from waist height so the legs keep their length.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 28f
                hipTilt = -5f
                shoulderTilt = 3f
                headTilt = -4f
                headPitch = -4f
                leftArm(upper = 42f, fore = -62f, hand = -80f)
                rightArm(upper = -13f, fore = -5f)
                leftLeg(thigh = -5f, shin = -1f)
                rightLeg(thigh = 7f, shin = -14f)
            }
        },
        pose("stand_hands_in_pockets", "Hands in pockets", PoseCategory.STANDING) {
            description = "Relaxed and effortless. Hands tucked into the front pockets with the thumbs out keep " +
                "the arms natural and the elbows away from the body."
            tips(
                "Slide your hands into your front pockets up to the knuckles, thumbs out",
                "Let your elbows bend outward so light shows between arms and waist",
                "Put your weight on your back leg and relax the front knee",
            )
            tags(PoseTag.CASUAL, PoseTag.COOL, PoseTag.CITY, PoseTag.NATURE)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Leave a little space above the head and below the feet.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = -22f
                hipTilt = 5f
                shoulderTilt = -3f
                headTilt = 3f
                headPitch = -3f
                leftArm(upper = 16f, fore = -28f, hand = -12f)
                rightArm(upper = -16f, fore = 28f, hand = 12f)
                rightLeg(thigh = 5f, shin = 0f)
                leftLeg(thigh = -6f, shin = 12f)
            }
        },
        pose("stand_crossed_ankles", "Crossed ankles", PoseCategory.STANDING) {
            description = "Crossing the feet at the ankles narrows the base, lengthens the legs and gives a chic, " +
                "slightly playful stance."
            tips(
                "Cross your front foot over the other at the ankles, toes pointing at the camera",
                "Hook one hand into your back pocket",
                "Let the other arm hang loosely with a soft elbow",
                "Stand tall and lift your chest",
            )
            tags(PoseTag.ELEGANT, PoseTag.SLIMMING, PoseTag.CITY, PoseTag.WALL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Keep the phone vertical so the crossed legs stay straight.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 15f
                headTilt = 5f
                headPitch = -3f
                leftArm(upper = 26f, fore = -36f, hand = -30f)
                rightArm(upper = -11f, fore = -2f)
                leftLeg(thigh = -9f, shin = -7f)
                rightLeg(thigh = 8f, shin = 7f)
            }
        },
        pose("stand_collar_touch", "Collar touch", PoseCategory.STANDING) {
            description = "A weight shift plus a hand at the collar looks natural and draws the eye to the face. " +
                "Great for a smart jacket or shirt."
            tips(
                "Shift your weight onto your back leg",
                "Lightly pinch your collar or lapel with your front hand",
                "Keep the other hand in your pocket",
                "Look just past the camera",
            )
            tags(PoseTag.CONFIDENT, PoseTag.ELEGANT, PoseTag.CITY, PoseTag.PARTY)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Focus on the eyes; the hand should frame the face, not hide it.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = -25f
                hipTilt = 4f
                shoulderTilt = -2f
                headTilt = 4f
                headPitch = -4f
                leftArm(upper = 20f, fore = -139f, hand = -160f, upperLen = 0.9f)
                rightArm(upper = -13f, fore = 25f, hand = 10f)
                rightLeg(thigh = 4f, shin = 0f)
                leftLeg(thigh = -6f, shin = 12f)
            }
        },
        pose("stand_look_away", "Candid look away", PoseCategory.STANDING) {
            description = "Looking off to the side instead of into the lens makes the photo feel like a stolen " +
                "moment rather than a posed one."
            tips(
                "Turn your head to look at something off to the side",
                "Hold the strap of your bag at shoulder height with your front hand",
                "Rest your weight on your back leg and bend the front knee slightly",
                "Relax your shoulders and breathe out before the shot",
            )
            tags(PoseTag.CANDID, PoseTag.CASUAL, PoseTag.NATURE, PoseTag.TRAVEL)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Leave empty space on the side the subject is looking toward.")
            difficulty = 1
            figure(cx = 0.46f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 25f
                headYaw = 80f
                headPitch = -4f
                hipTilt = -4f
                shoulderTilt = 2f
                leftArm(upper = 10f, fore = 4f)
                rightArm(upper = -18f, fore = 153f, hand = 165f)
                leftLeg(thigh = -4f, shin = 0f)
                rightLeg(thigh = 8f, shin = -10f)
            }
        },
        pose("stand_arms_crossed", "Relaxed arms crossed", PoseCategory.STANDING) {
            description = "Crossed arms read as self-assured when the body is angled and the expression is warm. " +
                "Keep it loose, not defensive."
            tips(
                "Cross your arms loosely at the lower chest, hands resting on the opposite arms",
                "Turn your body slightly and put your weight on your back leg",
                "Tilt your head a little and give a soft smile",
            )
            tags(PoseTag.CONFIDENT, PoseTag.COOL, PoseTag.OFFICE, PoseTag.CITY)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "A slightly lower angle adds authority; avoid shooting from above.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = -28f
                hipTilt = 4f
                shoulderTilt = -2f
                headTilt = 5f
                leftArm(upper = 12f, fore = -97f, hand = -100f)
                rightArm(upper = -12f, fore = 98f, hand = 104f)
                rightLeg(thigh = 4f, shin = 0f)
                leftLeg(thigh = -5f, shin = 13f)
            }
        },
        pose("stand_hand_in_hair", "Hand in hair", PoseCategory.STANDING) {
            description = "Raising one hand into the hair opens up the body, lifts the waistline and adds movement " +
                "and attitude."
            tips(
                "Slide one hand into your hair at the side of your head, elbow high and out",
                "Let the other arm relax with the hand on your thigh",
                "Pop your hip toward the raised elbow",
                "Tilt your head away from the raised arm",
            )
            tags(PoseTag.ELEGANT, PoseTag.PLAYFUL, PoseTag.BEACH, PoseTag.NATURE)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Shoot when the hand is moving through the hair for a natural look.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.82f) {
                facing = 20f
                hipTilt = -5f
                shoulderTilt = 3f
                headTilt = -8f
                headPitch = -3f
                leftArm(upper = 145f, fore = -95f, hand = -120f)
                rightArm(upper = -11f, fore = 4f)
                leftLeg(thigh = -5f, shin = 0f)
                rightLeg(thigh = 7f, shin = -12f)
            }
        },
        pose("stand_over_shoulder", "Over the shoulder", PoseCategory.STANDING) {
            description = "Back turned, face turned: the look-back shows off an outfit's back and the line of the " +
                "neck and shoulders."
            tips(
                "Turn your back to the camera",
                "Look back over your shoulder with your chin close to it",
                "Bend one knee slightly so your hips are not square",
            )
            tags(PoseTag.ELEGANT, PoseTag.ROMANTIC, PoseTag.TRAVEL, PoseTag.BEACH)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Wait for the moment the eyes reach the lens.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 150f
                headYaw = 55f
                headTilt = 4f
                hipTilt = 4f
                shoulderTilt = -2f
                leftArm(upper = -9f, fore = -4f)
                rightArm(upper = 11f, fore = 20f)
                leftLeg(thigh = 3f, shin = 0f)
                rightLeg(thigh = -7f, shin = 9f)
            }
        },
        pose("stand_power_pose", "Power pose", PoseCategory.STANDING) {
            description = "Feet planted wide and both hands on the hips: a bold, superhero-style stance that " +
                "radiates confidence."
            tips(
                "Plant your feet a little wider than your hips",
                "Put both hands on your hips with the elbows pointing out",
                "Pull your shoulders back and lift your chest",
                "Keep your chin level and look straight into the lens",
            )
            tags(PoseTag.CONFIDENT, PoseTag.COOL, PoseTag.CITY, PoseTag.OFFICE)
            camera(CameraHeight.KNEE, ShotType.FULL_BODY, note = "A low angle tilted slightly up makes the stance look heroic.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 8f
                leftArm(upper = 45f, fore = -65f, hand = -80f)
                rightArm(upper = -45f, fore = 65f, hand = 80f)
                leftLeg(thigh = 9f, shin = 6f)
                rightLeg(thigh = -9f, shin = -6f)
            }
        },
        pose("stand_sunglasses", "Sunglasses touch", PoseCategory.STANDING) {
            description = "Touching the sunglasses (or a hat brim) gives the hands a job and adds instant cool " +
                "with the elbow framing the face."
            tips(
                "Touch the arm of your sunglasses with your fingertips, elbow out to the side",
                "Keep your other hand in your pocket",
                "Put your weight on your back leg",
                "Angle your face slightly down",
            )
            tags(PoseTag.COOL, PoseTag.CONFIDENT, PoseTag.CITY, PoseTag.BEACH)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Watch for reflections in the lenses; shift a step if you see yourself.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.82f) {
                facing = -20f
                hipTilt = 4f
                shoulderTilt = -2f
                headPitch = -5f
                leftArm(upper = 55f, fore = -143f, hand = -150f, upperLen = 0.55f)
                rightArm(upper = -13f, fore = 25f, hand = 10f)
                rightLeg(thigh = 4f, shin = 0f)
                leftLeg(thigh = -5f, shin = 12f)
            }
        },
    )
}
