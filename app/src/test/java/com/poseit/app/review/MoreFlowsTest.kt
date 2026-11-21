package com.poseit.app.review

import android.Manifest
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.poseit.app.MainActivity
import com.poseit.app.PoseitApp
import com.poseit.app.data.AppSettings
import com.poseit.app.pose.library.BuiltInPoses
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Delete, discard, empty states, long-press menu and activity recreation on every screen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class MoreFlowsTest {

    private val crashes = CrashCollector()
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(crashes).around(compose)

    private val app: PoseitApp get() = ApplicationProvider.getApplicationContext()

    private fun waitText(text: String, timeoutMs: Long = 15_000) =
        compose.waitUntil(timeoutMs) { compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun waitDesc(desc: String, timeoutMs: Long = 15_000) =
        compose.waitUntil(timeoutMs) { compose.onAllNodesWithContentDescription(desc, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun waitGone(text: String, timeoutMs: Long = 15_000) =
        compose.waitUntil(timeoutMs) { compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }

    private fun has(text: String) = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(timeoutMs: Long = 15_000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (true) {
            compose.waitForIdle()
            if (cond()) return
            if (System.currentTimeMillis() > end) throw AssertionError("Condition not met after $timeoutMs ms")
            Thread.sleep(50)
        }
    }

    private fun byText(text: String): SemanticsNodeInteraction = compose.onAllNodes(hasText(text) and hasClickAction()).onFirst()
    private fun byDesc(desc: String): SemanticsNodeInteraction = compose.onAllNodes(hasContentDescription(desc) and hasClickAction()).onFirst()

    private fun shot(name: String) {
        runCatching { compose.onRoot().captureRoboImage("build/screenshots/review-more-$name.png") }
    }

    private fun systemBack(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun launch(onboardingDone: Boolean = true): ActivityScenario<MainActivity> {
        runBlocking { app.container.settingsRepository.update { AppSettings(onboardingDone = onboardingDone) } }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        if (onboardingDone) waitText("Pose camera") else waitText("Never wonder how to pose again")
        return scenario
    }

    private fun openPose(name: String) {
        compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(name))
        byText(name).performClick()
        waitDesc("More options")
    }

    private fun recreate(scenario: ActivityScenario<MainActivity>) {
        scenario.recreate()
        compose.waitForIdle()
    }

    @Test
    fun customPose_delete_returnsToLibrary() {
        launch().use { scenario ->
            byDesc("Create a pose").performClick()
            waitText("Draw your own")
            byText("Draw your own").performClick()
            waitText("Pose editor")
            byText("Save").performClick()
            waitText("Save pose")
            compose.onAllNodes(hasText("Save pose") and hasClickAction()).onFirst().performClick()
            waitFor { app.container.poseRepository.customPoses.value.isNotEmpty() }
            val name = app.container.poseRepository.customPoses.value.first().name
            waitDesc("More options")
            waitText(name)
            // Edit the custom pose: "Save changes" must update it in place.
            byText("Edit").performClick()
            waitText("Pose editor")
            byText("Save").performClick()
            waitText("Save changes")
            byText("Save changes").performClick()
            waitGone("Pose editor")
            waitDesc("More options")
            assertTrue(app.container.poseRepository.customPoses.value.size == 1)

            byText("Delete this pose").performScrollTo().performClick()
            waitText("Delete this pose?")
            byText("Delete").performClick()
            waitFor { app.container.poseRepository.customPoses.value.isEmpty() }
            waitText("Pose camera")
            shot("after-delete")
            // Back stack: Library only (editor + old detail were popped on save).
            systemBack(scenario)
            compose.waitForIdle()
            crashes.assertNoCrashes()
        }
    }

    @Test
    fun editor_dirty_discardDialog() {
        launch().use { scenario ->
            byDesc("Create a pose").performClick()
            waitText("Draw your own")
            byText("Draw your own").performClick()
            waitText("Pose editor")
            byText("Mirror").performClick()
            byText("Add person").performClick()
            waitText("Person 2")
            byDesc("Undo").performClick()
            byDesc("Redo").performClick()
            byText("Remove").performClick()
            compose.waitForIdle()
            shot("editor-dirty")
            systemBack(scenario)
            waitText("Discard your changes?")
            byText("Keep editing").performClick()
            waitGone("Discard your changes?")
            byDesc("Back").performClick()
            waitText("Discard your changes?")
            byText("Discard").performClick()
            waitText("Pose camera")
            crashes.assertNoCrashes()
        }
    }

    @Test
    fun library_search_emptyStates_longPress() {
        launch().use { scenario ->
            compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("zzzqqq")
            waitText("No poses match")
            shot("no-match")
            byText("Clear filters").performClick()
            waitText("POSE OF THE DAY")
            byText("Favourites").performClick()
            compose.waitForIdle()
            shot("favourites-tab")
            // DataStore outlives a Robolectric test, so earlier tests may have left recents / custom poses behind.
            byText("Recent").performClick()
            if (runBlocking { app.container.poseRepository.recent.first() }.isEmpty()) waitText("Nothing tried yet")
            byText("My poses").performClick()
            if (app.container.poseRepository.customPoses.value.isEmpty()) waitText("Make your first pose")
            shot("mine-empty")
            byText("All").performClick()
            compose.waitForIdle()
            val pose = BuiltInPoses.all.first()
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(pose.name))
            compose.onAllNodes(hasText(pose.name) and hasClickAction()).onFirst().performTouchInput { longClick() }
            waitText("Customize")
            byText("Customize").performClick()
            waitText("Based on ${pose.name}")
            systemBack(scenario)
            waitText("Pose camera")
            crashes.assertNoCrashes()
        }
    }

    @Test
    fun recreate_onEveryScreen() {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        launch(onboardingDone = false).use { scenario ->
            // Onboarding mid-way, then finish, then recreate: must stay on the library.
            byText("Next").performClick()
            recreate(scenario)
            waitText("Never wonder how to pose again")
            byText("Skip").performClick()
            waitText("Get started")
            byText("Get started").performClick() // CAMERA already granted: finishes directly
            waitText("Pose camera")
            recreate(scenario)
            waitText("Pose camera")
            assertTrue("Onboarding came back after recreation", !has("Get started"))

            val pose = BuiltInPoses.all.first()
            openPose(pose.name)
            recreate(scenario)
            waitDesc("More options")

            byText("Customize").performClick()
            waitText("Pose editor")
            byText("Save").performClick()
            waitText("Save pose")
            recreate(scenario)
            waitText("Pose editor")
            shot("editor-after-recreate")
            androidx.test.espresso.Espresso.pressBack() // close the sheet if it came back
            compose.waitForIdle()
            if (has("Save pose")) androidx.test.espresso.Espresso.pressBack()
            compose.waitForIdle()
            systemBack(scenario)
            waitDesc("More options")

            byText("Try this pose").performClick()
            waitDesc("Take photo")
            recreate(scenario)
            waitDesc("Take photo")
            shot("camera-after-recreate")
            systemBack(scenario)
            waitDesc("More options")
            systemBack(scenario)
            waitText("Pose camera")

            byDesc("Your photos").performClick()
            waitText("No photos yet")
            recreate(scenario)
            waitText("No photos yet")
            systemBack(scenario)
            waitText("Pose camera")

            byDesc("Settings").performClick()
            waitText("Voice guidance")
            recreate(scenario)
            waitText("Voice guidance")
            systemBack(scenario)
            waitText("Pose camera")

            byDesc("Create a pose").performClick()
            waitText("From a photo")
            byText("From a photo").performClick()
            waitText("Recreate any photo")
            recreate(scenario)
            waitText("Recreate any photo")
            systemBack(scenario)
            waitText("Pose camera")
            crashes.assertNoCrashes()
        }
    }

    @Test
    fun createFromPhoto_pickPhoto_detectionUnavailable_backResets() {
        launch().use { scenario ->
            byDesc("Create a pose").performClick()
            waitText("From a photo")
            byText("From a photo").performClick()
            waitText("Choose a photo")
            byText("Choose a photo").performScrollTo().performClick()
            compose.waitForIdle()
            val file = java.io.File(app.cacheDir, "pick.png")
            val bmp = android.graphics.Bitmap.createBitmap(300, 400, android.graphics.Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.rgb(120, 140, 160))
            file.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            val uri = android.net.Uri.parse("content://com.example.photos/picked/1")
            val bytes = file.readBytes()
            shadowOf(app.contentResolver).registerInputStreamSupplier(uri) { java.io.ByteArrayInputStream(bytes) }
            scenario.onActivity { act ->
                val shadow = shadowOf(act)
                val started = shadow.nextStartedActivityForResult
                assertTrue("The photo picker should have been launched", started != null)
                shadow.receiveResult(started.intent, android.app.Activity.RESULT_OK, android.content.Intent().setData(uri))
            }
            // MediaPipe has no native library on the JVM: the screen must land on its error state, not crash.
            waitFor { has("Something went wrong") || has("We couldn't spot anyone") || has("Your new pose") }
            shot("create-after-pick")
            // A perfectly valid PNG must at least be opened (detection itself can't run on the JVM).
            assertTrue("The picked photo was rejected as unreadable", !has("Couldn't open that photo. Try another one."))
            systemBack(scenario)
            waitText("Recreate any photo")
            systemBack(scenario)
            waitText("Pose camera")
            crashes.assertNoCrashes()
        }
    }

    /** A photo-based custom pose (reference + outline images on disk), as create-from-photo or an import produces. */
    @Test
    fun photoPose_detailModes_cameraGhost_editorPhotoToggle() {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        val refs = java.io.File(app.filesDir, "refs").apply { mkdirs() }
        fun png(name: String, w: Int, h: Int, color: Int): String {
            val f = java.io.File(refs, name)
            val b = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
            f.outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            return f.absolutePath
        }
        val base = BuiltInPoses.all.first { it.personCount == 1 }
        val template = base.copy(
            id = "custom_photo_test",
            name = "My photo pose",
            source = com.poseit.app.pose.PoseSource.FROM_PHOTO,
            category = com.poseit.app.pose.PoseCategory.CUSTOM,
            referenceImage = png("custom_photo_test.jpg", 600, 800, android.graphics.Color.rgb(200, 120, 90)),
            outlineImage = png("custom_photo_test_outline.png", 600, 800, android.graphics.Color.argb(200, 255, 255, 255)),
            createdAt = System.currentTimeMillis(),
        )
        launch().use { scenario ->
            runBlocking { app.container.poseRepository.saveCustom(template) }
            byText("My poses").performClick()
            waitText("My photo pose")
            byText("My photo pose").performClick()
            waitDesc("More options")
            waitText("Photo")
            byText("Photo").performClick()
            repeat(30) { compose.mainClock.advanceTimeBy(100); Thread.sleep(100); compose.waitForIdle() }
            shot("photo-pose-detail-photo")
            byText("Outline").performClick()
            compose.waitForIdle()
            shot("photo-pose-detail-outline")

            byText("Try this pose").performClick()
            waitDesc("Take photo")
            waitFor { compose.onAllNodes(hasContentDescription("Change guide style", substring = true)).fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodes(hasContentDescription("Change guide style", substring = true) and hasClickAction()).onFirst().performClick()
            waitText("Photo ghost")
            byText("Photo ghost").performClick()
            compose.waitForIdle()
            shot("photo-pose-camera-ghost")
            compose.onAllNodes(hasContentDescription("Change guide style", substring = true) and hasClickAction()).onFirst().performClick()
            waitText("Photo outline")
            byText("Photo outline").performClick()
            compose.waitForIdle()
            shot("photo-pose-camera-outline")
            systemBack(scenario)
            waitDesc("More options")

            byText("Edit").performClick()
            waitText("Pose editor")
            waitText("Photo")
            byText("Photo").performClick()
            compose.waitForIdle()
            byText("Photo").performClick()
            compose.waitForIdle()
            shot("photo-pose-editor")
            byText("Save").performClick()
            waitText("Save changes")
            byText("Save changes").performClick()
            waitGone("Pose editor")
            val saved = app.container.poseRepository.customPoses.value.single { it.id == template.id }
            assertTrue("Saving changes must keep the reference photo", saved.referenceImage == template.referenceImage)
            assertTrue("Saving changes must keep the outline", saved.outlineImage == template.outlineImage)
            crashes.assertNoCrashes()
        }
    }
}
