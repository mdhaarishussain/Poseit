package com.poseit.app.ui.camera

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.StudioTopBar
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioType

private const val PREFS = "poseit_camera_permissions"

internal fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Opens this app's page in the system settings (to grant a permanently denied permission). */
internal fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        safely { startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

/**
 * State of one runtime permission that remembers whether it was ever requested, so a permanent denial
 * ("don't ask again") can be told apart from "never asked" (both report no rationale).
 */
@Stable
internal class PermissionState(
    private val context: Context,
    val permission: String,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var granted by mutableStateOf(context.hasPermission(permission))
        private set
    var permanentlyDenied by mutableStateOf(false)
        private set

    val askedBefore: Boolean get() = prefs.getBoolean(permission, false)

    fun refresh() {
        granted = context.hasPermission(permission)
        permanentlyDenied = !granted && askedBefore && !shouldShowRationale()
    }

    fun onResult(isGranted: Boolean) {
        prefs.edit { putBoolean(permission, true) }
        granted = isGranted
        permanentlyDenied = !isGranted && !shouldShowRationale()
    }

    private fun shouldShowRationale(): Boolean {
        val activity = context.findActivity() ?: return true
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}

/** Remembers a [PermissionState] plus its launcher; re-checks whenever the screen resumes. */
@Composable
internal fun rememberPermission(permission: String, onResult: (Boolean) -> Unit = {}): Pair<PermissionState, () -> Unit> {
    val context = LocalContext.current
    val state = remember(permission) { PermissionState(context, permission) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        state.onResult(ok)
        onResult(ok)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state.refresh() }
    val request: () -> Unit = remember(launcher) {
        {
            try {
                launcher.launch(permission)
            } catch (e: RuntimeException) {
                // No permission UI (ActivityNotFoundException) or launcher not registered yet.
                Log.w(CAMERA_TAG, "Permission request failed", e)
                state.refresh()
            }
        }
    }
    return state to request
}

/**
 * Shows [content] once the camera permission is granted. Otherwise shows a rationale screen; asks the system
 * automatically the very first time, and offers "Open settings" when the permission is permanently denied.
 */
@Composable
internal fun CameraPermissionGate(onBack: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val (camera, requestCamera) = rememberPermission(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        if (!camera.granted && !camera.askedBefore) requestCamera()
    }
    if (camera.granted) {
        content()
    } else {
        PermissionRationale(
            permanentlyDenied = camera.permanentlyDenied,
            onAllow = requestCamera,
            onOpenSettings = { context.openAppSettings() },
            onBack = onBack,
        )
    }
}

/**
 * Why Poseit needs the camera, on the light Studio paper: a left-aligned Display heading, one paragraph, and the
 * one action at the bottom within thumb reach ("Allow camera", or "Open settings" after a permanent denial).
 */
@Composable
private fun PermissionRationale(
    permanentlyDenied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Studio.Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
    ) {
        StudioTopBar(title = "", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(top = Space.xl),
        ) {
            Text(
                "Let Poseit use the camera",
                style = StudioType.display,
                color = Studio.Ink,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .semantics { heading() },
            )
            Spacer(Modifier.height(Space.l))
            Text(
                "The camera shows the pose guide over the live picture and checks your pose on your phone. " +
                    "The picture never leaves your phone, and Poseit works offline.",
                style = StudioType.body,
                color = Studio.Ink,
                modifier = Modifier.widthIn(max = 560.dp),
            )
            if (permanentlyDenied) {
                Spacer(Modifier.height(Space.l))
                Text(
                    "Camera access is turned off for Poseit. Turn it on in the app settings under Permissions.",
                    style = StudioType.bodyStrong,
                    color = Studio.Ink,
                    modifier = Modifier.widthIn(max = 560.dp),
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter, vertical = Space.l),
        ) {
            if (permanentlyDenied) {
                PrimaryButton("Open settings", onClick = onOpenSettings, modifier = Modifier.fillMaxWidth())
            } else {
                PrimaryButton("Allow camera", onClick = onAllow, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(Space.s))
            TertiaryButton("Not now", onClick = onBack, modifier = Modifier.fillMaxWidth())
        }
    }
}
