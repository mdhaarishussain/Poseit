package com.poseit.app.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.poseit.app.appContainer
import com.poseit.app.camera.DeviceTilt
import com.poseit.app.camera.FlashMode
import com.poseit.app.camera.LevelSensor
import com.poseit.app.camera.Lens
import com.poseit.app.camera.PoseCamera
import com.poseit.app.camera.SceneAspect
import com.poseit.app.data.AppSettings
import com.poseit.app.data.GridType
import com.poseit.app.data.PhotoRepository
import com.poseit.app.data.PoseRepository
import com.poseit.app.data.SettingsRepository
import com.poseit.app.detect.DetectionResult
import com.poseit.app.detect.PoseDetector
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.coach.CaptureCoach
import com.poseit.app.pose.coach.CaptureState
import com.poseit.app.pose.coach.CoachSettings
import com.poseit.app.pose.detect.LandmarkSmoother
import com.poseit.app.pose.match.CoachHint
import com.poseit.app.pose.match.HintKind
import com.poseit.app.pose.match.LightingAnalyzer
import com.poseit.app.pose.match.LightingIssue
import com.poseit.app.pose.match.LumaGrid
import com.poseit.app.pose.match.MatchConfig
import com.poseit.app.pose.match.MatchResult
import com.poseit.app.pose.match.PoseMatcher
import com.poseit.app.pose.scene.OverlayTransform
import com.poseit.app.pose.scene.SceneMapping
import com.poseit.app.voice.VoiceCoach
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * State holder of the live pose-guide camera.
 *
 * Owns the pose detector (recreated when the model or the number of people changes), the landmark smoother,
 * the matcher pipeline, the capture coach (auto-capture, self-timer, photoshoot), the voice coach and the
 * level sensor. Detection results are processed on a single-threaded pipeline dispatcher; everything the
 * screen draws per frame is published through dedicated high-frequency flows ([live], [transform],
 * [capture], [tilt]) so the screen can read them inside draw lambdas without recomposing.
 *
 * The screen binds [camera] to its PreviewView and forwards frames to [onFrame].
 */
class CameraViewModel(
    private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val poseRepository: PoseRepository,
    private val photoRepository: PhotoRepository,
    private val initialPoseId: String?,
) : ViewModel() {

    /** CameraX wrapper. Bound and unbound by the screen, which owns the PreviewView and the lifecycle. */
    val camera: PoseCamera = PoseCamera(appContext)
    private val voice = VoiceCoach(appContext)
    private val levelSensor = LevelSensor(appContext)
    private val smoother = LandmarkSmoother()
    private val coach = CaptureCoach()
    private val hintStabilizer = HintStabilizer()
    private val handRaise = HandRaiseDetector()

    /** Serial dispatcher that owns the detection -> matching -> coaching pipeline (coach, smoother, stabilisers). */
    private val pipeline = Dispatchers.Default.limitedParallelism(1)

    /** viewModelScope that logs instead of crashing when something unexpected throws. */
    private val scope = CoroutineScope(
        viewModelScope.coroutineContext + CoroutineExceptionHandler { _, e -> Log.e(TAG, "Unexpected camera error", e) },
    )

    // ------------------------------------------------------------------ public state

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .catch { e ->
            Log.e(TAG, "Could not read settings", e)
            emit(AppSettings())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Custom poses first, then the built-in library. */
    val poses: StateFlow<List<PoseTemplate>> = poseRepository.allPoses
        .catch { e ->
            Log.e(TAG, "Could not read poses", e)
            emit(safely { poseRepository.builtIn } ?: emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, safely { poseRepository.builtIn } ?: emptyList())

    val favorites: StateFlow<Set<String>> = poseRepository.favorites
        .catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _ui = MutableStateFlow(CameraUiState())
    val ui: StateFlow<CameraUiState> = _ui.asStateFlow()

    private val _transform = MutableStateFlow(OverlayTransform.IDENTITY)
    /** User adjustment of the pose overlay. High frequency while dragging: read it in draw lambdas. */
    val transform: StateFlow<OverlayTransform> = _transform.asStateFlow()

    private val _live = MutableStateFlow(LiveFrame.EMPTY)
    /** Latest analysed frame (people + match). High frequency: read it in draw lambdas. */
    val live: StateFlow<LiveFrame> = _live.asStateFlow()

    private val _capture = MutableStateFlow<CaptureState>(CaptureState.Idle)
    /** Capture-coach state, updated ~30 times per second. High frequency: read it in draw lambdas. */
    val capture: StateFlow<CaptureState> = _capture.asStateFlow()

    private val _session = MutableStateFlow<SessionInfo?>(null)
    /** The running manual timer / photoshoot, or null. */
    val session: StateFlow<SessionInfo?> = _session.asStateFlow()

    /** Seconds to display during a countdown, or null. */
    val countdown: StateFlow<Int?> = _capture
        .map { (it as? CaptureState.Countdown)?.secondsLeft?.takeIf { s -> s > 0 } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Overall match in percent while somebody is matched against a pose, else null. */
    val scorePercent: StateFlow<Int?> = _live
        .map { frame ->
            frame.match.takeIf { it.peopleDetected > 0 && it.figures.isNotEmpty() }
                ?.let { (it.score * 100f).roundToInt().coerceIn(0, 100) }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _coaching = MutableStateFlow(CoachingState())
    val coaching: StateFlow<CoachingState> = _coaching.asStateFlow()

    private val _tilt = MutableStateFlow<DeviceTilt?>(null)
    /** Device tilt for the level indicator; null when hidden or unavailable. High frequency. */
    val tilt: StateFlow<DeviceTilt?> = _tilt.asStateFlow()

    val hasFlash: StateFlow<Boolean> = cameraState(false) { camera.hasFlash }
    val maxZoom: StateFlow<Float> = cameraState(1f) { camera.maxZoom }
    val exposureRange: StateFlow<IntRange> = cameraState(0..0) { camera.exposureRange }
    val exposureStepEv: StateFlow<Float> = cameraState(0f) { camera.exposureStepEv }

    private val _events = Channel<CameraEvent>(Channel.BUFFERED)
    val events: Flow<CameraEvent> = _events.receiveAsFlow()

    /** True while the screen is resumed; frames are ignored and the coach is reset otherwise. */
    private val active = MutableStateFlow(false)

    // ------------------------------------------------------------------ internal state

    private val detectorLock = Any()
    @Volatile private var detector: PoseDetector? = null
    private var detectorJob: Job? = null
    private var cleared = false

    @Volatile private var captureInFlight = false
    @Volatile private var userChosePose = false
    @Volatile private var sceneLockRequested = false
    @Volatile private var latestScore = 0f
    @Volatile private var lastDetectionAt = Long.MIN_VALUE
    @Volatile private var sceneCache: SceneCache? = null

    /**
     * Android 9 and below without storage permission: photos can't be saved, so the automatic paths (auto-capture,
     * hand raise) stay off instead of opening the permission dialog every few seconds. Refreshed on every resume
     * (the permission dialog pauses and resumes the screen, so a grant or denial is always picked up).
     */
    @Volatile private var storageMissing = false

    /** Camera-thread only. */
    private var lastLumaFrameTs = Long.MIN_VALUE

    // Pipeline-confined.
    private var manualCooldownUntil = 0L
    private var lastCueKey: Pair<Int, Int>? = null
    private var lastSpokenAt = Long.MIN_VALUE
    private var sessionShot = 1
    private var sessionTotal = 1
    private var lastLightingIssue: LightingIssue? = null
    private var lastDetectorErrorAt = Long.MIN_VALUE

    private var imageJob: Job? = null

    init {
        scope.launch { resolveInitialPose() }
        scope.launch { loadLastPhoto() }
        observeDetectorConfig()
        observeActive()
        observeLevel()
        observeVoiceSetting()
        observeCameraError()
    }

    // ------------------------------------------------------------------ screen lifecycle

    /** Called by the screen: true while RESUMED. Detection, coaching and the level pause otherwise. */
    fun setActive(value: Boolean) {
        if (value) storageMissing = safely { appContext.needsLegacyStoragePermission() } ?: false
        if (active.value == value) return
        active.value = value
        if (!value) {
            sceneLockRequested = false
            safely { voice.stop() }
        }
    }

    /**
     * Frame callback for [PoseCamera.bind]; runs on the camera's background thread. Never keeps [bitmap].
     * @param aspect the aspect of the binding that produced the frame (NOT the current UI aspect: after an aspect
     *   switch the old binding keeps delivering frames cropped to the old viewport until the camera is rebound)
     */
    fun onFrame(bitmap: Bitmap, timestampMs: Long, aspect: SceneAspect) {
        if (!active.value) return
        try {
            // Under the lock so a detector is never fed while (or after) it is being closed.
            synchronized(detectorLock) { detector?.detectLive(bitmap, timestampMs, aspect.ratio) }
            if (sceneLockRequested && aspect == _ui.value.aspect) {
                sceneLockRequested = false
                bitmap.copy(Bitmap.Config.ARGB_8888, false)?.let { copy ->
                    _ui.update { it.copy(sceneLock = copy.asImageBitmap()) }
                }
            }
            val due = lastLumaFrameTs == Long.MIN_VALUE ||
                timestampMs - lastLumaFrameTs >= LUMA_INTERVAL_MS ||
                timestampMs < lastLumaFrameTs
            if (due) {
                lastLumaFrameTs = timestampMs
                FrameAnalysis.buildLumaGrid(bitmap)?.let { grid -> scope.launch(pipeline) { analyzeLighting(grid) } }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Frame handling failed", e)
        }
    }

    /** Re-applies flash / zoom / exposure after (re)binding the camera. */
    fun onCameraBound() {
        val st = _ui.value
        safely { camera.setFlash(st.flash) }
        if (st.zoom > 1f) safely { camera.setZoom(st.zoom.coerceAtMost(maxZoom.value.coerceAtLeast(1f))) }
        if (st.exposureIndex != 0) safely { camera.setExposure(st.exposureIndex) }
    }

    /**
     * A synchronous bind failure. The reason itself is shown by the camera-error notice (PoseCamera reports it
     * through [PoseCamera.error] for every failure, deferred ones included); here we only fall back to the back lens.
     */
    fun onCameraError(error: Throwable) {
        Log.e(TAG, "Camera bind failed", error)
        val st = _ui.value
        if (st.lens == Lens.FRONT) {
            _ui.update { it.copy(lens = Lens.BACK) }
            _events.trySend(CameraEvent.Message("The front camera isn't available"))
        }
    }

    // ------------------------------------------------------------------ pose selection

    fun selectPose(pose: PoseTemplate?) {
        userChosePose = true
        if (pose === _ui.value.pose && _ui.value.poseLoaded) return
        applyPose(pose, markUsed = true)
        _ui.update { it.copy(poseLoaded = true) }
    }

    fun toggleFavorite(id: String) {
        scope.launch { safely { poseRepository.toggleFavorite(id) } }
    }

    /**
     * Returns the template figures in SCENE space for the given overlay state, cached per
     * (pose, transform, mirror, aspect). Safe to call from any thread (used by the pipeline and the overlay).
     */
    fun templateScene(pose: PoseTemplate, transform: OverlayTransform, mirrored: Boolean, aspect: SceneAspect): List<Skeleton> {
        sceneCache?.let { c ->
            if (c.pose === pose && c.transform == transform && c.mirrored == mirrored && c.aspect == aspect) return c.figures
        }
        val figures = safely { SceneMapping.templateToScene(pose, aspect.ratio, transform, mirrored) } ?: emptyList()
        sceneCache = SceneCache(pose, transform, mirrored, aspect, figures)
        return figures
    }

    // ------------------------------------------------------------------ camera controls

    fun flipLens() {
        _ui.update { it.copy(lens = if (it.lens == Lens.BACK) Lens.FRONT else Lens.BACK, zoom = 1f, exposureIndex = 0) }
        clearLive()
    }

    fun setAspect(aspect: SceneAspect) {
        if (_ui.value.aspect == aspect) return
        _ui.update { it.copy(aspect = aspect) }
        clearLive()
    }

    fun cycleFlash() {
        val next = FlashMode.entries[(_ui.value.flash.ordinal + 1) % FlashMode.entries.size]
        _ui.update { it.copy(flash = next) }
        safely { camera.setFlash(next) }
    }

    fun cycleGrid() = updateSettings { it.copy(grid = GridType.entries[(it.grid.ordinal + 1) % GridType.entries.size]) }

    fun setZoom(ratio: Float) {
        if (!ratio.isFinite()) return
        val r = ratio.coerceIn(1f, maxZoom.value.coerceAtLeast(1f))
        if (abs(r - _ui.value.zoom) < 0.005f) return
        _ui.update { it.copy(zoom = r) }
        safely { camera.setZoom(r) }
    }

    /**
     * Pinch step. Applied without [setZoom]'s small-change threshold: a slow pinch delivers many tiny factors that
     * must add up instead of each being discarded.
     */
    fun zoomBy(factor: Float) {
        if (!factor.isFinite() || factor <= 0f) return
        val current = _ui.value.zoom
        val target = (current * factor).coerceIn(1f, maxZoom.value.coerceAtLeast(1f))
        if (target == current) return
        _ui.update { it.copy(zoom = target) }
        safely { camera.setZoom(target) }
    }

    fun setExposure(index: Int) {
        val range = exposureRange.value
        if (range.isEmpty()) return
        val i = index.coerceIn(range.first, range.last)
        if (i == _ui.value.exposureIndex) return
        _ui.update { it.copy(exposureIndex = i) }
        safely { camera.setExposure(i) }
    }

    fun setTimer(option: TimerOption) = _ui.update { it.copy(timer = option) }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        scope.launch { safely { settingsRepository.update(transform) } }
    }

    fun setAudienceMode(mode: AudienceMode) = _ui.update { it.copy(audienceMode = mode) }

    fun setHandRaiseEnabled(enabled: Boolean) = _ui.update { it.copy(handRaiseEnabled = enabled) }

    // ------------------------------------------------------------------ overlay controls

    /** Applies a gesture step unless the overlay is locked. */
    fun updateTransform(change: (OverlayTransform) -> OverlayTransform) {
        if (_ui.value.overlayLocked) return
        _transform.update(change)
    }

    fun resetTransform() {
        if (!_ui.value.overlayLocked) _transform.value = OverlayTransform.IDENTITY
    }

    fun toggleOverlayLock() = _ui.update { it.copy(overlayLocked = !it.overlayLocked) }

    fun togglePoseMirror() = _ui.update { it.copy(poseMirrored = !it.poseMirrored) }

    fun setOverlayMode(mode: OverlayMode) {
        _ui.update { if (mode in it.availableOverlayModes) it.copy(overlayMode = mode) else it }
    }

    /** Background match: snapshot the next analysis frame, or clear the snapshot. */
    fun toggleSceneLock() {
        if (_ui.value.sceneLock != null) {
            sceneLockRequested = false
            _ui.update { it.copy(sceneLock = null) }
        } else {
            sceneLockRequested = true
        }
    }

    // ------------------------------------------------------------------ capture

    /** Shutter button / volume key: stops a running session, else starts the timer or takes a photo now. */
    fun onShutterPressed() {
        scope.launch(pipeline) {
            if (captureInFlight) return@launch
            val now = SystemClock.uptimeMillis()
            if (safely { coach.isSessionActive } == true) {
                safely { coach.cancel() }
                safely { voice.stop() }
                tick(now)
                return@launch
            }
            val state = safely { coach.state }
            if (state is CaptureState.Countdown || state is CaptureState.Holding) safely { coach.cancel() }
            when (val timer = _ui.value.timer) {
                TimerOption.Off -> {
                    manualCooldownUntil = now + MANUAL_COOLDOWN_MS
                    _capture.value = CaptureState.Capture
                    beginCapture(viaCoach = false)
                }
                else -> {
                    startTimer(timer, now)
                    tick(now)
                }
            }
        }
    }

    /** Stop button of a running timer / photoshoot. */
    fun stopSession() {
        scope.launch(pipeline) {
            safely { coach.cancel() }
            safely { voice.stop() }
            tick(SystemClock.uptimeMillis())
        }
    }

    // ------------------------------------------------------------------ cleanup

    override fun onCleared() {
        sceneLockRequested = false
        synchronized(detectorLock) {
            cleared = true
            detectorJob?.cancel()
            detectorJob = null
            detector?.let { safely { it.close() } }
            detector = null
        }
        safely { voice.close() }
        super.onCleared()
    }

    // ================================================================== implementation

    private suspend fun resolveInitialPose() {
        val requested = initialPoseId?.let { findPose(it, waitMillis = 1500L) }
        val pose = requested
            ?: recentPose()
            ?: safely { poseRepository.builtIn.firstOrNull() }
        if (userChosePose) return // the user picked something while we were still looking
        if (initialPoseId != null && requested == null) {
            _events.trySend(CameraEvent.Message("That pose isn't available, showing another one"))
        }
        applyPose(pose, markUsed = pose != null)
        _ui.update { it.copy(poseLoaded = true) }
    }

    /** Built-ins resolve at once; custom poses are loaded from disk asynchronously, so wait a little for them. */
    private suspend fun findPose(id: String, waitMillis: Long): PoseTemplate? =
        safely { poseRepository.pose(id) }
            ?: safely { withTimeoutOrNull(waitMillis) { poseRepository.poseFlow(id).filterNotNull().first() } }

    private suspend fun recentPose(): PoseTemplate? {
        val ids = safely { withTimeoutOrNull(1500L) { poseRepository.recent.first() } }.orEmpty()
        val first = ids.firstOrNull() ?: return null
        return findPose(first, waitMillis = 800L) ?: ids.drop(1).firstNotNullOfOrNull { safely { poseRepository.pose(it) } }
    }

    private suspend fun loadLastPhoto() {
        val latest = safely { photoRepository.list().firstOrNull()?.uri } ?: return
        _ui.update { if (it.lastPhoto == null) it.copy(lastPhoto = latest) else it }
    }

    private fun applyPose(pose: PoseTemplate?, markUsed: Boolean) {
        val raisesHand = pose?.let { safely { HandRaiseDetector.templateRaisesHand(it) } } ?: false
        _ui.update {
            it.copy(
                pose = pose,
                poseMirrored = false,
                overlayMode = OverlayMode.MANNEQUIN,
                referenceImage = null,
                outlineImage = null,
                templateRaisesHand = raisesHand,
            )
        }
        if (!_ui.value.overlayLocked) _transform.value = OverlayTransform.IDENTITY
        latestScore = 0f
        _live.update { it.copy(match = MatchResult.EMPTY) }
        _coaching.update { it.copy(hint = null) }
        scope.launch(pipeline) {
            safely { coach.reset() }
            hintStabilizer.reset()
            handRaise.reset()
            lastCueKey = null
            if (!captureInFlight) _capture.value = CaptureState.Idle
            _session.value = null
        }
        loadPoseImages(pose)
        if (pose != null && markUsed) scope.launch { safely { poseRepository.markUsed(pose.id) } }
    }

    private fun loadPoseImages(pose: PoseTemplate?) {
        imageJob?.cancel()
        imageJob = null
        if (pose == null || (pose.referenceImage == null && pose.outlineImage == null)) return
        imageJob = scope.launch {
            val (reference, outline) = withContext(Dispatchers.IO) {
                decodeGuideImage(pose.referenceImage) to decodeGuideImage(pose.outlineImage)
            }
            _ui.update { st ->
                if (st.pose !== pose) return@update st
                val mode = when {
                    st.overlayMode != OverlayMode.MANNEQUIN -> st.overlayMode
                    outline != null -> OverlayMode.PHOTO_OUTLINE
                    reference != null -> OverlayMode.PHOTO_GHOST
                    else -> OverlayMode.MANNEQUIN
                }
                st.copy(referenceImage = reference, outlineImage = outline, overlayMode = mode)
            }
        }
    }

    /** Decodes a guide image (reference photo or outline PNG), downsampled and upright. Null if missing/broken. */
    private fun decodeGuideImage(path: String?): ImageBitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.isFile || !file.canRead()) {
            Log.w(TAG, "Guide image missing: $path")
            return null
        }
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_GUIDE_IMAGE_PX) sample *= 2
            val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
            uprightByExif(path, decoded).asImageBitmap()
        } catch (e: Exception) {
            Log.w(TAG, "Could not decode guide image $path", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Guide image too large: $path", e)
            null
        }
    }

    private fun uprightByExif(path: String, bitmap: Bitmap): Bitmap {
        val exif = safely { ExifInterface(path) } ?: return bitmap
        val degrees = exif.rotationDegrees
        val flipped = exif.isFlipped
        if (degrees == 0 && !flipped) return bitmap
        val matrix = Matrix().apply {
            if (flipped) postScale(-1f, 1f)
            if (degrees != 0) postRotate(degrees.toFloat())
        }
        return safely { Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true) } ?: bitmap
    }

    private fun clearLive() {
        latestScore = 0f
        _live.value = LiveFrame.EMPTY
        scope.launch(pipeline) { safely { smoother.reset() } }
    }

    // ------------------------------------------------------------------ detector lifecycle

    private fun observeDetectorConfig() {
        scope.launch {
            combine(
                settingsRepository.settings.map { it.accurateModel }.catch { emit(false) },
                _ui.filter { it.poseLoaded }.map { (it.pose?.personCount ?: 1).coerceIn(1, MAX_POSES) },
            ) { accurate, maxPoses -> accurate to maxPoses }
                .distinctUntilChanged()
                .collectLatest { (accurate, maxPoses) -> recreateDetector(accurate, maxPoses) }
        }
    }

    /** Creating the landmarker takes 100-500 ms: done off the main thread, old instance closed first. */
    private suspend fun recreateDetector(accurate: Boolean, maxPoses: Int) = withContext(Dispatchers.Default) {
        releaseDetector()
        _ui.update { it.copy(detectorReady = false) }
        val created = try {
            PoseDetector(appContext, accurate, maxPoses)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "Pose detector could not be created", e)
            _ui.update { it.copy(detectorError = "Pose detection is unavailable. The camera still works.") }
            _events.trySend(CameraEvent.Message("Pose detection couldn't start – the camera still works"))
            return@withContext
        }
        if (!isActive || !installDetector(created)) safely { created.close() }
    }

    private fun installDetector(created: PoseDetector): Boolean {
        synchronized(detectorLock) {
            if (cleared) return false
            detector = created
            detectorJob = scope.launch(pipeline) {
                launch {
                    try {
                        created.results.collect { onDetection(it) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        Log.e(TAG, "Detection stream failed", e)
                    }
                }
                launch {
                    try {
                        created.errors.collect { onDetectorError(it) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        Log.e(TAG, "Detector error stream failed", e)
                    }
                }
            }
        }
        _ui.update { it.copy(detectorReady = true, detectorError = null) }
        scope.launch(pipeline) { safely { smoother.reset() } }
        return true
    }

    private fun releaseDetector() {
        synchronized(detectorLock) {
            detectorJob?.cancel()
            detectorJob = null
            detector?.let { safely { it.close() } }
            detector = null
        }
    }

    private fun onDetectorError(message: String) {
        Log.w(TAG, "Detector error: $message")
        val now = SystemClock.uptimeMillis()
        if (lastDetectorErrorAt != Long.MIN_VALUE && now - lastDetectorErrorAt < DETECTOR_ERROR_GAP_MS) return
        lastDetectorErrorAt = now
        _events.trySend(CameraEvent.Message("Pose detection hiccup – still trying"))
    }

    // ------------------------------------------------------------------ pipeline

    private fun onDetection(result: DetectionResult) {
        if (!active.value) return
        try {
            val now = SystemClock.uptimeMillis()
            val st = _ui.value
            // A frame from the previous binding (or in flight across an aspect switch) is in another scene space.
            if (abs(result.sceneAspect - st.aspect.ratio) > 1e-3f) return
            val s = settings.value
            val people = safely { smoother.smooth(result.people, result.frameTimestampMs) } ?: result.people
            val pose = st.pose?.takeIf { st.hasPose }
            val match = if (pose != null) matchPose(pose, people, st, s) else MatchResult.EMPTY
            latestScore = match.score
            lastDetectionAt = now
            _live.value = LiveFrame(people, people.map { it.visibleOnly() }, match, st.aspect)
            updateHint(if (pose != null) match.hints.firstOrNull() else null, now, s)
            checkHandRaise(people, st, now)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "Frame processing failed", e)
        }
    }

    private fun matchPose(pose: PoseTemplate, people: List<Skeleton>, st: CameraUiState, s: AppSettings): MatchResult {
        val scene = templateScene(pose, _transform.value, st.poseMirrored, st.aspect)
        if (scene.isEmpty()) return MatchResult.EMPTY
        // The lens actually bound: PoseCamera falls back to the other lens when the requested one doesn't exist.
        val lens = camera.lens.value
        return safely {
            PoseMatcher.match(
                templateFigures = scene,
                detected = people,
                sceneAspect = st.aspect.ratio,
                audience = st.audienceMode.resolve(lens),
                config = MatchConfig(perfectScore = s.matchThreshold),
                mirroredView = lens == Lens.FRONT,
            )
        } ?: MatchResult.EMPTY
    }

    private fun updateHint(top: CoachHint?, now: Long, s: AppSettings) {
        val shown = hintStabilizer.update(top, now)
        val current = _coaching.value.hint
        if (current?.text != shown?.text || current?.kind != shown?.kind) _coaching.update { it.copy(hint = shown) }
        speakHint(shown, now, s)
    }

    /** Speaks the displayed hint; VoiceCoach itself rate-limits repeats. Silent during countdowns. */
    private fun speakHint(hint: CoachHint?, now: Long, s: AppSettings) {
        if (hint == null || !s.voiceGuidance || captureInFlight) return
        val state = _capture.value
        if (state is CaptureState.Countdown || state is CaptureState.Capture) return
        if (lastSpokenAt != Long.MIN_VALUE && now - lastSpokenAt < SPEAK_POLL_MS) return
        lastSpokenAt = now
        safely { voice.say(if (hint.kind == HintKind.PERFECT) PERFECT_SPEECH else hint.speech) }
    }

    private fun checkHandRaise(people: List<Skeleton>, st: CameraUiState, now: Long) {
        if (!st.handRaiseActive) {
            handRaise.reset()
            return
        }
        val idle = !captureInFlight && !storageMissing &&
            (safely { coach.state } ?: CaptureState.Idle) == CaptureState.Idle &&
            safely { coach.isSessionActive } != true
        if (handRaise.update(people, st.aspect.ratio, now, allowTrigger = idle)) {
            startTimer(st.timer, now)
            if (settings.value.haptics) safely { voice.haptic() }
            tick(now)
        }
    }

    private fun startTimer(option: TimerOption, now: Long) {
        safely {
            when (option) {
                TimerOption.Off -> coach.startTimer(TimerOption.HAND_RAISE_DEFAULT_SECONDS, now)
                is TimerOption.Delay -> coach.startTimer(option.seconds, now)
                is TimerOption.Photoshoot -> coach.startTimer(
                    TimerOption.PHOTOSHOOT_START_DELAY_SECONDS,
                    now,
                    shots = option.shots,
                    intervalSeconds = option.intervalSeconds,
                )
            }
        }
    }

    private fun observeActive() {
        scope.launch(pipeline) {
            active.collectLatest { isActive ->
                if (isActive) {
                    while (currentCoroutineContext().isActive) {
                        tick(SystemClock.uptimeMillis())
                        delay(TICK_MS)
                    }
                } else {
                    resetPipeline()
                }
            }
        }
    }

    /** One step of the capture state machine (pipeline thread). */
    private fun tick(now: Long) {
        if (captureInFlight) return
        val st = _ui.value
        val s = settings.value
        val desired = CoachSettings(
            autoCapture = s.autoCapture && st.hasPose && st.detectorReady && !storageMissing,
            threshold = s.matchThreshold,
            countdownSeconds = s.countdownSeconds,
        )
        if (coach.settings != desired) coach.settings = desired
        val fresh = lastDetectionAt != Long.MIN_VALUE && now - lastDetectionAt <= STALE_DETECTION_MS
        val score = if (fresh && st.hasPose && now >= manualCooldownUntil) latestScore else 0f
        val state = safely { coach.update(score, now) } ?: return
        _capture.value = state
        updateSession(state)
        if (state == CaptureState.Capture) {
            beginCapture(viaCoach = true)
            return
        }
        cueCountdown(state, s)
    }

    private fun updateSession(state: CaptureState) {
        val running = safely { coach.isSessionActive } == true
        if (state is CaptureState.Countdown && state.manual) {
            sessionShot = state.shot
            sessionTotal = state.totalShots
        }
        _session.value = if (running) SessionInfo(sessionShot, sessionTotal) else null
        if (!running) {
            sessionShot = 1
            sessionTotal = 1
        }
    }

    /** Spoken "3, 2, 1" for the first shot (VoiceCoach drops repeats, so later photoshoot shots beep). */
    private fun cueCountdown(state: CaptureState, s: AppSettings) {
        if (state !is CaptureState.Countdown || state.secondsLeft <= 0) {
            lastCueKey = null
            return
        }
        val key = state.shot to state.secondsLeft
        if (key == lastCueKey) return
        lastCueKey = key
        when {
            s.voiceGuidance && state.secondsLeft <= 5 && state.shot <= 1 -> safely { voice.say(state.secondsLeft.toString(), urgent = true) }
            s.voiceGuidance || s.shutterSound -> safely { voice.tick() }
        }
    }

    private fun resetPipeline() {
        safely { coach.reset() }
        hintStabilizer.reset()
        handRaise.reset()
        safely { smoother.reset() }
        lastCueKey = null
        latestScore = 0f
        lastDetectionAt = Long.MIN_VALUE
        lastLightingIssue = null
        if (!captureInFlight) _capture.value = CaptureState.Idle
        _session.value = null
        _live.value = LiveFrame.EMPTY
        _coaching.value = CoachingState()
    }

    /** Called on the pipeline thread; the photo itself is taken on the main thread. */
    private fun beginCapture(viaCoach: Boolean) {
        captureInFlight = true
        lastCueKey = null
        scope.launch {
            var askedForStorage = false
            try {
                if (appContext.needsLegacyStoragePermission()) {
                    askedForStorage = true
                    storageMissing = true
                    _events.trySend(CameraEvent.RequestStoragePermission)
                } else {
                    takePhotoNow()
                }
            } finally {
                withContext(NonCancellable + pipeline) {
                    if (viaCoach) {
                        // No photo was taken: end the timer / photoshoot instead of re-arming it into another prompt.
                        if (askedForStorage) safely { coach.cancel() } else safely { coach.onCaptured(SystemClock.uptimeMillis()) }
                    }
                    captureInFlight = false
                    _capture.value = safely { coach.state } ?: CaptureState.Idle
                }
            }
        }
    }

    private suspend fun takePhotoNow() {
        val s = settings.value
        _ui.update { it.copy(capturing = true) }
        try {
            _events.trySend(CameraEvent.Flash)
            if (s.shutterSound) safely { voice.shutter() }
            if (s.haptics) safely { voice.haptic() }
            // The lens actually bound (PoseCamera may have fallen back to the other one), not the requested one.
            val uri = camera.takePhoto(mirror = camera.lens.value == Lens.FRONT && s.mirrorFrontPhotos)
            _ui.update { it.copy(lastPhoto = uri) }
            _events.trySend(CameraEvent.PhotoSaved(uri))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "Capture failed", e)
            _events.trySend(CameraEvent.Message("Couldn't save the photo. Please try again."))
        } finally {
            _ui.update { it.copy(capturing = false) }
        }
    }

    // ------------------------------------------------------------------ lighting, level, voice

    /** Pipeline thread. A lighting issue must be seen twice in a row before it is shown or cleared. */
    private fun analyzeLighting(grid: LumaGrid) {
        if (!active.value) return
        val st = _ui.value
        val frame = _live.value
        val face = frame.people.firstOrNull()
            ?.takeIf { frame.aspect == st.aspect }
            ?.let { FrameAnalysis.faceBox(it, st.aspect.ratio) }
        val result = safely { LightingAnalyzer.analyze(grid, face, st.audienceMode.resolve(camera.lens.value)) } ?: return
        val issue = result.issue
        if (issue == lastLightingIssue) {
            val hint = result.hint.takeIf { issue != LightingIssue.OK }
            if (_coaching.value.lighting?.text != hint?.text) _coaching.update { it.copy(lighting = hint) }
        }
        lastLightingIssue = issue
    }

    private fun observeLevel() {
        scope.launch {
            combine(active, settings.map { it.showLevel }) { on, show -> on && show }
                .distinctUntilChanged()
                .collectLatest { on ->
                    _tilt.value = null
                    if (!on) return@collectLatest
                    try {
                        levelSensor.tilt.collect { _tilt.value = it }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        Log.w(TAG, "Level sensor unavailable", e)
                    }
                }
        }
    }

    /** Mirrors [PoseCamera.error] (deferred bind failures, camera in use / disabled...) into the UI state. */
    private fun observeCameraError() {
        val errors = safely { camera.error } ?: return
        scope.launch { errors.collect { msg -> _ui.update { if (it.cameraError == msg) it else it.copy(cameraError = msg) } } }
    }

    private fun observeVoiceSetting() {
        scope.launch {
            settings.map { it.voiceGuidance }.distinctUntilChanged().collect { enabled ->
                safely { voice.enabled = enabled }
                if (!enabled) safely { voice.stop() }
            }
        }
    }

    private fun <T> cameraState(default: T, source: () -> StateFlow<T>): StateFlow<T> =
        safely { source() } ?: MutableStateFlow(default).asStateFlow()

    private fun Skeleton.visibleOnly(): Skeleton {
        val visible = points.filterKeys { isVisible(it, LIVE_MIN_VISIBILITY) }
        return if (visible.size == points.size) this else copy(points = visible)
    }

    private class SceneCache(
        val pose: PoseTemplate,
        val transform: OverlayTransform,
        val mirrored: Boolean,
        val aspect: SceneAspect,
        val figures: List<Skeleton>,
    )

    companion object {
        private const val TAG = "CameraViewModel"
        private const val MAX_POSES = 4
        private const val TICK_MS = 33L
        private const val STALE_DETECTION_MS = 600L
        private const val LUMA_INTERVAL_MS = 1000L
        private const val MANUAL_COOLDOWN_MS = 2500L
        private const val SPEAK_POLL_MS = 500L
        private const val DETECTOR_ERROR_GAP_MS = 15_000L
        private const val MAX_GUIDE_IMAGE_PX = 1280
        private const val LIVE_MIN_VISIBILITY = 0.5f
        const val PERFECT_SPEECH = "Perfect, hold it"

        fun factory(context: Context, initialPoseId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.appContainer
                CameraViewModel(
                    appContext = context.applicationContext,
                    settingsRepository = container.settingsRepository,
                    poseRepository = container.poseRepository,
                    photoRepository = container.photoRepository,
                    initialPoseId = initialPoseId,
                )
            }
        }
    }
}
