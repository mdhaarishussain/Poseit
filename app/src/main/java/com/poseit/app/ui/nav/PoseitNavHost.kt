package com.poseit.app.ui.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.poseit.app.ui.camera.CameraScreen
import com.poseit.app.ui.create.CreateFromPhotoScreen
import com.poseit.app.ui.detail.PoseDetailScreen
import com.poseit.app.ui.editor.PoseEditorScreen
import com.poseit.app.ui.gallery.GalleryScreen
import com.poseit.app.ui.library.LibraryScreen
import com.poseit.app.ui.onboarding.OnboardingScreen
import com.poseit.app.ui.settings.SettingsScreen

/** Key of a one-off confirmation ("Pose saved", "Pose deleted") handed to the screen below or above. */
private const val MESSAGE = "message"

object Routes {
    const val ONBOARDING = "onboarding"
    const val LIBRARY = "library"
    const val DETAIL = "pose/{poseId}"
    const val CAMERA = "camera?poseId={poseId}&mirrored={mirrored}"
    const val GALLERY = "gallery"
    const val CREATE = "create"
    const val EDITOR = "editor?poseId={poseId}"
    const val SETTINGS = "settings"

    fun detail(id: String) = "pose/${Uri.encode(id)}"
    fun camera(id: String?, mirrored: Boolean = false) = when {
        id == null -> "camera"
        mirrored -> "camera?poseId=${Uri.encode(id)}&mirrored=true"
        else -> "camera?poseId=${Uri.encode(id)}"
    }
    fun editor(id: String?) = if (id == null) "editor" else "editor?poseId=${Uri.encode(id)}"
}

/**
 * Runs [block] only while [entry] is the top of the back stack. During the NavHost cross-fade the screen that is
 * leaving (or being covered) stays composed and keeps receiving taps; without this guard a quick second tap on a
 * back arrow pops the start destination (blank screen) and a second tap on a card or sheet option pushes the
 * screen twice. Unlike a RESUMED-lifecycle check it never drops an event-driven callback (a save finishing while
 * the app is in the background, a deleted pose closing its detail screen).
 */
private inline fun NavHostController.ifCurrent(entry: NavBackStackEntry, block: NavHostController.() -> Unit) {
    if (currentBackStackEntry?.id == entry.id) block()
}

/** Shows a just-saved pose on its detail screen (above the library) with [message] as its confirmation. */
private fun NavHostController.openSaved(id: String, message: String) {
    navigate(Routes.detail(id)) { popUpTo(Routes.LIBRARY) }
    currentBackStackEntry?.savedStateHandle?.set(MESSAGE, message)
}

@Composable
fun PoseitNavHost(startOnboarding: Boolean) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.LIBRARY) {
        composable(Routes.ONBOARDING) { entry ->
            OnboardingScreen(onDone = {
                nav.ifCurrent(entry) {
                    // Replayed from Settings: go back there. First run (the intro is the start destination): Library.
                    if (previousBackStackEntry != null) {
                        popBackStack()
                    } else {
                        navigate(Routes.LIBRARY) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    }
                }
            })
        }
        composable(Routes.LIBRARY) { entry ->
            val message by entry.savedStateHandle.getStateFlow<String?>(MESSAGE, null).collectAsState()
            LibraryScreen(
                onOpenPose = { id -> nav.ifCurrent(entry) { navigate(Routes.detail(id)) } },
                onOpenCamera = { id -> nav.ifCurrent(entry) { navigate(Routes.camera(id)) } },
                onOpenGallery = { nav.ifCurrent(entry) { navigate(Routes.GALLERY) } },
                onCreateFromPhoto = { nav.ifCurrent(entry) { navigate(Routes.CREATE) } },
                onOpenEditor = { id -> nav.ifCurrent(entry) { navigate(Routes.editor(id)) } },
                onOpenSettings = { nav.ifCurrent(entry) { navigate(Routes.SETTINGS) } },
                message = message,
                onMessageShown = { entry.savedStateHandle[MESSAGE] = null },
            )
        }
        composable(Routes.DETAIL, arguments = listOf(navArgument("poseId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("poseId").orEmpty()
            val message by entry.savedStateHandle.getStateFlow<String?>(MESSAGE, null).collectAsState()
            PoseDetailScreen(
                poseId = id,
                onBack = { nav.ifCurrent(entry) { popBackStack() } },
                onDeleted = {
                    nav.ifCurrent(entry) {
                        previousBackStackEntry?.savedStateHandle?.set(MESSAGE, "Pose deleted")
                        popBackStack()
                    }
                },
                onTryPose = { poseId, mirrored -> nav.ifCurrent(entry) { navigate(Routes.camera(poseId, mirrored)) } },
                onEdit = { poseId -> nav.ifCurrent(entry) { navigate(Routes.editor(poseId)) } },
                message = message,
                onMessageShown = { entry.savedStateHandle[MESSAGE] = null },
            )
        }
        composable(
            Routes.CAMERA,
            arguments = listOf(
                navArgument("poseId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("mirrored") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            CameraScreen(
                initialPoseId = entry.arguments?.getString("poseId"),
                initialMirrored = entry.arguments?.getBoolean("mirrored") ?: false,
                onBack = { nav.ifCurrent(entry) { popBackStack() } },
                onOpenGallery = { nav.ifCurrent(entry) { navigate(Routes.GALLERY) } },
            )
        }
        composable(Routes.GALLERY) { entry ->
            GalleryScreen(
                onBack = { nav.ifCurrent(entry) { popBackStack() } },
                onOpenCamera = {
                    nav.ifCurrent(entry) {
                        // Opened from the camera: go back to it rather than stacking a second one.
                        if (previousBackStackEntry?.destination?.route == Routes.CAMERA) popBackStack() else navigate(Routes.camera(null))
                    }
                },
            )
        }
        composable(Routes.CREATE) { entry ->
            CreateFromPhotoScreen(
                onBack = { nav.ifCurrent(entry) { popBackStack() } },
                onSaved = { id -> nav.ifCurrent(entry) { openSaved(id, "Pose saved") } },
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(navArgument("poseId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            PoseEditorScreen(
                basePoseId = entry.arguments?.getString("poseId"),
                onBack = { nav.ifCurrent(entry) { popBackStack() } },
                onSaved = { id, message -> nav.ifCurrent(entry) { openSaved(id, message) } },
            )
        }
        composable(Routes.SETTINGS) { entry ->
            SettingsScreen(
                onBack = { nav.ifCurrent(entry) { popBackStack() } },
                onShowIntro = { nav.ifCurrent(entry) { navigate(Routes.ONBOARDING) } },
            )
        }
    }
}
