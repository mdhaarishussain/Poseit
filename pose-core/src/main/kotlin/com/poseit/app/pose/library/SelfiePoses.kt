package com.poseit.app.pose.library

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.dsl.pose

/**
 * Selfies: arm's-length front-camera shots and mirror selfies.
 *
 * In an arm's-length selfie the phone IS the camera, so the arm holding it reaches toward the viewer: its upper
 * arm is foreshortened (`upperLen` around 0.5) and leaves the frame at a bottom (or top) corner. Those off-frame
 * segments are simply not required by the matcher. With a positive `facing` the phone arm is the subject's
 * RIGHT arm (on the image left), which brings that shoulder slightly toward the lens.
 */
object SelfiePoses {
    val all: List<PoseTemplate> = listOf(
        pose("selfie_classic", "Classic Arm Selfie", PoseCategory.SELFIE) {
            description = "The everyday selfie done right: phone slightly above eye level, shoulders angled and chin " +
                "a touch forward for a defined jawline."
            tips(
                "Hold the phone at arm's length, a little above your eyes",
                "Turn your shoulders slightly toward the phone arm",
                "Push your chin forward and a little down",
                "Look into the lens, not at the screen",
            )
            tags(PoseTag.CASUAL, PoseTag.CONFIDENT, PoseTag.TRAVEL, PoseTag.CITY, PoseTag.CAFE)
            camera(CameraHeight.EYE, ShotType.CLOSE_UP, note = "Face a window so soft light falls on your face.")
            difficulty = 1
            figure(cx = 0.37f, top = 0.1f, bodyHeight = 1.8f) {
                facing = 18f
                headYaw = 5f
                headTilt = -5f
                headPitch = -3f
                rightArm(upper = -50f, fore = -60f, hand = -65f, upperLen = 0.5f, foreLen = 0.45f)
                leftArm(upper = 8f, fore = 3f)
            }
        },
        pose("selfie_peace", "Peace Sign", PoseCategory.SELFIE) {
            description = "A cheerful classic: the free hand makes a V right next to the face. The hand fills the " +
                "empty side of the frame and makes the photo instantly playful."
            tips(
                "Hold the phone at arm's length with one hand",
                "Raise your other hand beside your cheek and make a V with two fingers",
                "Tilt your head toward the peace sign",
                "Give a big, open smile",
            )
            tags(PoseTag.PLAYFUL, PoseTag.CASUAL, PoseTag.TRAVEL, PoseTag.PARTY, PoseTag.BEACH)
            camera(CameraHeight.EYE, ShotType.CLOSE_UP, note = "Keep the V sign fully inside the frame, palm facing the lens.")
            difficulty = 1
            figure(cx = 0.326f, top = 0.1f, bodyHeight = 1.8f) {
                facing = 15f
                headTilt = 8f
                headPitch = -2f
                leftArm(upper = 30f, fore = -159f, hand = 178f, upperLen = 0.8f)
                rightArm(upper = -45f, fore = -55f, hand = -60f, upperLen = 0.5f, foreLen = 0.45f)
            }
        },
        pose("selfie_high_angle", "High-Angle Selfie", PoseCategory.SELFIE) {
            description = "Holding the phone up high and looking up into it slims the face and shows off the eyes. " +
                "The most flattering angle for most people."
            tips(
                "Raise the phone above your head, angled down at you",
                "Look up into the lens with your chin slightly lifted",
                "Rest your free hand lightly at your waist",
                "Relax your forehead and smile softly",
            )
            tags(PoseTag.CASUAL, PoseTag.SLIMMING, PoseTag.HOME, PoseTag.CITY, PoseTag.TRAVEL)
            camera(CameraHeight.HIGH, ShotType.CLOSE_UP, note = "Don't go too high – about a forearm above eye level is enough.")
            difficulty = 1
            figure(cx = 0.482f, top = 0.1f, bodyHeight = 1.8f) {
                facing = 15f
                torsoLength = 0.85f
                headYaw = 5f
                headPitch = 10f
                headTilt = -4f
                rightArm(upper = -155f, fore = -160f, hand = -165f, upperLen = 0.5f, foreLen = 0.45f)
                leftArm(upper = 15f, fore = -30f, hand = -40f)
            }
        },
        pose("selfie_hand_on_cheek", "Cheek Rest Selfie", PoseCategory.SELFIE) {
            description = "Resting the free palm softly on the cheek frames the face and gives a sweet, relaxed " +
                "expression. Ideal for close-ups and new make-up looks."
            tips(
                "Hold the phone at arm's length slightly above your eyes",
                "Lay your free palm lightly along your cheek and jaw",
                "Lean your head gently into your hand",
                "Keep your fingers relaxed and together",
            )
            tags(PoseTag.ELEGANT, PoseTag.CASUAL, PoseTag.HOME, PoseTag.CAFE)
            camera(CameraHeight.EYE, ShotType.CLOSE_UP, note = "Turn the face toward the light so the hand doesn't shadow it.")
            difficulty = 1
            figure(cx = 0.261f, top = 0.1f, bodyHeight = 1.8f) {
                facing = 15f
                headTilt = 8f
                headPitch = -3f
                leftArm(upper = 5f, fore = -161f, hand = -170f, upperLen = 0.67f)
                rightArm(upper = -50f, fore = -60f, hand = -65f, upperLen = 0.5f, foreLen = 0.45f)
            }
        },
        pose("selfie_mirror", "Mirror Selfie", PoseCategory.SELFIE) {
            description = "The outfit-of-the-day mirror shot: phone held at chest height so the face stays visible, " +
                "free hand on the hip and one knee relaxed."
            tips(
                "Stand a step back from the mirror, turned slightly to one side",
                "Hold the phone at chest height so it doesn't cover your face",
                "Put your free hand on your hip",
                "Bend your front knee in and keep your weight on the back leg",
            )
            tags(PoseTag.CASUAL, PoseTag.SLIMMING, PoseTag.HOME, PoseTag.PARTY)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Wipe the mirror and turn on the room lights before shooting.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = -20f
                headYaw = -10f
                headPitch = -8f
                hipTilt = 5f
                shoulderTilt = -3f
                leftArm(upper = 11f, fore = -131f, hand = -175f, upperLen = 0.88f)
                rightArm(upper = -38f, fore = 55f, hand = 70f)
                rightLeg(thigh = 4f, shin = 1f)
                leftLeg(thigh = -6f, shin = 10f)
            }
        },
        pose("selfie_mirror_two_hands", "Two-Hand Mirror", PoseCategory.SELFIE) {
            description = "Holding the phone with both hands in front of the chest keeps the arms neat and close, " +
                "while a popped knee adds a cute, casual touch."
            tips(
                "Hold the phone with both hands in front of your chest",
                "Keep your elbows close to your sides",
                "Turn your front knee inward and rest on your back leg",
                "Look down at the screen, then let your eyes come up to the lens",
            )
            tags(PoseTag.CASUAL, PoseTag.PLAYFUL, PoseTag.HOME, PoseTag.CITY)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Keep the phone vertical so the mirror frame stays straight.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 15f
                headTilt = 4f
                headPitch = -10f
                hipTilt = -5f
                leftArm(upper = -2f, fore = -148f, hand = -165f, upperLen = 0.92f)
                rightArm(upper = -1f, fore = 144f, hand = 165f, upperLen = 0.92f)
                leftLeg(thigh = -4f, shin = -1f)
                rightLeg(thigh = 8f, shin = -10f)
            }
        },
        pose("selfie_look_up", "Sky Gaze Selfie", PoseCategory.SELFIE) {
            description = "Looking up past the phone toward the open sky gives a carefree, dreamy, " +
                "summery vibe – no forced smile needed."
            tips(
                "Hold the phone out at eye level",
                "Rest the fingertips of your free hand lightly on your collarbone",
                "Tilt your head back a little and look up at the sky",
                "Let your lips part in a relaxed smile",
            )
            tags(PoseTag.CANDID, PoseTag.COOL, PoseTag.BEACH, PoseTag.TRAVEL, PoseTag.NATURE)
            camera(CameraHeight.EYE, ShotType.CLOSE_UP, note = "Use the open sky as a clean background.")
            difficulty = 2
            figure(cx = 0.366f, top = 0.1f, bodyHeight = 1.8f) {
                facing = 20f
                headYaw = 30f
                headPitch = 18f
                headTilt = -6f
                leftArm(upper = 20f, fore = -140f, hand = -120f, upperLen = 0.85f)
                rightArm(upper = -55f, fore = -62f, hand = -66f, upperLen = 0.5f, foreLen = 0.45f)
            }
        },
        pose("selfie_lean_in", "Lean-In Selfie", PoseCategory.SELFIE) {
            description = "Leaning the upper body toward the phone with a hand on the hip adds energy and shows a " +
                "little more of the outfit than a close-up."
            tips(
                "Hold the phone out and a little low to one side",
                "Lean your upper body toward the phone",
                "Put your free hand on your hip, elbow out",
                "Tilt your head the same way you lean",
            )
            tags(PoseTag.PLAYFUL, PoseTag.CONFIDENT, PoseTag.CITY, PoseTag.PARTY)
            camera(CameraHeight.CHEST, ShotType.HALF_BODY, note = "Use the 0.5x lens or step back to fit the hip in.")
            difficulty = 2
            figure(cx = 0.41f, top = 0.1f, bodyHeight = 1.65f) {
                facing = 15f
                torsoLean = -10f
                headTilt = -6f
                headPitch = -4f
                rightArm(upper = -60f, fore = -68f, hand = -72f, upperLen = 0.55f, foreLen = 0.45f)
                leftArm(upper = 28f, fore = -50f, hand = -65f)
            }
        },
    )
}
