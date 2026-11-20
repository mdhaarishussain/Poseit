package com.poseit.app.pose.scene

import com.poseit.app.pose.Box
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneMappingTest {

    private val eps = 1e-5f

    private fun assertBox(expected: Box, actual: Box) {
        assertEquals("left", expected.left, actual.left, eps)
        assertEquals("top", expected.top, actual.top, eps)
        assertEquals("right", expected.right, actual.right, eps)
        assertEquals("bottom", expected.bottom, actual.bottom, eps)
    }

    private fun assertPoint(ex: Float, ey: Float, actual: Pair<Float, Float>) {
        assertEquals("x", ex, actual.first, eps)
        assertEquals("y", ey, actual.second, eps)
    }

    @Test
    fun fitFrameContainsAndCentres() {
        assertBox(Box(0f, 0f, 1f, 1f), SceneMapping.fitFrame(0.75f, 0.75f))
        // Square frame in a 3:4 scene: full width, letterboxed.
        assertBox(Box(0f, 0.125f, 1f, 0.875f), SceneMapping.fitFrame(1f, 0.75f))
        // 3:4 frame in a 9:16 scene: full width, height 0.5625 / 0.75 = 0.75.
        assertBox(Box(0f, 0.125f, 1f, 0.875f), SceneMapping.fitFrame(0.75f, 9f / 16f))
        // 9:16 frame in a 3:4 scene: full height, width 0.75, pillarboxed.
        assertBox(Box(0.125f, 0f, 0.875f, 1f), SceneMapping.fitFrame(9f / 16f, 0.75f))
        // Landscape 4:3 frame in a 3:4 scene.
        val land = SceneMapping.fitFrame(4f / 3f, 0.75f)
        assertEquals(1f, land.width, eps)
        assertEquals(0.75f / (4f / 3f), land.height, eps)
        assertEquals(0.5f, land.center.y, eps)
        // Invalid aspects fall back gracefully.
        assertBox(Box(0f, 0f, 1f, 1f), SceneMapping.fitFrame(Float.NaN, -2f))
    }

    @Test
    fun identityMapsThroughTheFittedRect() {
        val id = OverlayTransform.IDENTITY
        assertPoint(0.5f, 0.5f, SceneMapping.frameToScene(0.5f, 0.5f, 0.75f, 0.75f, id))
        assertPoint(0.2f, 0.9f, SceneMapping.frameToScene(0.2f, 0.9f, 0.75f, 0.75f, id))
        // Square frame: y is squeezed into 0.125..0.875.
        assertPoint(0f, 0.125f, SceneMapping.frameToScene(0f, 0f, 1f, 0.75f, id))
        assertPoint(1f, 0.875f, SceneMapping.frameToScene(1f, 1f, 1f, 0.75f, id))
    }

    @Test
    fun scaleIsAboutTheFittedCentre() {
        val t = OverlayTransform(scale = 2f)
        assertPoint(0.5f, 0.5f, SceneMapping.frameToScene(0.5f, 0.5f, 0.75f, 0.75f, t))
        assertPoint(0.9f, 0.1f, SceneMapping.frameToScene(0.7f, 0.3f, 0.75f, 0.75f, t))
        // Pillarboxed frame (centre x 0.5, width 0.75): frame x 1 -> scene 0.875 -> scaled 1.25.
        assertPoint(1.25f, 0.5f, SceneMapping.frameToScene(1f, 0.5f, 9f / 16f, 0.75f, t))
    }

    @Test
    fun rotationIsClockwiseAndIsotropic() {
        val t = OverlayTransform(rotation = 90f)
        // A point right of centre (isotropic offset 0.25 * 0.75 = 0.1875) ends up straight below the centre.
        assertPoint(0.5f, 0.5f + 0.1875f, SceneMapping.frameToScene(0.75f, 0.5f, 0.75f, 0.75f, t))
        // A point above the centre ends up to the right, converted back to width units.
        assertPoint(0.5f + 0.1f / 0.75f, 0.5f, SceneMapping.frameToScene(0.5f, 0.4f, 0.75f, 0.75f, t))
        // Distances are preserved in isotropic units (no shear).
        val (x, y) = SceneMapping.frameToScene(0.8f, 0.7f, 0.75f, 0.75f, OverlayTransform(rotation = 37f))
        val before = Vec2(0.3f * 0.75f, 0.2f)
        val after = Vec2((x - 0.5f) * 0.75f, y - 0.5f)
        assertEquals(before.length, after.length, eps)
    }

    @Test
    fun offsetsAreAppliedLastInSceneUnits() {
        val t = OverlayTransform(offsetX = 0.1f, offsetY = -0.05f, scale = 2f, rotation = 180f)
        // centre stays centre after scale/rotate, then shifts by the offsets
        assertPoint(0.6f, 0.45f, SceneMapping.frameToScene(0.5f, 0.5f, 0.75f, 0.75f, t))
        // (0.6, 0.5): +0.1 frame x -> scaled 0.2 -> rotated 180 -> -0.2 -> 0.3 -> +0.1 offset
        assertPoint(0.4f, 0.45f, SceneMapping.frameToScene(0.6f, 0.5f, 0.75f, 0.75f, t))
    }

    private val template = PoseTemplate(
        id = "t",
        name = "T",
        category = PoseCategory.STANDING,
        figures = listOf(
            Skeleton(
                points = mapOf(Joint.LEFT_SHOULDER to Vec2(0.6f, 0.3f), Joint.RIGHT_SHOULDER to Vec2(0.4f, 0.3f), Joint.NOSE to Vec2(0.5f, 0.2f)),
                visibility = mapOf(Joint.NOSE to 0.3f),
                unit = 0.2f,
            ),
        ),
        frameAspect = 1f,
    )

    @Test
    fun templateToSceneMapsPointsKeepsVisibilityAndConvertsUnit() {
        val s = SceneMapping.templateToScene(template, 0.75f, OverlayTransform(scale = 1.5f)).single()
        val fit = SceneMapping.fitFrame(1f, 0.75f)
        assertEquals(0.2f * fit.height * 1.5f, s.unit, eps)
        assertEquals(mapOf(Joint.NOSE to 0.3f), s.visibility)
        val expected = SceneMapping.frameToScene(0.6f, 0.3f, 1f, 0.75f, OverlayTransform(scale = 1.5f))
        assertEquals(expected.first, s.points.getValue(Joint.LEFT_SHOULDER).x, eps)
        assertEquals(expected.second, s.points.getValue(Joint.LEFT_SHOULDER).y, eps)

        val unknownUnit = template.copy(figures = listOf(template.figures.single().copy(unit = 0f)))
        assertEquals(0f, SceneMapping.templateToScene(unknownUnit, 0.75f).single().unit)
    }

    @Test
    fun mirroredFlipsAnatomically() {
        val s = SceneMapping.templateToScene(template, 1f, mirrored = true).single()
        // The left shoulder at x 0.6 becomes the RIGHT shoulder at x 0.4 (labels swapped).
        assertEquals(0.4f, s.points.getValue(Joint.RIGHT_SHOULDER).x, eps)
        assertEquals(0.6f, s.points.getValue(Joint.LEFT_SHOULDER).x, eps)
        assertEquals(0.5f, s.points.getValue(Joint.NOSE).x, eps)
        assertTrue(s.points.size == 3)
    }

    @Test
    fun nonFiniteTransformValuesAreIgnored() {
        val t = OverlayTransform(offsetX = Float.NaN, offsetY = Float.POSITIVE_INFINITY, scale = Float.NaN, rotation = Float.NaN)
        assertPoint(0.3f, 0.6f, SceneMapping.frameToScene(0.3f, 0.6f, 0.75f, 0.75f, t))
    }
}
