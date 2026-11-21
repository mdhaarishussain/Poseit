package com.poseit.app.pose.match

import com.poseit.app.pose.BodyPart
import com.poseit.app.pose.Box

/**
 * A coarse luminance image of the current camera frame: [width] x [height] cells, row-major, each 0..255,
 * covering the SCENE (not mirrored). The app builds it by downsampling the analysis bitmap (e.g. 32 x 43).
 */
class LumaGrid(val width: Int, val height: Int, val values: ByteArray) {
    init { require(values.size == width * height) }
    operator fun get(x: Int, y: Int): Int = values[y * width + x].toInt() and 0xFF
}

enum class LightingIssue { OK, FACE_TOO_DARK, BACKLIT, TOO_BRIGHT, SCENE_TOO_DARK }

/**
 * @param faceLuma mean luminance (0..255) of the face region, or null if no face box
 * @param sceneLuma mean luminance of the whole scene
 * @param hint a CoachHint (kind = FRAMING or VISIBILITY is fine; use HintKind.FRAMING) describing what to do, or null when OK
 */
data class LightingResult(val issue: LightingIssue, val faceLuma: Float?, val sceneLuma: Float, val hint: CoachHint?)

object LightingAnalyzer {

    /** Mean scene luminance below which the whole picture is too dark. */
    const val SCENE_DARK = 45f

    /** Face luminance below which the face is in shadow. */
    const val FACE_DARK = 70f

    /** Face luminance above which the face is blown out. */
    const val FACE_BRIGHT = 235f

    /** Scene luminance above which the picture is overexposed when no face is known. */
    const val SCENE_BRIGHT = 242f

    /** Backlight: background brighter than the face by more than this… */
    const val BACKLIGHT_DIFF = 60f

    /** …and brighter than this in absolute terms. */
    const val BACKLIGHT_MIN_BACKGROUND = 150f

    /**
     * Checks the lighting like a photographer would: is the face bright enough, is the subject backlit
     * (background much brighter than the face), is the scene too dark overall?
     *
     * Rules, first match wins: scene mean < [SCENE_DARK] -> SCENE_TOO_DARK; background (cells outside the face box) more
     * than [BACKLIGHT_DIFF] brighter than the face and above [BACKLIGHT_MIN_BACKGROUND] -> BACKLIT; face <
     * [FACE_DARK] -> FACE_TOO_DARK; face > [FACE_BRIGHT] (or, without a face, scene > [SCENE_BRIGHT]) -> TOO_BRIGHT.
     *
     * @param faceBox face region in SCENE-normalized coordinates (e.g. around nose/eyes/ears), or null
     */
    fun analyze(grid: LumaGrid, faceBox: Box?, audience: FramingAudience = FramingAudience.SUBJECT): LightingResult {
        val cells = grid.width * grid.height
        if (grid.width <= 0 || grid.height <= 0 || cells == 0) return LightingResult(LightingIssue.OK, null, 0f, null)

        var total = 0L
        for (y in 0 until grid.height) for (x in 0 until grid.width) total += grid[x, y]
        val sceneLuma = total.toFloat() / cells

        val face = faceBox?.let { normalize(it) }
        var faceSum = 0L
        var faceCount = 0
        var backgroundSum = 0L
        var backgroundCount = 0
        if (face != null) {
            for (y in 0 until grid.height) {
                val cy = (y + 0.5f) / grid.height
                for (x in 0 until grid.width) {
                    val cx = (x + 0.5f) / grid.width
                    val v = grid[x, y]
                    if (cx >= face.left && cx <= face.right && cy >= face.top && cy <= face.bottom) {
                        faceSum += v
                        faceCount++
                    } else {
                        backgroundSum += v
                        backgroundCount++
                    }
                }
            }
            if (faceCount == 0) {
                // Face smaller than one cell: sample the cell under its centre instead.
                val c = face.center
                val gx = (c.x * grid.width).toInt().coerceIn(0, grid.width - 1)
                val gy = (c.y * grid.height).toInt().coerceIn(0, grid.height - 1)
                val v = grid[gx, gy]
                faceSum = v.toLong()
                faceCount = 1
                backgroundSum = total - v
                backgroundCount = cells - 1
            }
        }
        val faceLuma = if (faceCount > 0) faceSum.toFloat() / faceCount else null
        val backgroundLuma = if (backgroundCount > 0) backgroundSum.toFloat() / backgroundCount else null

        val issue = when {
            sceneLuma < SCENE_DARK -> LightingIssue.SCENE_TOO_DARK
            faceLuma != null && backgroundLuma != null &&
                backgroundLuma - faceLuma > BACKLIGHT_DIFF && backgroundLuma > BACKLIGHT_MIN_BACKGROUND -> LightingIssue.BACKLIT
            faceLuma != null && faceLuma < FACE_DARK -> LightingIssue.FACE_TOO_DARK
            faceLuma != null && faceLuma > FACE_BRIGHT -> LightingIssue.TOO_BRIGHT
            faceLuma == null && sceneLuma > SCENE_BRIGHT -> LightingIssue.TOO_BRIGHT
            else -> LightingIssue.OK
        }
        return LightingResult(issue, faceLuma, sceneLuma, hintFor(issue, faceLuma, sceneLuma, backgroundLuma, audience))
    }

    /** Clamps the box to the scene and fixes swapped edges; null when it is invalid or entirely outside. */
    private fun normalize(b: Box): Box? {
        if (!b.left.isFinite() || !b.top.isFinite() || !b.right.isFinite() || !b.bottom.isFinite()) return null
        val l = minOf(b.left, b.right).coerceIn(0f, 1f)
        val r = maxOf(b.left, b.right).coerceIn(0f, 1f)
        val t = minOf(b.top, b.bottom).coerceIn(0f, 1f)
        val btm = maxOf(b.top, b.bottom).coerceIn(0f, 1f)
        val outside = maxOf(b.left, b.right) < 0f || minOf(b.left, b.right) > 1f ||
            maxOf(b.top, b.bottom) < 0f || minOf(b.top, b.bottom) > 1f
        return if (outside) null else Box(l, t, r, btm)
    }

    private fun hintFor(
        issue: LightingIssue,
        face: Float?,
        scene: Float,
        background: Float?,
        audience: FramingAudience,
    ): CoachHint? {
        val subject = audience == FramingAudience.SUBJECT
        val (text, severity) = when (issue) {
            LightingIssue.OK -> return null
            LightingIssue.SCENE_TOO_DARK -> (
                if (subject) "Too dark. Find more light" else "Too dark. Find more light or turn on the flash"
                ) to 0.55f + 0.3f * (1f - scene / SCENE_DARK)
            LightingIssue.BACKLIT -> (
                if (subject) "Strong light behind you. Turn around" else "They're backlit. Shoot with the light behind you"
                ) to 0.5f + 0.3f * (((background ?: 0f) - (face ?: 0f) - BACKLIGHT_DIFF) / 120f).coerceIn(0f, 1f)
            LightingIssue.FACE_TOO_DARK -> (
                if (subject) "Your face is in shadow. Turn toward the light" else "Their face is in shadow. Turn them toward the light"
                ) to 0.45f + 0.3f * (1f - (face ?: 0f) / FACE_DARK)
            LightingIssue.TOO_BRIGHT -> (
                if (subject) "Too much light on your face. Find some shade" else "The face is overexposed. Find shade or lower the brightness"
                ) to 0.45f
        }
        return CoachHint(
            text = text,
            kind = HintKind.FRAMING,
            severity = severity.coerceIn(0f, 1f),
            part = if (issue == LightingIssue.SCENE_TOO_DARK) null else BodyPart.HEAD,
            speech = text,
        )
    }
}
