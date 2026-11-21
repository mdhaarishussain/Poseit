package com.poseit.app.review

import android.Manifest
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.poseit.app.MainActivity
import com.poseit.app.PoseitApp
import com.poseit.app.data.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Exercises every control of the camera screen with the camera permission granted. On the JVM CameraX finds no camera
 * and MediaPipe has no native library, so both fail; the screen must keep working (as on a phone without a usable
 * camera / unsupported GPU) and nothing may throw uncaught.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class CameraInteractionTest {

    private val crashes = CrashCollector()
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(crashes).around(compose)

    private val app: PoseitApp get() = ApplicationProvider.getApplicationContext()

    private fun ComposeTestRule.waitForDesc(desc: String, substring: Boolean = false, timeoutMs: Long = 15_000) =
        waitUntil(timeoutMs) { onAllNodesWithContentDescription(desc, substring = substring, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeTestRule.waitForText(text: String, timeoutMs: Long = 15_000) =
        waitUntil(timeoutMs) { onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun byDesc(desc: String, substring: Boolean = false): SemanticsNodeInteraction =
        compose.onAllNodes(hasContentDescription(desc, substring = substring) and hasClickAction()).onFirst()

    private fun byText(text: String): SemanticsNodeInteraction = compose.onAllNodes(hasText(text) and hasClickAction()).onFirst()

    private fun settle(ms: Long = 1500) {
        var t = 0L
        while (t < ms) {
            compose.mainClock.advanceTimeBy(100)
            Thread.sleep(20)
            t += 100
        }
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        runCatching { compose.onRoot().captureRoboImage("build/screenshots/review-camera-$name.png") }
    }

    private fun systemBack(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    /** Back key to the focused window (a bottom sheet / dialog window handles it before the activity). */
    private fun pressBackKey() {
        androidx.test.espresso.Espresso.pressBack()
        compose.waitForIdle()
    }

    private fun openCamera(): ActivityScenario<MainActivity> {
        runBlocking { app.container.settingsRepository.update { AppSettings(onboardingDone = true) } }
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForText("Pose camera")
        compose.onAllNodesWithText("Pose camera", useUnmergedTree = true).onFirst().performClick()
        compose.waitForDesc("Take photo")
        settle()
        return scenario
    }

    @Test
    fun topBar_timer_photoshoot_shutter() {
        openCamera().use { scenario ->
            repeat(4) {
                byDesc("Grid:", substring = true).performClick()
                compose.waitForIdle()
            }
            repeat(3) {
                byDesc("Aspect ratio", substring = true).performClick()
                settle(1200)
            }
            shot("after-aspect")

            // Timer: 3 s, start then stop.
            byDesc("Timer off").performClick()
            compose.waitForText("3 seconds")
            byText("3 seconds").performClick()
            compose.waitForDesc("Start timer")
            byDesc("Start timer").performClick()
            settle(800)
            shot("timer-running")
            if (compose.onAllNodesWithContentDescription("Stop timer", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
                byDesc("Stop timer").performClick()
                settle(500)
            }
            // Photoshoot dialog.
            byDesc("Timer", substring = true).performClick()
            compose.waitForText("Photoshoot…")
            byText("Photoshoot…").performClick()
            compose.waitForText("Number of photos")
            shot("photoshoot-dialog")
            byText("Set").performClick()
            settle(500)
            // Back to no timer, then a manual shot with no bound camera.
            byDesc("Photoshoot", substring = true).performClick()
            compose.waitForText("Off")
            byText("Off").performClick()
            compose.waitForDesc("Take photo")
            byDesc("Take photo").performClick()
            settle(2000)
            shot("after-shutter")
            crashes.assertNoCrashes()
            systemBack(scenario)
            compose.waitForText("Pose camera")
        }
        crashes.assertNoCrashes()
    }

    @Test
    fun sheets_quickSettings_allPoses_tips() {
        openCamera().use { scenario ->
            byDesc("Quick settings").performClick()
            compose.waitForText("Quick settings")
            settle(600)
            shot("quick-settings")
            compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().size // force a pass
            listOf("Live skeleton", "Level").forEach { title ->
                runCatching { byText(title).performClick() }
                compose.waitForIdle()
            }
            pressBackKey()
            settle(600)

            byDesc("All poses").performClick()
            compose.waitForText("Choose a pose")
            settle(600)
            shot("all-poses")
            pressBackKey()
            settle(600)

            byDesc("Pose tips").performClick()
            settle(600)
            shot("tips")
            pressBackKey()
            settle(600)
            crashes.assertNoCrashes()
        }
        crashes.assertNoCrashes()
    }

    @Test
    fun rail_strip_flip_gallery_reenter() {
        openCamera().use { scenario ->
            byDesc("Lock pose guide in place").performClick()
            compose.waitForIdle()
            byDesc("Mirror pose").performClick()
            compose.waitForIdle()
            byDesc("Lock background to match the framing").performClick()
            settle(800)
            byDesc("No pose, plain camera").performClick()
            settle(800)
            shot("no-pose")
            byDesc("Switch camera").performClick()
            settle(1500)
            shot("after-flip")

            byDesc("Open gallery").performClick()
            compose.waitForText("Your photos")
            settle(800)
            systemBack(scenario)
            compose.waitForDesc("Take photo")
            settle(1500)
            shot("after-gallery-return")
            byDesc("Back").performClick()
            compose.waitForText("Pose camera")
            settle(500)

            // Open it again: a fresh view model while the old one was cleared.
            compose.onAllNodesWithText("Pose camera", useUnmergedTree = true).onFirst().performClick()
            compose.waitForDesc("Take photo")
            settle(1500)
            systemBack(scenario)
            compose.waitForText("Pose camera")
            crashes.assertNoCrashes()
        }
        crashes.assertNoCrashes()
    }
}
