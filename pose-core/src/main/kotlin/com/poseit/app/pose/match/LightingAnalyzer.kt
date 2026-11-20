package com.poseit.app.pose.match

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
    /**
     * Checks the lighting like a photographer would: is the face bright enough, is the subject backlit
     * (background much brighter than the face), is the scene too dark overall?
     * @param faceBox face region in SCENE-normalized coordinates (e.g. around nose/eyes/ears), or null
     */
    fun analyze(grid: LumaGrid, faceBox: Box?, audience: FramingAudience = FramingAudience.SUBJECT): LightingResult {
        TODO("implemented by the engine agent")
    }
}
