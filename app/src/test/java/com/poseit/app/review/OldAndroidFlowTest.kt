package com.poseit.app.review

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.poseit.app.MainActivity
import com.poseit.app.PoseitApp
import com.poseit.app.data.AppSettings
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.ui.camera.STORAGE_BLOCKED_NOTICE
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Android 7.0 (minSdk): catches calls to newer platform APIs (NoSuchMethodError on the old android.jar). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Config(sdk = [24], qualifiers = "w400dp-h860dp-xxhdpi")
class Android7FlowTest : OldAndroidFlows()

/** Android 9: legacy storage permission for saving, READ_EXTERNAL_STORAGE for the gallery. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "w400dp-h860dp-xxhdpi")
class Android9FlowTest : OldAndroidFlows()

abstract class OldAndroidFlows {

    private val crashes = CrashCollector()
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(crashes).around(compose)

    private val app: PoseitApp get() = ApplicationProvider.getApplicationContext()

    private fun waitText(text: String, timeoutMs: Long = 15_000) =
        compose.waitUntil(timeoutMs) { compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun waitDesc(desc: String, timeoutMs: Long = 15_000) =
        compose.waitUntil(timeoutMs) { compose.onAllNodesWithContentDescription(desc, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

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

    private fun systemBack(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    /** Answers the permission request the screen just made. */
    private fun answerPermission(scenario: ActivityScenario<MainActivity>, grant: Boolean) {
        scenario.onActivity { act ->
            val req = shadowOf(act).lastRequestedPermission ?: return@onActivity
            if (grant) shadowOf(app).grantPermissions(*req.requestedPermissions)
            // The platform clears this flag when it dispatches a real result; calling the callback directly doesn't.
            runCatching {
                android.app.Activity::class.java.getDeclaredField("mHasCurrentPermissionsRequest").apply { isAccessible = true }.setBoolean(act, false)
            }
            act.onRequestPermissionsResult(
                req.requestCode,
                req.requestedPermissions,
                IntArray(req.requestedPermissions.size) { if (grant) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED },
            )
            println("DBG answered ${req.requestedPermissions.toList()} grant=$grant")
        }
        compose.waitForIdle()
    }

    private fun settle(ms: Long = 1500) {
        var t = 0L
        while (t < ms) {
            compose.mainClock.advanceTimeBy(100)
            Thread.sleep(20)
            t += 100
        }
        compose.waitForIdle()
    }

    @Test
    fun walkEveryScreen() {
        runBlocking { app.container.settingsRepository.update { AppSettings(onboardingDone = false) } }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitText("Pick a pose to copy")
            byText("Skip").performClick()
            waitText("Get started")
            byText("Get started").performClick()
            answerPermission(scenario, grant = true)
            waitText("Pose camera")

            // Detail + favourite + customize + save.
            val pose = BuiltInPoses.all.first { it.personCount == 1 }
            compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(pose.name))
            byText(pose.name).performClick()
            waitText("Camera setup")
            byDesc("Add ${pose.name} to favourites").performClick()
            waitDesc("Remove ${pose.name} from favourites")
            byText("Customise").performClick()
            waitText("Pose editor")
            byText("Save").performClick()
            waitText("Save pose")
            compose.onAllNodes(hasText("Save pose") and hasClickAction()).onFirst().performClick()
            waitFor { app.container.poseRepository.customPoses.value.isNotEmpty() }
            waitText("Camera setup")

            // Camera (CAMERA granted during onboarding; storage asked on Android 9 and below).
            byText("Try this pose").performClick()
            waitDesc("Take photo")
            settle()
            // Until storage is granted auto-capture / hand-raise are paused, and the HUD says so.
            waitText(STORAGE_BLOCKED_NOTICE)
            answerPermission(scenario, grant = true)
            waitFor { compose.onAllNodesWithText(STORAGE_BLOCKED_NOTICE, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
            byDesc("Take photo").performClick()
            settle()
            byDesc("Quick settings").performClick()
            waitText("Quick settings")
            androidx.test.espresso.Espresso.pressBack()
            settle(500)
            byDesc("Open gallery").performClick()
            waitText("Your photos")
            answerPermission(scenario, grant = false)
            settle(800)
            systemBack(scenario)
            waitDesc("Take photo")
            systemBack(scenario)
            waitText("Camera setup")
            systemBack(scenario)
            waitText("Pose camera")

            // Settings, create, gallery from the library.
            byDesc("Settings").performClick()
            waitText("Voice guidance")
            byText("Voice guidance").performClick()
            settle(300)
            systemBack(scenario)
            waitText("Pose camera")
            byDesc("Create a pose").performClick()
            waitText("Pose from a photo")
            byText("Pose from a photo").performClick()
            waitText("Recreate any photo")
            systemBack(scenario)
            waitText("Pose camera")
            byDesc("Your photos").performClick()
            waitText("Your photos")
            settle(800)
            systemBack(scenario)
            waitText("Pose camera")
            crashes.assertNoCrashes()
        }
        crashes.assertNoCrashes()
    }
}
