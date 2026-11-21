package com.poseit.app.review

import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.pose.match.FramingAudience
import com.poseit.app.pose.match.HintKind
import com.poseit.app.pose.match.MatchConfig
import com.poseit.app.pose.match.MatchResult
import com.poseit.app.pose.match.PoseMatcher
import com.poseit.app.pose.scene.OverlayTransform
import com.poseit.app.pose.scene.SceneMapping
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Review experiment: every built-in pose, matched by a person who reproduces it EXACTLY (as MediaPipe would report
 * it), must reach PERFECT, otherwise auto-capture can never fire for that pose.
 */
class MatchLibraryReviewTest {

    private val aspect = 3f / 4f

    /** What MediaPipe would report for exactly this scene skeleton. */
    private fun asDetection(s: Skeleton, noise: Random? = null, amount: Float = 0f): Skeleton {
        val pts = LinkedHashMap<Joint, Vec2>()
        val vis = LinkedHashMap<Joint, Float>()
        for ((j, p) in s.points) {
            val q = if (noise != null) Vec2(p.x + (noise.nextFloat() - 0.5f) * 2f * amount, p.y + (noise.nextFloat() - 0.5f) * 2f * amount) else p
            pts[j] = q
            val inFrame = q.x in 0f..1f && q.y in 0f..1f
            val hidden = (s.visibility[j] ?: 1f) < 0.5f
            vis[j] = when {
                !inFrame -> 0.1f
                hidden -> 0.2f
                else -> 0.95f
            }
        }
        return Skeleton(pts, vis, 0f)
    }

    private fun run(t: PoseTemplate, mirrored: Boolean, audience: FramingAudience, cfg: MatchConfig, noise: Random? = null, amount: Float = 0f): MatchResult {
        val scene = SceneMapping.templateToScene(t, aspect, mirrored = mirrored)
        val det = scene.map { asDetection(it, noise, amount) }
        return PoseMatcher.match(scene, det.reversed(), aspect, audience, cfg)
    }

    private fun describe(r: MatchResult): String =
        "score=%.3f hints=%s figs=%s".format(r.score, r.hints.map { it.kind.name + ":" + it.text },
            r.figures.map { f -> "%.3f cov=%.2f seg=%s".format(f.score, f.coverage, f.segments.filter { it.required && it.score < 0.99f }.map { it.segment.name + "=" + "%.2f".format(it.score) }) })

    @Test
    fun everyBuiltInPoseReachesPerfectWhenMatchedExactly() {
        val failures = mutableListOf<String>()
        for (t in BuiltInPoses.all) {
            for (mirrored in listOf(false, true)) for (aud in FramingAudience.entries) {
                for (threshold in listOf(0.80f, 0.95f)) {
                    val r = run(t, mirrored, aud, MatchConfig(perfectScore = threshold))
                    if (r.hints.first().kind != HintKind.PERFECT) failures += "${t.id} mirrored=$mirrored $aud thr=$threshold ${describe(r)}"
                }
            }
        }
        failures.forEach(::println)
        assertTrue("${failures.size} exact matches not PERFECT:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun everyBuiltInPoseReachesPerfectWithJitter() {
        val failures = mutableListOf<String>()
        for (t in BuiltInPoses.all) {
            for (mirrored in listOf(false, true)) {
                val rnd = Random(t.id.hashCode().toLong())
                var perfect = 0
                val trials = 10
                var worst: MatchResult? = null
                repeat(trials) {
                    val r = run(t, mirrored, FramingAudience.SUBJECT, MatchConfig(perfectScore = 0.80f), rnd, 0.004f)
                    if (r.hints.first().kind == HintKind.PERFECT) perfect++ else if (worst == null || r.score < worst!!.score) worst = r
                }
                if (perfect < trials * 7 / 10) failures += "${t.id} mirrored=$mirrored perfect=$perfect/$trials worst: ${worst?.let(::describe)}"
            }
        }
        failures.forEach(::println)
        assertTrue("${failures.size} jittered matches rarely PERFECT:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun exactMatchHintsAreSensibleWhenNotPerfect() {
        // With the strictest possible threshold, an exact match must not produce pose corrections.
        val failures = mutableListOf<String>()
        for (t in BuiltInPoses.all) for (aud in FramingAudience.entries) {
            val r = run(t, false, aud, MatchConfig(perfectScore = 1.0f))
            val bad = r.hints.filter { it.kind == HintKind.POSE && !it.text.startsWith("Almost there") || it.kind == HintKind.VISIBILITY || it.kind == HintKind.MISSING_PERSON }
            if (bad.isNotEmpty()) failures += "${t.id} $aud ${describe(r)}"
        }
        failures.forEach(::println)
        assertTrue("${failures.size} exact matches with nonsense hints:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun multiPersonPoseNeverPerfectWithOnePerson() {
        val failures = mutableListOf<String>()
        for (t in BuiltInPoses.all.filter { it.personCount > 1 }) {
            val scene = SceneMapping.templateToScene(t, aspect)
            for (i in scene.indices) {
                val r = PoseMatcher.match(scene, listOf(asDetection(scene[i])), aspect, FramingAudience.SUBJECT, MatchConfig(perfectScore = 0.6f))
                if (r.hints.first().kind == HintKind.PERFECT || r.hints.none { it.kind == HintKind.MISSING_PERSON }) failures += "${t.id} only#$i ${describe(r)}"
            }
        }
        failures.forEach(::println)
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun armsUpVsArmsDownNeverPerfect() {
        val failures = mutableListOf<String>()
        for (t in BuiltInPoses.all.filter { it.personCount == 1 }) {
            val scene = SceneMapping.templateToScene(t, aspect).single()
            // Swing both arms about the shoulders by 150 degrees (arms down <-> arms up).
            val pts = scene.points.toMutableMap()
            for ((sh, js) in listOf(
                Joint.LEFT_SHOULDER to listOf(Joint.LEFT_ELBOW, Joint.LEFT_WRIST, Joint.LEFT_HAND),
                Joint.RIGHT_SHOULDER to listOf(Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST, Joint.RIGHT_HAND),
            )) {
                val c = scene.points.getValue(sh)
                for (j in js) {
                    val p = scene.points.getValue(j)
                    val d = Vec2((p.x - c.x) * aspect, p.y - c.y).rotated(150f)
                    pts[j] = Vec2(c.x + d.x / aspect, c.y + d.y)
                }
            }
            val r = PoseMatcher.match(listOf(scene), listOf(asDetection(Skeleton(pts, scene.visibility, scene.unit))), aspect,
                FramingAudience.SUBJECT, MatchConfig(perfectScore = 0.6f))
            if (r.hints.first().kind == HintKind.PERFECT) failures += "${t.id} ${describe(r)}"
        }
        failures.forEach(::println)
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
    @Test
    fun exactMatchStaysPerfectWhenTheOverlayIsDraggedOrScaled() {
        // Users drag / pinch the overlay and switch to 9:16; an exact match must still be PERFECT. Template joints that
        // land in the 0.02 band just OUTSIDE the scene are required by the matcher but MediaPipe reports them with low
        // visibility, so the pose can never be completed.
        var total = 0
        val failures = mutableListOf<String>()
        for (sceneAspect in listOf(9f / 16f, 3f / 4f)) for (t in BuiltInPoses.all) {
            for (scale in listOf(0.85f, 0.9f, 0.95f, 1f, 1.05f, 1.1f)) for (oy in listOf(-0.06f, -0.03f, 0f, 0.03f, 0.06f)) {
                val scene = SceneMapping.templateToScene(t, sceneAspect, OverlayTransform(0f, oy, scale))
                val r = PoseMatcher.match(scene, scene.map { asDetection(it) }, sceneAspect, FramingAudience.SUBJECT, MatchConfig(perfectScore = 0.8f))
                total++
                if (r.hints.first().kind != HintKind.PERFECT) failures += "aspect=$sceneAspect ${t.id} scale=$scale oy=$oy ${describe(r)}"
            }
        }
        failures.take(40).forEach(::println)
        assertTrue("${failures.size}/$total exact matches not PERFECT, e.g.:\n" + failures.take(20).joinToString("\n"), failures.isEmpty())
    }
}
