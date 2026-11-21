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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.ui.components.AccentButton
import com.poseit.app.ui.components.CardSegment
import com.poseit.app.ui.components.ConnectedCardDefaults
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.components.TopBarCard
import com.poseit.app.ui.components.TopBarHeight
import com.poseit.app.ui.components.TopBarInsets
import com.poseit.app.ui.components.frameMarks
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space

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
 * Why Poseit needs the camera, in the mockups' connected stack on the canvas: the top bar card fused to a headline
 * card (a Display headline and one paragraph) and, when the screen has room, a picture card where a figure stands
 * in viewfinder marks. The one action sits at the bottom within thumb reach ("Allow camera", or "Open settings"
 * after a permanent denial), with "Not now" under it.
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
            .background(PoseitColors.backgroundBody)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
    ) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val density = LocalDensity.current
            var headlinePx by remember { mutableIntStateOf(0) }
            val topInset = TopBarInsets.asPaddingValues().calculateTopPadding()
            // The picture card takes what the bar and the headline card leave, down to the buttons.
            val headline = with(density) { headlinePx.toDp() }
            val pictureHeight = maxHeight - topInset - Space.edge - TopBarHeight - headline - ConnectedCardDefaults.Gap * 2
            val showPicture = headlinePx > 0 && pictureHeight >= MinPictureHeight
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                TopBarCard(
                    title = "Camera",
                    onBack = onBack,
                    joined = {
                        CardSegment(Modifier.onSizeChanged { headlinePx = it.height }) {
                            Text(
                                "Let Poseit use the camera",
                                style = PoseitType.display,
                                color = PoseitColors.textPrimaryInverted,
                                modifier = Modifier
                                    .widthIn(max = 560.dp)
                                    .semantics { heading() },
                            )
                            Spacer(Modifier.height(Space.l))
                            Text(
                                "The camera shows the pose guide over the live picture and checks your pose on your phone. " +
                                    "The picture never leaves your phone, and Poseit works offline.",
                                style = PoseitType.body,
                                color = PoseitColors.textSecondaryInverted,
                                modifier = Modifier.widthIn(max = 560.dp),
                            )
                            if (permanentlyDenied) {
                                Spacer(Modifier.height(Space.l))
                                Text(
                                    "Camera access is turned off for Poseit. Turn it on in the app settings under Permissions.",
                                    style = PoseitType.bodyStrong,
                                    color = PoseitColors.textPrimaryInverted,
                                    modifier = Modifier.widthIn(max = 560.dp),
                                )
                            }
                        }
                        if (showPicture) ViewfinderPicture(Modifier.height(pictureHeight))
                    },
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = Space.edge, end = Space.edge, top = Space.l, bottom = Space.s),
        ) {
            if (permanentlyDenied) {
                AccentButton("Open settings", onClick = onOpenSettings, modifier = Modifier.fillMaxWidth())
            } else {
                AccentButton("Allow camera", onClick = onAllow, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(Space.xs))
            TertiaryButton("Not now", onClick = onBack, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Shorter than this, the picture card is left out (small phones, large text) and the words get the room. */
private val MinPictureHeight = 168.dp

/** A white lay figure in cream viewfinder marks on the stage: what the camera will show. Decorative. */
@Composable
private fun ViewfinderPicture(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = Space.xl),
        contentAlignment = Alignment.Center,
    ) {
        PoseThumbnail(
            template = remember { BuiltInPoses.all.first() },
            background = null,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(3f / 4f)
                .frameMarks(inset = Space.xs)
                .padding(Space.l),
        )
    }
}
