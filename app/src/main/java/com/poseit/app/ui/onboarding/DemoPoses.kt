package com.poseit.app.ui.onboarding

import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.dsl.pose

/**
 * A handful of original demo figures (authored with the pose DSL) for the intro pictures and the settings
 * previews. They never appear in the library and make those screens independent of the built-in catalogue.
 */
internal object DemoPoses {

    val handOnHip: PoseTemplate by lazy {
        pose("demo_hand_hip", "Hand on hip", PoseCategory.STANDING) {
            figure(cx = 0.5f, bottom = 0.94f, bodyHeight = 0.84f) {
                facing = 15f
                headTilt = -6f
                leftArm(upper = 42f, fore = -48f)
                rightArm(upper = -7f, fore = -3f)
                leftLeg(thigh = 2f, shin = 0f)
                rightLeg(thigh = -13f, shin = -5f)
            }
        }
    }

    val star: PoseTemplate by lazy {
        pose("demo_star", "Star jump", PoseCategory.FUN) {
            figure(cx = 0.5f, bottom = 0.9f, bodyHeight = 0.8f) {
                headPitch = 10f
                leftArm(upper = 132f, fore = 150f)
                rightArm(upper = -132f, fore = -150f)
                leftLeg(thigh = 24f, shin = 22f)
                rightLeg(thigh = -24f, shin = -22f)
            }
        }
    }

    /** One arm raised above the head, for the intro's camera pictures; small enough for the hand to stay in frame. */
    val raisedArm: PoseTemplate by lazy {
        pose("demo_raised_arm", "Raised arm", PoseCategory.FUN) {
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.66f) {
                headTilt = 6f
                rightArm(upper = -125f, fore = -165f)
                leftArm(upper = 12f, fore = 4f)
                leftLeg(thigh = 6f, shin = 2f)
                rightLeg(thigh = -3f, shin = -1f)
            }
        }
    }
}
