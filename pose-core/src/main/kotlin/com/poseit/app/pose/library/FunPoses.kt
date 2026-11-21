package com.poseit.app.pose.library

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.dsl.pose

/**
 * Energetic, playful poses: jumps, spins, kicks and celebrations. Jumping figures sit a little above the bottom
 * edge to suggest air under the feet; the photographer should use burst mode.
 */
object FunPoses {
    val all: List<PoseTemplate> = listOf(
        pose("fun_star_jump", "Star jump", PoseCategory.FUN) {
            description = "Arms and legs flung wide in mid-air: pure joy in one frame. Perfect on beaches, fields " +
                "and in front of big landmarks."
            tips(
                "Jump straight up and spread your arms and legs into a big X",
                "Point your toes and stretch your fingers",
                "Lift your chin and smile at the top of the jump",
            )
            tags(PoseTag.PLAYFUL, PoseTag.BEACH, PoseTag.NATURE, PoseTag.TRAVEL)
            camera(CameraHeight.KNEE, ShotType.FULL_BODY, note = "Shoot low in burst mode. A low angle makes the jump look higher.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.92f, bodyHeight = 0.74f) {
                headPitch = 8f
                leftArm(upper = 135f, fore = 142f, hand = 148f)
                rightArm(upper = -135f, fore = -142f, hand = -148f)
                leftLeg(thigh = 22f, shin = 24f, foot = 30f)
                rightLeg(thigh = -22f, shin = -24f, foot = -30f)
            }
        },
        pose("fun_tuck_jump", "Knee-tuck jump", PoseCategory.FUN) {
            description = "Pulling both knees up high mid-jump looks like flying. Turning a little sideways makes " +
                "the tuck easy to read."
            tips(
                "Turn slightly sideways to the camera",
                "Jump and pull both knees up toward your chest",
                "Throw your arms up high",
                "Jump several times: the camera only needs one perfect moment",
            )
            tags(PoseTag.PLAYFUL, PoseTag.BEACH, PoseTag.NATURE, PoseTag.CITY)
            camera(CameraHeight.KNEE, ShotType.FULL_BODY, note = "Use burst mode and a fast shutter so the jump stays sharp.")
            difficulty = 3
            figure(cx = 0.5f, bottom = 0.8f, bodyHeight = 0.8f) {
                facing = 60f
                torsoLean = 8f
                headPitch = 10f
                leftArm(upper = 150f, fore = 160f, hand = 165f)
                rightArm(upper = -165f, fore = -172f, hand = -175f)
                leftLeg(thigh = 120f, shin = -15f, foot = -40f)
                rightLeg(thigh = 105f, shin = -25f, foot = -50f)
            }
        },
        pose("fun_twirl", "Heel-flick twirl", PoseCategory.FUN) {
            description = "A spin with one arm curved overhead and a heel flicked up behind, made for skirts, " +
                "dresses and coats that swing."
            tips(
                "Spin on the spot and let your dress or coat swing out",
                "Curve one arm over your head and stretch the other out low",
                "Flick one heel up behind you",
                "Tilt your head back and laugh",
            )
            tags(PoseTag.PLAYFUL, PoseTag.ELEGANT, PoseTag.NATURE, PoseTag.BEACH, PoseTag.CITY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot a burst while the subject spins and keep the best frame.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.76f) {
                facing = 30f
                torsoLean = -5f
                headTilt = -8f
                headPitch = 10f
                leftArm(upper = 160f, fore = -140f, hand = -110f)
                rightArm(upper = -70f, fore = -80f, hand = -85f)
                leftLeg(thigh = 5f, shin = -60f, foot = -30f)
                rightLeg(thigh = 3f, shin = 0f)
            }
        },
        pose("fun_superhero", "Superhero stance", PoseCategory.FUN) {
            description = "Fists on hips, feet wide and chin up: the ultimate power pose. Instantly confident and " +
                "a little bit funny."
            tips(
                "Plant your feet wider than your shoulders",
                "Put your fists on your hips, elbows pointing out",
                "Push your chest forward and lift your chin",
                "Gaze into the distance like you're saving the day",
            )
            tags(PoseTag.CONFIDENT, PoseTag.PLAYFUL, PoseTag.CITY, PoseTag.TRAVEL, PoseTag.NATURE)
            camera(CameraHeight.KNEE, ShotType.FULL_BODY, note = "Shoot from knee height for a heroic, larger-than-life look.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                headYaw = 8f
                headPitch = 8f
                leftArm(upper = 38f, fore = -55f, hand = -70f)
                rightArm(upper = -38f, fore = 55f, hand = 70f)
                leftLeg(thigh = 14f, shin = 10f)
                rightLeg(thigh = -14f, shin = -10f)
            }
        },
        pose("fun_disco_point", "Disco point", PoseCategory.FUN) {
            description = "One arm pointing high to the sky, the other hand on the hip and the hips popped: a " +
                "retro dance-floor move that is impossible to take too seriously."
            tips(
                "Point one arm straight up and out at an angle",
                "Put your other hand on your hip",
                "Pop your hip out and bend the opposite knee in",
                "Follow your pointing finger with your eyes",
            )
            tags(PoseTag.PLAYFUL, PoseTag.PARTY, PoseTag.CITY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Leave room above the pointing hand.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.76f) {
                facing = 20f
                hipTilt = -6f
                headYaw = 40f
                headPitch = 10f
                leftArm(upper = 145f, fore = 148f, hand = 150f)
                rightArm(upper = -35f, fore = 50f, hand = 65f)
                leftLeg(thigh = -6f, shin = -2f)
                rightLeg(thigh = 18f, shin = -10f)
            }
        },
        pose("fun_laugh_bend", "Laughing bend", PoseCategory.FUN) {
            description = "Doubled over with laughter, one hand on the knee and the other at the mouth: the most " +
                "genuine-looking candid there is. Tell a joke just before the shot."
            tips(
                "Bend forward from the hips with soft knees",
                "Rest one hand on your knee",
                "Bring the other hand up to your mouth as you laugh",
                "Laugh for real: think of something funny",
            )
            tags(PoseTag.CANDID, PoseTag.PLAYFUL, PoseTag.CITY, PoseTag.PARTY, PoseTag.NATURE)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Keep shooting while they laugh. The second laugh is the best.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.86f) {
                facing = 70f
                torsoLean = 55f
                headTilt = -30f
                rightLeg(thigh = 47f, shin = -18f)
                leftLeg(thigh = 46f, shin = -18f)
                rightArm(upper = -8f, fore = -12f, hand = 5f)
                leftArm(upper = 0f, fore = 154f, hand = 170f, upperLen = 0.78f)
            }
        },
        pose("fun_high_kick", "High kick", PoseCategory.FUN) {
            description = "A big front kick with arms flung open is full of energy. Great for sporty outfits, " +
                "trainer shots and travel photos."
            tips(
                "Stand side-on to the camera",
                "Kick your front leg up as high as is comfortable, toes pointed",
                "Throw your arms open for balance",
                "Lean back slightly and keep smiling",
            )
            tags(PoseTag.PLAYFUL, PoseTag.CONFIDENT, PoseTag.BEACH, PoseTag.CITY, PoseTag.NATURE)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot side-on so the kicking leg stays sharp and in profile.")
            difficulty = 3
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.72f) {
                facing = 75f
                torsoLean = -12f
                headPitch = 5f
                rightLeg(thigh = 95f, shin = 92f, foot = 100f)
                leftLeg(thigh = -6f, shin = -2f)
                leftArm(upper = 120f, fore = 130f)
                rightArm(upper = -50f, fore = -20f)
            }
        },
        pose("fun_peace_lean", "Double peace lean", PoseCategory.FUN) {
            description = "Leaning to one side with two peace signs: cheeky, upbeat and great in colourful " +
                "streets or at parties."
            tips(
                "Lean your upper body to one side and push your hips the other way",
                "Make a V next to your face with one hand",
                "Hold the other V out at shoulder height",
                "Tilt your head into the lean and grin",
            )
            tags(PoseTag.PLAYFUL, PoseTag.CASUAL, PoseTag.PARTY, PoseTag.CITY, PoseTag.TRAVEL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Keep both hands fully inside the frame.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.82f) {
                facing = 10f
                torsoLean = -10f
                headTilt = -8f
                headYaw = 5f
                headPitch = 3f
                leftArm(upper = 35f, fore = -146f, hand = 175f, upperLen = 0.71f)
                rightArm(upper = -40f, fore = -170f, hand = 180f)
                leftLeg(thigh = -8f, shin = -2f)
                rightLeg(thigh = -6f, shin = 3f)
            }
        },
        pose("fun_victory", "Victory arms", PoseCategory.FUN) {
            description = "Fists up in a goal-post shape with one knee kicked up: the 'I did it!' moment after a " +
                "hike, a race or a graduation."
            tips(
                "Raise both arms with elbows bent and fists up",
                "Lift one knee up in front of you",
                "Throw your head back a little and cheer",
            )
            tags(PoseTag.PLAYFUL, PoseTag.CONFIDENT, PoseTag.NATURE, PoseTag.TRAVEL, PoseTag.PARTY)
            camera(CameraHeight.KNEE, ShotType.FULL_BODY, note = "A low angle against the sky makes it look triumphant.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.78f) {
                facing = -15f
                headPitch = 12f
                leftArm(upper = 120f, fore = 175f, hand = 178f)
                rightArm(upper = -120f, fore = -175f, hand = -178f)
                leftLeg(thigh = 40f, shin = -10f, thighLen = 0.6f)
                rightLeg(thigh = 5f, shin = 2f)
            }
        },
    )
}
