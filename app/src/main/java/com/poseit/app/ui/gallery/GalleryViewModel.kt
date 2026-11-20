package com.poseit.app.ui.gallery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poseit.app.data.PhotoDeleteResult
import com.poseit.app.data.PhotoRepository
import com.poseit.app.data.SavedPhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the gallery grid / viewer show. */
data class GalleryUiState(
    /** Visible photos, newest first (photos waiting for an undoable delete are hidden). */
    val photos: List<SavedPhoto> = emptyList(),
    /** True until the first query finished. */
    val loading: Boolean = true,
    /** A refresh triggered by pull-to-refresh is running. */
    val refreshing: Boolean = false,
)

/** One-off things the screen has to do. */
sealed interface GalleryEvent {
    data class Message(val text: String) : GalleryEvent

    /** Ask the system for permission to delete [photo] (a file this install does not own). */
    data class RequestDeleteConsent(val photo: SavedPhoto, val request: PhotoDeleteResult.NeedsConsent) : GalleryEvent
}

class GalleryViewModel(private val repository: PhotoRepository) : ViewModel() {

    private val all = MutableStateFlow<List<SavedPhoto>>(emptyList())
    private val loading = MutableStateFlow(true)
    private val refreshing = MutableStateFlow(false)

    /** Photos "deleted" with Undo still possible: hidden from the UI, removed for real by [commitPendingDelete]. */
    private val pending = MutableStateFlow<Map<Uri, SavedPhoto>>(emptyMap())

    /** Photos waiting for the system delete-consent dialog (kept hidden meanwhile). */
    private val awaitingConsent = MutableStateFlow<Map<Uri, PhotoDeleteResult.NeedsConsent>>(emptyMap())

    private val _events = Channel<GalleryEvent>(Channel.BUFFERED)
    val events: Flow<GalleryEvent> = _events.receiveAsFlow()

    val state: StateFlow<GalleryUiState> = combine(all, pending, awaitingConsent, loading, refreshing) { photos, hidden, consent, isLoading, isRefreshing ->
        GalleryUiState(
            photos = photos.filter { it.uri !in hidden && it.uri !in consent },
            loading = isLoading,
            refreshing = isRefreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GalleryUiState())

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    /** Re-queries MediaStore. [userInitiated] shows the pull-to-refresh indicator. */
    fun refresh(userInitiated: Boolean = false) {
        if (userInitiated) refreshing.value = true
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                all.value = repository.list()
            } finally {
                loading.value = false
                refreshing.value = false
            }
        }
    }

    /** Deletes right away (the user already confirmed). */
    fun deleteNow(photo: SavedPhoto) {
        viewModelScope.launch { performDelete(photo) }
    }

    /** Hides [photo] immediately; it is deleted for real by [commitPendingDelete] unless [undoDelete] is called first. */
    fun deleteDeferred(photo: SavedPhoto) {
        pending.update { it + (photo.uri to photo) }
    }

    fun undoDelete(uri: Uri) {
        pending.update { it - uri }
    }

    fun commitPendingDelete(uri: Uri) {
        val photo = pending.value[uri] ?: return
        viewModelScope.launch {
            try {
                performDelete(photo)
            } finally {
                pending.update { it - uri }
            }
        }
    }

    private suspend fun performDelete(photo: SavedPhoto) {
        when (val result = repository.deleteOrRequestConsent(photo)) {
            PhotoDeleteResult.Deleted -> all.update { list -> list.filterNot { it.uri == photo.uri } }
            is PhotoDeleteResult.NeedsConsent -> {
                awaitingConsent.update { it + (photo.uri to result) }
                _events.send(GalleryEvent.RequestDeleteConsent(photo, result))
            }
            PhotoDeleteResult.Failed -> {
                _events.send(GalleryEvent.Message("Couldn't delete this photo"))
                refresh()
            }
        }
    }

    /** Result of the system consent dialog launched for [GalleryEvent.RequestDeleteConsent]. */
    fun onDeleteConsentResult(photo: SavedPhoto, granted: Boolean) {
        val request = awaitingConsent.value[photo.uri] ?: return
        viewModelScope.launch {
            try {
                if (!granted) {
                    _events.send(GalleryEvent.Message("Photo kept"))
                } else if (request.retryAfterConsent && !repository.delete(photo)) {
                    _events.send(GalleryEvent.Message("Couldn't delete this photo"))
                }
            } finally {
                awaitingConsent.update { it - photo.uri }
                refresh()
            }
        }
    }

    override fun onCleared() {
        // Undo is no longer possible once the screen is gone: finish pending deletes outside the ViewModel scope.
        val leftovers = pending.value.values.toList()
        if (leftovers.isNotEmpty()) {
            cleanupScope.launch { leftovers.forEach { repository.delete(it) } }
        }
    }

    private companion object {
        /** Outlives the ViewModel so a deferred delete still completes after the user leaves the gallery. */
        val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
