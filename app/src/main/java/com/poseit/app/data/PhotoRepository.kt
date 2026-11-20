package com.poseit.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri

/** A photo taken with Poseit (stored in the shared gallery under Pictures/Poseit). */
data class SavedPhoto(
    val uri: Uri,
    val displayName: String,
    val dateTakenMillis: Long,
    val width: Int,
    val height: Int,
)

/**
 * Access to photos Poseit saved via MediaStore (RELATIVE_PATH = "Pictures/Poseit" on Android 10+,
 * the same folder via the legacy path on older versions).
 */
class PhotoRepository(private val context: Context) {

    /** Newest first. Safe to call from any thread (does its own IO dispatching). */
    suspend fun list(): List<SavedPhoto> {
        TODO("implemented by the gallery agent")
    }

    /** Deletes a photo Poseit created. Returns true on success. */
    suspend fun delete(photo: SavedPhoto): Boolean {
        TODO("implemented by the gallery agent")
    }

    /** A chooser intent to share [uri]. */
    fun shareIntent(uri: Uri): Intent {
        TODO("implemented by the gallery agent")
    }

    companion object {
        const val ALBUM = "Poseit"
    }
}
