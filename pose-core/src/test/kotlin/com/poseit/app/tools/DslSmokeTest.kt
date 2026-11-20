package com.poseit.app.tools

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.dsl.pose
import org.junit.Test
import java.io.File

class DslSmokeTest {
    @Test
    fun renderSamples() {
        val poses = listOf(
            pose("neutral", "Neutral", PoseCategory.STANDING) { figure { } },
            pose("tpose", "T pose", PoseCategory.STANDING) {
                figure { leftArm(90f); rightArm(-90f) }
            },
            pose("hand_hip", "Hand on hip", PoseCategory.STANDING) {
                figure(bodyHeight = 0.84f) {
                    facing = 20f
                    leftArm(upper = 40f, fore = -60f)
                    rightArm(upper = -6f, fore = -4f)
                    rightLeg(thigh = -12f, shin = -4f)
                    hipTilt = 4f; shoulderTilt = -3f; headTilt = 6f
                }
            },
            pose("profile", "Profile walk", PoseCategory.WALKING) {
                figure { facing = 90f; leftArm(-25f, -10f); rightArm(25f, 50f); leftLeg(20f, 5f); rightLeg(-18f, -30f) }
            },
            pose("back", "Back", PoseCategory.STANDING) {
                figure { facing = 180f; leftArm(-150f, -170f); rightArm(30f) }
            },
            pose("sit", "Sitting facing", PoseCategory.SITTING) {
                figure(bottom = 0.92f, bodyHeight = 0.9f) {
                    leftLeg(thigh = 20f, shin = 0f, thighLen = 0.45f)
                    rightLeg(thigh = -20f, shin = 0f, thighLen = 0.45f)
                    leftArm(15f, -30f); rightArm(-15f, 30f)
                }
            },
            pose("sit_side", "Sitting side", PoseCategory.SITTING) {
                figure(bottom = 0.92f, bodyHeight = 0.9f) {
                    facing = 70f
                    leftLeg(thigh = 88f, shin = 5f); rightLeg(thigh = 80f, shin = 15f)
                    leftArm(30f, 70f); rightArm(10f, 60f)
                }
            },
            pose("portrait", "Waist up", PoseCategory.PORTRAIT) {
                camera(CameraHeight.EYE, ShotType.HALF_BODY)
                figure(top = 0.1f, bodyHeight = 2.0f) { facing = -25f; headTilt = -6f; leftArm(10f, 150f, upperLen = 0.9f); rightArm(-15f, 40f) }
            },
            pose("couple", "Couple", PoseCategory.COUPLE) {
                figure(cx = 0.33f) { facing = 30f; leftArm(40f, -40f) }
                figure(cx = 0.67f) { facing = -30f; rightArm(-40f, 40f) }
            },
        )
        PosePreviewRenderer.renderSheet(poses, File("build/pose-previews/dsl-smoke.png"), columns = 5)
    }
}
