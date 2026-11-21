package com.poseit.app.review

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.poseit.app.PoseitApp
import com.poseit.app.ui.create.PhotoLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** "Create from photo" starts by decoding the picked image; it must succeed for a perfectly normal PNG / JPEG. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PhotoLoaderReviewTest {

    private val app: PoseitApp get() = ApplicationProvider.getApplicationContext()

    private fun register(name: String, width: Int, height: Int, format: Bitmap.CompressFormat): Uri {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF7090B0.toInt()) }
        val bytes = ByteArrayOutputStream().also { bmp.compress(format, 90, it) }.toByteArray()
        val uri = Uri.parse("content://com.example.photos/picked/$name")
        shadowOf(app.contentResolver).registerInputStreamSupplier(uri) { ByteArrayInputStream(bytes) }
        return uri
    }

    @Test
    fun smallPng_decodesAtFullSize() {
        val bitmap = PhotoLoader.loadUpright(app, register("small", 300, 400, Bitmap.CompressFormat.PNG), 1600)
        assertNotNull("A valid 300x400 PNG could not be opened", bitmap)
        assertEquals(300, bitmap!!.width)
        assertEquals(400, bitmap.height)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun largeJpeg_isDownscaledToMaxSide() {
        val bitmap = PhotoLoader.loadUpright(app, register("large", 4000, 3000, Bitmap.CompressFormat.JPEG), 1600)
        assertNotNull("A valid 4000x3000 JPEG could not be opened", bitmap)
        assertEquals(1600, maxOf(bitmap!!.width, bitmap.height))
    }
}
