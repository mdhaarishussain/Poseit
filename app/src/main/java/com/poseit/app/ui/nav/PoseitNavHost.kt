package com.poseit.app.ui.nav

import android.net.Uri
import androidx.compose.runtime.Composable
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

object Routes {
    const val ONBOARDING = "onboarding"
    const val LIBRARY = "library"
    const val DETAIL = "pose/{poseId}"
    const val CAMERA = "camera?poseId={poseId}"
    const val GALLERY = "gallery"
    const val CREATE = "create"
    const val EDITOR = "editor?poseId={poseId}"
    const val SETTINGS = "settings"

    fun detail(id: String) = "pose/${Uri.encode(id)}"
    fun camera(id: String?) = if (id == null) "camera" else "camera?poseId=${Uri.encode(id)}"
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

@Composable
fun PoseitNavHost(startOnboarding: Boolean) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.LIBRARY) {
        composable(Routes.ONBOARDING) { entry ->
            OnboardingScreen(onDone = {
                nav.ifCurrent(entry) { navigate(Routes.LIBRARY) { popUpTo(Routes.ONBOARDING) { inclusive = true } } }
            })
        }
        composable(Routes.LIBRARY) { entry ->
            LibraryScreen(
                onOpenPose = { id -> nav.ifCurrent(entry) { navigate(Routes.detail(id)) } },
                onOpenCamera = { id -> nav.ifCurrent(entry) { navigate(Routes.camera(id)) } },
                onOpenGallery = { nav.ifCurrent(entry) { navigate(Routes.GALLERY) } },
                onCreateFromPhoto = { nav.ifCurrent(entry) { navigate(Routes.CREATE) } },
                onOpenEditor = { id -> nav.ifCurrent(entry) { navigate(Routes.editor(id)) } },
                onOpenSettings = { nav.ifCurrent(entry) { navigate(Routes.SETTINGS) } },
            )
        }
        composable(Routes.DETAIL, arguments = listOf(navArgument("poseId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("poseId").orEmpty()
            PoseDetailScreen(
                poseId = id,
                onBack = { nav.ifCurrent(entry) { popBackStack() } },
                onTryPose = { poseId -> nav.ifCurrent(entry) { navigate(Routes.camera(poseId)) } },
                onEdit = { poseId -> nav.ifCurrent(entry) { navigate(Routes.editor(poseId)) } },
            )
        }
        composable(
            Routes.CAMERA,
            arguments = listOf(navArgument("poseId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            CameraScreen(
                initialPoseId = entry.arguments?.getString("poseId"),
                onBack = { nav.ifCurrent(entry) { popBackStack() } },
                onOpenGallery = { nav.ifCurrent(entry) { navigate(Routes.GALLERY) } },
            )
        }
        composable(Routes.GALLERY) { entry ->
            GalleryScreen(onBack = { nav.ifCurrent(entry) { popBackStack() } })
        }
        composable(Routes.CREATE) { entry ->
            CreateFromPhotoScreen(
                onBack = { nav.ifCurrent(entry) { popBackStack() } },
                onSaved = { id -> nav.ifCurrent(entry) { navigate(Routes.detail(id)) { popUpTo(Routes.LIBRARY) } } },
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(navArgument("poseId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            PoseEditorScreen(
                basePoseId = entry.arguments?.getString("poseId"),
                onBack = { nav.ifCurrent(entry) { popBackStack() } },
                onSaved = { id -> nav.ifCurrent(entry) { navigate(Routes.detail(id)) { popUpTo(Routes.LIBRARY) } } },
            )
        }
        composable(Routes.SETTINGS) { entry ->
            SettingsScreen(onBack = { nav.ifCurrent(entry) { popBackStack() } })
        }
    }
}
