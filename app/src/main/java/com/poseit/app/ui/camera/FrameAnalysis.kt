package com.poseit.app.ui.camera

import android.graphics.Bitmap
import android.util.Log
import com.poseit.app.pose.Box
import com.poseit.app.pose.Joint
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.match.LumaGrid
import kotlin.math.max
import kotlin.math.roundToInt

/** Helpers that turn camera frames and detections into inputs for the lighting check. */
internal object FrameAnalysis {

    /** Columns of the luma grid; rows follow the frame aspect (24 x 32 for 3:4). */
    const val LUMA_COLUMNS = 24

    /** The bitmap is first scaled to this many pixels per grid cell (per axis), then box-averaged. */
    private const val OVERSAMPLE = 4

    private const val TAG = "FrameAnalysis"

    fun lumaGridSize(width: Int, height: Int): Pair<Int, Int> {
        val aspect = if (height > 0) width.toFloat() / height else 0.75f
        val rows = (LUMA_COLUMNS / aspect.coerceAtLeast(0.1f)).roundToInt().coerceIn(8, 64)
        return LUMA_COLUMNS to rows
    }

    /**
     * Downsamples an upright scene frame into a [LumaGrid] (luma = 0.299 R + 0.587 G + 0.114 B).
     * Returns null for empty or unreadable bitmaps. Does not keep [bitmap].
     */
    fun buildLumaGrid(bitmap: Bitmap): LumaGrid? {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
        return try {
            val (gridW, gridH) = lumaGridSize(bitmap.width, bitmap.height)
            val sw = gridW * OVERSAMPLE
            val sh = gridH * OVERSAMPLE
            val scaled = Bitmap.createScaledBitmap(bitmap, sw, sh, true)
            val pixels = IntArray(sw * sh)
            scaled.getPixels(pixels, 0, sw, 0, 0, sw, sh)
            if (scaled !== bitmap) scaled.recycle()
            lumaGrid(pixels, sw, sh, gridW, gridH)
        } catch (e: Exception) {
            Log.w(TAG, "Could not build luma grid", e)
            null
        }
    }

    /** Box-averages ARGB [pixels] ([width] x [height], row-major) into a [gridW] x [gridH] luma grid. */
    fun lumaGrid(pixels: IntArray, width: Int, height: Int, gridW: Int, gridH: Int): LumaGrid {
        require(width > 0 && height > 0 && gridW > 0 && gridH > 0 && pixels.size >= width * height)
        val sums = FloatArray(gridW * gridH)
        val counts = IntArray(gridW * gridH)
        for (y in 0 until height) {
            val gy = y * gridH / height
            val row = y * width
            for (x in 0 until width) {
                val c = pixels[row + x]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val cell = gy * gridW + x * gridW / width
                sums[cell] += 0.299f * r + 0.587f * g + 0.114f * b
                counts[cell]++
            }
        }
        val values = ByteArray(gridW * gridH) { i ->
            val v = if (counts[i] > 0) sums[i] / counts[i] else 0f
            v.roundToInt().coerceIn(0, 255).toByte()
        }
        return LumaGrid(gridW, gridH, values)
    }

    /**
     * Face region (SCENE-normalized) around a person's visible nose / eyes / ears, expanded to cover the
     * whole face, or null when no face landmark is visible.
     */
    fun faceBox(person: Skeleton, sceneAspect: Float, minVisibility: Float = 0.5f): Box? {
        val aspect = sceneAspect.coerceAtLeast(0.1f)
        val face = Joint.FACE.filter { person.isVisible(it, minVisibility) }.mapNotNull { person[it] }
        if (face.isEmpty()) return null
        // Work in isotropic units (x * aspect) so the box has real-world proportions.
        val iso = face.map { Vec2(it.x * aspect, it.y) }
        val bounds = Box.around(iso) ?: return null
        val unitFace = person.estimatedUnit(aspect) * 0.42f // ear-to-ear width of an average head
        val span = max(max(bounds.width, bounds.height), unitFace)
        val halfW = span * 0.65f
        val halfH = span * 0.85f
        val c = bounds.center
        val cy = c.y - span * 0.05f
        val box = Box(
            left = ((c.x - halfW) / aspect).coerceIn(0f, 1f),
            top = (cy - halfH).coerceIn(0f, 1f),
            right = ((c.x + halfW) / aspect).coerceIn(0f, 1f),
            bottom = (cy + halfH).coerceIn(0f, 1f),
        )
        return box.takeIf { it.width > 1e-3f && it.height > 1e-3f }
    }
}
