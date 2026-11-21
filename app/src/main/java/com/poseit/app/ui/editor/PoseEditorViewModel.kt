package com.poseit.app.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poseit.app.data.PoseRepository
import com.poseit.app.pose.CameraAdvice
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import kotlin.math.max

/** One-off events for the editor screen. */
sealed interface EditorEvent {
    /** @param message what to confirm on the next screen: "Pose saved", "Changes saved" or "Saved as a new pose" */
    data class Saved(val id: String, val message: String = "Pose saved") : EditorEvent
    data class Message(val text: String) : EditorEvent
}

/**
 * State of the pose editor: the figures being edited (frame-normalised), the selected figure, undo/redo history and
 * the drag currently in progress. The figures, the selection and the save-sheet draft also go into [savedState], so
 * unsaved work survives the process being killed in the background (the undo history does not).
 */
class PoseEditorViewModel(
    private val poseRepository: PoseRepository,
    private val basePoseId: String?,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private data class Snapshot(val figures: List<Skeleton>, val selected: Int)

    private class Gesture(
        val figure: Int,
        val target: DragTarget,
        val start: Skeleton,
        val fingerStart: Vec2,
        val before: Snapshot,
        var moved: Boolean = false,
    )

    var loading by mutableStateOf(true)
        private set

    /** The pose we started from (null = neutral mannequin). */
    var base by mutableStateOf<PoseTemplate?>(null)
        private set

    var aspect by mutableFloatStateOf(DEFAULT_ASPECT)
        private set

    var figures by mutableStateOf<List<Skeleton>>(emptyList())
        private set

    var selected by mutableIntStateOf(0)
        private set

    var showGrid by mutableStateOf(true)

    /** Show the base pose's reference photo behind the figures (photo poses only). */
    var showReference by mutableStateOf(true)

    var referenceBitmap by mutableStateOf<ImageBitmap?>(null)
        private set

    /** What the current drag manipulates (for highlighting), null when idle. */
    var activeTarget by mutableStateOf<DragTarget?>(null)
        private set

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    var saving by mutableStateOf(false)
        private set

    /** Suggested name for the save sheet. */
    var suggestedName by mutableStateOf("")
        private set

    /**
     * What the user entered in the save sheet (null = not touched yet: use the suggestion / the base pose's values).
     * Kept here so closing the sheet with "Keep editing" or a swipe doesn't throw it away.
     */
    var draftName by mutableStateOf(savedState.get<String>(KEY_DRAFT_NAME))
        private set
    var draftCategory by mutableStateOf(savedState.get<String>(KEY_DRAFT_CATEGORY)?.let { n -> PoseCategory.entries.firstOrNull { it.name == n } })
        private set
    var draftTags by mutableStateOf(
        savedState.get<ArrayList<String>>(KEY_DRAFT_TAGS)?.mapNotNull { n -> PoseTag.entries.firstOrNull { it.name == n } }?.toSet(),
    )
        private set

    fun updateDraftName(name: String) {
        draftName = name
        savedState[KEY_DRAFT_NAME] = name
    }

    fun updateDraftCategory(category: PoseCategory) {
        draftCategory = category
        savedState[KEY_DRAFT_CATEGORY] = category.name
    }

    fun updateDraftTags(tags: Set<PoseTag>) {
        draftTags = tags
        savedState[KEY_DRAFT_TAGS] = ArrayList(tags.map { it.name })
    }

    private var initialFigures: List<Skeleton> = emptyList()
    private var savedFigures by mutableStateOf<List<Skeleton>>(emptyList())
    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()
    private var gesture: Gesture? = null

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    /** True when there are changes that have not been saved. */
    val isDirty: Boolean get() = !loading && figures != savedFigures

    /** Whether the base is one of the user's own poses ("Save changes" possible). */
    val canOverwriteBase: Boolean get() = base?.let { it.source != PoseSource.BUILT_IN } == true

    val canAddPerson: Boolean get() = figures.size < MAX_PEOPLE
    val canRemovePerson: Boolean get() = figures.size > 1

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val template = basePoseId?.let { id ->
            poseRepository.pose(id)
                // Custom poses load asynchronously at app start: give them a moment.
                ?: withTimeoutOrNull(LOAD_TIMEOUT_MS) { poseRepository.poseFlow(id).filterNotNull().first() }
        }
        if (basePoseId != null && template == null) {
            _events.send(EditorEvent.Message("Couldn't find that pose, so the editor starts from a neutral mannequin"))
        }
        val frameAspect = template?.frameAspect?.takeIf { it.isFinite() && it > 0.2f && it < 5f } ?: DEFAULT_ASPECT
        val start = template?.figures?.filter { it.points.isNotEmpty() }?.take(MAX_PEOPLE)?.takeIf { it.isNotEmpty() }
            ?: listOf(SkeletonEditing.neutralFigure(frameAspect))
        val restored = restoreFigures()
        base = template
        aspect = frameAspect
        figures = restored ?: start
        initialFigures = start
        savedFigures = start
        selected = if (restored != null) (savedState.get<Int>(KEY_SELECTED) ?: 0).coerceIn(0, restored.lastIndex) else 0
        suggestedName = when {
            template == null -> nextDefaultName()
            template.source == PoseSource.BUILT_IN -> "${template.name} (my version)"
            else -> template.name
        }
        loading = false
        template?.referenceImage?.let { path -> referenceBitmap = loadReference(path) }
    }

    /** The unsaved figures stored before the process was killed, or null. */
    private fun restoreFigures(): List<Skeleton>? {
        val stored = savedState.get<String>(KEY_FIGURES) ?: return null
        return try {
            stateJson.decodeFromString(ListSerializer(Skeleton.serializer()), stored)
                .filter { it.points.isNotEmpty() }
                .take(MAX_PEOPLE)
                .takeIf { it.isNotEmpty() }
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** Stores the figures and selection for process death; called after every committed change (not every drag frame). */
    private fun persist() {
        if (loading) return
        try {
            savedState[KEY_FIGURES] = stateJson.encodeToString(ListSerializer(Skeleton.serializer()), figures)
            savedState[KEY_SELECTED] = selected
        } catch (e: SerializationException) {
            Log.w(TAG, "Could not store the editor state", e)
        }
    }

    private fun nextDefaultName(): String {
        val names = poseRepository.customPoses.value.map { it.name.lowercase() }.toSet()
        var n = poseRepository.customPoses.value.size + 1
        while ("my pose $n" in names) n++
        return "My pose $n"
    }

    private suspend fun loadReference(path: String): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= REFERENCE_MAX_SIDE) sample *= 2
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(path, options)?.asImageBitmap()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load reference image", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Reference image too large", e)
            null
        }
    }

    // ---- gestures --------------------------------------------------------------------------------------------

    /**
     * A finger went down at [touch] (isotropic frame units). Returns true when it grabbed something (a handle or
     * a figure) and the following moves should edit.
     */
    fun onGestureStart(touch: Vec2, handleRadius: Float): Boolean {
        if (loading || figures.isEmpty()) return false
        val current = figures.getOrNull(selected)
        val onSelected = current?.let { SkeletonEditing.hitTest(it, aspect, touch, handleRadius) }
        val (index, target) = if (onSelected != null) {
            selected to onSelected
        } else {
            // Tapped another person? Select it; dragging it moves it.
            val other = figures.indices
                .filter { it != selected }
                .map { it to SkeletonEditing.distanceToFigure(figures[it], aspect, touch) }
                .filter { it.second <= handleRadius * 0.6f }
                .minByOrNull { it.second }
                ?: return false
            selected = other.first
            other.first to DragTarget.Move
        }
        gesture = Gesture(index, target, figures[index], touch, Snapshot(figures, selected))
        activeTarget = target
        return true
    }

    fun onGestureMove(touch: Vec2) {
        val g = gesture ?: return
        val updated = SkeletonEditing.drag(g.start, aspect, g.target, g.fingerStart, touch)
        if (!g.moved && updated == g.start) return
        g.moved = true
        figures = figures.toMutableList().also { if (g.figure in it.indices) it[g.figure] = updated }
    }

    fun onGestureEnd() {
        val g = gesture ?: return
        gesture = null
        activeTarget = null
        if (g.moved && figures != g.before.figures) pushUndo(g.before)
        persist()
    }

    /** A second finger arrived (view zoom): put the figure back as it was. */
    fun onGestureCancel() {
        val g = gesture ?: return
        gesture = null
        activeTarget = null
        if (g.moved) figures = g.before.figures
        persist()
    }

    fun select(index: Int) {
        if (index in figures.indices) selected = index
        persist()
    }

    // ---- toolbar ---------------------------------------------------------------------------------------------

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(Snapshot(figures, selected))
        restore(previous)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(Snapshot(figures, selected))
        restore(next)
    }

    fun mirrorSelected() = edit { list, sel ->
        list[sel] = SkeletonEditing.mirrorInPlace(list[sel])
        sel
    }

    fun reset() {
        if (figures == initialFigures) return
        pushUndo(Snapshot(figures, selected))
        figures = initialFigures
        selected = selected.coerceIn(0, initialFigures.lastIndex)
        persist()
    }

    fun addPerson() {
        if (!canAddPerson) return
        edit { list, _ ->
            list += SkeletonEditing.placementForNewFigure(list, aspect)
            list.lastIndex
        }
    }

    fun removeSelected() {
        if (!canRemovePerson) return
        edit { list, sel ->
            list.removeAt(sel)
            (sel - 1).coerceAtLeast(0)
        }
    }

    /** Applies an undoable change to a copy of the figures; [block] returns the new selection. */
    private inline fun edit(block: (MutableList<Skeleton>, Int) -> Int) {
        if (figures.isEmpty()) return
        val before = Snapshot(figures, selected)
        val list = figures.toMutableList()
        val newSelected = block(list, selected.coerceIn(0, list.lastIndex))
        if (list == figures) return
        pushUndo(before)
        figures = list
        selected = newSelected.coerceIn(0, list.lastIndex)
        persist()
    }

    private fun pushUndo(snapshot: Snapshot) {
        undoStack.addLast(snapshot)
        while (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        updateHistoryFlags()
    }

    private fun restore(snapshot: Snapshot) {
        figures = snapshot.figures
        selected = snapshot.selected.coerceIn(0, snapshot.figures.lastIndex.coerceAtLeast(0))
        updateHistoryFlags()
        persist()
    }

    private fun updateHistoryFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    // ---- saving ----------------------------------------------------------------------------------------------

    /**
     * Saves the figures. [asNew] (or a built-in / missing base) creates a new custom pose (source DRAWN); otherwise
     * the user's own base pose is updated in place.
     */
    fun save(name: String, category: PoseCategory, tags: Set<PoseTag>, asNew: Boolean) {
        if (saving || loading || figures.isEmpty()) return
        val cleanName = name.trim().ifEmpty { suggestedName.ifEmpty { "My pose" } }.take(MAX_NAME_LENGTH)
        val current = figures
        val baseTemplate = base
        saving = true
        viewModelScope.launch {
            try {
                val overwrite = !asNew && baseTemplate != null && baseTemplate.source != PoseSource.BUILT_IN
                val template = if (overwrite && baseTemplate != null) {
                    baseTemplate.copy(name = cleanName, category = category, tags = tags, figures = current, frameAspect = aspect)
                } else {
                    PoseTemplate(
                        id = "custom_" + UUID.randomUUID(),
                        name = cleanName,
                        category = category,
                        figures = current,
                        frameAspect = aspect,
                        description = baseTemplate?.description?.takeIf { it.isNotBlank() } ?: DEFAULT_DESCRIPTION,
                        tips = baseTemplate?.tips?.takeIf { it.isNotEmpty() } ?: DEFAULT_TIPS,
                        camera = baseTemplate?.camera ?: CameraAdvice(shot = inferShot(current)),
                        tags = tags,
                        difficulty = baseTemplate?.difficulty ?: 1,
                        source = PoseSource.DRAWN,
                        createdAt = System.currentTimeMillis(),
                    )
                }
                poseRepository.saveCustom(template)
                savedFigures = current
                val message = when {
                    overwrite -> "Changes saved"
                    baseTemplate != null && baseTemplate.source != PoseSource.BUILT_IN -> "Saved as a new pose"
                    else -> "Pose saved"
                }
                _events.send(EditorEvent.Saved(template.id, message))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Could not save pose", e)
                _events.send(EditorEvent.Message("Couldn't save the pose. Try again."))
            } finally {
                saving = false
            }
        }
    }

    companion object {
        const val MAX_PEOPLE = 4
        const val MAX_NAME_LENGTH = 40
        private const val MAX_HISTORY = 100
        private const val DEFAULT_ASPECT = 3f / 4f
        private const val LOAD_TIMEOUT_MS = 2_000L
        private const val REFERENCE_MAX_SIDE = 1024
        private const val TAG = "PoseEditor"
        private const val KEY_FIGURES = "editor.figures"
        private const val KEY_SELECTED = "editor.selected"
        private const val KEY_DRAFT_NAME = "editor.draftName"
        private const val KEY_DRAFT_CATEGORY = "editor.draftCategory"
        private const val KEY_DRAFT_TAGS = "editor.draftTags"
        private val stateJson = Json { ignoreUnknownKeys = true }
        private const val DEFAULT_DESCRIPTION = "Your own pose, drawn in the Poseit editor."
        private val DEFAULT_TIPS = listOf(
            "Match the guide one body part at a time: feet first, then hips, then arms.",
            "Relax your shoulders and breathe out just before the shot.",
        )

        /** Which shot the figures suggest, from the lowest joints that are inside the frame. */
        internal fun inferShot(figures: List<Skeleton>): ShotType {
            fun inFrame(j: Joint) = figures.any { f -> f[j]?.let { it.y in 0f..1.02f && it.x in -0.02f..1.02f } == true }
            return when {
                inFrame(Joint.LEFT_ANKLE) || inFrame(Joint.RIGHT_ANKLE) -> ShotType.FULL_BODY
                inFrame(Joint.LEFT_KNEE) || inFrame(Joint.RIGHT_KNEE) -> ShotType.THREE_QUARTER
                inFrame(Joint.LEFT_HIP) || inFrame(Joint.RIGHT_HIP) -> ShotType.HALF_BODY
                else -> ShotType.CLOSE_UP
            }
        }
    }
}
