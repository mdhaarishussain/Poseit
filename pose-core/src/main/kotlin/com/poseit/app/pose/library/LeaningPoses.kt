package com.poseit.app.pose.library

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.dsl.pose

/**
 * Single-person poses leaning on walls, doors, railings, poles and table edges. The prop is not drawn: each
 * description and tip says where it is, and the figure's lean makes the support obvious.
 */
object LeaningPoses {
    val all: List<PoseTemplate> = listOf(
        pose("lean_shoulder_wall", "Shoulder Lean", PoseCategory.LEANING) {
            description = "Resting one shoulder on a wall with the feet stepped out makes a long diagonal line " +
                "and instantly looks relaxed and cool."
            tips(
                "Stand side-on to a wall and rest your shoulder against it",
                "Step your feet out about a foot from the wall",
                "Cross your arms loosely",
                "Bend your outer knee slightly and relax your face",
            )
            tags(PoseTag.COOL, PoseTag.CASUAL, PoseTag.WALL, PoseTag.CITY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Include some of the wall so the lean makes sense.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 10f
                torsoLean = -12f
                headTilt = 6f
                headPitch = -3f
                leftArm(upper = 10f, fore = -80f, hand = -82f)
                rightArm(upper = -12f, fore = 100f, hand = 100f)
                rightLeg(thigh = 11f, shin = 9f)
                leftLeg(thigh = 0f, shin = 14f, foot = 30f)
            }
        },
        pose("lean_foot_on_wall", "Foot Up on the Wall", PoseCategory.LEANING) {
            description = "Back against the wall with one foot flat on it behind you: the bent knee breaks up " +
                "the straight lines and gives an easy, confident vibe."
            tips(
                "Lean your back and shoulders against the wall",
                "Bend one knee and press that foot flat against the wall",
                "Slip your far hand into your pocket and let the other arm hang",
                "Look off to the side for a candid feel",
            )
            tags(PoseTag.COOL, PoseTag.CANDID, PoseTag.WALL, PoseTag.CITY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Stand slightly to the side of the bent knee so it doesn't block the body.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 22f
                headYaw = -15f
                headTilt = 3f
                headPitch = -3f
                leftArm(upper = 14f, fore = -28f, hand = -12f)
                rightArm(upper = -14f, fore = -6f)
                leftLeg(thigh = 3f, shin = 1f)
                rightLeg(thigh = -10f, shin = 8f, foot = 5f, thighLen = 0.6f, shinLen = 0.9f)
            }
        },
        pose("lean_hand_on_wall", "Hand on the Wall", PoseCategory.LEANING) {
            description = "One palm flat on the wall at head height pulls the body into a long, open line and " +
                "shows off the waist."
            tips(
                "Stand an arm's length from the wall, side-on",
                "Place your palm flat on the wall at head height",
                "Lean gently into that hand and keep the other in your pocket",
                "Look at the camera over your raised arm",
            )
            tags(PoseTag.CONFIDENT, PoseTag.ELEGANT, PoseTag.WALL, PoseTag.TRAVEL)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Frame from the open side so the raised arm frames the face.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = -20f
                torsoLean = 5f
                headTilt = -2f
                leftArm(upper = 80f, fore = 160f, hand = 175f, foreLen = 0.75f)
                rightArm(upper = -14f, fore = 26f, hand = 10f)
                rightLeg(thigh = -6f, shin = -5f)
                leftLeg(thigh = -12f, shin = -4f)
            }
        },
        pose("lean_railing_forward", "Railing Lean", PoseCategory.LEANING) {
            description = "Forearms resting on a railing, body angled toward the view, face turned back: the " +
                "classic travel shot at a viewpoint or on a bridge."
            tips(
                "Rest both forearms on the railing in front of you",
                "Step your feet back a little and lean forward from the hips",
                "Cross your near foot behind you on its toes",
                "Turn your head back over your shoulder toward the camera",
            )
            tags(PoseTag.CANDID, PoseTag.ROMANTIC, PoseTag.TRAVEL, PoseTag.CITY)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Shoot from behind and to the side to include the view.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.86f) {
                facing = 70f
                torsoLean = 22f
                headYaw = 15f
                headTilt = -20f
                rightArm(upper = 25f, fore = 88f, hand = 100f)
                leftArm(upper = 18f, fore = 92f, hand = 100f)
                leftLeg(thigh = -3f, shin = -2f)
                rightLeg(thigh = 0f, shin = -28f, foot = 45f)
            }
        },
        pose("lean_crossed_legs", "Cross-Legged Lean", PoseCategory.LEANING) {
            description = "Leaning back against a wall with the ankles crossed and hands in pockets: the " +
                "ultimate laid-back pose that slims and lengthens the legs."
            tips(
                "Lean your upper back against the wall",
                "Walk your feet forward and cross your front foot over the other",
                "Point the toes of the crossed foot down",
                "Put your hands in your pockets and relax your shoulders",
            )
            tags(PoseTag.CASUAL, PoseTag.SLIMMING, PoseTag.WALL, PoseTag.CITY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "A three-quarter angle shows the lean best; avoid shooting straight on.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 40f
                torsoLean = -8f
                headTilt = 8f
                headYaw = 20f
                leftArm(upper = 14f, fore = -20f, hand = -10f)
                rightArm(upper = -16f, fore = 35f, hand = 15f)
                leftLeg(thigh = 10f, shin = 8f)
                rightLeg(thigh = 20f, shin = 28f, foot = 20f)
            }
        },
        pose("lean_door_frame", "Doorway Lean", PoseCategory.LEANING) {
            description = "One hand high on the door frame and the other on the hip turns any doorway into a " +
                "frame for a bold, fashion-style shot."
            tips(
                "Stand in a doorway and lean toward one side of the frame",
                "Reach up and rest that hand high on the frame",
                "Put your other hand on your hip",
                "Push your hip away from the frame",
            )
            tags(PoseTag.CONFIDENT, PoseTag.ELEGANT, PoseTag.WALL, PoseTag.HOME)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Centre the door frame so its lines frame the subject.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.78f) {
                facing = 10f
                torsoLean = -5f
                hipTilt = -4f
                headTilt = -2f
                rightArm(upper = -140f, fore = -160f, hand = -170f)
                leftArm(upper = 40f, fore = -60f, hand = -80f)
                leftLeg(thigh = 3f, shin = 0f)
                rightLeg(thigh = 2f, shin = -12f)
            }
        },
        pose("lean_pole_swing", "Pole Swing", PoseCategory.LEANING) {
            description = "Hold a lamp post or pole and swing your body out with the free arm open: joyful, " +
                "full of movement and great on a street corner."
            tips(
                "Grip the pole at head height with one hand",
                "Lean your body out away from the pole, arm straight",
                "Open your free arm wide to the side",
                "Kick your outer leg out with the toes pointed",
            )
            tags(PoseTag.PLAYFUL, PoseTag.CANDID, PoseTag.CITY, PoseTag.TRAVEL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Use burst mode and catch the moment of the swing.")
            difficulty = 3
            figure(cx = 0.5f, bottom = 0.94f, bodyHeight = 0.8f) {
                facing = -10f
                torsoLean = 14f
                headTilt = -8f
                headPitch = 8f
                rightArm(upper = -135f, fore = -140f, hand = -145f)
                leftArm(upper = 95f, fore = 110f, hand = 120f)
                rightLeg(thigh = -16f, shin = -14f)
                leftLeg(thigh = 38f, shin = 45f, foot = 70f)
            }
        },
        pose("lean_table_edge", "Table Edge Perch", PoseCategory.LEANING) {
            description = "Half-sitting against a desk, table or car with the legs stretched out reads " +
                "relaxed yet put-together."
            tips(
                "Rest the back of your hips against the edge of a table",
                "Stretch your legs out in front of you, feet apart",
                "Hold the edge loosely with both hands beside your hips",
                "Sit up tall and give a warm smile",
            )
            tags(PoseTag.CASUAL, PoseTag.CONFIDENT, PoseTag.OFFICE, PoseTag.HOME)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot from the side the legs point to so they look long.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.86f) {
                facing = 40f
                torsoLean = -3f
                headTilt = 3f
                headYaw = 15f
                leftArm(upper = -4f, fore = -12f)
                rightArm(upper = -16f, fore = -14f)
                leftLeg(thigh = 28f, shin = 28f)
                rightLeg(thigh = 20f, shin = 34f)
            }
        },
        pose("lean_railing_back", "Elbows on the Rail", PoseCategory.LEANING) {
            description = "Back to a railing or bar with both elbows propped behind you: open, confident and " +
                "relaxed, perfect for rooftops and piers."
            tips(
                "Stand with your back against a waist-high railing",
                "Prop both elbows on the rail and let your hands hang",
                "Relax one knee and shift your weight to the other leg",
                "Lift your chest and look into the lens",
            )
            tags(PoseTag.CONFIDENT, PoseTag.COOL, PoseTag.TRAVEL, PoseTag.PARTY)
            camera(CameraHeight.CHEST, ShotType.FULL_BODY, note = "Keep the horizon behind the subject level.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 8f
                headTilt = 4f
                leftArm(upper = 26f, fore = 72f, hand = 15f)
                rightArm(upper = -26f, fore = -72f, hand = -15f)
                leftLeg(thigh = -4f, shin = 10f)
                rightLeg(thigh = -2f, shin = -3f)
            }
        },
    )
}
