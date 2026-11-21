package com.poseit.app.ui.camera

import com.poseit.app.camera.Lens
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.match.FramingAudience
import com.poseit.app.pose.scene.OverlayTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraLogicTest {

    // ------------------------------------------------------------- overlay gestures

    @Test
    fun `drag converts pixels to scene fractions`() {
        val t = OverlayMath.applyGesture(OverlayTransform.IDENTITY, 100f, -50f, 1f, 0f, 1000f, 500f, flipX = false)
        assertEquals(0.1f, t.offsetX, 1e-6f)
        assertEquals(-0.1f, t.offsetY, 1e-6f)
    }

    @Test
    fun `mirrored preview reverses horizontal drag and rotation`() {
        val t = OverlayMath.applyGesture(OverlayTransform.IDENTITY, 100f, 0f, 1f, 10f, 1000f, 500f, flipX = true)
        assertEquals(-0.1f, t.offsetX, 1e-6f)
        assertEquals(-10f, t.rotation, 1e-6f)
    }

    @Test
    fun `scale and offsets are clamped`() {
        var t = OverlayTransform.IDENTITY
        repeat(20) { t = OverlayMath.applyGesture(t, 5000f, 5000f, 1.5f, 0f, 1000f, 1000f, flipX = false) }
        assertEquals(OverlayTransform.MAX_SCALE, t.scale, 1e-6f)
        assertEquals(OverlayMath.MAX_OFFSET, t.offsetX, 1e-6f)
        assertEquals(OverlayMath.MAX_OFFSET, t.offsetY, 1e-6f)
        repeat(20) { t = OverlayMath.applyGesture(t, 0f, 0f, 0.5f, 0f, 1000f, 1000f, flipX = false) }
        assertEquals(OverlayTransform.MIN_SCALE, t.scale, 1e-6f)
    }

    @Test
    fun `rotation wraps and bad input is ignored`() {
        val t = OverlayMath.applyGesture(OverlayTransform(rotation = 170f), 0f, 0f, 1f, 30f, 100f, 100f, flipX = false)
        assertEquals(-160f, t.rotation, 1e-4f)
        val same = OverlayMath.applyGesture(t, 0f, 0f, Float.NaN, Float.NaN, 100f, 100f, flipX = false)
        assertEquals(t, same)
        assertEquals(t, OverlayMath.applyGesture(t, 10f, 10f, 2f, 5f, 0f, 0f, flipX = false))
        assertEquals(180f, OverlayMath.normalizeDegrees(-180f), 1e-6f)
        assertEquals(0f, OverlayMath.normalizeDegrees(720f), 1e-6f)
    }

    // ------------------------------------------------------------- lighting inputs

    @Test
    fun `luma grid box-averages pixels`() {
        val w = 8
        val h = 4
        // Left half white, right half black.
        val pixels = IntArray(w * h) { i -> if (i % w < w / 2) 0xFFFFFFFF.toInt() else 0xFF000000.toInt() }
        val grid = FrameAnalysis.lumaGrid(pixels, w, h, 2, 1)
        assertEquals(255, grid[0, 0])
        assertEquals(0, grid[1, 0])

        // Pure green -> 0.587 * 255.
        val green = IntArray(16) { 0xFF00FF00.toInt() }
        assertEquals(150, FrameAnalysis.lumaGrid(green, 4, 4, 1, 1)[0, 0])
    }

    @Test
    fun `luma grid follows the frame aspect`() {
        assertEquals(FrameAnalysis.LUMA_COLUMNS to 32, FrameAnalysis.lumaGridSize(480, 640))
        assertEquals(FrameAnalysis.LUMA_COLUMNS to 24, FrameAnalysis.lumaGridSize(500, 500))
        assertEquals(FrameAnalysis.LUMA_COLUMNS to 43, FrameAnalysis.lumaGridSize(540, 960))
    }

    @Test
    fun `face box surrounds the visible face and stays in the scene`() {
        val face = Skeleton(
            points = mapOf(
                Joint.NOSE to Vec2(0.5f, 0.3f),
                Joint.LEFT_EAR to Vec2(0.54f, 0.29f),
                Joint.RIGHT_EAR to Vec2(0.46f, 0.29f),
                Joint.LEFT_SHOULDER to Vec2(0.6f, 0.4f),
                Joint.RIGHT_SHOULDER to Vec2(0.4f, 0.4f),
            ),
        )
        val box = requireNotNull(FrameAnalysis.faceBox(face, 0.75f))
        assertTrue(box.left < 0.46f && box.right > 0.54f)
        assertTrue(box.top < 0.29f && box.bottom > 0.3f)
        assertTrue(box.left >= 0f && box.top >= 0f && box.right <= 1f && box.bottom <= 1f)

        val edge = Skeleton(mapOf(Joint.NOSE to Vec2(0.01f, 0.01f)))
        val clamped = requireNotNull(FrameAnalysis.faceBox(edge, 0.75f))
        assertEquals(0f, clamped.left, 0f)
        assertEquals(0f, clamped.top, 0f)
    }

    @Test
    fun `no visible face means no face box`() {
        val hidden = Skeleton(
            points = mapOf(Joint.NOSE to Vec2(0.5f, 0.3f), Joint.LEFT_HIP to Vec2(0.5f, 0.6f)),
            visibility = mapOf(Joint.NOSE to 0.1f),
        )
        assertNull(FrameAnalysis.faceBox(hidden, 0.75f))
        assertNull(FrameAnalysis.faceBox(Skeleton(emptyMap()), 0.75f))
    }

    // ------------------------------------------------------------- screen options

    @Test
    fun `audience follows the lens in auto mode`() {
        assertEquals(FramingAudience.SUBJECT, AudienceMode.AUTO.resolve(Lens.FRONT))
        assertEquals(FramingAudience.PHOTOGRAPHER, AudienceMode.AUTO.resolve(Lens.BACK))
        assertEquals(FramingAudience.SUBJECT, AudienceMode.SUBJECT.resolve(Lens.BACK))
        assertEquals(FramingAudience.PHOTOGRAPHER, AudienceMode.PHOTOGRAPHER.resolve(Lens.FRONT))
    }

    @Test
    fun `timer labels`() {
        assertNull(TimerOption.Off.shortLabel)
        assertEquals("5s", TimerOption.Delay(5).shortLabel)
        assertEquals("×10", TimerOption.Photoshoot(10, 3).shortLabel)
        assertEquals("∞", TimerOption.Photoshoot(0, 3).shortLabel)
        assertEquals("3 / 10", SessionInfo(3, 10).label)
        assertEquals("4", SessionInfo(4, 0).label)
        assertTrue(SessionInfo(1, 0).isPhotoshoot)
    }

    @Test
    fun `strip shows favourites then the category and always the current pose`() {
        fun pose(id: String, category: PoseCategory, source: PoseSource = PoseSource.BUILT_IN) =
            PoseTemplate(id, id, category, listOf(Skeleton(mapOf(Joint.NOSE to Vec2(0.5f, 0.2f)))), source = source)
        val a = pose("a", PoseCategory.STANDING)
        val b = pose("b", PoseCategory.STANDING)
        val c = pose("c", PoseCategory.SITTING)
        val mine = pose("mine", PoseCategory.STANDING, PoseSource.FROM_PHOTO)
        val all = listOf(mine, a, b, c)

        assertEquals(listOf("c", "a", "b"), buildStripPoses(all, setOf("c"), PoseCategory.STANDING, a).map { it.id })
        assertEquals(listOf("mine"), buildStripPoses(all, emptySet(), PoseCategory.CUSTOM, mine).map { it.id })
        // The current pose is prepended when it is not part of the strip.
        assertEquals(listOf("c", "a", "b"), buildStripPoses(all, emptySet(), PoseCategory.STANDING, c).map { it.id })
        assertTrue(buildStripPoses(emptyList(), emptySet(), PoseCategory.STANDING, null).isEmpty())
    }

    @Test
    fun `overlay modes depend on the decoded images`() {
        val state = CameraUiState()
        assertEquals(listOf(OverlayMode.MANNEQUIN), state.availableOverlayModes)
        assertTrue(state.handRaiseActive)
        assertEquals(false, state.copy(templateRaisesHand = true).handRaiseActive)
    }
}
