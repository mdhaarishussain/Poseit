package com.poseit.app.pose.library

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.dsl.pose

/**
 * Friends and group poses for three or four people, mostly in a landscape (4:3) frame.
 *
 * `camera(landscape = true)` must be called BEFORE the figures are added, because each figure is laid out in the
 * frame aspect that is current when [com.poseit.app.pose.dsl.PoseBuilder.figure] runs.
 */
object GroupPoses {
    val all: List<PoseTemplate> = listOf(
        pose("group_arms_around", "Shoulder line-up", PoseCategory.GROUP) {
            description = "The classic friends photo: everyone close together, arms around shoulders and waists. " +
                "Simple, warm and it fits any number of people."
            tips(
                "Stand shoulder to shoulder, the tallest person in the middle",
                "Middle person: drape your arms over your friends' shoulders",
                "Outer friends: wrap your inner arm around the middle person's waist",
                "Lean in slightly so there are no gaps",
            )
            tags(PoseTag.CASUAL, PoseTag.PLAYFUL, PoseTag.TRAVEL, PoseTag.PARTY, PoseTag.CITY)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, landscape = true, note = "Step back rather than zooming, and keep everyone's feet in.")
            difficulty = 1
            figure(cx = 0.39f, bottom = 0.94f, bodyHeight = 0.82f) {
                facing = 15f
                headTilt = 6f
                leftArm(upper = 40f, fore = 80f, hand = 85f, foreLen = 0.7f)
                rightArm(upper = -10f, fore = 28f)
                leftLeg(thigh = -4f, shin = 6f)
                rightLeg(thigh = 3f, shin = 0f)
            }
            figure(cx = 0.5f, bottom = 0.94f, bodyHeight = 0.88f) {
                headPitch = 3f
                leftArm(upper = 80f, fore = 60f, hand = 20f, foreLen = 0.45f)
                rightArm(upper = -80f, fore = -60f, hand = -20f, foreLen = 0.45f)
                leftLeg(thigh = 5f, shin = 2f)
                rightLeg(thigh = -5f, shin = -2f)
            }
            figure(cx = 0.63f, bottom = 0.94f, bodyHeight = 0.8f) {
                facing = -15f
                headTilt = -6f
                rightArm(upper = -40f, fore = -80f, hand = -85f, foreLen = 0.7f)
                leftArm(upper = 38f, fore = -55f, hand = -70f)
                leftLeg(thigh = -3f, shin = 0f)
                rightLeg(thigh = 6f, shin = -6f)
            }
        },
        pose("group_v_formation", "V-formation", PoseCategory.GROUP) {
            description = "One person steps forward in the middle while the others stand behind on either side, " +
                "turned in. It gives the group depth and a strong, team-poster look."
            tips(
                "Middle person: step forward and fold your arms",
                "Side people: stand a step behind, turned toward the middle",
                "Everyone shift your weight onto your back leg",
                "Look straight into the camera with a confident smile",
            )
            tags(PoseTag.CONFIDENT, PoseTag.COOL, PoseTag.OFFICE, PoseTag.CITY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, landscape = true, note = "Shoot from waist height so the front person doesn't hide the others.")
            difficulty = 1
            figure(cx = 0.32f, bottom = 0.88f, bodyHeight = 0.74f) {
                facing = 30f
                headYaw = 10f
                hipTilt = -4f
                leftArm(upper = 16f, fore = -28f, hand = -12f)
                rightArm(upper = -16f, fore = 28f, hand = 12f)
                leftLeg(thigh = -4f, shin = 0f)
                rightLeg(thigh = 9f, shin = -5f)
            }
            figure(cx = 0.5f, bottom = 0.96f, bodyHeight = 0.84f) {
                headPitch = 3f
                leftArm(upper = 10f, fore = -98f, hand = -105f, upperLen = 0.9f)
                rightArm(upper = -8f, fore = 96f, hand = 100f, upperLen = 0.9f)
                leftLeg(thigh = 7f, shin = 2f)
                rightLeg(thigh = -7f, shin = -2f)
            }
            figure(cx = 0.68f, bottom = 0.88f, bodyHeight = 0.74f) {
                facing = -30f
                headYaw = -10f
                hipTilt = 4f
                leftArm(upper = 38f, fore = -55f, hand = -70f)
                rightArm(upper = -6f, fore = -2f)
                rightLeg(thigh = 3f, shin = 0f)
                leftLeg(thigh = -9f, shin = 5f)
            }
        },
        pose("group_jump", "Jump together", PoseCategory.GROUP) {
            description = "Everyone jumping at once, each in their own style, captures the energy of a trip or a " +
                "celebration like nothing else."
            tips(
                "Count down out loud and jump on 'three'",
                "Each person picks a different jump: star, knee tuck or heel kick",
                "Throw your arms up high",
                "Repeat a few times: somebody always jumps late",
            )
            tags(PoseTag.PLAYFUL, PoseTag.CANDID, PoseTag.BEACH, PoseTag.NATURE, PoseTag.TRAVEL)
            camera(CameraHeight.KNEE, ShotType.FULL_BODY, landscape = true, note = "Shoot low in burst mode while everyone is in the air.")
            difficulty = 2
            figure(cx = 0.22f, bottom = 0.86f, bodyHeight = 0.64f) {
                headPitch = 8f
                leftArm(upper = 135f, fore = 142f, hand = 148f)
                rightArm(upper = -135f, fore = -142f, hand = -148f)
                leftLeg(thigh = 22f, shin = 24f, foot = 30f)
                rightLeg(thigh = -22f, shin = -24f, foot = -30f)
            }
            figure(cx = 0.5f, bottom = 0.8f, bodyHeight = 0.72f) {
                facing = 40f
                torsoLean = 6f
                headPitch = 10f
                leftArm(upper = 155f, fore = 165f, hand = 170f)
                rightArm(upper = -160f, fore = -170f, hand = -175f)
                leftLeg(thigh = 115f, shin = -15f, foot = -40f)
                rightLeg(thigh = 100f, shin = -25f, foot = -50f)
            }
            figure(cx = 0.77f, bottom = 0.84f, bodyHeight = 0.68f) {
                facing = -40f
                headPitch = 8f
                leftArm(upper = 160f, fore = 170f, hand = 175f)
                rightArm(upper = -140f, fore = -155f, hand = -160f)
                leftLeg(thigh = -10f, shin = 110f, foot = 120f)
                rightLeg(thigh = 0f, shin = 100f, foot = 110f)
            }
        },
        pose("group_walk_line", "Walking line", PoseCategory.GROUP) {
            description = "Four friends walking toward the camera, chatting and laughing, look natural and " +
                "cinematic. The street-style squad shot."
            tips(
                "Walk side by side toward the camera at a slow pace",
                "Talk and laugh with the person next to you",
                "Give your hands something to do: pockets, a coffee, a jacket",
                "Keep the line loose rather than perfectly straight",
            )
            tags(PoseTag.CANDID, PoseTag.COOL, PoseTag.CITY, PoseTag.TRAVEL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, landscape = true, note = "Walk backwards in front of them and shoot in bursts.")
            difficulty = 1
            figure(cx = 0.18f, bottom = 0.94f, bodyHeight = 0.8f) {
                facing = 15f
                headYaw = 35f
                leftArm(upper = 16f, fore = -28f, hand = -12f)
                rightArm(upper = -16f, fore = 28f, hand = 12f)
                rightLeg(thigh = -4f, shin = 2f)
                leftLeg(thigh = 4f, shin = 10f, thighLen = 0.97f, shinLen = 0.86f)
            }
            figure(cx = 0.39f, bottom = 0.94f, bodyHeight = 0.76f) {
                facing = -5f
                headYaw = -30f
                headPitch = 5f
                rightArm(upper = -6f, fore = 140f, hand = 160f)
                leftArm(upper = 10f, fore = 6f)
                leftLeg(thigh = -3f, shin = 2f)
                rightLeg(thigh = 3f, shin = -8f, thighLen = 0.97f, shinLen = 0.86f)
            }
            figure(cx = 0.61f, bottom = 0.94f, bodyHeight = 0.82f) {
                facing = 5f
                leftArm(upper = 12f, fore = 20f)
                rightArm(upper = -14f, fore = -6f)
                rightLeg(thigh = -4f, shin = 3f)
                leftLeg(thigh = 3f, shin = 9f, thighLen = 0.97f, shinLen = 0.86f)
            }
            figure(cx = 0.82f, bottom = 0.94f, bodyHeight = 0.76f) {
                facing = -15f
                headYaw = 10f
                rightArm(upper = -150f, fore = 30f)
                leftArm(upper = 16f, fore = -28f, hand = -12f)
                leftLeg(thigh = -3f, shin = 2f)
                rightLeg(thigh = 4f, shin = -8f, thighLen = 0.97f, shinLen = 0.86f)
            }
        },
        pose("group_sitting_row", "Sitting row", PoseCategory.GROUP) {
            description = "Friends sitting side by side, each with a different relaxed leg position, make a " +
                "laid-back group shot on steps, benches or a low wall."
            tips(
                "Sit side by side on steps, a bench or a low wall",
                "Everyone pick a different leg pose so it doesn't look stiff",
                "Lean slightly toward the middle",
                "Keep your hands relaxed on your knees or lap",
            )
            tags(PoseTag.CASUAL, PoseTag.CANDID, PoseTag.STAIRS, PoseTag.CITY, PoseTag.NATURE)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, landscape = true, note = "Crouch down so the camera is at the group's chest height.")
            difficulty = 1
            figure(cx = 0.27f, bottom = 0.94f, bodyHeight = 0.92f) {
                facing = 10f
                torsoLength = 0.9f
                leftLeg(thigh = 25f, shin = 5f, thighLen = 0.45f)
                rightLeg(thigh = -25f, shin = -5f, thighLen = 0.45f)
                leftArm(upper = 8f, fore = -35f, hand = -40f, upperLen = 0.9f)
                rightArm(upper = -8f, fore = 35f, hand = 40f, upperLen = 0.9f)
            }
            figure(cx = 0.5f, bottom = 0.94f, bodyHeight = 0.86f) {
                facing = 10f
                torsoLean = -3f
                headTilt = 5f
                headYaw = 5f
                rightLeg(thigh = -10f, shin = 0f, thighLen = 0.45f)
                leftLeg(thigh = 55f, shin = -88f, thighLen = 0.6f, shinLen = 0.92f)
                leftArm(upper = 8f, fore = 5f, hand = 0f)
                rightArm(upper = -25f, fore = -15f)
            }
            figure(cx = 0.73f, bottom = 0.94f, bodyHeight = 0.9f) {
                facing = -20f
                headTilt = 6f
                headYaw = -5f
                leftLeg(thigh = -25f, shin = -8f, thighLen = 0.5f)
                rightLeg(thigh = -18f, shin = -4f, thighLen = 0.5f)
                leftArm(upper = 4f, fore = -30f)
                rightArm(upper = -6f, fore = 25f)
            }
        },
        pose("group_levels", "Stacked levels", PoseCategory.GROUP) {
            description = "Putting heads at three different heights (one sitting, one crouching, one standing) " +
                "turns a flat line-up into a dynamic, magazine-style composition."
            tips(
                "One person sits on a step or box, one crouches in front, one stands",
                "Keep the three heads at clearly different heights",
                "Stay close so the group reads as one shape",
                "Everyone angle slightly toward the centre",
            )
            tags(PoseTag.COOL, PoseTag.CONFIDENT, PoseTag.CITY, PoseTag.STAIRS, PoseTag.WALL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, landscape = true, note = "Shoot from waist height and keep the horizon level.")
            difficulty = 2
            figure(cx = 0.28f, bottom = 0.95f, bodyHeight = 0.86f) {
                facing = 30f
                headYaw = 5f
                leftLeg(thigh = 72f, shin = 8f, thighLen = 0.7f)
                rightLeg(thigh = 62f, shin = -2f, thighLen = 0.72f)
                leftArm(upper = 10f, fore = 40f, hand = 50f)
                rightArm(upper = 5f, fore = 45f)
            }
            figure(cx = 0.5f, bottom = 0.97f, bodyHeight = 0.8f) {
                facing = 10f
                torsoLength = 0.85f
                headPitch = 3f
                leftLeg(thigh = 125f, shin = 5f, thighLen = 0.55f)
                rightLeg(thigh = -125f, shin = -5f, thighLen = 0.55f)
                leftArm(upper = 21f, fore = -30f, hand = -40f)
                rightArm(upper = -21f, fore = 30f, hand = 40f)
            }
            figure(cx = 0.72f, bottom = 0.93f, bodyHeight = 0.86f) {
                facing = -20f
                headYaw = -5f
                hipTilt = 4f
                leftArm(upper = 16f, fore = -28f, hand = -12f)
                rightArm(upper = -16f, fore = 28f, hand = 12f)
                rightLeg(thigh = 3f, shin = 0f)
                leftLeg(thigh = -8f, shin = 6f)
            }
        },
        pose("group_back_to_back", "Back to back", PoseCategory.GROUP) {
            description = "The middle person faces the camera while the outer two stand back-to-back against their " +
                "shoulders, arms folded. A bold, album-cover look."
            tips(
                "Middle person: face the camera and fold your arms",
                "Outer people: turn sideways and lean your back against the middle person's shoulder",
                "Everyone fold your arms",
                "Outer people: turn your heads toward the camera",
            )
            tags(PoseTag.COOL, PoseTag.CONFIDENT, PoseTag.CITY, PoseTag.WALL, PoseTag.OFFICE)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "A plain wall or graffiti background makes this pop.")
            difficulty = 1
            figure(cx = 0.248f, bottom = 0.94f, bodyHeight = 0.82f) {
                facing = -80f
                torsoLean = 4f
                headYaw = -45f
                leftArm(upper = -6f, fore = -100f, foreLen = 0.32f)
                rightArm(upper = -4f, fore = -96f, foreLen = 0.3f)
                leftLeg(thigh = -3f, shin = 0f)
                rightLeg(thigh = 4f, shin = 1f)
            }
            figure(cx = 0.5f, bottom = 0.94f, bodyHeight = 0.86f) {
                headPitch = 3f
                leftArm(upper = 10f, fore = -98f, hand = -105f, upperLen = 0.9f)
                rightArm(upper = -8f, fore = 96f, hand = 100f, upperLen = 0.9f)
                leftLeg(thigh = 6f, shin = 2f)
                rightLeg(thigh = -6f, shin = -2f)
            }
            figure(cx = 0.745f, bottom = 0.94f, bodyHeight = 0.82f) {
                facing = 80f
                torsoLean = -4f
                headYaw = 45f
                leftArm(upper = 4f, fore = 96f, foreLen = 0.3f)
                rightArm(upper = 6f, fore = 100f, foreLen = 0.32f)
                rightLeg(thigh = 3f, shin = 0f)
                leftLeg(thigh = -4f, shin = -1f)
            }
        },
    )
}
