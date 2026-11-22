package com.poseit.app.review

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.poseit.app.MainActivity
import com.poseit.app.PoseitApp
import com.poseit.app.data.AppSettings
import com.poseit.app.pose.library.BuiltInPoses
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Drives the REAL MainActivity (nav graph, view models, DataStore, repositories) on the JVM.
 * CameraX / MediaPipe cannot work here; the camera test only checks that failures are caught and shown.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class AppFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: PoseitApp get() = ApplicationProvider.getApplicationContext()

    @org.junit.Before
    fun logToStdout() {
        org.robolectric.shadows.ShadowLog.stream = System.out
    }

    /** DataStore delegates are process singletons (they outlive one Robolectric test), so reset what we rely on. */
    private fun resetSettings(onboardingDone: Boolean) = runBlocking {
        app.container.settingsRepository.update { AppSettings(onboardingDone = onboardingDone) }
    }

    private fun ComposeTestRule.waitForText(text: String, timeoutMs: Long = 15_000, substring: Boolean = false) =
        waitUntil(timeoutMs) { onAllNodesWithText(text, substring = substring, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeTestRule.waitForDesc(desc: String, timeoutMs: Long = 15_000) =
        waitUntil(timeoutMs) { onAllNodesWithContentDescription(desc, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeTestRule.waitGone(text: String, timeoutMs: Long = 15_000) =
        waitUntil(timeoutMs) { onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }

    /** Like waitUntil, but idles the main looper first (conditions that only read repositories don't). */
    private fun waitFor(timeoutMs: Long = 15_000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (true) {
            compose.waitForIdle()
            if (cond()) return
            if (System.currentTimeMillis() > end) throw AssertionError("Condition not met after $timeoutMs ms")
            Thread.sleep(50)
        }
    }

    private fun shot(name: String) {
        runCatching { compose.onRoot().captureRoboImage("build/screenshots/review-flow-$name.png") }
    }

    private fun clickable(text: String): SemanticsNodeInteraction =
        compose.onAllNodes(hasText(text) and hasClickAction()).onFirst()

    private fun systemBack(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
    }

    private fun launchToLibrary(): ActivityScenario<MainActivity> {
        resetSettings(onboardingDone = true)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForDesc("Pose camera")
        // The pill bar's "Pose camera" button can appear a frame before the pose grid is scrollable.
        compose.waitUntil(10_000) { compose.onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes().isNotEmpty() }
        return scenario
    }

    @Test
    fun onboarding_finish_landsOnLibrary_andBackExits() {
        resetSettings(onboardingDone = false)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitForText("Pick a pose to copy")
            shot("onboarding-1")
            repeat(3) {
                compose.onNodeWithText("Next").performClick()
                compose.waitForIdle()
            }
            compose.waitForText("Get started")
            compose.onNodeWithText("Get started").performClick()
            compose.waitForIdle()
            // The camera permission request goes to the (shadow) system: answer it with "deny".
            var activity: Activity? = null
            scenario.onActivity { act ->
                activity = act
                val req = shadowOf(act).lastRequestedPermission
                assertTrue("Get started should request CAMERA", req != null && Manifest.permission.CAMERA in req.requestedPermissions)
                act.onRequestPermissionsResult(
                    req.requestCode,
                    req.requestedPermissions,
                    IntArray(req.requestedPermissions.size) { PackageManager.PERMISSION_DENIED },
                )
            }
            compose.waitForDesc("Pose camera")
            compose.waitGone("Pick a pose to copy")
            shot("library-after-onboarding")
            assertTrue(runBlocking { app.container.settingsRepository.settings.first().onboardingDone })

            systemBack(scenario)
            compose.waitForIdle()
            assertTrue("Back on the library should leave the app", activity!!.isFinishing)
        }
    }

    @Test
    fun detail_favourite_customize_save_showsInMyPoses() {
        launchToLibrary().use { scenario ->
            val pose = BuiltInPoses.all.first { it.personCount == 1 }
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(pose.name))
            clickable(pose.name).performClick()
            compose.waitForText("Camera setup")
            shot("detail")

            val favBefore = runBlocking { app.container.poseRepository.favorites.first() }.contains(pose.id)
            val addDesc = "Add ${pose.name} to favourites"
            val removeDesc = "Remove ${pose.name} from favourites"
            compose.waitForDesc(if (favBefore) removeDesc else addDesc)
            compose.onNodeWithContentDescription(if (favBefore) removeDesc else addDesc).performClick()
            compose.waitForDesc(if (favBefore) addDesc else removeDesc)
            assertEquals(!favBefore, runBlocking { app.container.poseRepository.favorites.first() }.contains(pose.id))

            clickable("Customise").performClick()
            compose.waitForText("Pose editor")
            compose.waitForText("Based on ${pose.name}")
            compose.waitUntil(10_000) {
                runCatching { compose.onNode(hasText("Save") and hasClickAction()).assertIsEnabledCompat() }.isSuccess
            }
            shot("editor")
            clickable("Save").performClick()
            compose.waitForText("Save pose")
            shot("save-sheet")
            val newName = "${pose.name} (my version)"
            compose.onAllNodes(hasText("Save pose") and hasClickAction()).onFirst().performClick()
            waitFor { app.container.poseRepository.customPoses.value.any { it.name == newName } }
            // Lands on the new pose's detail screen.
            compose.waitForText(newName)
            compose.waitGone("Pose editor")
            shot("after-save")

            systemBack(scenario)
            compose.waitForDesc("Pose camera")
            clickable("My poses").performClick()
            compose.waitForIdle()
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(newName))
            compose.waitForText(newName)
            shot("my-poses")
        }
    }

    @Test
    fun detail_rapidDoubleBackArrow_doesNotBlankScreen() {
        launchToLibrary().use { _ ->
            val pose = BuiltInPoses.all.first()
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(pose.name))
            clickable(pose.name).performClick()
            compose.waitForText("Camera setup")
            val back = compose.onNodeWithContentDescription("Back")
            back.performClick()
            runCatching { back.performClick() }
            compose.waitForIdle()
            compose.waitForDesc("Pose camera")
            shot("after-double-back-arrow")
        }
    }

    @Test
    fun detail_rapidDoubleSystemBack_doesNotBlankScreen() {
        launchToLibrary().use { scenario ->
            val pose = BuiltInPoses.all.first()
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(pose.name))
            clickable(pose.name).performClick()
            compose.waitForText("Camera setup")
            // Two back events one frame apart (a fast double tap on the system Back button / gesture).
            compose.mainClock.autoAdvance = false
            systemBack(scenario)
            compose.mainClock.advanceTimeByFrame()
            var finishing = false
            scenario.onActivity {
                it.onBackPressedDispatcher.onBackPressed()
                finishing = it.isFinishing
            }
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            shot("after-double-system-back")
            // Either the library is shown or the app was closed by the second Back; never an empty NavHost.
            val libraryShown = compose.onAllNodesWithContentDescription("Pose camera", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            assertTrue("Screen is blank after two Back presses (library shown=$libraryShown, finishing=$finishing)", libraryShown || finishing)
        }
    }

    @Test
    fun settings_togglesPersist_andShowIntroAgain() {
        launchToLibrary().use { scenario ->
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.waitForText("Voice guidance")
            shot("settings")
            val before = runBlocking { app.container.settingsRepository.settings.first() }
            clickable("Voice guidance").performClick()
            waitFor { runBlocking { app.container.settingsRepository.settings.first() }.voiceGuidance != before.voiceGuidance }
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Haptics"))
            clickable("Haptics").performClick()
            waitFor { runBlocking { app.container.settingsRepository.settings.first() }.haptics != before.haptics }

            // "Show intro again" plays the intro right away; Back on its first page returns to Settings.
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Show intro again"))
            clickable("Show intro again").performClick()
            compose.waitForText("Pick a pose to copy")
            shot("intro-again")
            systemBack(scenario)
            compose.waitGone("Pick a pose to copy")
            compose.waitForText("Show intro again")

            // Finishing the replayed intro also returns to Settings (not a second Library on top of it).
            shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Show intro again"))
            clickable("Show intro again").performClick()
            compose.waitForText("Pick a pose to copy")
            repeat(3) {
                compose.onNodeWithText("Next").performClick()
                compose.waitForIdle()
            }
            compose.waitForText("Get started")
            compose.onNodeWithText("Get started").performClick()
            compose.waitGone("Pick a pose to copy")
            compose.waitForText("Show intro again")
            assertTrue(runBlocking { app.container.settingsRepository.settings.first() }.onboardingDone)
            systemBack(scenario)
            compose.waitForDesc("Pose camera")
        }
        // The intro was seen, so a fresh launch goes straight to the library.
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForDesc("Pose camera")
        }
    }

    @Test
    fun gallery_showsEmptyState() {
        launchToLibrary().use { scenario ->
            compose.onNodeWithText("Photos").performClick()
            compose.waitForText("No photos yet")
            shot("gallery-empty")
            systemBack(scenario)
            compose.waitForDesc("Pose camera")
        }
    }

    @Test
    fun createFromPhoto_introOpensFromSheet() {
        launchToLibrary().use { scenario ->
            compose.onNodeWithContentDescription("Create a pose").performClick()
            compose.waitForText("Pose from a photo")
            compose.waitForIdle()
            shot("create-sheet")
            clickable("Pose from a photo").performClick()
            compose.waitForText("Recreate any photo")
            compose.waitForText("Choose a photo")
            shot("create-intro")
            systemBack(scenario)
            compose.waitForDesc("Pose camera")
        }
    }

    @Test
    fun drawYourOwn_editorFromScratch_saves() {
        launchToLibrary().use { _ ->
            compose.onNodeWithContentDescription("Create a pose").performClick()
            compose.waitForText("Draw a pose")
            clickable("Draw a pose").performClick()
            compose.waitForText("Pose editor")
            compose.waitForText("New pose")
            clickable("Save").performClick()
            compose.waitForText("Save pose")
            compose.onAllNodes(hasText("Save pose") and hasClickAction()).onFirst().performClick()
            waitFor { app.container.poseRepository.customPoses.value.isNotEmpty() }
            compose.waitGone("Pose editor")
            shot("after-draw-save")
        }
    }

    @Test
    fun camera_permissionDenied_showsRationale_thenGranted_composes() {
        launchToLibrary().use { scenario ->
            compose.onNodeWithContentDescription("Pose camera", useUnmergedTree = true).performClick()
            compose.waitForText("Let Poseit use the camera")
            shot("camera-rationale")
            compose.onNodeWithText("Allow camera").assertExistsCompat()
            compose.onNodeWithText("Not now").assertExistsCompat()

            // Grant it and deliver the result of the automatic request the gate made.
            shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
            scenario.onActivity { act ->
                val req = shadowOf(act).lastRequestedPermission
                assertTrue("The gate should have asked for CAMERA", req != null && Manifest.permission.CAMERA in req.requestedPermissions)
                act.onRequestPermissionsResult(
                    req.requestCode,
                    req.requestedPermissions,
                    IntArray(req.requestedPermissions.size) { PackageManager.PERMISSION_GRANTED },
                )
            }
            compose.waitGone("Let Poseit use the camera")
            // Let CameraX / MediaPipe initialisation fail (no camera, no native libs on the JVM) and settle.
            repeat(20) {
                compose.mainClock.advanceTimeBy(200)
                Thread.sleep(50)
            }
            compose.waitForIdle()
            shot("camera-granted")
            systemBack(scenario)
            compose.waitForDesc("Pose camera")
        }
    }

    @Test
    fun camera_tryThisPose_fromDetail_withPermission() {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        launchToLibrary().use { scenario ->
            val pose = BuiltInPoses.all.first()
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(pose.name))
            clickable(pose.name).performClick()
            compose.waitForText("Camera setup")
            clickable("Try this pose").performClick()
            repeat(20) {
                compose.mainClock.advanceTimeBy(200)
                Thread.sleep(50)
            }
            compose.waitForIdle()
            shot("camera-try-pose")
            systemBack(scenario)
            compose.waitForText("Camera setup")
        }
    }
}

private fun SemanticsNodeInteraction.assertExistsCompat() = assertExists()

private fun SemanticsNodeInteraction.assertIsEnabledCompat() {
    val node = fetchSemanticsNode()
    check(!node.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)) { "disabled" }
}
