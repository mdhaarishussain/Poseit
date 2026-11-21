package com.poseit.app.pose.library

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.dsl.pose

/**
 * Single-person walking and movement poses. Each one freezes a natural moment of a stride, so the subject
 * can simply walk and the photographer shoots in burst mode.
 */
object WalkingPoses {
    val all: List<PoseTemplate> = listOf(
        pose("walk_toward_camera", "Walk toward the camera", PoseCategory.WALKING) {
            description = "Walking straight at the camera with a confident stride is the easiest way to look " +
                "natural: the movement takes care of the pose."
            tips(
                "Walk slowly toward the camera, placing one foot in front of the other",
                "Let your arms swing naturally with soft elbows",
                "Keep your chin slightly down and look past the lens",
            )
            tags(PoseTag.CONFIDENT, PoseTag.CANDID, PoseTag.CITY, PoseTag.TRAVEL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Use burst mode and pick the frame where the front heel touches down.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 20f
                hipTilt = -3f
                headPitch = -4f
                headYaw = 10f
                leftLeg(thigh = 9f, shin = 3f)
                rightLeg(thigh = -6f, shin = -18f, foot = 20f, shinLen = 0.8f)
                rightArm(upper = -4f, fore = 32f, foreLen = 0.8f)
                leftArm(upper = 16f, fore = 10f)
            }
        },
        pose("walk_across", "Walk across", PoseCategory.WALKING) {
            description = "A side-on stride across the frame shows the full length of the step and the outfit's " +
                "silhouette. Turning the face to the camera adds connection."
            tips(
                "Walk across in front of the camera with long, relaxed steps",
                "Swing your arms opposite to your legs",
                "Turn your face toward the camera without stopping",
            )
            tags(PoseTag.CANDID, PoseTag.CASUAL, PoseTag.CITY, PoseTag.TRAVEL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Leave space in front of the subject in the walking direction.")
            difficulty = 1
            figure(cx = 0.47f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 85f
                headYaw = 35f
                rightLeg(thigh = 22f, shin = 8f, foot = 100f)
                leftLeg(thigh = -14f, shin = -38f, foot = 55f)
                rightArm(upper = -28f, fore = -15f)
                leftArm(upper = 22f, fore = 50f)
            }
        },
        pose("walk_look_back", "Walk away, look back", PoseCategory.WALKING) {
            description = "Walking away and glancing back over the shoulder feels spontaneous and a little " +
                "mysterious, and shows the back of the outfit."
            tips(
                "Walk away from the camera at an easy pace",
                "Glance back over your shoulder mid-step",
                "Keep your shoulders relaxed and your arms swinging",
            )
            tags(PoseTag.CANDID, PoseTag.ROMANTIC, PoseTag.TRAVEL, PoseTag.BEACH)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Call the subject's name to catch a genuine look back.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 165f
                headYaw = 80f
                headTilt = 4f
                hipTilt = 3f
                leftLeg(thigh = 4f, shin = 2f)
                rightLeg(thigh = -4f, shin = 12f, foot = 5f, shinLen = 0.78f)
                rightArm(upper = 8f, fore = 25f, upperLen = 0.9f, foreLen = 0.7f)
                leftArm(upper = -14f, fore = -10f)
            }
        },
        pose("walk_kerb_step", "Off the kerb", PoseCategory.WALKING) {
            description = "Stepping down from a kerb or step freezes a dynamic, street-style moment with " +
                "long lines from the hip to the pointed toe."
            tips(
                "Stand on the edge of a kerb or step",
                "Reach your front foot down to the road, toe first",
                "Let the back knee bend and swing the opposite arm forward",
                "Look ahead, not at your feet",
            )
            tags(PoseTag.COOL, PoseTag.CANDID, PoseTag.CITY, PoseTag.STAIRS)
            camera(CameraHeight.KNEE, ShotType.FULL_BODY, note = "A low angle exaggerates the step down and lengthens the legs.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 25f
                hipTilt = -5f
                shoulderTilt = 2f
                headPitch = -3f
                leftLeg(thigh = 5f, shin = -6f, thighLen = 0.85f, shinLen = 0.9f)
                rightLeg(thigh = -7f, shin = -3f, foot = 8f)
                leftArm(upper = 22f, fore = 40f, foreLen = 0.8f)
                rightArm(upper = -20f, fore = -12f)
            }
        },
        pose("walk_skip", "Happy skip", PoseCategory.WALKING) {
            description = "A playful skip with one knee up and an arm swinging high is pure joy, and almost " +
                "impossible to do without a real smile."
            tips(
                "Skip forward and lift one knee high",
                "Swing the opposite arm up in front of you",
                "Push off from the toes of your standing foot",
                "Laugh and throw your head back slightly",
            )
            tags(PoseTag.PLAYFUL, PoseTag.CANDID, PoseTag.BEACH, PoseTag.NATURE)
            camera(CameraHeight.KNEE, ShotType.FULL_BODY, note = "Shoot in burst mode and pick the frame at the top of the skip.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.78f) {
                facing = 20f
                headPitch = 10f
                leftLeg(thigh = 62f, shin = -8f, foot = 30f, thighLen = 0.8f)
                rightLeg(thigh = -4f, shin = -2f, foot = 15f)
                rightArm(upper = -135f, fore = -150f, hand = -155f)
                leftArm(upper = 35f, fore = 50f)
            }
        },
        pose("walk_hat_hold", "Hat in the wind", PoseCategory.WALKING) {
            description = "Holding your hat on while you walk gives the hands a reason to be up and adds a " +
                "breezy, cinematic feel."
            tips(
                "Walk at an easy pace across the frame",
                "Hold the top of your hat with your near hand, elbow up",
                "Let your other arm swing naturally",
                "Look down and away with a little smile",
            )
            tags(PoseTag.CANDID, PoseTag.ELEGANT, PoseTag.BEACH, PoseTag.TRAVEL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot with the wind or the light behind the subject.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.82f) {
                facing = -30f
                headPitch = -12f
                headTilt = -4f
                leftArm(upper = 140f, fore = -100f, hand = -115f)
                rightArm(upper = -10f, fore = -5f)
                rightLeg(thigh = -18f, shin = -8f, foot = -100f)
                leftLeg(thigh = 14f, shin = 32f, foot = -40f)
            }
        },
        pose("walk_pockets", "Pocket stroll", PoseCategory.WALKING) {
            description = "Strolling with hands in pockets and eyes on the ground is the most natural candid " +
                "there is: relaxed, thoughtful and unposed."
            tips(
                "Put your hands in your pockets, thumbs out",
                "Take small, slow steps",
                "Look down at the ground a few steps ahead and smile",
            )
            tags(PoseTag.CASUAL, PoseTag.CANDID, PoseTag.CITY, PoseTag.NATURE)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Ask the subject to walk back and forth while you shoot in bursts.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 25f
                headPitch = -18f
                leftArm(upper = 14f, fore = -28f, hand = -12f)
                rightArm(upper = -16f, fore = 28f, hand = 12f)
                leftLeg(thigh = 8f, shin = 3f)
                rightLeg(thigh = -6f, shin = -20f, foot = 20f)
            }
        },
        pose("walk_twirl", "Arms-out spin", PoseCategory.WALKING) {
            description = "Spinning with the arms out makes skirts and coats flare and brings out genuine " +
                "laughter. Catch the moment mid-turn."
            tips(
                "Spin slowly on the spot with your arms open",
                "Kick one foot up behind you as you turn",
                "Tilt your head back and laugh",
            )
            tags(PoseTag.PLAYFUL, PoseTag.ROMANTIC, PoseTag.NATURE, PoseTag.BEACH)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Use a fast shutter or burst mode to freeze the spin.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.76f) {
                facing = 30f
                headPitch = 8f
                headTilt = -6f
                leftArm(upper = 105f, fore = 125f, hand = 135f)
                rightArm(upper = -95f, fore = -75f, hand = -60f)
                leftLeg(thigh = -5f, shin = -70f, foot = -60f)
                rightLeg(thigh = 3f, shin = 0f, foot = 20f)
            }
        },
        pose("walk_coffee_run", "Coffee run", PoseCategory.WALKING) {
            description = "Walking with a coffee in hand gives the arms something to do and tells a little " +
                "everyday story. A street-style essential."
            tips(
                "Hold your cup at chest height in your far hand",
                "Walk at a relaxed pace and let the other arm swing",
                "Glance down at your cup or off to the side",
            )
            tags(PoseTag.CASUAL, PoseTag.CANDID, PoseTag.CITY, PoseTag.CAFE)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot a little ahead of the subject as they walk toward you.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = -30f
                headPitch = -8f
                headYaw = -45f
                rightArm(upper = -10f, fore = 140f, hand = 160f, upperLen = 0.85f, foreLen = 0.89f)
                leftArm(upper = -10f, fore = -30f)
                rightLeg(thigh = -16f, shin = -6f, foot = -100f)
                leftLeg(thigh = 10f, shin = 30f, foot = -40f)
            }
        },
        pose("walk_jacket_shoulder", "Jacket over shoulder", PoseCategory.WALKING) {
            description = "A jacket hooked over the shoulder on one finger is effortlessly cool and frames " +
                "the upper body."
            tips(
                "Hook your jacket on your fingers and sling it over your shoulder",
                "Keep your other hand in your pocket",
                "Walk slowly with a relaxed stride",
                "Look off to the side with a slight smile",
            )
            tags(PoseTag.COOL, PoseTag.CONFIDENT, PoseTag.CITY, PoseTag.TRAVEL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "A slightly low angle adds swagger.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 20f
                headYaw = 50f
                rightArm(upper = -25f, fore = 164f, hand = 175f)
                leftArm(upper = 14f, fore = -28f, hand = -12f)
                leftLeg(thigh = 8f, shin = 3f)
                rightLeg(thigh = -6f, shin = -22f, foot = 25f)
            }
        },
    )
}
