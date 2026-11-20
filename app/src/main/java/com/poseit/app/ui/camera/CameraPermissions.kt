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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.poseit.app.ui.theme.PoseitColors

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

@Composable
private fun PermissionRationale(
    permanentlyDenied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PoseitColors.Background, Color.Black)))
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Column(
            Modifier
                .align(Alignment.Center)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .background(PoseitColors.Violet.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint = PoseitColors.Violet,
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Let Poseit use the camera",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "The camera shows the pose guide over the live picture and checks your pose on the phone. " +
                    "Nothing is uploaded – Poseit works completely offline.",
                style = MaterialTheme.typography.bodyLarge,
                color = PoseitColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            if (permanentlyDenied) {
                Text(
                    "Camera access is turned off for Poseit. Turn it on in the app settings under Permissions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PoseitColors.Amber,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Open settings")
                }
            } else {
                Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow camera")
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack) { Text("Not now") }
        }
    }
}
