package com.poseit.app.pose.library

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.dsl.pose

/**
 * Two-person poses. The first figure is the taller partner, usually on the image left facing right (positive
 * `facing`); the second is the shorter partner facing left. Feet share one ground line, so different body
 * heights produce realistic height differences.
 *
 * Contact between the figures (hugs, held hands) is approximate: the 2D mannequins cannot express which limb is
 * in front, so arms that wrap around a partner simply cross the partner's silhouette.
 */
object CouplePoses {
    val all: List<PoseTemplate> = listOf(
        pose("couple_side_hug", "Side hug", PoseCategory.COUPLE) {
            description = "Standing side by side with an arm around each other is relaxed, warm and works for " +
                "every kind of couple. The go-to for travel and event photos."
            tips(
                "Stand hip to hip, both turned slightly toward each other",
                "Taller partner: wrap your arm around your partner's shoulders",
                "Shorter partner: slide your arm around their waist",
                "Lean your heads a little toward each other",
            )
            tags(PoseTag.CASUAL, PoseTag.ROMANTIC, PoseTag.TRAVEL, PoseTag.CITY, PoseTag.PARTY)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Centre the pair and leave space on both sides.")
            difficulty = 1
            figure(cx = 0.46f, bottom = 0.95f, bodyHeight = 0.86f) {
                facing = 20f
                headYaw = 5f
                headTilt = 8f
                hipTilt = -4f
                leftArm(upper = 85f, fore = 30f, hand = 15f, upperLen = 0.85f, foreLen = 0.45f)
                rightArm(upper = -10f, fore = 28f)
                leftLeg(thigh = -3f, shin = 0f)
                rightLeg(thigh = 8f, shin = -6f)
            }
            figure(cx = 0.55f, bottom = 0.95f, bodyHeight = 0.8f) {
                facing = -20f
                headYaw = -5f
                headTilt = -10f
                hipTilt = 4f
                rightArm(upper = -30f, fore = -85f, hand = -90f, foreLen = 0.6f)
                leftArm(upper = 38f, fore = -55f, hand = -70f)
                rightLeg(thigh = 3f, shin = 0f)
                leftLeg(thigh = -8f, shin = 6f)
            }
        },
        pose("couple_forehead_touch", "Forehead touch", PoseCategory.COUPLE) {
            description = "Foreheads together, eyes closed or looking down: one of the most intimate and timeless " +
                "couple shots, perfect at golden hour."
            tips(
                "Face each other and step in close",
                "Taller partner: hold your partner's waist and lean your head down",
                "Shorter partner: rest your hands on their chest",
                "Touch foreheads gently and close your eyes or smile",
            )
            tags(PoseTag.ROMANTIC, PoseTag.ELEGANT, PoseTag.NATURE, PoseTag.BEACH, PoseTag.CITY)
            camera(CameraHeight.CHEST, ShotType.THREE_QUARTER, note = "Shoot from the side so both profiles are visible.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 1.2f, bodyHeight = 1.1f) {
                facing = 70f
                torsoLean = 10f
                headTilt = 20f
                headPitch = -15f
                rightArm(upper = 45f, fore = 55f)
                leftArm(upper = 50f, fore = 62f)
                rightLeg(thigh = 10f, shin = 0f)
                leftLeg(thigh = -5f, shin = -2f)
            }
            figure(cx = 0.48f, bottom = 1.2f, bodyHeight = 1.03f) {
                facing = -70f
                torsoLean = -6f
                headTilt = -14f
                headPitch = 5f
                rightArm(upper = -19f, fore = -138f, hand = -120f, upperLen = 0.92f)
                leftArm(upper = -25f, fore = -125f, hand = -110f, upperLen = 0.93f)
                leftLeg(thigh = -8f, shin = 0f)
                rightLeg(thigh = 5f, shin = 0f)
            }
        },
        pose("couple_back_hug", "Back hug", PoseCategory.COUPLE) {
            description = "One partner hugs the other from behind with their cheek beside the partner's head. Cosy, " +
                "protective and full of smiles."
            tips(
                "Front partner: face the camera and relax into the hug",
                "Back partner: wrap your arms around their waist from behind",
                "Bring your head down next to your partner's head",
                "Front partner: rest your hands on their forearms",
            )
            tags(PoseTag.ROMANTIC, PoseTag.CASUAL, PoseTag.HOME, PoseTag.NATURE, PoseTag.BEACH)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Focus on the front partner's eyes.")
            difficulty = 1
            // Back partner first so the front partner is drawn on top.
            figure(cx = 0.43f, bottom = 0.95f, bodyHeight = 0.86f) {
                facing = 15f
                torsoLean = 6f
                headTilt = 10f
                headYaw = 20f
                headPitch = -5f
                leftArm(upper = 20f, fore = -82f, hand = -88f, upperLen = 0.95f)
                rightArm(upper = -10f, fore = 84f, hand = 88f, upperLen = 0.85f, foreLen = 0.97f)
                leftLeg(thigh = 4f, shin = 1f)
                rightLeg(thigh = -6f, shin = -2f)
            }
            figure(cx = 0.55f, bottom = 0.95f, bodyHeight = 0.8f) {
                facing = 10f
                headTilt = -8f
                headPitch = -3f
                leftArm(upper = 8f, fore = -78f, hand = -85f)
                rightArm(upper = -10f, fore = 72f, hand = 80f)
                leftLeg(thigh = -4f, shin = 6f)
                rightLeg(thigh = 3f, shin = 0f)
            }
        },
        pose("couple_walk_hands", "Hand-in-hand walk", PoseCategory.COUPLE) {
            description = "Walking side-on while holding hands, one partner glancing back: natural movement that " +
                "hides camera nerves. Great on beaches, bridges and streets."
            tips(
                "Walk slowly across the frame holding hands",
                "Front partner: look back over your shoulder at your partner",
                "Take small, relaxed steps and let your free arms swing",
                "Walk past the camera several times and pick the best step",
            )
            tags(PoseTag.CANDID, PoseTag.ROMANTIC, PoseTag.BEACH, PoseTag.CITY, PoseTag.TRAVEL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot side-on in burst mode as they walk past.")
            difficulty = 1
            figure(cx = 0.33f, bottom = 0.95f, bodyHeight = 0.86f) {
                facing = 75f
                headYaw = 80f
                headPitch = -5f
                leftArm(upper = 15f, fore = 25f)
                rightArm(upper = -22f, fore = -12f)
                rightLeg(thigh = 20f, shin = 5f)
                leftLeg(thigh = -18f, shin = -30f)
            }
            figure(cx = 0.67f, bottom = 0.95f, bodyHeight = 0.8f) {
                facing = 75f
                headYaw = 15f
                headTilt = -5f
                rightArm(upper = -15f, fore = -25f)
                leftArm(upper = 18f, fore = 35f)
                rightLeg(thigh = 20f, shin = 5f)
                leftLeg(thigh = -18f, shin = -30f)
            }
        },
        pose("couple_piggyback", "Piggyback", PoseCategory.COUPLE) {
            description = "A piggyback ride is playful and full of laughter. The rider peeks over the carrier's " +
                "shoulder so both faces are in the frame."
            tips(
                "Carrier: bend your knees slightly and hold your partner's legs at your hips",
                "Rider: wrap your arms loosely around your partner's shoulders",
                "Rider: rest your head beside your partner's head",
                "Both look at the camera and laugh",
            )
            tags(PoseTag.PLAYFUL, PoseTag.CANDID, PoseTag.BEACH, PoseTag.NATURE, PoseTag.CITY)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Take the photo quickly. Piggybacks are tiring.")
            difficulty = 2
            // Rider first (behind), carrier second (in front). The rider leans so the head peeks over a shoulder.
            figure(cx = 0.47f, top = 0.05f, bodyHeight = 0.78f) {
                facing = 15f
                torsoLean = 16f
                headYaw = 5f
                headTilt = -14f
                leftArm(upper = -10f, fore = -70f, upperLen = 0.65f)
                rightArm(upper = -40f, fore = 75f, upperLen = 0.65f)
                leftLeg(thigh = 72f, shin = 10f, thighLen = 0.55f, shinLen = 0.9f)
                rightLeg(thigh = -72f, shin = -10f, thighLen = 0.55f, shinLen = 0.9f)
            }
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 15f
                torsoLength = 0.94f
                headYaw = 10f
                headPitch = 3f
                leftArm(upper = 30f, fore = 10f, foreLen = 0.5f)
                rightArm(upper = -15f, fore = 10f, foreLen = 0.5f)
                leftLeg(thigh = 6f, shin = -4f)
                rightLeg(thigh = -8f, shin = 4f)
            }
        },
        pose("couple_dip", "Romantic dip", PoseCategory.COUPLE) {
            description = "The classic dance-floor dip: one partner lunges and supports, the other leans back with " +
                "one knee lifted. Dramatic, romantic and great at weddings."
            tips(
                "Leader: step into a lunge and support your partner's upper back firmly",
                "Follower: hold on around their neck and lean back slowly",
                "Follower: bend your front knee and point your toes to the floor",
                "Look into each other's eyes",
            )
            tags(PoseTag.ROMANTIC, PoseTag.ELEGANT, PoseTag.PARTY, PoseTag.CITY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, landscape = true, note = "Shoot from the side, slightly low.")
            difficulty = 3
            figure(cx = 0.44f, bottom = 0.93f, bodyHeight = 0.86f) {
                facing = 70f
                torsoLean = 30f
                headTilt = 10f
                headPitch = -20f
                rightLeg(thigh = 55f, shin = -8f)
                leftLeg(thigh = -40f, shin = -42f)
                rightArm(upper = 45f, fore = 50f)
                leftArm(upper = 15f, fore = 50f, foreLen = 0.6f)
            }
            figure(cx = 0.51f, bottom = 0.93f, bodyHeight = 0.8f) {
                facing = -70f
                torsoLean = 50f
                headTilt = 15f
                rightLeg(thigh = 50f, shin = -20f)
                leftLeg(thigh = -65f, shin = -15f, foot = -40f)
                rightArm(upper = -120f, fore = -85f, foreLen = 0.7f)
                leftArm(upper = 40f, fore = 30f)
            }
        },
        pose("couple_hold_hands_facing", "Face to face", PoseCategory.COUPLE) {
            description = "Facing each other while holding both hands is simple and sweet. The gap between you " +
                "frames the background, lovely for proposals and anniversaries."
            tips(
                "Stand facing each other about an arm's length apart",
                "Hold both of your partner's hands at waist height",
                "Keep your elbows soft and relaxed",
                "Look at each other and laugh",
            )
            tags(PoseTag.ROMANTIC, PoseTag.CASUAL, PoseTag.NATURE, PoseTag.BEACH, PoseTag.TRAVEL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot side-on and centre the gap between the couple.")
            difficulty = 1
            figure(cx = 0.36f, bottom = 0.95f, bodyHeight = 0.86f) {
                facing = 55f
                headYaw = 70f
                headPitch = -5f
                rightArm(upper = 40f, fore = 62f)
                leftArm(upper = 15f, fore = 40f)
                leftLeg(thigh = -3f, shin = 0f)
                rightLeg(thigh = 10f, shin = -4f)
            }
            figure(cx = 0.65f, bottom = 0.95f, bodyHeight = 0.8f) {
                facing = -55f
                headYaw = -70f
                headPitch = 5f
                leftArm(upper = -40f, fore = -62f)
                rightArm(upper = -15f, fore = -40f)
                rightLeg(thigh = 3f, shin = 0f)
                leftLeg(thigh = -10f, shin = 4f)
            }
        },
        pose("couple_sit_lean", "Sitting close", PoseCategory.COUPLE) {
            description = "Sitting side by side with one head resting on the other's shoulder feels calm and " +
                "content. Use a bench, low wall or steps."
            tips(
                "Sit close together on a bench or step",
                "Taller partner: put your arm around your partner's shoulders",
                "Shorter partner: lean in and rest your head on their shoulder",
                "Keep your knees angled slightly toward each other",
            )
            tags(PoseTag.ROMANTIC, PoseTag.CASUAL, PoseTag.NATURE, PoseTag.STAIRS, PoseTag.HOME)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Get down to the couple's eye level.")
            difficulty = 1
            figure(cx = 0.45f, bottom = 0.93f, bodyHeight = 0.9f) {
                facing = 15f
                headTilt = 8f
                headYaw = 10f
                leftLeg(thigh = 15f, shin = 2f, thighLen = 0.45f)
                rightLeg(thigh = -12f, shin = -4f, thighLen = 0.45f)
                leftArm(upper = 95f, fore = 20f, hand = 10f, upperLen = 0.9f, foreLen = 0.5f)
                rightArm(upper = -4f, fore = 10f, hand = 15f)
            }
            figure(cx = 0.548f, bottom = 0.93f, bodyHeight = 0.84f) {
                facing = -15f
                torsoLean = -10f
                headTilt = -24f
                headYaw = -10f
                leftLeg(thigh = 10f, shin = -6f, thighLen = 0.45f)
                rightLeg(thigh = -14f, shin = 0f, thighLen = 0.45f)
                leftArm(upper = 5f, fore = -40f)
                rightArm(upper = -5f, fore = 30f)
            }
        },
        pose("couple_walk_toward", "Walk and chat", PoseCategory.COUPLE) {
            description = "Walking toward the camera hand in hand while chatting looks effortless and alive. The " +
                "photographer simply walks backwards and keeps shooting."
            tips(
                "Hold hands and walk slowly toward the camera",
                "Turn your heads and talk or laugh with each other",
                "Take small steps and let your free arms swing naturally",
            )
            tags(PoseTag.CANDID, PoseTag.CASUAL, PoseTag.CITY, PoseTag.NATURE, PoseTag.TRAVEL)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Walk backwards with them and shoot in burst mode.")
            difficulty = 1
            figure(cx = 0.33f, bottom = 0.95f, bodyHeight = 0.86f) {
                facing = 10f
                headYaw = 35f
                headTilt = 4f
                leftArm(upper = 10f, fore = 8f)
                rightArm(upper = -12f, fore = -2f, upperLen = 0.95f)
                rightLeg(thigh = -4f, shin = 3f)
                leftLeg(thigh = 3f, shin = 8f, thighLen = 0.97f, shinLen = 0.86f)
            }
            figure(cx = 0.67f, bottom = 0.95f, bodyHeight = 0.8f) {
                facing = -10f
                headYaw = -35f
                headTilt = -4f
                rightArm(upper = -10f, fore = -8f)
                leftArm(upper = 10f, fore = 18f)
                leftLeg(thigh = 4f, shin = -3f)
                rightLeg(thigh = -3f, shin = -8f, thighLen = 0.97f, shinLen = 0.86f)
            }
        },
        pose("couple_cheek_kiss", "Cheek kiss", PoseCategory.COUPLE) {
            description = "One partner plants a kiss on the other's cheek while they smile at the camera. Sweet, " +
                "fun and easy for camera-shy couples."
            tips(
                "Kissing partner: turn toward your partner and lean in to their cheek",
                "Hold your partner's waist with both hands",
                "Other partner: face the camera and smile big",
                "Lean your head slightly toward the kiss",
            )
            tags(PoseTag.ROMANTIC, PoseTag.PLAYFUL, PoseTag.CITY, PoseTag.PARTY, PoseTag.TRAVEL)
            camera(CameraHeight.CHEST, ShotType.THREE_QUARTER, note = "Focus on the smiling partner's eyes.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 1.2f, bodyHeight = 1.1f) {
                facing = 60f
                torsoLean = 15f
                headTilt = 12f
                headPitch = -10f
                leftArm(upper = 45f, fore = 75f, foreLen = 0.7f)
                rightArm(upper = 30f, fore = 60f)
                leftLeg(thigh = 8f, shin = 0f)
                rightLeg(thigh = -6f, shin = -3f)
            }
            figure(cx = 0.472f, bottom = 1.2f, bodyHeight = 1.03f) {
                facing = -10f
                headTilt = -6f
                headYaw = -5f
                rightArm(upper = -30f, fore = -80f, foreLen = 0.5f)
                leftArm(upper = 14f, fore = -50f, hand = -60f)
                leftLeg(thigh = -6f, shin = 6f)
                rightLeg(thigh = 3f, shin = 0f)
            }
        },
    )
}
