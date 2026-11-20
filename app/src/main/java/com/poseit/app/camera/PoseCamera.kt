package com.poseit.app.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

enum class Lens { BACK, FRONT }
enum class FlashMode { OFF, AUTO, ON }

/** Aspect ratio of the preview / saved photo (portrait). */
enum class SceneAspect(val ratio: Float, val label: String) {
    RATIO_3_4(3f / 4f, "3:4"),
    RATIO_9_16(9f / 16f, "9:16"),
    RATIO_1_1(1f, "1:1"),
}

/** Thrown by [PoseCamera] when the camera can't be started or a photo can't be taken. */
class PoseCameraException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * CameraX wrapper: Preview + ImageCapture + ImageAnalysis bound together in a UseCaseGroup with the
 * PreviewView's ViewPort, so the analysed frames, the visible preview and the saved photo all show the same
 * area (= SCENE space).
 *
 * Analysis frames are delivered to [onFrame] on a background executor as upright (rotated), NON-mirrored
 * ARGB bitmaps already cropped to the viewport, together with a timestamp in ms. The callback must not keep
 * the bitmap after returning unless it copies it.
 *
 * Threading: [bind], [unbind] and [focusAt] belong on the main thread (calls from other threads are re-posted
 * to it); the setters and [takePhoto] may be called from any thread.
 */
class PoseCamera(private val context: Context) {

    private val appContext: Context = context.applicationContext ?: context
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * One background thread for frame analysis (frames are processed strictly in order). The thread times out
     * when idle, so an instance that is simply dropped doesn't leak it.
     */
    private val analysisExecutor = ThreadPoolExecutor(
        1, 1, 5L, TimeUnit.SECONDS, LinkedBlockingQueue(),
    ) { runnable -> Thread(runnable, "PoseCamera-analysis") }.apply { allowCoreThreadTimeOut(true) }

    private val _lens = MutableStateFlow(Lens.BACK)
    val lens: StateFlow<Lens> = _lens.asStateFlow()

    private val _maxZoom = MutableStateFlow(1f)

    /** Max zoom ratio supported by the current camera (1 when unknown). */
    val maxZoom: StateFlow<Float> = _maxZoom.asStateFlow()

    private val _hasFlash = MutableStateFlow(false)
    val hasFlash: StateFlow<Boolean> = _hasFlash.asStateFlow()

    private val _exposureRange = MutableStateFlow(0..0)

    /** Supported exposure-compensation index range (0..0 when unsupported). */
    val exposureRange: StateFlow<IntRange> = _exposureRange.asStateFlow()

    private val _exposureStepEv = MutableStateFlow(0f)

    /** Size of one exposure-compensation step in EV (e.g. 1/3). */
    val exposureStepEv: StateFlow<Float> = _exposureStepEv.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)

    /** Why the camera can't start or stopped working (user-readable); null while it works. */
    val error: StateFlow<String?> = _error.asStateFlow()

    // ------------------------------------------------------------------ main-thread state

    /** Started right away so the (slow) first CameraX initialisation overlaps with the screen's layout. */
    private var providerFuture: ListenableFuture<ProcessCameraProvider>? = requestProvider()

    /** Incremented by every bind/unbind; a deferred bind only runs while its generation is still current. */
    private var bindGeneration = 0
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var observedInfo: CameraInfo? = null

    @Volatile private var camera: Camera? = null
    @Volatile private var imageCapture: ImageCapture? = null
    @Volatile private var analyzerToken = NO_ANALYZER
    @Volatile private var targetRotation = Surface.ROTATION_0
    @Volatile private var minZoom = 1f

    @Volatile private var flashMode = FlashMode.OFF
    @Volatile private var pendingZoom: Float? = null
    @Volatile private var pendingExposure: Int? = null

    // ------------------------------------------------------------------ analysis-thread state

    private val converter = FrameConverter()
    private val timestamps = MonotonicTimestamps()

    private val zoomObserver = Observer<ZoomState> { state -> onZoomState(state) }
    private val cameraStateObserver = Observer<CameraState> { state -> onCameraState(state) }

    /**
     * (Re)binds all use cases. Safe to call again to switch lens or aspect.
     *
     * Binds synchronously when CameraX is initialised and the view is laid out, and throws a
     * [PoseCameraException] if that fails. Otherwise the bind completes as soon as both are ready, and a failure
     * is reported through [error]. Falls back to the other lens if the requested one doesn't exist ([lens]
     * reports the lens actually in use).
     *
     * @param onFrame receives (uprightBitmap, timestampMs); called on a background thread
     */
    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lens: Lens,
        aspect: SceneAspect,
        onFrame: (Bitmap, Long) -> Unit,
    ) {
        if (!isMainThread()) {
            mainHandler.post { runCatching { bind(lifecycleOwner, previewView, lens, aspect, onFrame) } }
            return
        }
        val request = BindRequest(++bindGeneration, lifecycleOwner, previewView, lens, aspect, onFrame)
        attemptBind(request, rethrow = true)
    }

    fun setFlash(mode: FlashMode) {
        flashMode = mode
        imageCapture?.let(::applyFlash)
    }

    /** @param ratio 1..maxZoom */
    fun setZoom(ratio: Float) {
        if (ratio.isNaN()) return
        val cam = camera
        if (cam == null) {
            pendingZoom = ratio
            return
        }
        val max = _maxZoom.value.coerceAtLeast(1f)
        val min = minZoom.coerceIn(0.1f, 1f)
        try {
            cam.cameraControl.setZoomRatio(ratio.coerceIn(min, max))
        } catch (e: Exception) {
            Log.w(TAG, "setZoom failed", e)
        }
    }

    /** Exposure compensation index within [exposureRange]. */
    fun setExposure(index: Int) {
        val cam = camera
        if (cam == null) {
            pendingExposure = index
            return
        }
        val range = _exposureRange.value
        if (range.first == range.last) return // unsupported
        try {
            cam.cameraControl.setExposureCompensationIndex(index.coerceIn(range))
        } catch (e: Exception) {
            Log.w(TAG, "setExposure failed", e)
        }
    }

    /** Tap-to-focus at view coordinates. */
    fun focusAt(previewView: PreviewView, x: Float, y: Float) {
        if (!isMainThread()) {
            mainHandler.post { focusAt(previewView, x, y) }
            return
        }
        val cam = camera ?: return
        try {
            val point = previewView.meteringPointFactory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(FOCUS_AUTO_CANCEL_SECONDS, TimeUnit.SECONDS)
                .build()
            // The future fails harmlessly (e.g. fixed-focus lens or camera closing); nothing to handle.
            cam.cameraControl.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.w(TAG, "Tap to focus failed", e)
        }
    }

    /**
     * Takes a FULL-RESOLUTION photo with ImageCapture (CAPTURE_MODE_MAXIMIZE_QUALITY, never a preview frame) and
     * saves it to MediaStore under Pictures/Poseit (JPEG). The overlay is never part of the photo.
     * @param mirror save horizontally mirrored (used for front camera when the user prefers mirrored selfies)
     * @return the content Uri of the saved image
     * @throws Exception if capture fails
     */
    suspend fun takePhoto(mirror: Boolean): Uri = withContext(Dispatchers.Main.immediate) {
        val capture = imageCapture ?: throw PoseCameraException("The camera isn't ready yet")
        capture.targetRotation = targetRotation
        val options = ImageCapture.OutputFileOptions
            .Builder(appContext.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newPhotoValues())
            .setMetadata(ImageCapture.Metadata().apply { isReversedHorizontal = mirror })
            .build()
        suspendCancellableCoroutine { cont ->
            capture.takePicture(
                options,
                mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val uri = outputFileResults.savedUri
                        if (uri != null) {
                            cont.resume(uri)
                        } else {
                            cont.resumeWithException(PoseCameraException("The photo was saved without a location"))
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed", exception)
                        cont.resumeWithException(exception)
                    }
                },
            )
        }
    }

    fun unbind() {
        if (!isMainThread()) {
            mainHandler.post { unbind() }
            return
        }
        bindGeneration++
        releaseUseCases()
    }

    // ------------------------------------------------------------------ binding

    private class BindRequest(
        val generation: Int,
        val lifecycleOwner: LifecycleOwner,
        val previewView: PreviewView,
        val lens: Lens,
        val aspect: SceneAspect,
        val onFrame: (Bitmap, Long) -> Unit,
    )

    private fun attemptBind(request: BindRequest, rethrow: Boolean) {
        if (request.generation != bindGeneration) return // superseded or unbound meanwhile
        try {
            if (request.lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
            val provider = readyProvider()
            if (provider == null) {
                providerFuture?.addListener({ attemptBind(request, rethrow = false) }, mainExecutor)
                return
            }
            val viewPort = viewPortFor(request.previewView, request.aspect)
            if (viewPort == null) {
                // Not laid out / attached yet: try again after the next layout pass.
                request.previewView.doOnNextLayout { attemptBind(request, rethrow = false) }
                return
            }
            performBind(provider, request, viewPort)
        } catch (e: Exception) {
            val failure = e as? PoseCameraException ?: PoseCameraException("Couldn't start the camera", e)
            Log.e(TAG, failure.message, e)
            _error.value = failure.message
            if (rethrow) throw failure
        }
    }

    private fun performBind(provider: ProcessCameraProvider, request: BindRequest, viewPort: ViewPort) {
        val lens = resolveLens(provider, request.lens)
            ?: throw PoseCameraException("No camera is available on this device")
        releaseUseCases(provider)

        val rotation = request.previewView.display?.rotation ?: Surface.ROTATION_0
        val ratio = if (request.aspect == SceneAspect.RATIO_9_16) AspectRatio.RATIO_16_9 else AspectRatio.RATIO_4_3
        val newPreview = Preview.Builder()
            .setResolutionSelector(resolutionSelector(ratio, null))
            .setTargetRotation(rotation)
            .build()
            .also { it.setSurfaceProvider(request.previewView.surfaceProvider) }
        val newCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector(ratio, ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY))
            .setTargetRotation(rotation)
            .build()
        val analysisSize = if (ratio == AspectRatio.RATIO_16_9) Size(640, 360) else Size(640, 480)
        val newAnalysis = ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                resolutionSelector(
                    ratio,
                    ResolutionStrategy(analysisSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                ),
            )
            .setTargetRotation(rotation)
            .build()
        val token = request.generation
        analyzerToken = token
        newAnalysis.setAnalyzer(analysisExecutor, FrameAnalyzer(token, request.onFrame))

        val selector = lens.selector
        var analysisBound = true
        val boundCamera = try {
            provider.bindToLifecycle(
                request.lifecycleOwner,
                selector,
                UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(newPreview)
                    .addUseCase(newCapture)
                    .addUseCase(newAnalysis)
                    .build(),
            )
        } catch (e: IllegalArgumentException) {
            // Some LEGACY devices can't stream preview + full-resolution capture + analysis at once.
            // Keep the camera usable (without pose detection) instead of failing completely.
            Log.w(TAG, "Preview + capture + analysis not supported; binding without analysis", e)
            newAnalysis.clearAnalyzer()
            analyzerToken = NO_ANALYZER
            analysisBound = false
            provider.unbind(newPreview, newCapture, newAnalysis)
            provider.bindToLifecycle(
                request.lifecycleOwner,
                selector,
                UseCaseGroup.Builder().setViewPort(viewPort).addUseCase(newPreview).addUseCase(newCapture).build(),
            )
        }

        camera = boundCamera
        preview = newPreview
        imageCapture = newCapture
        imageAnalysis = if (analysisBound) newAnalysis else null
        targetRotation = rotation
        _lens.value = lens
        _error.value = null

        val info = boundCamera.cameraInfo
        _hasFlash.value = info.hasFlashUnit()
        val exposure = info.exposureState
        if (exposure.isExposureCompensationSupported) {
            val range = exposure.exposureCompensationRange
            _exposureRange.value = range.lower..range.upper
            _exposureStepEv.value = exposure.exposureCompensationStep.toFloat()
        } else {
            _exposureRange.value = 0..0
            _exposureStepEv.value = 0f
        }
        info.zoomState.value?.let(::onZoomState)
        info.zoomState.observe(request.lifecycleOwner, zoomObserver)
        info.cameraState.observe(request.lifecycleOwner, cameraStateObserver)
        observedInfo = info

        applyFlash(newCapture)
        pendingZoom?.let { zoom ->
            pendingZoom = null
            setZoom(zoom)
        }
        pendingExposure?.let { index ->
            pendingExposure = null
            setExposure(index)
        }
    }

    /** Unbinds only this instance's use cases (another screen's camera may be bound to the same provider). */
    private fun releaseUseCases(provider: ProcessCameraProvider? = currentProviderOrNull()) {
        observedInfo?.let { info ->
            info.zoomState.removeObserver(zoomObserver)
            info.cameraState.removeObserver(cameraStateObserver)
        }
        observedInfo = null
        analyzerToken = NO_ANALYZER
        imageAnalysis?.clearAnalyzer()
        val useCases = listOfNotNull<UseCase>(preview, imageCapture, imageAnalysis)
        if (provider != null && useCases.isNotEmpty()) {
            try {
                provider.unbind(*useCases.toTypedArray())
            } catch (e: Exception) {
                Log.w(TAG, "Unbinding use cases failed", e)
            }
        }
        camera = null
        preview = null
        imageCapture = null
        imageAnalysis = null
    }

    /**
     * The ViewPort of [previewView] (null until it is attached and laid out). If the view's shape doesn't match
     * the requested [aspect] (e.g. it is still being resized), a ViewPort with the requested aspect is used
     * instead so the photo and the analysed frames always have the aspect the matcher assumes.
     */
    private fun viewPortFor(previewView: PreviewView, aspect: SceneAspect): ViewPort? {
        val fromView = previewView.viewPort ?: return null
        val viewRatio = fromView.aspectRatio.toFloat()
        if (abs(viewRatio - aspect.ratio) <= aspect.ratio * VIEWPORT_ASPECT_TOLERANCE) return fromView
        return ViewPort.Builder(aspect.rational, fromView.rotation)
            .setScaleType(fromView.scaleType)
            .setLayoutDirection(fromView.layoutDirection)
            .build()
    }

    private fun resolutionSelector(ratio: Int, strategy: ResolutionStrategy?): ResolutionSelector =
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy(ratio, AspectRatioStrategy.FALLBACK_RULE_AUTO))
            .apply { if (strategy != null) setResolutionStrategy(strategy) }
            .build()

    private fun resolveLens(provider: ProcessCameraProvider, wanted: Lens): Lens? {
        fun available(lens: Lens) = try {
            provider.hasCamera(lens.selector)
        } catch (e: Exception) {
            false
        }
        val other = if (wanted == Lens.BACK) Lens.FRONT else Lens.BACK
        return when {
            available(wanted) -> wanted
            available(other) -> other.also { Log.w(TAG, "No $wanted camera, using $other") }
            else -> null
        }
    }

    private fun applyFlash(capture: ImageCapture) {
        val mode = if (_hasFlash.value) {
            when (flashMode) {
                FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            }
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
        try {
            capture.flashMode = mode
        } catch (e: Exception) {
            Log.w(TAG, "setFlash failed", e)
        }
    }

    private fun onZoomState(state: ZoomState) {
        minZoom = state.minZoomRatio
        _maxZoom.value = state.maxZoomRatio.coerceAtLeast(1f)
    }

    private fun onCameraState(state: CameraState) {
        val error = state.error
        when {
            error != null -> _error.value = describeCameraError(error.code)
            state.type == CameraState.Type.OPEN -> _error.value = null
        }
    }

    private fun describeCameraError(code: Int): String = when (code) {
        CameraState.ERROR_CAMERA_IN_USE, CameraState.ERROR_MAX_CAMERAS_IN_USE ->
            "The camera is being used by another app"
        CameraState.ERROR_CAMERA_DISABLED -> "The camera is disabled on this device"
        CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> "Turn off Do Not Disturb to use the camera"
        CameraState.ERROR_STREAM_CONFIG -> "This camera configuration isn't supported"
        else -> "The camera stopped unexpectedly"
    }

    // ------------------------------------------------------------------ provider

    private fun requestProvider(): ListenableFuture<ProcessCameraProvider>? = try {
        ProcessCameraProvider.getInstance(appContext)
    } catch (e: Exception) {
        Log.e(TAG, "CameraX initialisation failed", e)
        null
    }

    /**
     * The provider if CameraX is initialised, null while initialising; throws if initialisation failed. A failed
     * initialisation is forgotten, so the next [bind] call retries it (never in a loop on its own).
     */
    private fun readyProvider(): ProcessCameraProvider? {
        val future = providerFuture
            ?: requestProvider()?.also { providerFuture = it }
            ?: throw PoseCameraException("The camera service is unavailable")
        if (!future.isDone) return null
        return try {
            future.get()
        } catch (e: Exception) {
            providerFuture = null
            throw PoseCameraException("The camera service couldn't be started", e.cause ?: e)
        }
    }

    private fun currentProviderOrNull(): ProcessCameraProvider? {
        val future = providerFuture ?: return null
        return if (future.isDone) runCatching { future.get() }.getOrNull() else null
    }

    // ------------------------------------------------------------------ analysis

    private inner class FrameAnalyzer(
        private val token: Int,
        private val onFrame: (Bitmap, Long) -> Unit,
    ) : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            try {
                if (token != analyzerToken) return // unbound or rebound meanwhile
                val timestampMs = timestamps.next(image.imageInfo.timestamp / 1_000_000L)
                val frame = converter.convert(image) ?: return
                onFrame(frame, timestampMs)
            } catch (e: Exception) {
                Log.w(TAG, "Frame analysis failed", e)
            } catch (e: OutOfMemoryError) {
                converter.release()
                Log.w(TAG, "Out of memory while converting a frame", e)
            } finally {
                image.close()
            }
        }
    }

    // ------------------------------------------------------------------ photo files

    private fun newPhotoValues(): ContentValues {
        val now = System.currentTimeMillis()
        val name = "Poseit_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(now)) + ".jpg"
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, now)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM)
                if (!dir.exists() && !dir.mkdirs()) Log.w(TAG, "Couldn't create $dir")
                @Suppress("DEPRECATION")
                put(MediaStore.MediaColumns.DATA, File(dir, name).absolutePath)
            }
        }
    }

    private fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()

    private val Lens.selector: CameraSelector
        get() = when (this) {
            Lens.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            Lens.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
        }

    private val SceneAspect.rational: Rational
        get() = when (this) {
            SceneAspect.RATIO_3_4 -> Rational(3, 4)
            SceneAspect.RATIO_9_16 -> Rational(9, 16)
            SceneAspect.RATIO_1_1 -> Rational(1, 1)
        }

    private companion object {
        const val TAG = "PoseCamera"
        const val ALBUM = "Poseit"
        const val NO_ANALYZER = -1
        const val FOCUS_AUTO_CANCEL_SECONDS = 3L

        /** Relative difference between the view's and the requested aspect that still counts as "the same". */
        const val VIEWPORT_ASPECT_TOLERANCE = 0.03f
    }
}
