package com.poseit.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.poseit.app.appContainer
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.library.BuiltInPoses
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The app's sample-photo source: credits parsed from the assets, built-in poses only, downsampled WebP decoding. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PosePhotosTest {

    private val photos: PosePhotos get() = ApplicationProvider.getApplicationContext<Context>().appContainer.posePhotos

    @Test
    fun everyShippedPhotoIsListedInLibraryOrder() {
        val shipped = File("src/main/assets/pose_photos").listFiles { f -> f.extension == "webp" }.orEmpty().map { it.nameWithoutExtension }.toSet()
        assertEquals(shipped, photos.all.map { it.poseId }.toSet())
        val order = BuiltInPoses.all.map { it.id }
        assertEquals(photos.all.map { it.poseId }.sortedBy { order.indexOf(it) }, photos.all.map { it.poseId })
    }

    @Test
    fun photoForGivesTheCredit() {
        val p = need(photos.photoFor("stand_hand_on_hip"))
        assertEquals("Photo: Mixkit", p.attribution)
        assertEquals("Hand on hip", p.poseName)
        assertEquals("pose_photos/stand_hand_on_hip.webp", p.assetPath)
        assertEquals("licenses/Mixkit-Stock-Video-Free-License.txt", p.licenceTextPath)
        assertEquals(0.75f, p.aspect, 0.01f)
        assertTrue(p.stillFromVideo)
        assertEquals("Photo of a person in the Hand on hip pose", p.contentDescription)
        assertEquals("Not named on the Mixkit clip page", p.authorForDisplay)
        assertEquals("A still frame from a video, cropped and resized.", p.changesSentence)
    }

    @Test
    fun commonsPhotoKeepsItsAuthorAndChanges() {
        val p = need(photos.photoFor("stand_hand_in_hair"))
        assertEquals(PosePhoto.SOURCE_COMMONS, p.source)
        assertEquals("CC0", p.licence)
        assertTrue(p.mirrored)
        assertEquals(listOf("Cropped", "Flipped left to right", "Resized"), p.plainChanges)
        assertTrue(p.attribution.startsWith("Photo: ") && "Wikimedia Commons" in p.attribution)
        assertTrue(p.pageUrl.startsWith("https://commons.wikimedia.org/"))
    }

    @Test
    fun groupAndCouplePhotos() {
        val group = need(photos.photoFor("group_arms_around"))
        assertTrue(group.isLandscape)
        assertEquals(4f / 3f, group.aspect, 0.01f)
        assertEquals("Photo of a group of people in the Shoulder line-up pose", group.contentDescription)
        val couple = need(photos.photoFor("couple_back_hug"))
        assertEquals("Photo of two people in the Back hug pose", couple.contentDescription)
    }

    @Test
    fun posesWithoutAPhotoAndPeoplesOwnPosesGetNone() {
        assertNull(photos.photoFor("selfie_classic"))
        assertNull(photos.photoFor("custom_0001"))
        val builtIn = BuiltInPoses.byId("stand_hand_on_hip")!!
        assertNotNull(photos.photoFor(builtIn))
        // A pose someone made from a built-in one never inherits its photo.
        assertNull(photos.photoFor(builtIn.copy(source = PoseSource.DRAWN)))
        assertNull(photos.photoFor(builtIn.copy(id = "custom_x", source = PoseSource.FROM_PHOTO)))
    }

    @Test
    fun sampleSizeNeverDropsBelowTheBox() {
        assertEquals(1, PosePhotos.sampleSizeFor(675, 900, 675, 900))
        assertEquals(1, PosePhotos.sampleSizeFor(675, 900, 400, 400))
        assertEquals(2, PosePhotos.sampleSizeFor(675, 900, 300, 400))
        assertEquals(4, PosePhotos.sampleSizeFor(675, 900, 100, 133))
        assertEquals(8, PosePhotos.sampleSizeFor(675, 900, 84, 112))
        assertEquals(1, PosePhotos.sampleSizeFor(675, 900, 0, 0))
        // A landscape photo in a portrait box: the height decides.
        assertEquals(2, PosePhotos.sampleSizeFor(900, 675, 200, 300))
    }

    @Test
    fun decodesDownsampledAndCaches() = runBlocking {
        val p = photos.photoFor("port_dreamy_gaze")!!
        assertNull(photos.cachedBitmap(p, 100, 133))
        val bmp = need(photos.bitmap(p, 100, 133))
        assertTrue("decoded at a quarter: ${bmp.width}x${bmp.height}", bmp.width in 168..169 && bmp.height == 225)
        val px = IntArray(bmp.width * bmp.height)
        bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        assertTrue("the photo has real pixels", px.toSet().size > 1000)
        assertSame(bmp, photos.cachedBitmap(p, 100, 133))
        assertSame(bmp, photos.bitmap(p, 110, 140))
    }

    @Test
    fun readsTheLicenceText() = runBlocking {
        val p = photos.photoFor("sit_one_knee_up")!!
        assertEquals("CC BY 2.0", p.licence)
        assertTrue(photos.licenceText(p.licenceTextPath).contains("Attribution 2.0"))
    }

    private fun <T> need(value: T?): T {
        assertNotNull(value)
        return value!!
    }
}
