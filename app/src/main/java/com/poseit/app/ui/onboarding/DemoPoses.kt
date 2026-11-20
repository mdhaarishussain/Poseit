package com.poseit.app.ui.onboarding

import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.dsl.pose
import com.poseit.app.pose.library.BuiltInPoses

/**
 * A handful of original demo figures (authored with the pose DSL) for the intro animations and the settings
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

    val wave: PoseTemplate by lazy {
        pose("demo_wave", "Hello wave", PoseCategory.FUN) {
            figure(cx = 0.5f, bottom = 0.94f, bodyHeight = 0.84f) {
                headTilt = 6f
                rightArm(upper = -125f, fore = -165f)
                leftArm(upper = 10f, fore = 4f)
                leftLeg(thigh = 6f, shin = 2f)
                rightLeg(thigh = -3f, shin = -1f)
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

    val crossedArms: PoseTemplate by lazy {
        pose("demo_crossed", "Crossed arms", PoseCategory.PROFESSIONAL) {
            figure(cx = 0.5f, bottom = 0.94f, bodyHeight = 0.84f) {
                facing = -20f
                leftArm(upper = 18f, fore = -82f, foreLen = 0.8f)
                rightArm(upper = -18f, fore = 82f, foreLen = 0.8f)
                leftLeg(thigh = 8f, shin = 4f)
                rightLeg(thigh = -2f, shin = 0f)
            }
        }
    }

    val walking: PoseTemplate by lazy {
        pose("demo_walk", "Walking", PoseCategory.WALKING) {
            figure(cx = 0.5f, bottom = 0.94f, bodyHeight = 0.84f) {
                facing = 60f
                leftArm(upper = -22f, fore = -8f)
                rightArm(upper = 26f, fore = 44f)
                leftLeg(thigh = 22f, shin = 6f)
                rightLeg(thigh = -16f, shin = -34f)
            }
        }
    }

    /** Start / end of the "match the outline" animation: smaller figures so the raised arm stays in frame. */
    val matchStart: PoseTemplate by lazy {
        pose("demo_match_start", "Arms down", PoseCategory.STANDING) {
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.66f) {
                leftArm(upper = 9f, fore = 5f)
                rightArm(upper = -9f, fore = -5f)
                leftLeg(thigh = 4f, shin = 2f)
                rightLeg(thigh = -4f, shin = -2f)
            }
        }
    }

    val matchTarget: PoseTemplate by lazy {
        pose("demo_match_target", "Wave", PoseCategory.FUN) {
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.66f) {
                headTilt = 6f
                rightArm(upper = -125f, fore = -165f)
                leftArm(upper = 12f, fore = 4f)
                leftLeg(thigh = 6f, shin = 2f)
                rightLeg(thigh = -3f, shin = -1f)
            }
        }
    }

    val all: List<PoseTemplate> by lazy { listOf(handOnHip, wave, star, crossedArms, walking) }

    /**
     * Poses for the intro's mannequin carousel: a few full-body solo poses from different built-in categories,
     * or the demo figures when the library has too few.
     */
    fun carousel(max: Int = 5): List<PoseTemplate> {
        val builtIn = runCatching { BuiltInPoses.all }.getOrDefault(emptyList())
            .filter { it.personCount == 1 && it.frameAspect < 1f && it.camera.shot == ShotType.FULL_BODY }
            .groupBy { it.category }
            .values
            .mapNotNull { it.firstOrNull() }
            .take(max)
        return if (builtIn.size >= 3) builtIn else all.take(max)
    }

    /** [skeleton] translated so its hip centre sits on [anchor]'s hip centre (no-op when either is missing). */
    fun alignHips(skeleton: Skeleton, anchor: Skeleton): Skeleton {
        val from = skeleton.midHip() ?: return skeleton
        val to = anchor.midHip() ?: return skeleton
        val delta = to - from
        return skeleton.mapPoints { it + delta }
    }

    /** Linear blend of two skeletons (joints missing from [a] snap to [b]). */
    fun blend(a: Skeleton, b: Skeleton, t: Float): Skeleton {
        val k = t.coerceIn(0f, 1f)
        return Skeleton(
            points = b.points.mapValues { (joint, pb) -> a.points[joint]?.let { Vec2.lerp(it, pb, k) } ?: pb },
            unit = a.unit + (b.unit - a.unit) * k,
        )
    }
}
