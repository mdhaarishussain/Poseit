package com.poseit.app.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.ui.create.CreateFromPhotoScreen
import com.poseit.app.ui.detail.PoseDetailScreen
import com.poseit.app.ui.editor.PoseEditorScreen
import com.poseit.app.ui.gallery.GalleryScreen
import com.poseit.app.ui.library.LibraryScreen
import com.poseit.app.ui.onboarding.OnboardingScreen
import com.poseit.app.ui.settings.SettingsScreen
import com.poseit.app.ui.theme.PoseitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Small phone (360x640 dp) at the default font size. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class SmallPhoneScreenshotTest : SmallPhoneShots("small")

/** Small phone (360x640 dp) with the system font size at 130 %. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
class SmallPhoneLargeFontScreenshotTest : SmallPhoneShots("small-font130")

/**
 * Small phone (360x640 dp) with the system font size at 200 %, the largest Android 14+ offers: button pairs stack and
 * labels wrap instead of being cut.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi", fontScale = 2.0f)
class SmallPhoneHugeFontScreenshotTest : SmallPhoneShots("small-font200")

abstract class SmallPhoneShots(private val prefix: String) {

    @get:Rule
    val compose = createComposeRule()

    private fun set(content: @Composable () -> Unit) {
        compose.setContent {
            PoseitTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
            }
        }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/review-$prefix-$name.png")
    }

    private fun waitText(text: String, timeoutMs: Long = 10_000) =
        compose.waitUntil(timeoutMs) { compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun shot(name: String, content: @Composable () -> Unit) {
        set(content)
        capture(name)
    }

    @Test
    fun library() {
        set { LibraryScreen({}, {}, {}, {}, {}, {}) }
        waitText("Today's pose")
        capture("library")
    }

    @Test
    fun detail() {
        set { PoseDetailScreen(poseId = BuiltInPoses.all.first().id, onBack = {}, onTryPose = { _, _ -> }, onEdit = {}) }
        waitText("Try this pose")
        capture("detail")
    }

    @Test
    fun detailLongestName() {
        val pose = BuiltInPoses.all.maxBy { it.name.length }
        set { PoseDetailScreen(poseId = pose.id, onBack = {}, onTryPose = { _, _ -> }, onEdit = {}) }
        waitText("Try this pose")
        capture("detail-longest-name")
    }

    @Test
    fun settings() {
        set { SettingsScreen(onBack = {}, onShowIntro = {}) }
        waitText("Voice guidance")
        capture("settings")
    }

    @Test
    fun onboardingFirstPage() = shot("onboarding-1") { OnboardingScreen(onDone = {}) }

    @Test
    fun onboardingLastPage() {
        set { OnboardingScreen(onDone = {}) }
        compose.onAllNodes(hasText("Skip") and hasClickAction()).onFirst().performClick()
        waitText("Get started")
        capture("onboarding-4")
    }

    @Test
    fun onboardingPage2() {
        set { OnboardingScreen(onDone = {}) }
        compose.onAllNodes(hasText("Next") and hasClickAction()).onFirst().performClick()
        waitText("Match the guide")
        capture("onboarding-2")
    }

    @Test
    fun createFromPhoto() {
        set { CreateFromPhotoScreen(onBack = {}, onSaved = {}) }
        waitText("Choose a photo")
        capture("create")
    }

    @Test
    fun gallery() {
        set { GalleryScreen(onBack = {}, onOpenCamera = {}) }
        waitText("No photos yet")
        capture("gallery")
    }

    @Test
    fun editorAndSaveSheet() {
        set { PoseEditorScreen(basePoseId = BuiltInPoses.all.first().id, onBack = {}, onSaved = { _, _ -> }) }
        waitText("Based on ${BuiltInPoses.all.first().name}")
        capture("editor")
        compose.onAllNodes(hasText("Save") and hasClickAction()).onFirst().performClick()
        waitText("Save pose")
        compose.waitForIdle()
        captureScreenRoboImage("build/screenshots/review-$prefix-editor-save-sheet.png")
    }

    @Test
    fun editorCouple() {
        val couple = BuiltInPoses.all.first { it.personCount >= 2 }
        set { PoseEditorScreen(basePoseId = couple.id, onBack = {}, onSaved = { _, _ -> }) }
        waitText("Person 1")
        capture("editor-couple")
    }
}

/**
 * Robolectric draws no system bars, so a 360x640 dp phone's usable area (minus a 24 dp status bar and a 48 dp
 * 3-button navigation bar) is emulated with a 568 dp tall screen, at 130 % font size.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h568dp-xhdpi", fontScale = 1.3f)
class SmallPhoneUsableAreaScreenshotTest : SmallPhoneShots("small-usable-font130")
