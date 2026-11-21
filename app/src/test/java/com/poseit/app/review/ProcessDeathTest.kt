package com.poseit.app.review

import android.os.Bundle
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.poseit.app.AppContainer
import com.poseit.app.MainActivity
import com.poseit.app.PoseitApp
import com.poseit.app.data.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Process death while on a custom pose's screens: the activity comes back from its saved state with fresh view models
 * and a fresh AppContainer (custom poses are re-read from disk asynchronously).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class ProcessDeathTest {

    private val crashes = CrashCollector()
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(crashes).around(compose)

    private val app: PoseitApp get() = ApplicationProvider.getApplicationContext()

    private fun waitText(text: String, timeoutMs: Long = 15_000) =
        compose.waitUntil(timeoutMs) { compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun waitDesc(desc: String, timeoutMs: Long = 15_000) =
        compose.waitUntil(timeoutMs) { compose.onAllNodesWithContentDescription(desc, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

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

    /** Saves the activity state, kills it and the in-memory app graph, and restores a new activity from the bundle. */
    private fun processDeath(controller: ActivityController<MainActivity>): ActivityController<MainActivity> {
        val bundle = Bundle()
        controller.saveInstanceState(bundle)
        controller.pause().stop().destroy()
        compose.waitForIdle()
        PoseitApp::class.java.getDeclaredField("container").apply { isAccessible = true }.set(app, AppContainer(app))
        val restored = Robolectric.buildActivity(MainActivity::class.java).setup(bundle)
        compose.waitForIdle()
        return restored
    }

    @Test
    fun customPoseDetailAndEditor_surviveProcessDeath() {
        runBlocking { app.container.settingsRepository.update { AppSettings(onboardingDone = true) } }
        var controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            waitText("Pose camera")
            byDesc("Create a pose").performClick()
            waitText("Draw a pose")
            byText("Draw a pose").performClick()
            waitText("Pose editor")
            byText("Save").performClick()
            waitText("Save pose")
            compose.onAllNodes(hasText("Save pose") and hasClickAction()).onFirst().performClick()
            waitFor { app.container.poseRepository.customPoses.value.isNotEmpty() }
            val pose = app.container.poseRepository.customPoses.value.first()
            waitText("Camera setup")
            waitText(pose.name)

            controller = processDeath(controller)
            waitDesc("Back")
            waitFor(10_000) { has(pose.name) || has("Pose not found") }
            runCatching { compose.onRoot().captureRoboImage("build/screenshots/review-process-death-detail.png") }
            assertTrue("Custom pose detail showed 'Pose not found' after process death", has(pose.name) && !has("Pose not found"))

            byText("Edit").performClick()
            waitText("Pose editor")
            waitText("Based on ${pose.name}")
            byText("Save").performClick()
            waitText("Save changes")

            controller = processDeath(controller)
            waitText("Pose editor")
            waitFor(10_000) { has("Based on ${pose.name}") }
            // The save sheet itself is not restored (acceptable); reopen it.
            if (!has("Save changes")) byText("Save").performClick()
            waitText("Save changes")
            runCatching { compose.onRoot().captureRoboImage("build/screenshots/review-process-death-editor.png") }
            byText("Save changes").performClick()
            waitFor { !has("Pose editor") }
            assertEquals(1, app.container.poseRepository.customPoses.value.size)
            crashes.assertNoCrashes()
        } finally {
            runCatching { controller.pause().stop().destroy() }
        }
    }
}
