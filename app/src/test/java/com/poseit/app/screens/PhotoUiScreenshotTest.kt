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
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import com.poseit.app.ui.components.PhotoCompanionCard
import com.poseit.app.ui.components.PhotoCompanionTitles
import com.poseit.app.ui.components.TopBarCard
import com.poseit.app.ui.components.photoBadgeLabel
import com.poseit.app.ui.components.photoCompanionTitle
import com.poseit.app.ui.components.photoCreditLabel
import com.poseit.app.ui.components.rememberPosePhoto
import com.poseit.app.ui.detail.PoseDetailScreen
import com.poseit.app.ui.library.LibraryScreen
import com.poseit.app.ui.library.PoseCard
import com.poseit.app.ui.library.TodaysPoseCard
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitTheme
import com.poseit.app.ui.theme.Space
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
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

    /** The companion card's heading for a solo pose: "See it on a person". */
    private val companion = hasText(photoCompanionTitle(1))
    private fun credit(id: String) = hasContentDescription(photoCreditLabel(posePhotos.photoFor(id)!!)) and hasClickAction()

    /** A library tile's print, the photo's credit button: it says what the photo shows and whose it is. */
    private fun badge(id: String) = hasContentDescription(photoBadgeLabel(posePhotos.photoFor(id)!!)) and hasClickAction()

    private fun assertNoCompanion() = PhotoCompanionTitles.forEach { compose.onNode(hasText(it)).assertDoesNotExist() }

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
        // TalkBack: heading, the line, the photo, then its credit. traversalIndex only orders siblings inside one
        // traversal group, so the four nodes must share one parent, and that parent must be the group.
        val nodes: List<SemanticsNode> = listOf(companion, hasText("A real photo", substring = true), hasContentDescription("Photo of a person", substring = true), credit("stand_hand_on_hip"))
            .map { compose.onNode(it).fetchSemanticsNode() }
        val order = nodes.map { it.config.getOrElse(SemanticsProperties.TraversalIndex) { Float.NaN } }
        assertEquals(listOf(0f, 1f, 2f, 3f), order)
        assertEquals("the four nodes share one semantics parent", 1, nodes.map { it.parent?.id }.toSet().size)
        assertTrue("their parent is the traversal group", nodes[0].parent!!.config.getOrElse(SemanticsProperties.IsTraversalGroup) { false })
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
        // Figure only: no companion card (under any of its headings), no credit, no empty frame.
        assertNoCompanion()
        compose.onNode(hasContentDescription("Photo credit", substring = true)).assertDoesNotExist()
        compose.onNode(hasContentDescription("Mannequin showing", substring = true)).assertIsDisplayed()
        capture("detail-no-photo")
    }

    @Test
    fun detailCouple() {
        detail("couple_forehead_touch")
        // The heading says who is in the picture: two people, not "a person".
        compose.onNode(hasText(photoCompanionTitle(2)) and isHeading()).assertIsDisplayed()
        compose.onNode(companion).assertDoesNotExist()
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
        compose.onNode(hasText("See it on a group") and isHeading()).assertExists()
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
        compose.onNode(hasText("Source page")).assertExists()
        compose.onNode(hasText("Read the licence text")).assertExists()
        captureScreenRoboImage("build/screenshots/photo-ui-detail-credit-sheet.png")
    }

    @Test
    @Config(qualifiers = "w400dp-h1600dp-xxhdpi")
    fun library() {
        set { LibraryScreen({}, {}, {}, {}, {}, {}) }
        waitText("Today's pose")
        // Tiles of poses with a photo carry the print, a 48dp credit button. Its label names the pose's photo, so
        // the match can only be the Hand on hip tile's print: never Today's pose credit line, whatever the date.
        val badges = compose.onAllNodes(badge("stand_hand_on_hip")).fetchSemanticsNodes()
        assertEquals(1, badges.size)
        val print = badges.single()
        assertEquals(Role.Button, print.config[SemanticsProperties.Role])
        assertTrue(print.boundsInRoot.width >= 48f * 3 - 1 && print.boundsInRoot.height >= 48f * 3 - 1)
        // Tiles without a photo have no print: Hands in pockets is on screen, with no photo of it anywhere.
        assertNull(posePhotos.photoFor("stand_hands_in_pockets"))
        compose.onNode(hasText("Hands in pockets")).assertExists()
        compose.onNode(hasContentDescription("in the Hands in pockets pose", substring = true)).assertDoesNotExist()
        capture("library")
        // The grid hoists one credit sheet for every tile; the print opens it with the photo at a readable size.
        compose.onNode(badge("stand_hand_on_hip")).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Sample photo for Hand on hip")).assertExists()
        compose.onNode(hasContentDescription("Photo of a person in the Hand on hip pose")).assertExists()
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
        // The print is a button with a 48dp target, and TalkBack hears what the photo shows before whose it is.
        val print = compose.onNode(badge("stand_hand_on_hip")).fetchSemanticsNode()
        assertEquals(Role.Button, print.config[SemanticsProperties.Role])
        assertTrue(print.boundsInRoot.width >= 48f * 3 - 1 && print.boundsInRoot.height >= 48f * 3 - 1)
        assertEquals("Photo of a person in the Hand on hip pose. Photo credit: Mixkit", photoBadgeLabel(posePhotos.photoFor("stand_hand_on_hip")!!))
        compose.onNode(badge("stand_hand_on_hip")).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Sample photo for Hand on hip")).assertExists()
        // The sheet starts with the photo itself, big enough to see the pose, beside the pose's figure.
        val photo = compose.onNode(hasContentDescription("Photo of a person in the Hand on hip pose")).fetchSemanticsNode()
        assertTrue("photo ${photo.boundsInRoot.width / 3}dp wide", photo.boundsInRoot.width >= 100f * 3)
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
        // The print says what it shows (it joins the card's label); the long Commons credit of the group photo
        // makes its print the credit button instead of a three-line block of text.
        compose.onNode(hasContentDescription("Photo of a person in the Peace sign pose", substring = true), useUnmergedTree = true).assertExists()
        compose.onNode(badge("group_arms_around")).assertExists()
        capture("today")
    }

    @Test
    fun cameraTipsSheet() {
        set { PoseTipsSheet(pose = BuiltInPoses.byId("stand_hand_on_hip")!!, favorite = false, onToggleFavorite = {}, onDismiss = {}) }
        compose.waitForIdle()
        compose.onNode(companion).assertExists()
        compose.onNode(credit("stand_hand_on_hip")).assertExists()
        // The compact companion's photo stays smaller than the card's figure (drawn in a 112 x 132dp box): at most
        // three quarters of the box's height.
        val photo = compose.onNode(hasContentDescription("Photo of a person in the Hand on hip pose")).fetchSemanticsNode().boundsInRoot
        assertTrue("photo ${photo.height / 3}dp tall", photo.height <= 99f * 3 + 1)
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
        assertNoCompanion()
        captureScreenRoboImage("build/screenshots/photo-ui-camera-tips-no-photo.png")
    }

    /**
     * A photo whose file can't be decoded must not leave an empty frame with a credit under it: the pose loses its
     * photo and falls back to the figure-only layout.
     */
    @Test
    fun aPhotoThatFailsToDecodeFallsBackToTheFigure() {
        val id = "port_chin_on_hand"
        val real = posePhotos.photoFor(id)!!
        posePhotos.clear()
        try {
            set {
                // The pose's companion card, its photo pointed at a file that is not there, as a damaged asset is.
                val photo = rememberPosePhoto(id)
                if (photo != null) PhotoCompanionCard(photo.copy(assetPath = "pose_photos/missing.webp"))
            }
            compose.waitUntil(5_000) { posePhotos.isBroken(id) }
            compose.waitForIdle()
            assertNull(posePhotos.photoFor(id))
            assertNoCompanion()
            compose.onNode(hasContentDescription("Photo credit", substring = true)).assertDoesNotExist()
            compose.onNode(hasContentDescription("Photo of a person", substring = true)).assertDoesNotExist()
        } finally {
            posePhotos.forgetFailures()
        }
        assertNotNull(posePhotos.photoFor(real.poseId))
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

/**
 * Favourites live in a DataStore that outlives one test class in the same JVM, so a heart another class tapped would
 * show up filled in these captures. Every photo capture starts with none.
 */
internal fun clearFavourites() = runBlocking {
    val poses = ApplicationProvider.getApplicationContext<Context>().appContainer.poseRepository
    poses.favorites.first().forEach { poses.toggleFavorite(it) }
}
