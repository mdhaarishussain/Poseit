package com.poseit.app.pose.match

import com.poseit.app.pose.Box
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LightingAnalyzerTest {

    private val face = Box(0.4f, 0.15f, 0.6f, 0.3f)

    /** A 32 x 40 grid with [background] everywhere and [faceLuma] inside [faceBox]. */
    private fun grid(background: Int, faceLuma: Int = background, faceBox: Box = face, w: Int = 32, h: Int = 40): LumaGrid {
        val values = ByteArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val cx = (x + 0.5f) / w
            val cy = (y + 0.5f) / h
            val inFace = cx >= faceBox.left && cx <= faceBox.right && cy >= faceBox.top && cy <= faceBox.bottom
            values[y * w + x] = (if (inFace) faceLuma else background).toByte()
        }
        return LumaGrid(w, h, values)
    }

    @Test
    fun evenLightIsOk() {
        val r = LightingAnalyzer.analyze(grid(130), face)
        assertEquals(LightingIssue.OK, r.issue)
        assertNull(r.hint)
        assertEquals(130f, r.faceLuma!!, 0.01f)
        assertEquals(130f, r.sceneLuma, 0.01f)
    }

    @Test
    fun faceInShadow() {
        val r = LightingAnalyzer.analyze(grid(background = 120, faceLuma = 40), face)
        assertEquals(LightingIssue.FACE_TOO_DARK, r.issue)
        assertEquals(40f, r.faceLuma!!, 0.01f)
        assertEquals("Your face is in shadow. Turn toward the light", r.hint!!.text)
        assertEquals(HintKind.FRAMING, r.hint!!.kind)
        val p = LightingAnalyzer.analyze(grid(background = 120, faceLuma = 40), face, FramingAudience.PHOTOGRAPHER)
        assertEquals("Their face is in shadow. Turn them toward the light", p.hint!!.text)
    }

    @Test
    fun backlitSubject() {
        val r = LightingAnalyzer.analyze(grid(background = 220, faceLuma = 90), face)
        assertEquals(LightingIssue.BACKLIT, r.issue)
        assertEquals("Strong light behind you. Turn around", r.hint!!.text)
        val p = LightingAnalyzer.analyze(grid(background = 220, faceLuma = 90), face, FramingAudience.PHOTOGRAPHER)
        assertNotEquals(r.hint!!.text, p.hint!!.text)
        // A bright background alone (face well lit) is fine.
        assertEquals(LightingIssue.OK, LightingAnalyzer.analyze(grid(background = 200, faceLuma = 170), face).issue)
        // A large difference against a dim background is not backlight.
        assertEquals(LightingIssue.OK, LightingAnalyzer.analyze(grid(background = 140, faceLuma = 75), face).issue)
    }

    @Test
    fun darkScene() {
        val r = LightingAnalyzer.analyze(grid(background = 25, faceLuma = 30), face)
        assertEquals(LightingIssue.SCENE_TOO_DARK, r.issue)
        assertEquals("Too dark. Find more light", r.hint!!.text)
        val noFace = LightingAnalyzer.analyze(grid(20), null)
        assertEquals(LightingIssue.SCENE_TOO_DARK, noFace.issue)
        assertNull(noFace.faceLuma)
    }

    @Test
    fun overexposedFace() {
        val r = LightingAnalyzer.analyze(grid(background = 150, faceLuma = 250), face)
        assertEquals(LightingIssue.TOO_BRIGHT, r.issue)
        assertNotNull(r.hint)
        assertEquals(LightingIssue.TOO_BRIGHT, LightingAnalyzer.analyze(grid(250), null).issue)
    }

    @Test
    fun noFaceOnlyChecksTheScene() {
        val r = LightingAnalyzer.analyze(grid(background = 130, faceLuma = 20), null)
        assertEquals(LightingIssue.OK, r.issue)
        assertNull(r.faceLuma)
    }

    @Test
    fun oddFaceBoxes() {
        // Smaller than a cell: the cell under its centre is used.
        val tiny = Box(0.501f, 0.201f, 0.502f, 0.202f)
        val r = LightingAnalyzer.analyze(grid(background = 120, faceLuma = 40, faceBox = Box(0.48f, 0.18f, 0.53f, 0.23f)), tiny)
        assertEquals(40f, r.faceLuma!!, 0.01f)
        // Swapped edges are normalised.
        val swapped = Box(face.right, face.bottom, face.left, face.top)
        assertEquals(LightingIssue.FACE_TOO_DARK, LightingAnalyzer.analyze(grid(120, 40), swapped).issue)
        // Entirely outside the scene or non-finite: treated as no face.
        assertNull(LightingAnalyzer.analyze(grid(120, 40), Box(1.2f, 0.1f, 1.4f, 0.3f)).faceLuma)
        assertNull(LightingAnalyzer.analyze(grid(120, 40), Box(Float.NaN, 0.1f, 0.5f, 0.3f)).faceLuma)
        // Face box covering everything: no background, no backlight check.
        val full = LightingAnalyzer.analyze(grid(100), Box(-1f, -1f, 2f, 2f))
        assertEquals(LightingIssue.OK, full.issue)
    }

    @Test
    fun emptyGridDoesNotCrash() {
        val r = LightingAnalyzer.analyze(LumaGrid(0, 0, ByteArray(0)), face)
        assertEquals(LightingIssue.OK, r.issue)
    }
}
