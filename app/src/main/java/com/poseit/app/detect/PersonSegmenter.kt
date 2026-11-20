package com.poseit.app.detect

import android.content.Context
import android.graphics.Bitmap
import java.io.Closeable

/**
 * Turns a reference photo into a clean "guide" image (SOVS / Superpose-style outline) using MediaPipe
 * ImageSegmenter with assets/models/deeplab_v3.tflite (PASCAL VOC classes, person = 15).
 * Construct and call off the main thread.
 */
class PersonSegmenter(context: Context) : Closeable {

    /**
     * @param bitmap upright reference photo
     * @return ARGB_8888 bitmap with the SAME aspect ratio as [bitmap] (longest side <= 1024 px): transparent background,
     *   a translucent white fill (~25 % alpha) over every person and a crisp white outline (~0.6 % of the image height,
     *   with a thin dark halo so it stays visible on bright scenes) around them. Null if no person was found (< 1 % of
     *   pixels) or segmentation failed.
     */
    fun createGuide(bitmap: Bitmap): Bitmap? {
        TODO("implemented by the camera-infra agent")
    }

    override fun close() {
        TODO("implemented by the camera-infra agent")
    }
}
