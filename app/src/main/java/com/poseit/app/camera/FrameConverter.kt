package com.poseit.app.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import androidx.camera.core.ImageProxy

/**
 * Turns CameraX analysis frames into upright, viewport-cropped, NON-mirrored ARGB_8888 bitmaps.
 *
 * Not thread-safe: use it from the single analysis thread only. The returned bitmap is owned by the converter
 * and is overwritten by the next [convert] call, so consumers must not keep it (copy it if needed).
 */
internal class FrameConverter {

    /** Full-size copy of the camera buffer (reused while the stream size stays the same). */
    private var sourceBuffer: Bitmap? = null

    /** Upright, cropped output (reused while the output size stays the same). */
    private var output: Bitmap? = null
    private var outputCanvas: Canvas? = null

    private val matrix = Matrix()

    // Rotations are multiples of 90° with integer translations, so pixels map 1:1 and need no filtering.
    private val paint = Paint()

    /** Returns the upright frame, or null if the image can't be read. */
    fun convert(image: ImageProxy): Bitmap? {
        if (image.width <= 0 || image.height <= 0) return null
        val source = readPixels(image) ?: return null
        val crop = clampCrop(image.cropRect, source.bitmap.width, source.bitmap.height)
        val rotation = normalizeRotation(image.imageInfo.rotationDegrees)
        val fullFrame = crop.left == 0 && crop.top == 0 &&
            crop.width() == source.bitmap.width && crop.height() == source.bitmap.height
        if (rotation == 0 && fullFrame) return source.bitmap

        val (outWidth, outHeight) = uprightSize(crop.width(), crop.height(), rotation)
        val out = obtainOutput(outWidth, outHeight)
        matrix.reset()
        matrix.postTranslate(-crop.left.toFloat(), -crop.top.toFloat())
        matrix.postRotate(rotation.toFloat())
        // Rotating about the origin moves the cropped rectangle out of the positive quadrant: shift it back.
        when (rotation) {
            90 -> matrix.postTranslate(crop.height().toFloat(), 0f)
            180 -> matrix.postTranslate(crop.width().toFloat(), crop.height().toFloat())
            270 -> matrix.postTranslate(0f, crop.width().toFloat())
        }
        outputCanvas?.drawBitmap(source.bitmap, matrix, paint)
        if (source.temporary) source.bitmap.recycle()
        return out
    }

    /** Drops the cached bitmaps (e.g. after an OutOfMemoryError). */
    fun release() {
        sourceBuffer = null
        output = null
        outputCanvas = null
    }

    private class Source(val bitmap: Bitmap, val temporary: Boolean)

    private fun readPixels(image: ImageProxy): Source? {
        val width = image.width
        val height = image.height
        if (image.format == PixelFormat.RGBA_8888 && image.planes.size == 1) {
            val plane = image.planes[0]
            // Fast path: tightly packed RGBA rows can be copied straight into a reused bitmap.
            if (plane.pixelStride == 4 && plane.rowStride == width * 4) {
                val buffer = plane.buffer
                buffer.rewind()
                if (buffer.remaining() >= width * height * 4) {
                    val bitmap = sourceBuffer?.takeIf { it.width == width && it.height == height && !it.isRecycled }
                        ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { sourceBuffer = it }
                    bitmap.copyPixelsFromBuffer(buffer)
                    return Source(bitmap, temporary = false)
                }
            }
        }
        // Padded rows or an unexpected format: let CameraX do the conversion (allocates a new bitmap).
        val bitmap = image.toBitmap()
        return Source(bitmap, temporary = true)
    }

    private fun obtainOutput(width: Int, height: Int): Bitmap {
        output?.let { if (it.width == width && it.height == height && !it.isRecycled) return it }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output = bitmap
        outputCanvas = Canvas(bitmap)
        return bitmap
    }

    companion object {
        /** Snaps any angle to 0, 90, 180 or 270. */
        fun normalizeRotation(degrees: Int): Int {
            val positive = ((degrees % 360) + 360) % 360
            return ((positive + 45) / 90 % 4) * 90
        }

        /** Width and height of a [width] x [height] buffer after rotating it by [rotation] degrees. */
        fun uprightSize(width: Int, height: Int, rotation: Int): Pair<Int, Int> =
            if (rotation == 90 || rotation == 270) height to width else width to height

        /** Intersects [crop] with the buffer; falls back to the full buffer when the result is empty. */
        fun clampCrop(crop: Rect, width: Int, height: Int): Rect {
            val left = crop.left.coerceIn(0, width)
            val top = crop.top.coerceIn(0, height)
            val right = crop.right.coerceIn(left, width)
            val bottom = crop.bottom.coerceIn(top, height)
            return if (right - left <= 0 || bottom - top <= 0) Rect(0, 0, width, height) else Rect(left, top, right, bottom)
        }
    }
}

/**
 * Makes camera timestamps strictly increasing (MediaPipe's live-stream mode rejects anything else), while keeping
 * the real spacing between frames. A backwards jump (e.g. a camera whose clock source changes after a rebind) is
 * absorbed into a permanent offset instead of collapsing every later frame onto "last + 1".
 */
internal class MonotonicTimestamps {
    private var last = Long.MIN_VALUE
    private var offset = 0L

    fun next(rawMs: Long): Long {
        var t = rawMs + offset
        if (last != Long.MIN_VALUE && t <= last) {
            offset += last + 1 - t
            t = last + 1
        }
        last = t
        return t
    }
}
