package com.poseit.app.pose.library

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.dsl.pose

/**
 * Single-person squatting, kneeling, floor-sitting and lying poses. Lying figures are shown from the side and
 * scaled down so the whole body still fits the portrait frame; the overhead pose is shot from directly above.
 */
object FloorPoses {
    val all: List<PoseTemplate> = listOf(
        pose("floor_street_squat", "Street Squat", PoseCategory.FLOOR) {
            description = "The streetwear staple: a low squat with knees wide and forearms on the knees looks " +
                "grounded, bold and effortlessly cool."
            tips(
                "Squat down low with your feet a little wider than your hips",
                "Let your knees open out to the sides",
                "Rest your forearms on your knees and let your hands hang",
                "Keep your back long and look straight into the lens",
            )
            tags(PoseTag.COOL, PoseTag.CONFIDENT, PoseTag.CITY, PoseTag.WALL)
            camera(CameraHeight.KNEE, ShotType.FULL_BODY, note = "Get down to knee height so the subject isn't swallowed by the frame.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.9f, bodyHeight = 1.05f) {
                facing = 15f
                torsoLean = 4f
                headTilt = 4f
                leftLeg(thigh = 125f, shin = 8f, thighLen = 0.62f)
                rightLeg(thigh = -128f, shin = -8f, thighLen = 0.62f)
                leftArm(upper = 18f, fore = -35f, hand = -20f, upperLen = 0.8f)
                rightArm(upper = -20f, fore = 35f, hand = 20f, upperLen = 0.8f)
            }
        },
        pose("floor_low_crouch", "Low Crouch", PoseCategory.FLOOR) {
            description = "Crouched low on the balls of the feet with the knees together and the forearms " +
                "resting on them: compact, sporty and full of attitude."
            tips(
                "Crouch down onto the balls of your feet, heels lifted",
                "Keep your knees together, pointing forward",
                "Rest your forearms on your knees and let your hands hang",
                "Lift your chin and look into the lens",
            )
            tags(PoseTag.COOL, PoseTag.PLAYFUL, PoseTag.CITY, PoseTag.STAIRS)
            camera(CameraHeight.KNEE, ShotType.FULL_BODY, note = "Shoot at knee height from the side the knees point to.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.9f, bodyHeight = 1.2f) {
                facing = 70f
                torsoLean = 15f
                headTilt = -15f
                headYaw = 20f
                rightLeg(thigh = 125f, shin = -30f, foot = 60f)
                leftLeg(thigh = 128f, shin = -32f, foot = 60f, thighLen = 0.9f)
                rightArm(upper = 55f, fore = 30f, hand = 10f)
                leftArm(upper = 46f, fore = 25f, hand = 10f, upperLen = 0.8f)
            }
        },
        pose("floor_one_knee", "One Knee Down", PoseCategory.FLOOR) {
            description = "Kneeling on one knee with a forearm on the other is strong and steady, and lowers " +
                "the subject without looking small."
            tips(
                "Kneel on your back knee with your front foot flat",
                "Rest your forearm across your front knee",
                "Keep your back straight and chest open",
                "Turn your face toward the camera",
            )
            tags(PoseTag.CONFIDENT, PoseTag.COOL, PoseTag.NATURE, PoseTag.CITY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot from the side of the front knee for a clean silhouette.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.9f, bodyHeight = 0.98f) {
                facing = 55f
                torsoLean = 5f
                headYaw = 20f
                headTilt = -4f
                rightLeg(thigh = 95f, shin = 3f)
                leftLeg(thigh = -5f, shin = -95f, foot = -120f)
                rightArm(upper = 22f, fore = 85f, hand = 60f)
                leftArm(upper = 12f, fore = 50f)
            }
        },
        pose("floor_cross_legged", "Cross-Legged", PoseCategory.FLOOR) {
            description = "Sitting cross-legged on the floor, grass or a blanket is grounded and approachable, " +
                "ideal for picnics and cosy indoor shots."
            tips(
                "Sit cross-legged with your shins crossed in front of you",
                "Sit up tall, as if a string pulls the top of your head",
                "Rest your hands on your knees with relaxed fingers",
                "Tilt your head slightly and smile",
            )
            tags(PoseTag.CASUAL, PoseTag.CANDID, PoseTag.NATURE, PoseTag.HOME)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Shoot from just above the subject's eye level, angled slightly down.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.86f, bodyHeight = 1.0f) {
                facing = 10f
                headTilt = 6f
                leftLeg(thigh = 72f, shin = -80f, foot = -95f, thighLen = 0.65f)
                rightLeg(thigh = -72f, shin = 80f, foot = 95f, thighLen = 0.65f)
                leftArm(upper = 18f, fore = 25f, hand = 30f)
                rightArm(upper = -18f, fore = -25f, hand = -30f)
            }
        },
        pose("floor_side_lie", "Side Lie on Elbow", PoseCategory.FLOOR) {
            description = "Lying on your side propped on one elbow, head in hand, top knee bent: relaxed, " +
                "flattering curves for the beach, a picnic blanket or a bed."
            tips(
                "Lie on your side facing the camera and prop yourself up on your elbow",
                "Rest your head lightly in that hand",
                "Bend your top knee and place that foot in front of your lower knee",
                "Rest your top hand on your hip",
            )
            tags(PoseTag.ROMANTIC, PoseTag.SLIMMING, PoseTag.BEACH, PoseTag.HOME)
            camera(CameraHeight.GROUND, ShotType.FULL_BODY, note = "Get the phone almost on the ground and shoot along the body.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.78f, bodyHeight = 0.64f) {
                facing = 0f
                torsoLean = 55f
                headTilt = 15f
                leftArm(upper = 25f, fore = 175f, hand = 180f)
                rightArm(upper = -44f, fore = -60f, hand = -70f)
                leftLeg(thigh = -87f, shin = -88f, foot = -90f)
                rightLeg(thigh = -110f, shin = -20f, foot = -100f)
            }
        },
        pose("floor_feet_up", "Feet in the Air", PoseCategory.FLOOR) {
            description = "Lying on your stomach, propped on your elbows with your feet kicked up behind you: " +
                "a sweet, playful classic for the beach or the bed."
            tips(
                "Lie on your stomach and prop yourself up on your elbows",
                "Rest your chin in your hands",
                "Bend your knees and kick your feet up behind you",
                "Smile at the camera",
            )
            tags(PoseTag.PLAYFUL, PoseTag.ROMANTIC, PoseTag.BEACH, PoseTag.HOME)
            camera(CameraHeight.GROUND, ShotType.FULL_BODY, note = "Lie down yourself and shoot from the subject's eye level.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.78f, bodyHeight = 0.8f) {
                facing = 90f
                torsoLean = 62f
                headYaw = 30f
                headTilt = -52f
                rightArm(upper = 10f, fore = 180f, hand = 180f)
                leftArm(upper = 14f, fore = 176f, hand = 178f)
                rightLeg(thigh = -87f, shin = 150f, foot = 170f)
                leftLeg(thigh = -88f, shin = 168f, foot = -170f)
            }
        },
        pose("floor_lean_back", "Lean Back & Relax", PoseCategory.FLOOR) {
            description = "Sitting with the legs out and leaning back on straight arms looks sunny and carefree, " +
                "like the best moment of a lazy afternoon."
            tips(
                "Sit with your legs stretched out in front of you",
                "Lean back onto your hands placed behind your hips",
                "Bend your near knee and keep the other leg long",
                "Tip your face up toward the light",
            )
            tags(PoseTag.CASUAL, PoseTag.CANDID, PoseTag.BEACH, PoseTag.NATURE)
            camera(CameraHeight.KNEE, ShotType.FULL_BODY, note = "Shoot from the feet end at a slight angle so the legs look long.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.8f, bodyHeight = 0.66f) {
                facing = 60f
                torsoLean = -28f
                headTilt = 20f
                headYaw = 20f
                headPitch = 8f
                leftArm(upper = -30f, fore = -32f, hand = -60f)
                rightArm(upper = -22f, fore = -25f, hand = -60f)
                leftLeg(thigh = 86f, shin = 88f, foot = 160f)
                rightLeg(thigh = 128f, shin = 38f)
            }
        },
        pose("floor_overhead_lie", "Overhead Dreamer", PoseCategory.FLOOR) {
            description = "Lying on your back on grass, sand or a bed, shot from straight above: hair spread out " +
                "and arms framing the face make a dreamy, magazine-style image."
            tips(
                "Lie on your back and spread your hair out around your head",
                "Rest one hand on top of your head and let the other arm lie out to the side",
                "Let one knee fall gently out to the side",
                "Look straight up at the camera",
            )
            tags(PoseTag.ROMANTIC, PoseTag.ELEGANT, PoseTag.NATURE, PoseTag.HOME)
            camera(CameraHeight.HIGH, ShotType.FULL_BODY, note = "Stand over the subject (safely on a chair if needed) and shoot straight down.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.82f) {
                facing = 0f
                headTilt = 14f
                leftArm(upper = 150f, fore = -110f, hand = -125f)
                rightArm(upper = -40f, fore = -20f, hand = -10f)
                leftLeg(thigh = 25f, shin = -15f)
                rightLeg(thigh = -4f, shin = -2f)
            }
        },
        pose("floor_sit_on_heels", "Sit on Your Heels", PoseCategory.FLOOR) {
            description = "Kneeling and sitting back on the heels with a straight back is graceful and calm, " +
                "and works beautifully in dresses and on a beach."
            tips(
                "Kneel down and sit back onto your heels",
                "Keep your back straight and shoulders relaxed",
                "Rest your hands on your thighs",
                "Tilt your head slightly and soften your eyes",
            )
            tags(PoseTag.ELEGANT, PoseTag.ROMANTIC, PoseTag.BEACH, PoseTag.HOME)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot from a three-quarter angle at the subject's chest height.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.86f, bodyHeight = 1.0f) {
                facing = 45f
                headTilt = -6f
                headYaw = 25f
                leftLeg(thigh = 80f, shin = -95f, foot = -100f, thighLen = 0.85f)
                rightLeg(thigh = 84f, shin = -92f, foot = -100f, thighLen = 0.9f)
                leftArm(upper = 10f, fore = 60f, hand = 80f)
                rightArm(upper = 16f, fore = 64f, hand = 80f)
            }
        },
    )
}
