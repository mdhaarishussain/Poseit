package com.poseit.app.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.poseit.app.appContainer
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.ui.camera.PoseTipsSheet
import com.poseit.app.ui.components.PhotoCompanionTitles
import com.poseit.app.ui.components.photoCompanionTitle
import com.poseit.app.ui.components.TopBarCard
import com.poseit.app.ui.components.photoCreditLabel
import com.poseit.app.ui.detail.PoseDetailScreen
import com.poseit.app.ui.library.LibraryScreen
import com.poseit.app.ui.library.TodaysPoseCard
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitTheme
import com.poseit.app.ui.theme.Space
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The photo screens on a small phone (360x640 dp) at the default font size. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class PhotoUiSmallPhoneScreenshotTest : PhotoUiSmallPhoneShots("small")

/** The photo screens on a small phone (360x640 dp) with the system font size at 130 %. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
class PhotoUiSmallPhoneLargeFontScreenshotTest : PhotoUiSmallPhoneShots("small-font130")

/** The photo screens on a small phone (360x640 dp) with the system font size at 200 %: the photo moves under the words. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi", fontScale = 2.0f)
class PhotoUiSmallPhoneHugeFontScreenshotTest : PhotoUiSmallPhoneShots("small-font200")

/**
 * The sample photos in the pose guide on a small phone, rendered into app/build/screenshots/photo-ui-<prefix>-*.png:
 * the detail screen (first screen, and scrolled to the whole photo card) for a solo, a couple and the landscape group
 * photo, a pose without a photo, the library, Today's pose and the camera's pose tips.
 */
abstract class PhotoUiSmallPhoneShots(private val prefix: String) {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val photos = InlinePosePhotos()

    private val posePhotos get() = ApplicationProvider.getApplicationContext<Context>().appContainer.posePhotos

    @Before
    fun startWithoutFavourites() = clearFavourites()

    private fun set(content: @Composable () -> Unit) {
        compose.setContent {
            PoseitTheme { Box(Modifier.fillMaxSize().background(PoseitColors.backgroundBody)) { content() } }
        }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/photo-ui-$prefix-$name.png")
    }

    private fun waitText(text: String) =
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun credit(id: String) = hasContentDescription(photoCreditLabel(posePhotos.photoFor(id)!!)) and hasClickAction()

    private fun detail(id: String) {
        set { PoseDetailScreen(poseId = id, onBack = {}, onTryPose = { _, _ -> }, onEdit = {}) }
        waitText("Try this pose")
    }

    /**
     * The detail page scrolled until the photo card's credit, its last line, is on screen. On this short phone the
     * figure's card shrinks, and the photo shrinks with it: it is never as tall as the figure's preview.
     */
    private fun detailPhotoCard(id: String, name: String) {
        detail(id)
        compose.onNode(credit(id)).performScrollTo()
        compose.waitForIdle()
        compose.onNode(credit(id)).assertIsDisplayed()
        compose.onNode(hasContentDescription("Photo of", substring = true)).assertIsDisplayed()
        // Unclipped sizes: the preview may sit partly above the top edge once the card is scrolled into view.
        val figure = compose.onNode(hasContentDescription("Mannequin showing", substring = true)).fetchSemanticsNode().size
        val photo = compose.onNode(hasContentDescription("Photo of", substring = true)).fetchSemanticsNode().size
        assertTrue("photo ${photo.height} px vs figure ${figure.height} px", photo.height < figure.height)
        // At most half the preview card's height (40% of 640dp), as the detail screen asks of its companion.
        assertTrue("photo ${photo.height / 2}dp tall", photo.height <= 0.5f * 256f * 2 + 1)
        capture(name)
    }

    @Test
    fun detail() {
        detail("stand_hand_on_hip")
        compose.onNode(hasText(photoCompanionTitle(1))).assertExists()
        capture("detail")
    }

    @Test
    fun detailPhotoCard() = detailPhotoCard("stand_hand_on_hip", "detail-photo-card")

    @Test
    fun detailCouple() = detailPhotoCard("couple_forehead_touch", "detail-couple")

    @Test
    fun detailGroup() = detailPhotoCard("group_arms_around", "detail-group")

    @Test
    fun detailCommonsCredit() = detailPhotoCard("sit_one_knee_up", "detail-commons-credit")

    @Test
    fun detailWithoutPhoto() {
        detail("stand_hands_in_pockets")
        PhotoCompanionTitles.forEach { compose.onNode(hasText(it)).assertDoesNotExist() }
        capture("detail-no-photo")
    }

    @Test
    fun library() {
        set { LibraryScreen({}, {}, {}, {}, {}, {}) }
        waitText("Today's pose")
        // The first tile, Hand on hip, has a photo: its print is in the grid.
        assertTrue(compose.onAllNodes(credit("stand_hand_on_hip")).fetchSemanticsNodes().isNotEmpty())
        capture("library")
    }

    @Test
    fun todaysPose() {
        set {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Space.edge),
                verticalArrangement = Arrangement.spacedBy(Space.section),
            ) {
                listOf("selfie_peace", "port_hand_in_hair").forEach { id ->
                    TopBarCard(
                        title = "Poseit",
                        wordmark = true,
                        windowInsets = WindowInsets(0),
                        joined = { TodaysPoseCard(BuiltInPoses.byId(id)!!, onOpen = {}, onTry = {}) },
                    )
                }
            }
        }
        capture("today")
    }

    @Test
    fun cameraTips() {
        set { PoseTipsSheet(pose = BuiltInPoses.byId("stand_hand_on_hip")!!, favorite = false, onToggleFavorite = {}, onDismiss = {}) }
        compose.waitForIdle()
        compose.onNode(credit("stand_hand_on_hip")).assertExists()
        captureScreenRoboImage("build/screenshots/photo-ui-$prefix-camera-tips.png")
    }
}
