package com.poseit.app.screens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.poseit.app.appContainer
import com.poseit.app.data.PosePhoto
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.ui.components.CardSegment
import com.poseit.app.ui.components.ConnectedCardStack
import com.poseit.app.ui.components.PhotoCredit
import com.poseit.app.ui.components.PhotoCreditContent
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.PosePhoto
import com.poseit.app.ui.components.SheetHandle
import com.poseit.app.ui.settings.PhotoCreditsScreen
import com.poseit.app.ui.settings.SettingsScreen
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitTheme
import com.poseit.app.ui.theme.Space
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The sample photos' credits (docs/POSE_PHOTOS.md): Settings > About > Photo credits, the credit sheet, the licence
 * page, a credit line opening its sheet, and the About card with its new link. Rendered into
 * app/build/screenshots/photo-credits*.png and settings-about.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class PhotoCreditsScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val photos = InlinePosePhotos()

    private fun set(content: @Composable () -> Unit) {
        compose.setContent {
            PoseitTheme { Box(Modifier.fillMaxSize().background(PoseitColors.backgroundBody)) { content() } }
        }
        compose.waitForIdle()
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("build/screenshots/$name.png")

    @Composable
    private fun photo(id: String): PosePhoto = LocalContext.current.appContainer.posePhotos.photoFor(id)!!

    @Test
    fun photoCreditsScreen() {
        set { PhotoCreditsScreen(onBack = {}) }
        compose.onNode(hasText("Photo credits")).assertIsDisplayed()
        compose.onNode(hasText("Hand on hip") and hasClickAction(), useUnmergedTree = false).assertExists()
        capture("photo-credits")
    }

    @Test
    @Config(qualifiers = "w400dp-h4200dp-mdpi")
    fun photoCreditsWholeList() {
        set { PhotoCreditsScreen(onBack = {}) }
        // Every photo has a row with its pose and credit line.
        val all = ApplicationProvider.getApplicationContext<Context>().appContainer.posePhotos.all
        assertEquals(34, all.size)
        all.forEach { p ->
            compose.onNode(hasText(p.poseName) and hasClickAction()).assertExists()
            assertTrue(p.attribution, compose.onAllNodes(hasText(p.attribution, substring = true)).fetchSemanticsNodes().isNotEmpty())
        }
        compose.onNode(hasText("Wikimedia Commons")).assertExists()
        // Mixkit's one licence is named once, under its heading, not in each of its 27 rows; Commons rows, whose
        // licences differ, each name theirs.
        assertEquals(1, compose.onAllNodes(hasText("Licence: Mixkit Stock Video Free License", substring = true)).fetchSemanticsNodes().size)
        assertTrue(compose.onAllNodes(hasText("Licence: CC0", substring = true)).fetchSemanticsNodes().size >= 2)
        capture("photo-credits-all")
    }

    @Test
    fun rowOpensTheCreditSheet() {
        set { PhotoCreditsScreen(onBack = {}) }
        compose.onAllNodes(hasText("Hand on hip") and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Source page")).assertExists()
        compose.onNode(hasText("Read the licence text")).assertExists()
        // The links that leave the app say so to TalkBack.
        compose.onNode(hasText("Licence page") and hasClickAction()).assertExists()
        assertEquals(2, compose.onAllNodes(hasContentDescription("opens in your browser", substring = true), useUnmergedTree = true).fetchSemanticsNodes().size)
        captureScreenRoboImage("build/screenshots/photo-credits-sheet.png")
    }

    @Test
    @Config(qualifiers = "w400dp-h1100dp-xxhdpi")
    fun creditSheetContents() {
        set {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Space.l),
                verticalArrangement = Arrangement.spacedBy(Space.l),
            ) {
                SheetLike { PhotoCreditContent(photo("sit_one_knee_up")) }
                SheetLike { PhotoCreditContent(photo("walk_twirl")) }
            }
        }
        // CC BY 2.0 asks for the work's title: the Commons photo's sheet names it (without "File:", the extension
        // and the Flickr id); the Mixkit sheet, whose licence doesn't, shows no title.
        assertEquals(1, compose.onAllNodes(hasText("Original title", substring = true)).fetchSemanticsNodes().size)
        compose.onNode(hasText("Ladies Grey Loungewear Set", substring = true)).assertExists()
        compose.onNode(hasContentDescription("Photo of a person in the One knee up pose")).assertExists()
        capture("photo-credits-sheet-content")
    }

    @Test
    fun licencePage() {
        set { SheetLike { PhotoCreditContent(photo("sit_one_knee_up"), showLicence = true) } }
        compose.onNode(hasContentDescription("Back to the photo credit")).assertExists()
        compose.onNode(hasText("Attribution 2.0", substring = true)).assertExists()
        // The text opens with a header written for the reader, not the maintainers' notes or repository paths.
        compose.onNode(hasText("Photo credits", substring = true)).assertExists()
        compose.onNode(hasText("app/src", substring = true)).assertDoesNotExist()
        capture("photo-credits-licence")
    }

    @Test
    fun creditLineOpensItsSheet() {
        set {
            Column(Modifier.padding(Space.edge)) {
                ConnectedCardStack(Modifier.fillMaxWidth()) {
                    CardSegment {
                        val p = photo("stand_hand_on_hip")
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            val pose = BuiltInPoses.byId(p.poseId)!!
                            PoseThumbnail(pose, Modifier.weight(1f).aspectRatio(0.75f).background(PoseitColors.backgroundStage, PoseitShapes.Inner))
                            PosePhoto(p, Modifier.weight(1f).aspectRatio(p.aspect), shape = PoseitShapes.Inner)
                        }
                        PhotoCredit(p)
                    }
                }
            }
        }
        val label = "Photo credit: Mixkit"
        val credit = compose.onNode(hasContentDescription(label) and hasClickAction())
        credit.assertExists()
        // Announced as a button that says what it opens; the photo says what it shows.
        val node = credit.fetchSemanticsNode()
        assertEquals(Role.Button, node.config[SemanticsProperties.Role])
        assertTrue(node.boundsInRoot.height >= 48f * 3 - 1)
        compose.onNode(hasContentDescription("Photo of a person in the Hand on hip pose")).assertExists()
        capture("photo-credit-line")
        credit.performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Licence page")).assertExists()
        compose.onNode(hasText("Not named on the Mixkit clip page")).assertExists()
        captureScreenRoboImage("build/screenshots/photo-credit-line-sheet.png")
    }

    @Test
    @Config(qualifiers = "w400dp-h2600dp-xxhdpi")
    fun settingsAbout() {
        var opened = 0
        set { SettingsScreen(onBack = {}, onShowIntro = {}, onOpenPhotoCredits = { opened++ }) }
        compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Photo credits"))
        compose.waitForIdle()
        compose.onNode(hasText("Mixkit and Wikimedia Commons contributors", substring = true)).assertExists()
        capture("settings-about")
        compose.onNode(hasText("Photo credits") and hasClickAction()).performClick()
        assertEquals(1, opened)
        compose.onNode(hasText("Open-source notices") and hasClickAction()).performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Sample pose photos (27)")).assertExists()
        compose.onNode(hasText("Mixkit, Mixkit Stock Video Free License")).assertExists()
        captureScreenRoboImage("build/screenshots/settings-notices.png")
    }

    /** A cream sheet as a bottom sheet draws it, for rendering sheet content inline. */
    @Composable
    private fun SheetLike(content: @Composable () -> Unit) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(PoseitColors.backgroundSurface, PoseitShapes.SheetTop)
                .padding(bottom = Space.l),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            SheetHandle()
            content()
        }
    }
}
