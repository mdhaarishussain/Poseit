package com.poseit.app.pose.library

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.dsl.pose

/**
 * Business and personal-branding poses: headshots, team pages, conference and office portraits.
 *
 * With a positive `facing` the subject's LEFT side is the far side; "weight on the back leg" is then the left leg.
 */
object ProfessionalPoses {
    val all: List<PoseTemplate> = listOf(
        pose("pro_headshot", "LinkedIn headshot", PoseCategory.PROFESSIONAL) {
            description = "The profile-picture standard: shoulders angled away, face turned back to the camera and " +
                "chin pushed slightly forward. Looks approachable and sharp."
            tips(
                "Turn your shoulders about 30° away from the camera",
                "Turn your face back toward the lens",
                "Push your chin slightly forward and down to define the jawline",
                "Smile with your eyes: think of something that made you laugh",
            )
            tags(PoseTag.CONFIDENT, PoseTag.OFFICE, PoseTag.CITY)
            camera(CameraHeight.EYE, ShotType.CLOSE_UP, note = "Use a plain, uncluttered background and soft window light.")
            difficulty = 1
            figure(cx = 0.517f, top = 0.1f, bodyHeight = 2.2f) {
                facing = 30f
                headYaw = 8f
                headPitch = -4f
                headTilt = -3f
                leftArm(upper = 8f, fore = 3f)
                rightArm(upper = -8f, fore = -3f)
            }
        },
        pose("pro_arms_crossed", "Confident crossed arms", PoseCategory.PROFESSIONAL) {
            description = "A solid, grounded stance with folded arms says expertise and authority. The angled body " +
                "and relaxed face keep it from looking defensive."
            tips(
                "Stand with your feet hip-width apart",
                "Fold your arms loosely across your lower chest",
                "Turn your body slightly, then face the camera",
                "Relax your shoulders and give a small, confident smile",
            )
            tags(PoseTag.CONFIDENT, PoseTag.OFFICE, PoseTag.CITY, PoseTag.WALL)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "A slightly low camera makes the subject look more authoritative.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = -25f
                headYaw = -5f
                headPitch = 2f
                leftArm(upper = 11f, fore = -100f, hand = -105f, upperLen = 0.9f)
                rightArm(upper = -6f, fore = 95f, hand = 100f, upperLen = 0.9f, foreLen = 0.97f)
                leftLeg(thigh = 6f, shin = 2f)
                rightLeg(thigh = -8f, shin = -2f)
            }
        },
        pose("pro_blazer_pocket", "Blazer pocket", PoseCategory.PROFESSIONAL) {
            description = "One hand in the trouser pocket, the other at the jacket button: relaxed but polished. " +
                "A go-to for speaker bios and team pages."
            tips(
                "Slide your near hand into your trouser pocket, thumb out",
                "Rest your other hand on your jacket button",
                "Shift your weight onto your back leg",
                "Keep your chin level and look straight into the lens",
            )
            tags(PoseTag.CONFIDENT, PoseTag.COOL, PoseTag.OFFICE, PoseTag.CITY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Button the jacket and pull it straight before the shot.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 25f
                headYaw = 5f
                headPitch = -3f
                hipTilt = -5f
                shoulderTilt = 3f
                leftArm(upper = 6f, fore = -83f, hand = -90f, upperLen = 0.92f, foreLen = 0.8f)
                rightArm(upper = -10f, fore = 28f)
                leftLeg(thigh = -4f, shin = -1f)
                rightLeg(thigh = 10f, shin = -6f)
            }
        },
        pose("pro_desk_edge", "Desk-edge perch", PoseCategory.PROFESSIONAL) {
            description = "Perching on the edge of a desk feels relaxed yet in charge. Perfect for office, studio " +
                "and founder portraits. The desk itself is not drawn."
            tips(
                "Sit on the front edge of a desk, one foot planted on the floor",
                "Let the other leg hang relaxed",
                "Rest one hand on the desk beside you and the other on your thigh",
                "Lean back slightly and turn your face to the camera",
            )
            tags(PoseTag.CONFIDENT, PoseTag.CASUAL, PoseTag.OFFICE, PoseTag.HOME)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Shoot from waist height and include a bit of the desk.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.93f, bodyHeight = 0.88f) {
                facing = 35f
                torsoLean = -4f
                headYaw = 15f
                headPitch = -2f
                rightLeg(thigh = 42f, shin = 5f, thighLen = 0.8f)
                leftLeg(thigh = 60f, shin = 15f, thighLen = 0.75f)
                rightArm(upper = -22f, fore = -18f)
                leftArm(upper = 12f, fore = -26f, hand = -40f, upperLen = 0.95f)
            }
        },
        pose("pro_open_palm", "Open-palm speaker", PoseCategory.PROFESSIONAL) {
            description = "An open palm mid-sentence looks engaged and trustworthy: the classic keynote or panel " +
                "shot. Keep talking while the photo is taken for a natural expression."
            tips(
                "Hold your near hand out at waist height, palm up and open",
                "Keep the elbow bent and close to your body",
                "Rest your weight on your back leg",
                "Actually talk while the photos are taken",
            )
            tags(PoseTag.CONFIDENT, PoseTag.CANDID, PoseTag.OFFICE, PoseTag.PARTY)
            camera(CameraHeight.CHEST, ShotType.THREE_QUARTER, note = "Use burst mode and pick the frame with the best expression.")
            difficulty = 2
            figure(cx = 0.55f, top = 0.06f, bodyHeight = 1.15f) {
                facing = 20f
                torsoLean = -2f
                headYaw = 5f
                headTilt = -3f
                rightArm(upper = -12f, fore = -115f, hand = -105f, foreLen = 0.8f)
                leftArm(upper = 8f, fore = -6f)
                leftLeg(thigh = -3f, shin = 0f)
                rightLeg(thigh = 8f, shin = -4f)
            }
        },
        pose("pro_table_lean", "Boardroom lean", PoseCategory.PROFESSIONAL) {
            description = "Both hands planted on a table and the body leaning in projects energy and leadership. " +
                "Great for meeting-room and startup portraits."
            tips(
                "Stand behind a table and place both hands flat, a little wider than your shoulders",
                "Lean your upper body slightly toward the camera",
                "Keep your elbows soft, not locked",
                "Look straight into the lens with a calm smile",
            )
            tags(PoseTag.CONFIDENT, PoseTag.OFFICE, PoseTag.CAFE)
            camera(CameraHeight.CHEST, ShotType.HALF_BODY, note = "Shoot at chest height from the other side of the table.")
            difficulty = 1
            figure(cx = 0.5f, top = 0.08f, bodyHeight = 1.4f) {
                facing = 10f
                torsoLength = 0.86f
                headPitch = -2f
                leftArm(upper = 16f, fore = 6f, hand = 45f)
                rightArm(upper = -15f, fore = -6f, hand = -45f)
            }
        },
        pose("pro_cuff_adjust", "Cuff adjust", PoseCategory.PROFESSIONAL) {
            description = "Adjusting a cuff or watch gives the hands a purpose and adds a polished, sophisticated " +
                "detail. Ideal with suits, shirts and wristwatches."
            tips(
                "Bring one forearm across your waist",
                "Pinch that cuff or watch strap with your other hand",
                "Look down at your wrist, or glance up at the camera",
                "Keep your weight on your back leg",
            )
            tags(PoseTag.ELEGANT, PoseTag.COOL, PoseTag.OFFICE, PoseTag.PARTY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Zoom in afterwards for a detail crop of the hands.")
            difficulty = 2
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = 20f
                headYaw = -10f
                headPitch = -15f
                hipTilt = -4f
                leftArm(upper = 6f, fore = -82f, hand = -85f)
                rightArm(upper = -25f, fore = 79f, hand = 85f)
                leftLeg(thigh = -3f, shin = 0f)
                rightLeg(thigh = 9f, shin = -5f)
            }
        },
        pose("pro_folder_hold", "Folder in hand", PoseCategory.PROFESSIONAL) {
            description = "Holding a folder, notebook or tablet against the chest gives the hands a job and tells a " +
                "work story at a glance."
            tips(
                "Hold a folder against your chest with your near arm",
                "Let the other arm hang relaxed",
                "Stand tall with your weight on your back leg",
                "Tilt your head slightly and smile",
            )
            tags(PoseTag.CONFIDENT, PoseTag.CASUAL, PoseTag.OFFICE, PoseTag.CITY)
            camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "A blurred office or street background works best.")
            difficulty = 1
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                facing = -20f
                headYaw = -5f
                headPitch = -2f
                headTilt = 3f
                hipTilt = 4f
                leftArm(upper = 9f, fore = -122f, hand = -130f, upperLen = 0.93f)
                rightArm(upper = -6f, fore = 2f)
                rightLeg(thigh = 3f, shin = 0f)
                leftLeg(thigh = -8f, shin = 6f)
            }
        },
    )
}
