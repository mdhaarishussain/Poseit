package com.poseit.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
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

/**
 * Renders screens on the JVM (Robolectric native graphics) into app/build/screenshots/ so the UI can be
 * reviewed without a device. Run: ./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*'
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            PoseitTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Test
    fun library() = shot("library") {
        LibraryScreen({}, {}, {}, {}, {}, {})
    }

    @Test
    fun detail() = shot("detail") {
        PoseDetailScreen(poseId = BuiltInPoses.all.firstOrNull()?.id ?: "none", onBack = {}, onTryPose = { _, _ -> }, onEdit = {})
    }

    @Test
    fun detailCouple() = shot("detail_couple") {
        val id = BuiltInPoses.all.firstOrNull { it.personCount == 2 }?.id ?: "none"
        PoseDetailScreen(poseId = id, onBack = {}, onTryPose = { _, _ -> }, onEdit = {})
    }

    @Test
    fun settings() = shot("settings") { SettingsScreen(onBack = {}, onShowIntro = {}) }

    @Test
    fun onboarding() = shot("onboarding") { OnboardingScreen(onDone = {}) }

    @Test
    fun gallery() = shot("gallery") { GalleryScreen(onBack = {}, onOpenCamera = {}) }

    @Test
    fun createFromPhoto() = shot("create") { CreateFromPhotoScreen(onBack = {}, onSaved = {}) }

    @Test
    fun editor() = shot("editor") { PoseEditorScreen(basePoseId = null, onBack = {}, onSaved = { _, _ -> }) }

    @Test
    fun editorFromPose() = shot("editor_from_pose") {
        PoseEditorScreen(basePoseId = BuiltInPoses.all.getOrNull(3)?.id, onBack = {}, onSaved = { _, _ -> })
    }
}
