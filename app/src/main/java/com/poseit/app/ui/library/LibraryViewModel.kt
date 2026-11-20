package com.poseit.app.ui.library

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poseit.app.data.PoseRepository
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the library screen renders. */
data class LibraryUiState(
    val loading: Boolean = true,
    val filter: LibraryFilter = LibraryFilter(),
    /** The poses to show in the grid (tab + refinements applied). */
    val poses: List<PoseTemplate> = emptyList(),
    /** How many poses the current tab holds before refinements (drives the empty-state wording). */
    val tabTotal: Int = 0,
    val favorites: Set<String> = emptySet(),
    val poseOfTheDay: PoseTemplate? = null,
    val categories: List<PoseCategory> = emptyList(),
    val locationTags: List<PoseTag> = emptyList(),
    val moodTags: List<PoseTag> = emptyList(),
    val customCount: Int = 0,
) {
    /** The hero card only shows on the unfiltered home view. */
    val showHero: Boolean get() = poseOfTheDay != null && filter.tab == LibraryTab.ALL && !filter.hasRefinements
}

class LibraryViewModel(
    private val repository: PoseRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /**
     * The search text lives in snapshot state (not a flow) so the text field updates synchronously while typing;
     * it is fed into the pipeline with [snapshotFlow].
     */
    var query by mutableStateOf("")
        private set

    private val refinements = MutableStateFlow(LibraryFilter())

    val state: StateFlow<LibraryUiState> = combine(
        repository.allPoses,
        repository.favorites,
        repository.recent,
        refinements,
        snapshotFlow { query },
    ) { allRaw, favorites, recent, refine, q ->
        // Custom poses come first, so a (corrupt) custom pose reusing a built-in id wins, like PoseRepository.pose().
        val all = allRaw.distinctBy { it.id }
        val filter = refine.copy(query = q)
        val tabPoses = LibraryFiltering.posesForTab(filter.tab, all, favorites, recent)
        val (locations, moods) = LibraryFiltering.availableTags(all, listOf(filter.location, filter.mood))
        LibraryUiState(
            loading = false,
            filter = filter,
            poses = LibraryFiltering.refine(tabPoses, filter),
            tabTotal = tabPoses.size,
            favorites = favorites,
            poseOfTheDay = LibraryFiltering.poseOfTheDay(all, LibraryFiltering.localEpochDay(clock())),
            categories = LibraryFiltering.availableCategories(all, filter.category),
            locationTags = locations,
            moodTags = moods,
            customCount = all.count { it.isCustom },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryUiState())

    fun onQueryChange(text: String) {
        query = text
    }

    fun selectTab(tab: LibraryTab) = refinements.update { it.copy(tab = tab) }

    fun selectPeople(people: PeopleFilter) = refinements.update { it.copy(people = people) }

    /** Tapping the selected category again clears it. */
    fun toggleCategory(category: PoseCategory?) =
        refinements.update { it.copy(category = if (category == it.category) null else category) }

    fun toggleLocation(tag: PoseTag) =
        refinements.update { it.copy(location = if (tag == it.location) null else tag) }

    fun toggleMood(tag: PoseTag) =
        refinements.update { it.copy(mood = if (tag == it.mood) null else tag) }

    fun clearRefinements() {
        query = ""
        refinements.update { it.cleared() }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Could not update favourites", e)
            }
        }
    }

    private companion object {
        const val TAG = "LibraryViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
