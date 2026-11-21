package com.poseit.app.review

import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.dsl.FigureBuilder
import com.poseit.app.pose.dsl.pose
import com.poseit.app.pose.match.FramingAudience
import com.poseit.app.pose.match.HintKind
import com.poseit.app.pose.match.MatchConfig
import com.poseit.app.pose.match.MatchResult
import com.poseit.app.pose.match.PoseMatcher
import com.poseit.app.pose.scene.SceneMapping
import org.junit.Assert.assertTrue
import org.junit.Test

/** Review experiment: direction words of pose and framing hints, for both audiences and facings. */
class HintDirectionReviewTest {
    private val aspect = 3f / 4f

    private fun fig(block: FigureBuilder.() -> Unit): Skeleton =
        SceneMapping.templateToScene(pose("t", "T", PoseCategory.STANDING) { figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.8f, block = block) }, aspect).single()

    private fun det(s: Skeleton) = Skeleton(s.points, s.points.keys.associateWith { (s.visibility[it] ?: 1f).coerceAtMost(0.95f) }, 0f)

    private fun m(t: Skeleton, d: Skeleton, a: FramingAudience, strict: Boolean = true): MatchResult =
        PoseMatcher.match(listOf(t), listOf(det(d)), aspect, a, if (strict) MatchConfig(perfectScore = 0.999f) else MatchConfig())

    private fun texts(r: MatchResult) = r.hints.map { it.text }

    @Test
    fun framingDirections() {
        for (facing in listOf(0f, 180f)) {
            val t = fig { this.facing = facing }
            val right = t.mapPoints { Vec2(it.x + 0.25f, it.y) } // person too far toward the image right
            val subj = texts(m(t, right, FramingAudience.SUBJECT))
            val phot = texts(m(t, right, FramingAudience.PHOTOGRAPHER))
            println("facing=$facing tooRight subject=$subj photographer=$phot")
            // Person must move toward the image LEFT: facing the camera that is their RIGHT, back to camera their LEFT.
            val expected = if (facing == 0f) "Step to your right" else "Step to your left"
            assertTrue("$facing $subj", subj.contains(expected))
            // Content must move left in the image: pan the phone right.
            assertTrue(phot.toString(), phot.contains("Move the phone right"))
        }
    }

    @Test
    fun armDirections() {
        // Left arm raised sideways (facing camera: image right) vs arm down.
        for (facing in listOf(0f, 180f)) {
            val side = if (facing == 0f) 90f else -90f
            val t = fig { this.facing = facing; leftArm(upper = side) }
            val d = fig { this.facing = facing; leftArm(upper = side / 9f) }
            val r = m(t, d, FramingAudience.SUBJECT, strict = false)
            println("facing=$facing ${texts(r)}")
            assertTrue(texts(r).toString(), texts(r).any { it == "Raise your left arm" })
            val p = m(t, d, FramingAudience.PHOTOGRAPHER, strict = false)
            assertTrue(texts(p).toString(), texts(p).any { it == "Ask them to raise their left arm" })
        }
    }

    @Test
    fun leanDirections() {
        val t = fig { torsoLean = 15f } // top toward image right = subject's LEFT when facing the camera
        val d = fig { torsoLean = -20f }
        val r = m(t, d, FramingAudience.SUBJECT, strict = false)
        println("lean ${texts(r)}")
        assertTrue(texts(r).toString(), texts(r).any { it == "Lean to your left" })
    }
}
