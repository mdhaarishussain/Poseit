package com.poseit.app.ui.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poseit.app.data.PoseRepository
import com.poseit.app.pose.PoseTemplate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.io.File

/** A pose plus which of its optional images exist on this device. */
data class PoseDetails(
    val pose: PoseTemplate,
    val photoPath: String?,
    val outlinePath: String?,
)

sealed interface PoseDetailUiState {
    data object Loading : PoseDetailUiState
    data object NotFound : PoseDetailUiState
    data class Ready(val details: PoseDetails, val isFavorite: Boolean) : PoseDetailUiState
}

class PoseDetailViewModel(
    private val poseId: String,
    private val repository: PoseRepository,
) : ViewModel() {

    private val _deleted = MutableStateFlow(false)
    /** Becomes true once the (custom) pose has been deleted; the screen then navigates back. */
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()
    private var deleting = false

    @OptIn(ExperimentalCoroutinesApi::class)
    private val details: Flow<PoseDetails?> = repository.poseFlow(poseId)
        .distinctUntilChanged()
        .transformLatest { pose ->
            if (pose != null) {
                emit(
                    PoseDetails(
                        pose = pose,
                        photoPath = pose.referenceImage?.takeIf(::isReadableFile),
                        outlinePath = pose.outlineImage?.takeIf(::isReadableFile),
                    ),
                )
            } else {
                // Custom poses are read from disk asynchronously: give the repository a moment before
                // declaring the pose missing, so the screen doesn't flash "not found".
                delay(NOT_FOUND_GRACE_MS)
                emit(null)
            }
        }
        .flowOn(Dispatchers.IO)

    val state: StateFlow<PoseDetailUiState> = combine(details, repository.favorites.map { poseId in it }) { d, fav ->
        if (d == null) PoseDetailUiState.NotFound else PoseDetailUiState.Ready(d, fav)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PoseDetailUiState.Loading)

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(poseId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Could not update favourites", e)
            }
        }
    }

    fun delete() {
        if (deleting) return
        deleting = true
        viewModelScope.launch {
            try {
                repository.deleteCustom(poseId)
                _deleted.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete pose $poseId", e)
                deleting = false
            }
        }
    }

    private fun isReadableFile(path: String): Boolean = runCatching { File(path).let { it.isFile && it.canRead() } }.getOrDefault(false)

    private companion object {
        const val TAG = "PoseDetailViewModel"
        const val NOT_FOUND_GRACE_MS = 1_200L
    }
}
