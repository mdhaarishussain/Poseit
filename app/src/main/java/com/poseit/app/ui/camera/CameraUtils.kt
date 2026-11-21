package com.poseit.app.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.coroutines.cancellation.CancellationException

internal const val CAMERA_TAG = "PoseitCamera"

/**
 * Runs [block] and returns its result, or null (logged) if it throws. Cancellation is re-thrown.
 *
 * The camera screen talks to CameraX, MediaPipe, TextToSpeech and sensors; none of their failures may crash the
 * screen, so calls into them go through here.
 */
internal inline fun <T> safely(block: () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Log.w(CAMERA_TAG, "Ignored failure: ${e.message}", e)
    null
}

internal fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** Android 9 and below need WRITE_EXTERNAL_STORAGE to save into the shared Pictures folder. */
internal fun Context.needsLegacyStoragePermission(): Boolean =
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
