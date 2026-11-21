package com.poseit.app.pose.detect

import com.poseit.app.pose.Joint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LandmarkMappingTest {

    private val eps = 1e-5f

    /** [n] landmarks; landmark i sits at (x(i), y(i)). */
    private fun landmarks(x: (Int) -> Float = { 0.5f }, y: (Int) -> Float = { 0.5f }, n: Int = 33) =
        List(n) { RawLandmark(x(it), y(it), visibility = 0.9f, presence = 0.8f) }

    @Test
    fun wideImageCropsTheSides() {
        // 640x480 landscape (a = 4/3) into a 3:4 scene: f = 0.75 / (4/3) = 0.5625
        val f = 0.75f / (4f / 3f)
        val left = (1f - f) / 2f
        val lms = landmarks(x = { i -> if (i == 11) left else if (i == 12) left + f else 0.5f }, y = { 0.3f })
        val s = LandmarkMapping.toScene(lms, 640, 480, 0.75f)
        assertEquals(0f, s.points.getValue(Joint.LEFT_SHOULDER).x, eps)
        assertEquals(1f, s.points.getValue(Joint.RIGHT_SHOULDER).x, eps)
        assertEquals(0.5f, s.points.getValue(Joint.NOSE).x, eps)
        assertEquals(0.3f, s.points.getValue(Joint.NOSE).y, eps) // y untouched
    }

    @Test
    fun tallImageCropsTopAndBottom() {
        // 720x1280 (a = 9/16) into a 3:4 scene: f = 0.5625 / 0.75 = 0.75, offset 0.125
        val lms = landmarks(x = { 0.2f }, y = { i -> if (i == 0) 0.125f else if (i == 27) 0.875f else 0.5f })
        val s = LandmarkMapping.toScene(lms, 720, 1280, 0.75f)
        assertEquals(0f, s.points.getValue(Joint.NOSE).y, eps)
        assertEquals(1f, s.points.getValue(Joint.LEFT_ANKLE).y, eps)
        assertEquals(0.5f, s.points.getValue(Joint.LEFT_HIP).y, eps)
        assertEquals(0.2f, s.points.getValue(Joint.NOSE).x, eps) // x untouched
    }

    @Test
    fun sameAspectIsIdentityAndPointsOutsideStayOutside() {
        val lms = landmarks(x = { i -> i / 40f }, y = { i -> 1.2f - i / 30f })
        val s = LandmarkMapping.toScene(lms, 480, 640, 0.75f)
        for (j in Joint.entries) {
            assertEquals(j.mpIndex / 40f, s.points.getValue(j).x, eps)
            assertEquals(1.2f - j.mpIndex / 30f, s.points.getValue(j).y, eps)
        }
        assertTrue(s.points.getValue(Joint.NOSE).y > 1f)
    }

    @Test
    fun keepsOnlyPoseitJointsWithMinConfidence() {
        val s = LandmarkMapping.toScene(landmarks(), 480, 640, 0.75f)
        assertEquals(Joint.entries.toSet(), s.points.keys)
        assertEquals(0.8f, s.visibility.getValue(Joint.LEFT_WRIST), eps) // min(0.9, 0.8)
        val lowVis = List(33) { RawLandmark(0.5f, 0.5f, visibility = 0.3f, presence = 0.95f) }
        assertEquals(0.3f, LandmarkMapping.toScene(lowVis, 480, 640, 0.75f).visibility.getValue(Joint.NOSE), eps)
        assertEquals(0f, s.unit)
    }

    @Test
    fun missingIndicesAndBadValuesAreSkipped() {
        val partial = LandmarkMapping.toScene(landmarks(n = 15), 480, 640, 0.75f)
        assertTrue(Joint.RIGHT_ELBOW in partial.points) // index 14
        assertFalse(Joint.LEFT_WRIST in partial.points) // index 15
        assertFalse(Joint.LEFT_HIP in partial.points)

        val bad = List(33) { i ->
            when (i) {
                0 -> RawLandmark(Float.NaN, 0.5f)
                11 -> RawLandmark(0.5f, 0.5f, visibility = Float.NaN)
                else -> RawLandmark(0.5f, 0.5f)
            }
        }
        val s = LandmarkMapping.toScene(bad, 0, 0, Float.NaN)
        assertFalse(Joint.NOSE in s.points)
        assertEquals(0f, s.visibility.getValue(Joint.LEFT_SHOULDER))
        assertEquals(0.5f, s.points.getValue(Joint.RIGHT_SHOULDER).x, eps)
        assertTrue(LandmarkMapping.toScene(emptyList(), 480, 640, 0.75f).points.isEmpty())
    }
}
