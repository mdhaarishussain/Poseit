package com.poseit.app.detect

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import java.io.Closeable
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Turns a reference photo into a clean "guide" image (SOVS / Superpose-style outline) using MediaPipe
 * ImageSegmenter with assets/models/deeplab_v3.tflite (PASCAL VOC classes, person = 15).
 * Construct and call off the main thread. Never throws: failures are logged and [createGuide] returns null.
 */
class PersonSegmenter(context: Context) : Closeable {

    private val appContext: Context = context.applicationContext ?: context
    private val lock = Any()

    // Guarded by lock.
    private var closed = false
    private var segmenter: ImageSegmenter? = null
    private var personCategory = PASCAL_VOC_PERSON

    init {
        synchronized(lock) { segmenter = createSegmenter() }
    }

    /**
     * @param bitmap upright reference photo
     * @return ARGB_8888 bitmap with the SAME aspect ratio as [bitmap] (longest side <= 1024 px): transparent background,
     *   a translucent white fill (~25 % alpha) over every person and a crisp white outline (~0.6 % of the image height,
     *   with a thin dark halo so it stays visible on bright scenes) around them. Null if no person was found (< 1 % of
     *   pixels) or segmentation failed.
     */
    fun createGuide(bitmap: Bitmap): Bitmap? {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
        synchronized(lock) {
            if (closed) return null
            val seg = segmenter ?: createSegmenter()?.also { segmenter = it } ?: return null
            var input: Bitmap? = null
            return try {
                val prepared = prepareInput(bitmap)
                input = prepared
                val result = seg.segment(BitmapImageBuilder(prepared).build())
                val maskImage = result.categoryMask().orElse(null) ?: return null
                val mask = try {
                    readPersonMask(maskImage)
                } finally {
                    maskImage.close()
                }
                val pixels = GuideRenderer.render(mask.pixels, mask.width, mask.height) ?: return null
                val guide = Bitmap.createBitmap(pixels, mask.width, mask.height, Bitmap.Config.ARGB_8888)
                if (mask.width == prepared.width && mask.height == prepared.height) {
                    guide
                } else {
                    // The category mask normally matches the input size; keep the promised aspect if it doesn't.
                    Bitmap.createScaledBitmap(guide, prepared.width, prepared.height, true).also {
                        if (it !== guide) guide.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Person segmentation failed", e)
                null
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "Not enough memory to segment the photo", e)
                null
            } finally {
                input?.let { if (it !== bitmap) it.recycle() }
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            try {
                segmenter?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Closing the segmenter failed", e)
            }
            segmenter = null
        }
    }

    private class PersonMask(val pixels: BooleanArray, val width: Int, val height: Int)

    /** Must be called with [lock] held. */
    private fun createSegmenter(): ImageSegmenter? = try {
        val options = ImageSegmenter.ImageSegmenterOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL).setDelegate(Delegate.CPU).build())
            .setRunningMode(RunningMode.IMAGE)
            .setOutputCategoryMask(true)
            .setOutputConfidenceMasks(false)
            .build()
        ImageSegmenter.createFromOptions(appContext, options).also { created ->
            // Trust the model's own label map when it has one; PASCAL VOC's index otherwise.
            val index = runCatching { created.labels }.getOrNull()
                ?.indexOfFirst { it.trim().equals("person", ignoreCase = true) }
                ?: -1
            personCategory = if (index >= 0) index else PASCAL_VOC_PERSON
        }
    } catch (e: Exception) {
        Log.e(TAG, "Couldn't load the segmentation model $MODEL", e)
        null
    } catch (e: LinkageError) {
        Log.e(TAG, "Segmentation isn't supported on this device", e)
        null
    }

    private fun readPersonMask(image: MPImage): PersonMask {
        val width = image.width
        val height = image.height
        val n = width * height
        check(n > 0) { "Empty category mask" }
        val buffer = ByteBufferExtractor.extract(image).duplicate()
        buffer.rewind()
        check(buffer.remaining() >= n) { "Category mask has ${buffer.remaining()} bytes, expected $n" }
        val categories = ByteArray(n)
        buffer.get(categories, 0, n)
        val person = personCategory
        val pixels = BooleanArray(n) { i -> (categories[i].toInt() and 0xFF) == person }
        return PersonMask(pixels, width, height)
    }

    /** ARGB_8888 copy scaled so the longest side is at most [MAX_SIDE] (the original if it already fits). */
    private fun prepareInput(bitmap: Bitmap): Bitmap {
        val argb = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val longest = max(argb.width, argb.height)
        if (longest <= MAX_SIDE) return argb
        val scale = MAX_SIDE.toFloat() / longest
        val width = (argb.width * scale).roundToInt().coerceIn(1, MAX_SIDE)
        val height = (argb.height * scale).roundToInt().coerceIn(1, MAX_SIDE)
        val scaled = Bitmap.createScaledBitmap(argb, width, height, true)
        if (argb !== bitmap && argb !== scaled) argb.recycle()
        return scaled
    }

    private companion object {
        const val TAG = "PersonSegmenter"
        const val MODEL = "models/deeplab_v3.tflite"
        const val PASCAL_VOC_PERSON = 15
        const val MAX_SIDE = 1024
    }
}
