package com.poseit.app.ui.camera

import android.Manifest
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poseit.app.input.HardwareKeys
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/** The live pose-guide camera. [initialPoseId] null = last used / first pose. */
@Composable
fun CameraScreen(initialPoseId: String?, onBack: () -> Unit, onOpenGallery: () -> Unit) {
    CameraWindowEffects()
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        CameraPermissionGate(onBack = onBack) {
            val context = LocalContext.current
            val vm: CameraViewModel = viewModel(factory = remember(initialPoseId) { CameraViewModel.factory(context, initialPoseId) })
            CameraContent(vm, onBack, onOpenGallery)
        }
    }
}

/** Light system-bar icons on the black camera UI, and keep the screen on (hands-free shooting). */
@Composable
private fun CameraWindowEffects() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val lightStatus = controller?.isAppearanceLightStatusBars
        val lightNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        val keepOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            controller?.let { c ->
                if (lightStatus != null) c.isAppearanceLightStatusBars = lightStatus
                if (lightNav != null) c.isAppearanceLightNavigationBars = lightNav
            }
            view.keepScreenOn = keepOn
        }
    }
}

@Composable
private fun CameraContent(vm: CameraViewModel, onBack: () -> Unit, onOpenGallery: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val poses by vm.poses.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val hasFlash by vm.hasFlash.collectAsStateWithLifecycle()
    val maxZoom by vm.maxZoom.collectAsStateWithLifecycle()
    val exposureRange by vm.exposureRange.collectAsStateWithLifecycle()
    val exposureStep by vm.exposureStepEv.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val flashAlpha = remember { Animatable(0f) }
    var showQuickSettings by rememberSaveable { mutableStateOf(false) }
    var showAllPoses by rememberSaveable { mutableStateOf(false) }
    var showTips by rememberSaveable { mutableStateOf(false) }
    var showPhotoshoot by rememberSaveable { mutableStateOf(false) }

    fun showMessage(text: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(text, actionLabel = actionLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    // ---- storage permission (Android 9 and below only)
    var shutterAfterStorageGrant by remember { mutableStateOf(false) }
    val (storage, requestStorage) = rememberPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) { granted ->
        if (granted && shutterAfterStorageGrant) vm.onShutterPressed()
        if (!granted) showMessage("Poseit needs storage access to save photos on this Android version")
        shutterAfterStorageGrant = false
    }
    fun askStorage(thenShoot: Boolean) {
        if (storage.permanentlyDenied) {
            showMessage("Allow storage access to save photos", "Settings") { context.openAppSettings() }
        } else {
            shutterAfterStorageGrant = thenShoot
            requestStorage()
        }
    }
    LaunchedEffect(Unit) {
        if (context.needsLegacyStoragePermission() && !storage.askedBefore) askStorage(thenShoot = false)
    }

    val onShutter: () -> Unit = {
        if (session == null && context.needsLegacyStoragePermission()) askStorage(thenShoot = true) else vm.onShutterPressed()
    }
    val currentShutter by rememberUpdatedState(onShutter)
    val currentOpenGallery by rememberUpdatedState(onOpenGallery)

    // ---- lifecycle: detection + volume-key shutter only while RESUMED
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    HardwareKeys.captureVolumeKeys = true
                    vm.setActive(true)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    HardwareKeys.captureVolumeKeys = false
                    vm.setActive(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            HardwareKeys.captureVolumeKeys = false
            vm.setActive(false)
        }
    }
    LaunchedEffect(Unit) { HardwareKeys.shutter.collect { currentShutter() } }

    // ---- one-shot events
    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                CameraEvent.Flash -> launch {
                    flashAlpha.snapTo(0.85f)
                    flashAlpha.animateTo(0f, tween(durationMillis = 320))
                }
                is CameraEvent.PhotoSaved -> showMessage("Saved to Pictures/Poseit", "View") { currentOpenGallery() }
                is CameraEvent.Message -> showMessage(event.text)
                // A grant then takes the photo the coach wanted to take.
                CameraEvent.RequestStoragePermission -> askStorage(thenShoot = true)
            }
        }
    }

    // ---- pose pill for ~4 s after a pose change
    var pillVisible by remember { mutableStateOf(false) }
    LaunchedEffect(ui.pose?.id) {
        pillVisible = ui.pose != null
        if (pillVisible) {
            delay(POSE_PILL_MILLIS)
            pillVisible = false
        }
    }

    // ---- strip contents: favourites + current category
    var stripCategory by rememberSaveable { mutableStateOf(PoseCategory.STANDING) }
    LaunchedEffect(ui.pose) { ui.pose?.let { stripCategory = it.stripCategory } }
    val stripPoses = remember(poses, favorites, stripCategory, ui.pose) {
        buildStripPoses(poses, favorites, stripCategory, ui.pose)
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    CameraBinding(vm, previewView, lifecycleOwner, ui)

    Column(Modifier.fillMaxSize()) {
        CameraTopBar(
            flash = ui.flash,
            hasFlash = hasFlash,
            grid = settings.grid,
            aspect = ui.aspect,
            timer = ui.timer,
            onBack = onBack,
            onFlash = vm::cycleFlash,
            onGrid = vm::cycleGrid,
            onAspect = { vm.setAspect(ui.aspect.next()) },
            onTimer = vm::setTimer,
            onPhotoshoot = { showPhotoshoot = true },
            onQuickSettings = { showQuickSettings = true },
        )
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val density = LocalDensity.current
            var controlsHeightPx by remember { mutableIntStateOf(0) }
            val controlsHeight = with(density) { controlsHeightPx.toDp() }
            val ratio = ui.aspect.ratio
            val previewWidth = min(maxWidth, maxHeight * ratio)
            val previewHeight = previewWidth / ratio
            val free = maxHeight - controlsHeight
            // Centre the preview above the controls when it fits; otherwise top-align it and let the controls overlap.
            val previewTop = if (previewHeight <= free) (free - previewHeight) / 2 else 0.dp

            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = previewTop)
                    .width(previewWidth)
                    .aspectRatio(ratio)
                    .clipToBounds(),
            ) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                PoseOverlay(
                    vm = vm,
                    ui = ui,
                    settings = settings,
                    onTapFocus = { p -> safely { vm.camera.focusAt(previewView, p.x, p.y) } },
                )
                PreviewHud(
                    vm = vm,
                    ui = ui,
                    session = session,
                    pillVisible = pillVisible,
                    onOpenTips = { showTips = true },
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = flashAlpha.value }
                        .background(Color.White),
                )
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { controlsHeightPx = it.height }
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.75f))))
                    .navigationBarsPadding()
                    .padding(top = 12.dp),
            ) {
                PoseStrip(
                    poses = stripPoses,
                    selectedId = ui.pose?.id,
                    favorites = favorites,
                    onSelect = vm::selectPose,
                    onOpenAll = { showAllPoses = true },
                )
                ShutterRow(
                    lastPhoto = ui.lastPhoto,
                    sessionActive = session != null,
                    timer = ui.timer,
                    capturing = ui.capturing,
                    onGallery = onOpenGallery,
                    onShutter = onShutter,
                    onFlip = vm::flipLens,
                )
            }

            SnackbarHost(
                snackbar,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = controlsHeight + 8.dp)
                    .widthIn(max = 560.dp),
            )
        }
    }

    if (showQuickSettings) {
        QuickSettingsSheet(
            settings = settings,
            ui = ui,
            maxZoom = maxZoom,
            exposureRange = exposureRange,
            exposureStepEv = exposureStep,
            onUpdateSettings = vm::updateSettings,
            onAudience = vm::setAudienceMode,
            onHandRaise = vm::setHandRaiseEnabled,
            onZoom = vm::setZoom,
            onExposure = vm::setExposure,
            onDismiss = { showQuickSettings = false },
        )
    }
    if (showAllPoses) {
        AllPosesSheet(
            poses = poses,
            favorites = favorites,
            selected = ui.pose,
            onSelect = {
                vm.selectPose(it)
                showAllPoses = false
            },
            onToggleFavorite = vm::toggleFavorite,
            onDismiss = { showAllPoses = false },
        )
    }
    val tipsPose = ui.pose
    LaunchedEffect(tipsPose == null) { if (tipsPose == null) showTips = false }
    if (showTips && tipsPose != null) {
        PoseTipsSheet(
            pose = tipsPose,
            favorite = tipsPose.id in favorites,
            onToggleFavorite = { vm.toggleFavorite(tipsPose.id) },
            onDismiss = { showTips = false },
        )
    }
    if (showPhotoshoot) {
        PhotoshootDialog(
            initial = ui.timer as? TimerOption.Photoshoot,
            onConfirm = {
                vm.setTimer(it)
                showPhotoshoot = false
            },
            onDismiss = { showPhotoshoot = false },
        )
    }
}

/** Everything drawn as Compose UI on top of the preview (badges, hints, rail, countdown). */
@Composable
private fun PreviewHud(
    vm: CameraViewModel,
    ui: CameraUiState,
    session: SessionInfo?,
    pillVisible: Boolean,
    onOpenTips: () -> Unit,
) {
    // Collected here so hint / countdown changes only recompose the HUD.
    val coaching by vm.coaching.collectAsStateWithLifecycle()
    val countdown by vm.countdown.collectAsStateWithLifecycle()
    Box(
        Modifier
            .fillMaxSize()
            .padding(10.dp),
    ) {
        when {
            session != null && session.isPhotoshoot -> ShotCounter(session, vm::stopSession, Modifier.align(Alignment.TopStart))
            ui.detectorError != null && ui.hasPose -> DetectorNotice(Modifier.align(Alignment.TopStart))
        }
        if (ui.hasPose) {
            ScoreBadge(vm, ui.detectorReady, ui.detectorError, Modifier.align(Alignment.TopEnd))
        }
        PosePill(
            pose = ui.pose,
            visible = pillVisible,
            onClick = onOpenTips,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 66.dp),
        )
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 4.dp, end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (ui.hasPose && countdown == null) HintBanner(coaching.hint)
            LightingChip(
                hint = coaching.lighting.takeIf { countdown == null },
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        ToolRail(
            ui = ui,
            onTips = onOpenTips,
            onToggleLock = vm::toggleOverlayLock,
            onToggleMirror = vm::togglePoseMirror,
            onOverlayMode = vm::setOverlayMode,
            onToggleSceneLock = vm::toggleSceneLock,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        ui.cameraError?.let { CameraErrorNotice(it, Modifier.align(Alignment.Center)) }
        CountdownNumber(countdown, Modifier.align(Alignment.Center))
    }
}

/**
 * Binds the camera once the PreviewView has its final size; rebinds on lens / aspect change and unbinds when
 * the screen leaves composition.
 */
@Composable
private fun CameraBinding(vm: CameraViewModel, previewView: PreviewView, lifecycleOwner: LifecycleOwner, ui: CameraUiState) {
    val lens = ui.lens
    val aspect = ui.aspect
    LaunchedEffect(previewView, lifecycleOwner, lens, aspect) {
        previewView.awaitLayout(aspect.ratio)
        try {
            // Each binding tags its frames with its own aspect, so frames still arriving from the old binding after
            // an aspect switch are never mapped into the new scene.
            vm.camera.bind(lifecycleOwner, previewView, lens, aspect) { bitmap, timestampMs -> vm.onFrame(bitmap, timestampMs, aspect) }
            vm.onCameraBound()
            // Some devices finish opening the camera asynchronously: apply flash/zoom/exposure once more.
            delay(REAPPLY_DELAY_MILLIS)
            vm.onCameraBound()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            vm.onCameraError(e)
        }
    }
    DisposableEffect(previewView) {
        onDispose { safely { vm.camera.unbind() } }
    }
}

/** Waits (at most ~0.8 s) until the view is laid out with the requested aspect ratio. */
private suspend fun View.awaitLayout(targetAspect: Float) {
    withTimeoutOrNull(800L) {
        while (!(isLaidOut && width > 0 && height > 0 && abs(width.toFloat() / height - targetAspect) < 0.03f)) {
            withFrameNanos { }
        }
    }
}

/** Favourites first, then the strip category; the current pose is always included. */
internal fun buildStripPoses(
    all: List<PoseTemplate>,
    favorites: Set<String>,
    category: PoseCategory,
    current: PoseTemplate?,
): List<PoseTemplate> {
    val list = (all.filter { it.id in favorites } + all.filter { it.stripCategory == category }).distinctBy { it.id }
    return if (current != null && list.none { it.id == current.id }) listOf(current) + list else list
}

private const val POSE_PILL_MILLIS = 4000L
private const val REAPPLY_DELAY_MILLIS = 600L
