package com.poseit.app.pose.detect

import com.poseit.app.pose.Joint
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.abs
import kotlin.math.sqrt

class LandmarkSmootherTest {

    private val frameMs = 33L

    /** A person whose torso is centred at ([cx], 0.5) and whose left wrist is at [wrist]. */
    private fun person(cx: Float, wrist: Vec2 = Vec2(cx + 0.15f, 0.55f), visibility: Float = 0.9f): Skeleton {
        val pts = mapOf(
            Joint.NOSE to Vec2(cx, 0.2f),
            Joint.LEFT_SHOULDER to Vec2(cx + 0.08f, 0.32f),
            Joint.RIGHT_SHOULDER to Vec2(cx - 0.08f, 0.32f),
            Joint.LEFT_HIP to Vec2(cx + 0.04f, 0.68f),
            Joint.RIGHT_HIP to Vec2(cx - 0.04f, 0.68f),
            Joint.LEFT_WRIST to wrist,
        )
        return Skeleton(pts, pts.keys.associateWith { visibility })
    }

    private fun std(values: List<Float>): Float {
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size).toFloat()
    }

    @Test
    fun reducesJitterOfAStillPerson() {
        val smoother = LandmarkSmoother()
        val rnd = Random(42)
        val raw = mutableListOf<Float>()
        val out = mutableListOf<Float>()
        for (i in 0 until 120) {
            val x = 0.65f + (rnd.nextFloat() - 0.5f) * 0.02f
            val y = 0.55f + (rnd.nextFloat() - 0.5f) * 0.02f
            val s = smoother.smooth(listOf(person(0.5f, Vec2(x, y))), i * frameMs).single()
            if (i >= 15) {
                raw += x
                out += s.points.getValue(Joint.LEFT_WRIST).x
            }
        }
        val rawStd = std(raw)
        val outStd = std(out)
        assertTrue("raw $rawStd vs smoothed $outStd", outStd < 0.6f * rawStd)
        assertEquals(0.65f, out.average().toFloat(), 0.004f)
    }

    @Test
    fun followsFastMovesWithLittleLag() {
        val smoother = LandmarkSmoother()
        var t = 0L
        repeat(10) { smoother.smooth(listOf(person(0.5f, Vec2(0.2f, 0.5f))), t); t += frameMs }
        // Sweep the wrist across 60 % of the frame in 300 ms, then hold.
        var lastOut = 0f
        for (k in 1..9) {
            val x = 0.2f + 0.6f * k / 9f
            lastOut = smoother.smooth(listOf(person(0.5f, Vec2(x, 0.5f))), t).single().points.getValue(Joint.LEFT_WRIST).x
            t += frameMs
            if (k >= 4) assertTrue("frame $k lag ${x - lastOut}", x - lastOut < 0.05f)
        }
        assertEquals(0.8f, lastOut, 0.03f)
        repeat(4) {
            lastOut = smoother.smooth(listOf(person(0.5f, Vec2(0.8f, 0.5f))), t).single().points.getValue(Joint.LEFT_WRIST).x
            t += frameMs
        }
        assertEquals("settles within ~130 ms", 0.8f, lastOut, 0.01f)
    }

    @Test
    fun tracksPeopleAcrossFramesEvenWhenOrderChanges() {
        val smoother = LandmarkSmoother()
        val a = person(0.3f)
        val b = person(0.7f)
        smoother.smooth(listOf(a, b), 0)
        val swapped = smoother.smooth(listOf(b.shiftedX(0.004f), a.shiftedX(-0.004f)), frameMs)
        val outB = swapped[0].points.getValue(Joint.NOSE).x
        val outA = swapped[1].points.getValue(Joint.NOSE).x
        assertTrue("B stays near 0.7: $outB", abs(outB - 0.7f) < 0.006f)
        assertTrue("A stays near 0.3: $outA", abs(outA - 0.3f) < 0.006f)
    }

    @Test
    fun dropsTracksAfterAGapAndHandlesClockIssues() {
        val smoother = LandmarkSmoother()
        smoother.smooth(listOf(person(0.5f)), 0)
        // Within the timeout the new position is smoothed...
        val near = smoother.smooth(listOf(person(0.52f)), 300).single()
        assertNotEquals(0.52f, near.points.getValue(Joint.NOSE).x)
        // ...after > 500 ms unseen, the person starts fresh.
        val fresh = smoother.smooth(listOf(person(0.56f)), 1000).single()
        assertEquals(0.56f, fresh.points.getValue(Joint.NOSE).x, 1e-6f)

        // Duplicate timestamp: no time passed, keep the previous estimate (and never NaN).
        val dup = smoother.smooth(listOf(person(0.6f)), 1000).single()
        assertEquals(0.56f, dup.points.getValue(Joint.NOSE).x, 1e-6f)

        // Clock going backwards resets everything.
        val back = smoother.smooth(listOf(person(0.4f)), 10).single()
        assertEquals(0.4f, back.points.getValue(Joint.NOSE).x, 1e-6f)

        smoother.reset()
        val afterReset = smoother.smooth(listOf(person(0.45f)), 20).single()
        assertEquals(0.45f, afterReset.points.getValue(Joint.NOSE).x, 1e-6f)
    }

    @Test
    fun smoothsVisibilityAndKeepsShape() {
        val smoother = LandmarkSmoother()
        smoother.smooth(listOf(person(0.5f, visibility = 1f)), 0)
        val dropped = smoother.smooth(listOf(person(0.5f, visibility = 0f)), frameMs).single()
        val v = dropped.visibility.getValue(Joint.NOSE)
        assertTrue("visibility $v", v in 0.05f..0.95f)

        // Inputs without a visibility map produce outputs without one; units are kept.
        val noVis = Skeleton(mapOf(Joint.NOSE to Vec2(0.5f, 0.5f)), unit = 0.2f)
        val out = LandmarkSmoother().smooth(listOf(noVis), 0).single()
        assertTrue(out.visibility.isEmpty())
        assertEquals(0.2f, out.unit)
    }

    @Test
    fun edgeCases() {
        val smoother = LandmarkSmoother()
        assertTrue(smoother.smooth(emptyList(), 0).isEmpty())
        val empty = Skeleton(emptyMap())
        assertEquals(listOf(empty), smoother.smooth(listOf(empty), 33))
        val nan = Skeleton(mapOf(Joint.NOSE to Vec2(Float.NaN, 0.5f), Joint.LEFT_HIP to Vec2(0.5f, 0.6f)))
        val out = smoother.smooth(listOf(nan), 66).single()
        assertTrue(Joint.NOSE !in out.points)
        out.points.values.forEach { assertTrue(it.x.isFinite() && it.y.isFinite()) }
    }

    @Test
    fun oneEuroFilterBasics() {
        val f = OneEuroFilter(minCutoff = 1f, beta = 0f, dCutoff = 1f)
        assertEquals(5f, f.filter(5f, 0f))
        // beta = 0 makes it a plain low-pass: the output moves toward the input but not all the way.
        val y = f.filter(6f, 1f / 30f)
        assertTrue(y > 5f && y < 6f)
        assertEquals(y, f.filter(100f, 0f)) // dt = 0: unchanged
        assertEquals(y, f.filter(Float.NaN, 0.03f))
    }

    private fun Skeleton.shiftedX(dx: Float) = mapPoints { Vec2(it.x + dx, it.y) }
}
