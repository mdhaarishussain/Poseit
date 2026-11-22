package com.poseit.app.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.poseit.app.appContainer
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.ui.camera.PoseTipsSheet
import com.poseit.app.ui.components.PhotoCompanionTitle
import com.poseit.app.ui.components.TopBarCard
import com.poseit.app.ui.components.photoCreditLabel
import com.poseit.app.ui.components.rememberPosePhoto
import com.poseit.app.ui.detail.PoseDetailScreen
import com.poseit.app.ui.library.LibraryScreen
import com.poseit.app.ui.library.PoseCard
import com.poseit.app.ui.library.TodaysPoseCard
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitTheme
import com.poseit.app.ui.theme.Space
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The sample photos in the pose guide, "figure first, photo as companion": the detail screen's "See it on a person"
 * card (solo, couple, the landscape group photo, a long Commons credit, the mirrored figure), a pose without a
 * photo, the library tiles' photo prints, Today's pose with and without a photo, and the camera's pose tips sheet.
 * Rendered into app/build/screenshots/photo-ui-*.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class PhotoUiScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val photos = InlinePosePhotos()

    private val posePhotos get() = ApplicationProvider.getApplicationContext<Context>().appContainer.posePhotos

    private fun set(content: @Composable () -> Unit) {
        compose.setContent {
            PoseitTheme { Box(Modifier.fillMaxSize().background(PoseitColors.backgroundBody)) { content() } }
        }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/photo-ui-$name.png")
    }

    private fun waitText(text: String) =
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun detail(id: String) {
        set { PoseDetailScreen(poseId = id, onBack = {}, onTryPose = { _, _ -> }, onEdit = {}) }
        waitText("Try this pose")
    }

    /** Scrolls the detail page until [matcher] (e.g. the end of the photo card) is on screen. */
    private fun scrollTo(matcher: SemanticsMatcher) {
        compose.onNode(matcher).performScrollTo()
        compose.waitForIdle()
    }

    private val companion = hasText(PhotoCompanionTitle)
    private fun credit(id: String) = hasContentDescription(photoCreditLabel(posePhotos.photoFor(id)!!)) and hasClickAction()

    @Test
    fun detailWithPhoto() {
        detail("stand_hand_on_hip")
        compose.onNode(companion and isHeading()).assertIsDisplayed()
        compose.onNode(hasContentDescription("Photo of a person in the Hand on hip pose")).assertIsDisplayed()
        // The credit line is a 48dp button that says what it opens.
        val node = compose.onNode(credit("stand_hand_on_hip")).fetchSemanticsNode()
        assertEquals(Role.Button, node.config[SemanticsProperties.Role])
        assertTrue(node.boundsInRoot.height >= 48f * 3 - 1)
        // The figure keeps the stage: the mannequin is still there, and bigger than the photo.
        val figure = compose.onNode(hasContentDescription("Mannequin showing the Hand on hip pose")).fetchSemanticsNode().boundsInRoot
        val photo = compose.onNode(hasContentDescription("Photo of a person in the Hand on hip pose")).fetchSemanticsNode().boundsInRoot
        assertTrue("figure ${figure.height} vs photo ${photo.height}", figure.height > photo.height)
        // TalkBack: heading, the line, the photo, then its credit.
        val order = listOf(companion, hasText("A real photo", substring = true), hasContentDescription("Photo of a person", substring = true), credit("stand_hand_on_hip"))
            .map { compose.onNode(it).fetchSemanticsNode().config.getOrElse(SemanticsProperties.TraversalIndex) { Float.NaN } }
        assertEquals(listOf(0f, 1f, 2f, 3f), order)
        capture("detail-photo")
    }

    @Test
    @Config(qualifiers = "w400dp-h1500dp-xxhdpi")
    fun detailWithPhotoWholeStack() {
        detail("stand_hand_on_hip")
        capture("detail-photo-whole")
    }

    @Test
    fun detailWithoutPhoto() {
        val id = "stand_hands_in_pockets"
        assertNull(posePhotos.photoFor(id))
        detail(id)
        // Figure only: no companion card, no credit, no empty frame.
        compose.onNode(companion).assertDoesNotExist()
        compose.onNode(hasContentDescription("Photo credit", substring = true)).assertDoesNotExist()
        compose.onNode(hasContentDescription("Mannequin showing", substring = true)).assertIsDisplayed()
        capture("detail-no-photo")
    }

    @Test
    fun detailCouple() {
        detail("couple_forehead_touch")
        compose.onNode(hasContentDescription("Photo of two people in the Forehead touch pose")).assertIsDisplayed()
        capture("detail-couple")
    }

    @Test
    fun detailGroupLandscape() {
        val photo = posePhotos.photoFor("group_arms_around")!!
        assertTrue(photo.isLandscape)
        detail("group_arms_around")
        scrollTo(credit("group_arms_around"))
        compose.onNode(hasText("It works with more people too", substring = true)).assertExists()
        compose.onNode(hasContentDescription("Photo of a group of people in the Shoulder line-up pose")).assertIsDisplayed()
        capture("detail-group")
    }

    @Test
    @Config(qualifiers = "w400dp-h1500dp-xxhdpi")
    fun detailGroupWholeStack() {
        detail("group_arms_around")
        capture("detail-group-whole")
    }

    @Test
    fun detailLongCommonsCredit() {
        detail("sit_one_knee_up")
        scrollTo(credit("sit_one_knee_up"))
        compose.onNode(credit("sit_one_knee_up")).assertIsDisplayed()
        capture("detail-commons-credit")
    }

    @Test
    fun detailMirroredFigure() {
        detail("stand_hand_on_hip")
        compose.onNode(hasContentDescription("Mirror") and hasClickAction()).performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        // The photo is a real photograph: it is not flipped with the figure, and the card says so.
        compose.onNode(hasText("The figure is mirrored. The photo shows the original side.")).assertIsDisplayed()
        capture("detail-mirrored")
    }

    @Test
    fun detailCreditOpensTheSheet() {
        detail("stand_hand_on_hip")
        compose.onNode(credit("stand_hand_on_hip")).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("View source")).assertExists()
        compose.onNode(hasText("Read the licence")).assertExists()
        captureScreenRoboImage("build/screenshots/photo-ui-detail-credit-sheet.png")
    }

    @Test
    @Config(qualifiers = "w400dp-h1600dp-xxhdpi")
    fun library() {
        set { LibraryScreen({}, {}, {}, {}, {}, {}) }
        waitText("Today's pose")
        // Tiles of poses with a photo carry the print, a 48dp credit button; tiles without one don't.
        val badge = compose.onAllNodes(credit("stand_hand_on_hip")).onFirst().fetchSemanticsNode()
        assertEquals(Role.Button, badge.config[SemanticsProperties.Role])
        assertTrue(badge.boundsInRoot.width >= 48f * 3 - 1 && badge.boundsInRoot.height >= 48f * 3 - 1)
        capture("library")
    }

    @Test
    fun libraryBadgeOpensTheCredit() {
        set {
            val poses = listOf("stand_hand_on_hip", "stand_hands_in_pockets").map { BuiltInPoses.byId(it)!! }
            Row(Modifier.padding(Space.edge), horizontalArrangement = Arrangement.spacedBy(Space.edge)) {
                poses.forEach { pose ->
                    PoseCard(pose, isFavorite = false, onOpen = {}, onToggleFavorite = {}, onTry = {}, onCustomise = {}, modifier = Modifier.weight(1f))
                }
            }
        }
        assertEquals(1, compose.onAllNodes(hasContentDescription("Photo credit", substring = true)).fetchSemanticsNodes().size)
        compose.onNode(credit("stand_hand_on_hip")).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Sample photo for Hand on hip")).assertExists()
        captureScreenRoboImage("build/screenshots/photo-ui-library-credit-sheet.png")
    }

    /** Every pose with a photo as a library tile, three across at the phone grid's tile width: the print never hides the figure. */
    @Test
    @Config(qualifiers = "w600dp-h4200dp-mdpi")
    fun everyPhotoTile() {
        set {
            Column(Modifier.padding(Space.edge), verticalArrangement = Arrangement.spacedBy(Space.edge)) {
                posePhotos.all.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.edge)) {
                        row.forEach { p ->
                            PoseCard(BuiltInPoses.byId(p.poseId)!!, isFavorite = false, onOpen = {}, onToggleFavorite = {}, onTry = {}, onCustomise = {}, modifier = Modifier.weight(1f), nameLines = 2)
                        }
                        repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
        capture("tiles-all")
    }

    @Test
    @Config(qualifiers = "w400dp-h1300dp-xxhdpi")
    fun todaysPose() {
        set {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Space.edge),
                verticalArrangement = Arrangement.spacedBy(Space.section),
            ) {
                // With a photo (portrait), without one, and the landscape group photo.
                listOf("selfie_peace", "selfie_classic", "group_arms_around").forEach { id ->
                    TopBarCard(
                        title = "Poseit",
                        wordmark = true,
                        windowInsets = WindowInsets(0),
                        joined = { TodaysPoseCard(BuiltInPoses.byId(id)!!, onOpen = {}, onTry = {}) },
                    )
                }
            }
        }
        assertNull(posePhotos.photoFor("selfie_classic"))
        assertEquals(2, compose.onAllNodes(hasContentDescription("Photo credit", substring = true) and hasClickAction()).fetchSemanticsNodes().size)
        capture("today")
    }

    @Test
    fun cameraTipsSheet() {
        set { PoseTipsSheet(pose = BuiltInPoses.byId("stand_hand_on_hip")!!, favorite = false, onToggleFavorite = {}, onDismiss = {}) }
        compose.waitForIdle()
        compose.onNode(companion).assertExists()
        compose.onNode(credit("stand_hand_on_hip")).assertExists()
        captureScreenRoboImage("build/screenshots/photo-ui-camera-tips.png")
    }

    @Test
    fun cameraTipsSheetCoupleMirrored() {
        set { PoseTipsSheet(pose = BuiltInPoses.byId("couple_back_hug")!!, favorite = true, onToggleFavorite = {}, onDismiss = {}, mirrored = true) }
        compose.waitForIdle()
        compose.onNode(hasText("The figure is mirrored", substring = true)).assertExists()
        captureScreenRoboImage("build/screenshots/photo-ui-camera-tips-couple-mirrored.png")
    }

    @Test
    fun cameraTipsSheetWithoutPhoto() {
        set { PoseTipsSheet(pose = BuiltInPoses.byId("selfie_classic")!!, favorite = false, onToggleFavorite = {}, onDismiss = {}) }
        compose.waitForIdle()
        compose.onNode(companion).assertDoesNotExist()
        captureScreenRoboImage("build/screenshots/photo-ui-camera-tips-no-photo.png")
    }

    @Test
    fun posesPeopleMakeNeverGetAPhoto() {
        val builtIn = BuiltInPoses.byId("stand_hand_on_hip")!!
        var fromPhoto: Any? = Unit
        var drawn: Any? = Unit
        var original: Any? = null
        set {
            fromPhoto = rememberPosePhoto(builtIn.copy(source = PoseSource.FROM_PHOTO))
            drawn = rememberPosePhoto(builtIn.copy(source = PoseSource.DRAWN))
            original = rememberPosePhoto(builtIn)
        }
        assertNull(fromPhoto)
        assertNull(drawn)
        assertNotNull(original)
    }
}
