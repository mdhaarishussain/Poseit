package com.poseit.app.ui.create

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poseit.app.data.PoseRepository
import com.poseit.app.detect.PersonSegmenter
import com.poseit.app.detect.PoseDetector
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.io.CustomPoseFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Where the "create from photo" flow is. */
enum class CreateStep { INTRO, LOADING, DETECTING, NO_PERSON, ERROR, REVIEW }

enum class OutlineStatus { IDLE, WORKING, READY, UNAVAILABLE }

data class CreateUiState(
    val step: CreateStep = CreateStep.INTRO,
    /** The picked photo, upright and downscaled. */
    val photo: Bitmap? = null,
    /** Detected people in image-normalised coordinates, ordered left to right. */
    val people: List<Skeleton> = emptyList(),
    /** Indices into [people] that will be saved. */
    val selected: Set<Int> = emptySet(),
    val outline: Bitmap? = null,
    val outlineStatus: OutlineStatus = OutlineStatus.IDLE,
    val errorMessage: String? = null,
    val saving: Boolean = false,
) {
    val imageAspect: Float get() = photo?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: (3f / 4f)
    val selectedPeople: List<Skeleton> get() = selected.sorted().mapNotNull { people.getOrNull(it) }
}

sealed interface CreateEvent {
    data class Saved(val id: String) : CreateEvent
    data class Message(val text: String) : CreateEvent
}

/**
 * Pick a photo -> decode it upright -> detect up to 4 people (MediaPipe, accurate model) -> let the user choose who to
 * keep and name the pose -> save the reference JPEG, the segmentation outline PNG and the custom template.
 */
class CreateFromPhotoViewModel(
    context: Context,
    private val poseRepository: PoseRepository,
) : ViewModel() {

    private val appContext = context.applicationContext

    var state by mutableStateOf(CreateUiState())
        private set

    var name by mutableStateOf("")
    var category by mutableStateOf(PoseCategory.CUSTOM)
    var tags by mutableStateOf<Set<PoseTag>>(emptySet())

    private val _events = Channel<CreateEvent>(Channel.BUFFERED)
    val events: Flow<CreateEvent> = _events.receiveAsFlow()

    private var analyzeJob: Job? = null
    private var outlineJob: Job? = null

    fun onPhotoPicked(uri: Uri) {
        analyzeJob?.cancel()
        outlineJob?.cancel()
        analyzeJob = viewModelScope.launch {
            state = CreateUiState(step = CreateStep.LOADING)
            val photo = withContext(Dispatchers.IO) { PhotoLoader.loadUpright(appContext, uri, MAX_SIDE) }
            if (photo == null) {
                state = CreateUiState(step = CreateStep.ERROR, errorMessage = "Couldn't open that photo. Try another one.")
                return@launch
            }
            state = state.copy(step = CreateStep.DETECTING, photo = photo)
            val detected = withContext(Dispatchers.Default) { detect(photo) }
            if (detected == null) {
                state = state.copy(step = CreateStep.ERROR, errorMessage = "Pose detection isn't available on this device right now.")
                return@launch
            }
            val people = detected
                .filter { person -> person.points.keys.count { person.isVisible(it, MIN_VISIBILITY) } >= MIN_VISIBLE_JOINTS }
                .sortedBy { centerX(it) }
                .take(MAX_PEOPLE)
            if (people.isEmpty()) {
                state = state.copy(step = CreateStep.NO_PERSON)
                return@launch
            }
            if (name.isBlank()) name = nextDefaultName()
            state = state.copy(step = CreateStep.REVIEW, people = people, selected = people.indices.toSet())
            startOutline(photo)
        }
    }

    /** Back to the explainer (e.g. "pick another photo" from an error). */
    fun reset() {
        analyzeJob?.cancel()
        outlineJob?.cancel()
        state = CreateUiState()
    }

    fun togglePerson(index: Int) {
        val s = state
        if (index !in s.people.indices) return
        val next = if (index in s.selected) s.selected - index else s.selected + index
        // At least one person has to stay selected.
        if (next.isNotEmpty()) state = s.copy(selected = next)
    }

    private fun startOutline(photo: Bitmap) {
        outlineJob?.cancel()
        state = state.copy(outline = null, outlineStatus = OutlineStatus.WORKING)
        outlineJob = viewModelScope.launch {
            val outline = withContext(Dispatchers.Default) { segment(photo) }
            if (state.photo === photo) {
                state = state.copy(outline = outline, outlineStatus = if (outline != null) OutlineStatus.READY else OutlineStatus.UNAVAILABLE)
            }
        }
    }

    fun save() {
        val s = state
        val photo = s.photo ?: return
        val people = s.selectedPeople
        if (s.step != CreateStep.REVIEW || s.saving || people.isEmpty()) return
        val poseName = name.trim().ifEmpty { nextDefaultName() }.take(POSE_NAME_MAX_LENGTH)
        val poseCategory = category
        val poseTags = tags
        state = s.copy(saving = true)
        viewModelScope.launch {
            val id = "custom_" + UUID.randomUUID()
            val refsDir = File(appContext.filesDir, "refs")
            val refFile = File(refsDir, "$id.jpg")
            val outlineFile = File(refsDir, "${id}_outline.png")
            try {
                // The outline preview is usually ready by now; otherwise wait for it (or make it now).
                outlineJob?.join()
                val outline = when (state.outlineStatus) {
                    OutlineStatus.READY -> state.outline
                    OutlineStatus.UNAVAILABLE -> null
                    else -> withContext(Dispatchers.Default) { segment(photo) }
                }
                val outlinePath = withContext(Dispatchers.IO) {
                    if (!PhotoLoader.write(photo, refFile, Bitmap.CompressFormat.JPEG, JPEG_QUALITY)) {
                        throw java.io.IOException("Could not write the reference photo")
                    }
                    // A missing outline is fine: the mannequin and the photo still work as guides.
                    outline?.takeIf { PhotoLoader.write(it, outlineFile, Bitmap.CompressFormat.PNG, 100) }?.let { outlineFile.absolutePath }
                }
                val template: PoseTemplate = CustomPoseFactory.fromDetection(
                    id = id,
                    name = poseName,
                    category = poseCategory,
                    people = people,
                    imageAspect = s.imageAspect,
                    referenceImage = refFile.absolutePath,
                    createdAt = System.currentTimeMillis(),
                ).copy(outlineImage = outlinePath, tags = poseTags)
                check(template.figures.any { it.points.isNotEmpty() }) { "No usable joints in the detection" }
                poseRepository.saveCustom(template)
                _events.send(CreateEvent.Saved(id))
            } catch (e: CancellationException) {
                cleanup(refFile, outlineFile)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not save pose from photo", e)
                cleanup(refFile, outlineFile)
                _events.send(CreateEvent.Message("Couldn't save the pose. Try again."))
            } catch (e: Error) {
                // NotImplementedError / OutOfMemoryError from a dependency: report instead of crashing.
                Log.e(TAG, "Could not save pose from photo", e)
                cleanup(refFile, outlineFile)
                _events.send(CreateEvent.Message("Couldn't save the pose. Try again."))
            } finally {
                state = state.copy(saving = false)
            }
        }
    }

    private fun cleanup(vararg files: File) {
        files.forEach { runCatching { it.delete() } }
    }

    /** Runs MediaPipe on [photo]. Null when the detector can't run at all (missing model, native crash, ...). */
    private fun detect(photo: Bitmap): List<Skeleton>? = try {
        // Still-photo only: no live-stream (GPU) landmarker, so the full model is loaded once, not twice.
        PoseDetector(appContext, accurate = true, maxPoses = MAX_PEOPLE, liveStream = false).use { it.detectStill(photo) }
    } catch (e: Exception) {
        Log.e(TAG, "Pose detection failed", e)
        null
    } catch (e: Error) {
        // NotImplementedError, UnsatisfiedLinkError, OutOfMemoryError ... must not crash the screen.
        Log.e(TAG, "Pose detection failed", e)
        null
    }

    private fun segment(photo: Bitmap): Bitmap? = try {
        PersonSegmenter(appContext).use { it.createGuide(photo) }
    } catch (e: Exception) {
        Log.w(TAG, "Segmentation failed", e)
        null
    } catch (e: Error) {
        Log.w(TAG, "Segmentation failed", e)
        null
    }

    private fun nextDefaultName(): String {
        val custom = poseRepository.customPoses.value
        val names = custom.map { it.name.lowercase() }.toSet()
        var n = custom.size + 1
        while ("my pose $n" in names) n++
        return "My pose $n"
    }

    private fun centerX(s: Skeleton): Float {
        val torso = listOf(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER, Joint.LEFT_HIP, Joint.RIGHT_HIP).mapNotNull { s[it] }
        val pts = torso.ifEmpty { s.points.values.toList() }
        return if (pts.isEmpty()) 0.5f else pts.map { it.x }.average().toFloat()
    }

    companion object {
        const val MAX_PEOPLE = 4
        private const val MAX_SIDE = 1600
        private const val JPEG_QUALITY = 90
        const val MIN_VISIBILITY = 0.5f
        private const val MIN_VISIBLE_JOINTS = 4
        private const val TAG = "CreateFromPhoto"
    }
}
