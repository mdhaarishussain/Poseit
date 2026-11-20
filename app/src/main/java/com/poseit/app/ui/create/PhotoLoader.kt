package com.poseit.app.ui.create

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/** Decoding and encoding of reference photos. Everything here is blocking: call it on Dispatchers.IO. */
internal object PhotoLoader {

    private const val TAG = "PhotoLoader"

    /**
     * Decodes [uri] upright (EXIF orientation applied), downscaled so the longest side is at most [maxSide] px, as an
     * ARGB_8888 bitmap (what MediaPipe expects). Returns null when the image can't be read.
     */
    fun loadUpright(context: Context, uri: Uri, maxSide: Int = 1600): Bitmap? = try {
        decode(context, uri, maxSide)
    } catch (e: IOException) {
        Log.w(TAG, "Could not read $uri", e)
        null
    } catch (e: SecurityException) {
        Log.w(TAG, "No access to $uri", e)
        null
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "Could not decode $uri", e)
        null
    } catch (e: OutOfMemoryError) {
        Log.w(TAG, "Image too large: $uri", e)
        null
    }

    private fun decode(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Power-of-two subsampling while the result stays >= maxSide, then an exact scale below.
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

        val orientation = try {
            resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
                ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        } catch (e: RuntimeException) {
            // Some files have broken EXIF blocks; treat them as upright.
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = orientationMatrix(orientation)
        val scale = min(1f, maxSide.toFloat() / max(decoded.width, decoded.height))
        if (scale < 1f) matrix.postScale(scale, scale)
        var result = if (matrix.isIdentity) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it !== decoded) decoded.recycle()
            }
        }
        if (result.config != Bitmap.Config.ARGB_8888) {
            val converted = result.copy(Bitmap.Config.ARGB_8888, false)
            if (converted !== result) result.recycle()
            result = converted
        }
        return result
    }

    /** The transform that turns stored pixels upright for an EXIF orientation value. */
    fun orientationMatrix(orientation: Int): Matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                setRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                setRotate(-90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            else -> reset()
        }
    }

    /** Writes [bitmap] atomically (temp file + rename). Returns false on failure (nothing is left behind). */
    fun write(bitmap: Bitmap, file: File, format: Bitmap.CompressFormat, quality: Int): Boolean {
        val tmp = File(file.parentFile, file.name + ".tmp")
        return try {
            file.parentFile?.mkdirs()
            val ok = FileOutputStream(tmp).use { out -> bitmap.compress(format, quality, out).also { out.fd.sync() } }
            if (!ok) {
                tmp.delete()
                return false
            }
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
            true
        } catch (e: IOException) {
            Log.w(TAG, "Could not write $file", e)
            tmp.delete()
            false
        }
    }
}
