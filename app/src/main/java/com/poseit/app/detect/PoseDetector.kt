package com.poseit.app.detect

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.detect.LandmarkMapping
import com.poseit.app.pose.detect.RawLandmark
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * @param people detected people in SCENE space (see com.poseit.app.pose.scene.SceneMapping docs), at most maxPoses
 * @param frameTimestampMs timestamp passed to [PoseDetector.detectLive]
 * @param inferenceMs how long MediaPipe took
 * @param sceneAspect the scene aspect passed to [PoseDetector.detectLive] for this frame (the space [people] are in)
 */
data class DetectionResult(
    val people: List<Skeleton>,
    val frameTimestampMs: Long,
    val inferenceMs: Long,
    val sceneAspect: Float,
)

/**
 * Wrapper around MediaPipe Pose Landmarker (models in assets/models/pose_landmarker_{lite,full}.task).
 *
 * - Live mode: feed upright, NON-mirrored camera frames with [detectLive]; results arrive asynchronously on
 *   [results]. Frames arriving while the previous one is still being processed may be dropped.
 * - Still mode: [detectStill] for photos picked from the gallery (blocking; call off the main thread).
 *
 * Creating the landmarker is slow (~100-500 ms); construct off the main thread. Falls back from GPU to CPU
 * delegate if GPU init fails, and rebuilds the live landmarker (on the CPU) if it stops producing results.
 * Never throws from detect calls – errors are logged and reported via [errors].
 * The constructor throws if no landmarker can be created at all (e.g. the model asset is missing).
 *
 * Landmarker setup adapted from the MediaPipe Pose Landmarker Android sample (PoseLandmarkerHelper.kt,
 * Copyright 2023 The TensorFlow Authors, Apache License 2.0), without its front-camera mirroring: frames are fed
 * non-mirrored so MediaPipe's LEFT/RIGHT stay anatomical.
 *
 * @param accurate true = full model, false = lite model
 * @param maxPoses maximum number of people to detect (2 for couples, 3 for groups)
 * @param preferGpu try the GPU delegate first for live detection (falls back to CPU automatically)
 * @param liveStream build the live-stream landmarker up front; false for still-photo-only use ([detectLive] is then
 *   a no-op and nothing is loaded until the first [detectStill])
 */
class PoseDetector(
    context: Context,
    val accurate: Boolean,
    val maxPoses: Int = 2,
    preferGpu: Boolean = true,
    liveStream: Boolean = true,
) : Closeable {

    private val appContext: Context = context.applicationContext ?: context
    private val numPoses = maxPoses.coerceIn(1, MAX_POSES)
    private val modelPath = if (accurate) MODEL_FULL else MODEL_LITE

    private val _results = MutableSharedFlow<DetectionResult>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val results: SharedFlow<DetectionResult> = _results.asSharedFlow()

    private val _errors = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val closed = AtomicBoolean(false)

    // Live landmarker; guarded by liveLock. The result/error listeners never take the lock: closing a landmarker
    // waits for MediaPipe's graph thread, which may be inside a listener at that moment.
    private val liveLock = Any()
    private var live: PoseLandmarker? = null
    @Volatile private var liveDelegate = Delegate.CPU

    /** Set by the error listener when the GPU graph fails before producing anything; handled on the next frame. */
    @Volatile private var gpuRuntimeFailure = false
    @Volatile private var hadLiveResult = false

    /**
     * Set when the live graph has stopped producing output (see [STALLED_SENDS]); handled on the next frame.
     * MediaPipe doesn't report a graph that died while running (e.g. a GPU delegate failure or reset): its error
     * listener only sees result-conversion errors, and detectAsync just logs and drops every later frame.
     */
    @Volatile private var liveStalled = false

    /** Consecutive frames sent because the previous result never arrived; reset by any result or error. */
    private val staleSends = AtomicInteger(0)

    // Still landmarker (IMAGE mode, CPU), created on first use; guarded by stillLock.
    private val stillLock = Any()
    private var still: PoseLandmarker? = null

    // Frame bookkeeping (camera thread + MediaPipe callback thread).
    private val inFlight = AtomicBoolean(false)
    @Volatile private var inFlightSince = 0L
    @Volatile private var lastSentTimestamp = Long.MIN_VALUE
    private val pendingFrames = ConcurrentHashMap<Long, PendingFrame>()
    private val lastErrorReportAt = AtomicLong(Long.MIN_VALUE)

    private class PendingFrame(val width: Int, val height: Int, val sceneAspect: Float, val sentAtMs: Long)

    init {
        if (liveStream) live = createLiveLandmarker(if (preferGpu) Delegate.GPU else Delegate.CPU)
    }

    /**
     * @param uprightBitmap ARGB_8888 frame already rotated upright, NOT mirrored
     * @param timestampMs monotonically increasing frame time
     * @param sceneAspect aspect (w/h) of the visible scene; landmarks are mapped with LandmarkMapping.toScene
     */
    fun detectLive(uprightBitmap: Bitmap, timestampMs: Long, sceneAspect: Float) {
        if (closed.get() || uprightBitmap.isRecycled) return
        val width = uprightBitmap.width
        val height = uprightBitmap.height
        if (width <= 0 || height <= 0) return
        // MediaPipe's live stream requires strictly increasing timestamps.
        if (timestampMs <= lastSentTimestamp) return
        val now = SystemClock.uptimeMillis()
        if (!inFlight.compareAndSet(false, true)) {
            // Busy with the previous frame. If its result never arrived, stop waiting for it after a while.
            if (now - inFlightSince < STALE_FRAME_MS) return
            pendingFrames.clear()
            if (staleSends.incrementAndGet() >= STALLED_SENDS) liveStalled = true
        }
        val aspect = if (sceneAspect.isFinite() && sceneAspect > 0f) sceneAspect else width.toFloat() / height
        var input: Bitmap? = null
        try {
            synchronized(liveLock) {
                if (closed.get()) {
                    inFlight.set(false)
                    return
                }
                if (gpuRuntimeFailure || liveStalled) rebuildLiveLocked()
                val landmarker = live
                if (landmarker == null) {
                    inFlight.set(false)
                    return
                }
                val argb = argbCopyIfNeeded(uprightBitmap)
                input = argb
                inFlightSince = now
                lastSentTimestamp = timestampMs
                pendingFrames[timestampMs] = PendingFrame(width, height, aspect, now)
                // MediaPipe copies the pixels into its own buffer before detectAsync returns, so the caller may
                // reuse the bitmap right after this call.
                landmarker.detectAsync(BitmapImageBuilder(argb).build(), timestampMs)
            }
        } catch (e: Exception) {
            pendingFrames.remove(timestampMs)
            inFlight.set(false)
            report("Live pose detection failed", e)
        } finally {
            input?.let { if (it !== uprightBitmap) it.recycle() }
        }
    }

    /**
     * Returns people in IMAGE-normalized coordinates (sceneAspect = bitmap aspect). Blocking.
     * Null when the still landmarker can't be created at all (missing model, unsupported device), so callers can
     * tell "pose detection unavailable" from "nobody in the photo".
     */
    fun detectStill(bitmap: Bitmap): List<Skeleton>? {
        if (closed.get() || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return emptyList()
        synchronized(stillLock) {
            if (closed.get()) return emptyList()
            val landmarker = still ?: createStillLandmarker()?.also { still = it } ?: return null
            var input: Bitmap? = null
            return try {
                val prepared = prepareStillInput(bitmap)
                input = prepared
                val result = landmarker.detect(BitmapImageBuilder(prepared).build())
                toSkeletons(result, prepared.width, prepared.height, prepared.width.toFloat() / prepared.height)
            } catch (e: Exception) {
                report("Pose detection on the photo failed", e)
                emptyList()
            } catch (e: OutOfMemoryError) {
                report("The photo is too large to analyse", e)
                emptyList()
            } finally {
                input?.let { if (it !== bitmap) it.recycle() }
            }
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        synchronized(liveLock) {
            live?.closeQuietly()
            live = null
        }
        synchronized(stillLock) {
            still?.closeQuietly()
            still = null
        }
        pendingFrames.clear()
        inFlight.set(false)
    }

    // ------------------------------------------------------------------ live results

    /**
     * @param input a fresh bitmap MediaPipe creates for every result (never the frame we passed in); closed here so
     *   its ~1 MB of pixels is freed at once instead of waiting for the garbage collector
     */
    private fun onLiveResult(result: PoseLandmarkerResult, input: MPImage) {
        try {
            staleSends.set(0)
            val timestamp = result.timestampMs()
            val frame = pendingFrames.remove(timestamp)
            if (pendingFrames.isNotEmpty()) pendingFrames.keys.removeAll { it < timestamp }
            if (timestamp >= lastSentTimestamp) inFlight.set(false)
            if (closed.get() || frame == null) return
            hadLiveResult = true
            val people = toSkeletons(result, frame.width, frame.height, frame.sceneAspect)
            val inferenceMs = SystemClock.uptimeMillis() - frame.sentAtMs
            _results.tryEmit(DetectionResult(people, timestamp, inferenceMs, frame.sceneAspect))
        } catch (e: Throwable) {
            // Never let an exception escape into MediaPipe's native callback thread.
            inFlight.set(false)
            if (!closed.get()) report("Couldn't read the pose detection result", e)
        } finally {
            try {
                input.close()
            } catch (e: Throwable) {
                Log.w(TAG, "Couldn't release the result image", e)
            }
        }
    }

    private fun onLiveError(error: RuntimeException) {
        // The graph is still producing output (this listener only sees result-conversion errors), so it isn't stalled.
        staleSends.set(0)
        pendingFrames.clear()
        inFlight.set(false)
        if (closed.get()) return
        if (liveDelegate == Delegate.GPU && !hadLiveResult) gpuRuntimeFailure = true
        report("Live pose detection failed", error)
    }

    // ------------------------------------------------------------------ landmarkers

    /** Creates the live landmarker, falling back from GPU to CPU. Throws if neither works. */
    private fun createLiveLandmarker(preferred: Delegate): PoseLandmarker {
        if (preferred == Delegate.GPU) {
            try {
                return buildLandmarker(RunningMode.LIVE_STREAM, Delegate.GPU).also { liveDelegate = Delegate.GPU }
            } catch (e: Exception) {
                Log.w(TAG, "GPU delegate unavailable, using CPU", e)
            } catch (e: LinkageError) {
                Log.w(TAG, "GPU delegate unavailable, using CPU", e)
            }
        }
        try {
            return buildLandmarker(RunningMode.LIVE_STREAM, Delegate.CPU).also { liveDelegate = Delegate.CPU }
        } catch (e: Exception) {
            throw IllegalStateException("Couldn't load the pose model $modelPath", e)
        } catch (e: LinkageError) {
            throw IllegalStateException("Pose detection isn't supported on this device", e)
        }
    }

    /**
     * Rebuilds the live landmarker (under [liveLock], from [detectLive], which then sends its frame to the new one):
     * - the GPU graph failed before producing anything (some drivers): switch to the CPU;
     * - the graph stopped producing output: a GPU graph is rebuilt on the CPU (GPU delegate failure or reset is
     *   the likely cause), a CPU graph on the CPU again.
     * Leaves [inFlight] alone: the calling detectLive owns it.
     */
    private fun rebuildLiveLocked() {
        val stalled = liveStalled
        gpuRuntimeFailure = false
        liveStalled = false
        staleSends.set(0)
        if (!stalled && liveDelegate != Delegate.GPU) return
        Log.w(TAG, if (stalled) "Live pose detection stopped producing results; restarting it" else "GPU pose detection failed at run time; switching to CPU")
        live?.closeQuietly()
        live = null
        pendingFrames.clear()
        // A new graph accepts any timestamp.
        lastSentTimestamp = Long.MIN_VALUE
        live = try {
            buildLandmarker(RunningMode.LIVE_STREAM, Delegate.CPU).also { liveDelegate = Delegate.CPU }
        } catch (e: Exception) {
            report("Couldn't restart pose detection", e)
            return
        } catch (e: LinkageError) {
            report("Couldn't restart pose detection", e)
            return
        }
        if (stalled) report("Pose detection restarted", IllegalStateException("no results for a few seconds"))
    }

    private fun createStillLandmarker(): PoseLandmarker? = try {
        buildLandmarker(RunningMode.IMAGE, Delegate.CPU)
    } catch (e: Exception) {
        report("Couldn't load the pose model", e)
        null
    } catch (e: LinkageError) {
        report("Pose detection isn't supported on this device", e)
        null
    }

    private fun buildLandmarker(mode: RunningMode, delegate: Delegate): PoseLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(modelPath)
            .setDelegate(delegate)
            .build()
        val builder = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(mode)
            .setNumPoses(numPoses)
            .setMinPoseDetectionConfidence(MIN_CONFIDENCE)
            .setMinPosePresenceConfidence(MIN_CONFIDENCE)
            .setMinTrackingConfidence(MIN_CONFIDENCE)
            .setOutputSegmentationMasks(false)
        if (mode == RunningMode.LIVE_STREAM) {
            builder.setResultListener(this::onLiveResult)
            builder.setErrorListener(this::onLiveError)
        }
        return PoseLandmarker.createFromOptions(appContext, builder.build())
    }

    // ------------------------------------------------------------------ helpers

    private fun toSkeletons(result: PoseLandmarkerResult, width: Int, height: Int, sceneAspect: Float): List<Skeleton> {
        val people = result.landmarks()
        if (people.isEmpty()) return emptyList()
        return people.take(numPoses).map { person -> LandmarkMapping.toScene(person.toRaw(), width, height, sceneAspect) }
    }

    private fun List<NormalizedLandmark>.toRaw(): List<RawLandmark> = map { landmark ->
        RawLandmark(
            x = landmark.x(),
            y = landmark.y(),
            visibility = landmark.visibility().orElse(1f),
            presence = landmark.presence().orElse(1f),
        )
    }

    /** MediaPipe only accepts ARGB_8888 bitmaps. */
    private fun argbCopyIfNeeded(bitmap: Bitmap): Bitmap =
        if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)

    /** ARGB_8888 and at most [MAX_STILL_SIDE] px (landmarks are normalized, so scaling doesn't change them). */
    private fun prepareStillInput(bitmap: Bitmap): Bitmap {
        val argb = argbCopyIfNeeded(bitmap)
        val longest = max(argb.width, argb.height)
        if (longest <= MAX_STILL_SIDE) return argb
        val scale = MAX_STILL_SIDE.toFloat() / longest
        val width = (argb.width * scale).roundToInt().coerceAtLeast(1)
        val height = (argb.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(argb, width, height, true)
        if (argb !== bitmap && argb !== scaled) argb.recycle()
        return scaled
    }

    /** Logs and emits an error, at most once per [ERROR_REPORT_INTERVAL_MS] so a failing graph can't flood. */
    private fun report(message: String, error: Throwable) {
        val now = SystemClock.uptimeMillis()
        val last = lastErrorReportAt.get()
        if (last != Long.MIN_VALUE && now - last < ERROR_REPORT_INTERVAL_MS) return
        if (!lastErrorReportAt.compareAndSet(last, now)) return
        Log.e(TAG, message, error)
        val detail = error.message?.takeIf { it.isNotBlank() }
        _errors.tryEmit(if (detail != null) "$message: $detail" else message)
    }

    private fun PoseLandmarker.closeQuietly() {
        try {
            close()
        } catch (e: Exception) {
            Log.w(TAG, "Closing the pose landmarker failed", e)
        }
    }

    private companion object {
        const val TAG = "PoseDetector"
        const val MODEL_FULL = "models/pose_landmarker_full.task"
        const val MODEL_LITE = "models/pose_landmarker_lite.task"
        const val MIN_CONFIDENCE = 0.5f
        const val MAX_POSES = 6

        /** A frame whose result hasn't arrived after this long is considered lost. */
        const val STALE_FRAME_MS = 1_000L

        /** This many lost frames in a row (about 3 s without any result) means the graph has died: rebuild it. */
        const val STALLED_SENDS = 3
        const val ERROR_REPORT_INTERVAL_MS = 1_000L

        /** Longest side fed to the still landmarker (MediaPipe downsamples to 256 px anyway). */
        const val MAX_STILL_SIDE = 1920
    }
}
