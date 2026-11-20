package com.poseit.app.ui.nav

import android.net.Uri
import androidx.compose.runtime.Composable
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

@Composable
fun PoseitNavHost(startOnboarding: Boolean) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.LIBRARY) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
                nav.navigate(Routes.LIBRARY) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onOpenPose = { nav.navigate(Routes.detail(it)) },
                onOpenCamera = { nav.navigate(Routes.camera(it)) },
                onOpenGallery = { nav.navigate(Routes.GALLERY) },
                onCreateFromPhoto = { nav.navigate(Routes.CREATE) },
                onOpenEditor = { nav.navigate(Routes.editor(it)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.DETAIL, arguments = listOf(navArgument("poseId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("poseId").orEmpty()
            PoseDetailScreen(
                poseId = id,
                onBack = { nav.popBackStack() },
                onTryPose = { nav.navigate(Routes.camera(it)) },
                onEdit = { nav.navigate(Routes.editor(it)) },
            )
        }
        composable(
            Routes.CAMERA,
            arguments = listOf(navArgument("poseId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            CameraScreen(
                initialPoseId = entry.arguments?.getString("poseId"),
                onBack = { nav.popBackStack() },
                onOpenGallery = { nav.navigate(Routes.GALLERY) },
            )
        }
        composable(Routes.GALLERY) {
            GalleryScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.CREATE) {
            CreateFromPhotoScreen(
                onBack = { nav.popBackStack() },
                onSaved = { id -> nav.navigate(Routes.detail(id)) { popUpTo(Routes.LIBRARY) } },
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(navArgument("poseId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            PoseEditorScreen(
                basePoseId = entry.arguments?.getString("poseId"),
                onBack = { nav.popBackStack() },
                onSaved = { id -> nav.navigate(Routes.detail(id)) { popUpTo(Routes.LIBRARY) } },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
