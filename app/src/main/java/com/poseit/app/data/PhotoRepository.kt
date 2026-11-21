package com.poseit.app.data

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A photo taken with Poseit (stored in the shared gallery under Pictures/Poseit). */
data class SavedPhoto(
    val uri: Uri,
    val displayName: String,
    val dateTakenMillis: Long,
    val width: Int,
    val height: Int,
)

/** Outcome of [PhotoRepository.deleteOrRequestConsent]. */
sealed interface PhotoDeleteResult {
    /** The photo is gone. */
    data object Deleted : PhotoDeleteResult

    /**
     * The system has to ask the user first (the file is not owned by this install, e.g. after a reinstall).
     * Launch [intentSender] with `ActivityResultContracts.StartIntentSenderForResult`; on RESULT_OK call
     * [PhotoRepository.delete] again when [retryAfterConsent] is true (Android 10), otherwise the system has
     * already deleted it (Android 11+).
     */
    class NeedsConsent(val intentSender: IntentSender, val retryAfterConsent: Boolean) : PhotoDeleteResult

    data object Failed : PhotoDeleteResult
}

/**
 * Access to photos Poseit saved via MediaStore (RELATIVE_PATH = "Pictures/Poseit" on Android 10+,
 * the same folder via the legacy path on older versions).
 */
class PhotoRepository(private val context: Context) {

    private val resolver get() = context.contentResolver

    private val collection: Uri
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    /**
     * The runtime permission needed to list photos on this device, or null when none is needed.
     * Android 13+ shows an app its own MediaStore images without any permission; Android 10-12 do too, but only
     * for files created by the current install, so the permission is still requested there (best effort).
     */
    val readPermission: String?
        get() = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) Manifest.permission.READ_EXTERNAL_STORAGE else null

    /** True when [readPermission] is needed and not granted yet. */
    fun needsReadPermission(): Boolean {
        val permission = readPermission ?: return false
        return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

    /** Newest first. Safe to call from any thread (does its own IO dispatching). */
    suspend fun list(): List<SavedPhoto> = withContext(Dispatchers.IO) {
        try {
            query()
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to read photos", e)
            emptyList()
        } catch (e: RuntimeException) {
            // Some OEM MediaStore implementations throw on unexpected columns / selections.
            Log.e(TAG, "Could not query photos", e)
            emptyList()
        }
    }

    private fun query(): List<SavedPhoto> {
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.DATE_TAKEN)
            add(MediaStore.Images.Media.DATE_ADDED)
            add(MediaStore.Images.Media.WIDTH)
            add(MediaStore.Images.Media.HEIGHT)
            add(MediaStore.Images.Media.ORIENTATION)
        }.toTypedArray()

        val selection: String
        val args: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // RELATIVE_PATH is stored with a trailing slash ("Pictures/Poseit/"); also accept sub-folders but not
            // look-alikes such as "Pictures/PoseitOld/".
            val base = "${Environment.DIRECTORY_PICTURES}/$ALBUM"
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ? OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            args = arrayOf("$base/", "$base/%")
        } else {
            @Suppress("DEPRECATION")
            selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            args = arrayOf("%/${Environment.DIRECTORY_PICTURES}/$ALBUM/%")
        }

        val photos = ArrayList<SavedPhoto>()
        val cursor: Cursor = resolver.query(
            collection,
            projection,
            selection,
            args,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        ) ?: return emptyList()
        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val orientationCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val taken = if (c.isNull(takenCol)) 0L else c.getLong(takenCol)
                val addedMillis = if (c.isNull(addedCol)) 0L else c.getLong(addedCol) * 1000L
                val w = if (c.isNull(widthCol)) 0 else c.getInt(widthCol)
                val h = if (c.isNull(heightCol)) 0 else c.getInt(heightCol)
                val rotated = !c.isNull(orientationCol) && c.getInt(orientationCol).let { it == 90 || it == 270 }
                photos += SavedPhoto(
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = c.getString(nameCol).orEmpty(),
                    dateTakenMillis = if (taken > 0L) taken else addedMillis,
                    width = if (rotated) h else w,
                    height = if (rotated) w else h,
                )
            }
        }
        // DATE_TAKEN can be missing on some devices, so sort on the resolved timestamp; newest first.
        return photos.sortedWith(compareByDescending<SavedPhoto> { it.dateTakenMillis }.thenByDescending { it.uri.lastPathSegment?.toLongOrNull() ?: 0L })
    }

    /** Deletes a photo Poseit created. Returns true on success. */
    suspend fun delete(photo: SavedPhoto): Boolean = deleteOrRequestConsent(photo) == PhotoDeleteResult.Deleted

    /**
     * Like [delete], but when the system needs the user's consent (a file this install does not own) returns an
     * [IntentSender] the UI can launch instead of silently failing.
     */
    suspend fun deleteOrRequestConsent(photo: SavedPhoto): PhotoDeleteResult = withContext(Dispatchers.IO) {
        try {
            val rows = resolver.delete(photo.uri, null, null)
            if (rows > 0 || !exists(photo.uri)) PhotoDeleteResult.Deleted else PhotoDeleteResult.Failed
        } catch (e: SecurityException) {
            consentFor(photo.uri, e)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Could not delete ${photo.uri}", e)
            PhotoDeleteResult.Failed
        }
    }

    private fun consentFor(uri: Uri, e: SecurityException): PhotoDeleteResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return try {
                PhotoDeleteResult.NeedsConsent(
                    MediaStore.createDeleteRequest(resolver, listOf(uri)).intentSender,
                    retryAfterConsent = false,
                )
            } catch (inner: RuntimeException) {
                Log.e(TAG, "Could not create a delete request for $uri", inner)
                PhotoDeleteResult.Failed
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
            return PhotoDeleteResult.NeedsConsent(e.userAction.actionIntent.intentSender, retryAfterConsent = true)
        }
        Log.w(TAG, "Not allowed to delete $uri", e)
        return PhotoDeleteResult.Failed
    }

    private fun exists(uri: Uri): Boolean = try {
        resolver.query(uri, arrayOf(MediaStore.Images.Media._ID), null, null, null)?.use { it.count > 0 } ?: false
    } catch (_: RuntimeException) {
        // Can't tell (e.g. no read permission): assume it is still there so the caller reports a failure.
        true
    }

    /** A chooser intent to share [uri]. */
    fun shareIntent(uri: Uri): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            // ClipData makes the read grant reach the chosen app on all Android versions.
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share photo").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** An intent that opens [uri] in the user's gallery / photo viewer app. */
    fun viewIntent(uri: Uri): Intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    companion object {
        const val ALBUM = "Poseit"
        private const val TAG = "PhotoRepository"
    }
}
