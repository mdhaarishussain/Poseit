package com.poseit.app.ui.camera

import android.Manifest
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
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
import com.poseit.app.ui.components.PoseitSnackbarHost
import com.poseit.app.ui.components.ProvideSurfaceTone
import com.poseit.app.ui.theme.LightSystemBarIcons
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * The live pose-guide camera. [initialPoseId] null = last used / first pose; [initialMirrored] starts that pose
 * mirrored (the pose detail screen's Mirror toggle).
 */
@Composable
fun CameraScreen(initialPoseId: String?, onBack: () -> Unit, onOpenGallery: () -> Unit, initialMirrored: Boolean = false) {
    // Sheets and dialogs opened from the camera sit over the black surround: give their windows white bar icons.
    LightSystemBarIcons { CameraScreenContent(initialPoseId, onBack, onOpenGallery, initialMirrored) }
}

@Composable
private fun CameraScreenContent(initialPoseId: String?, onBack: () -> Unit, onOpenGallery: () -> Unit, initialMirrored: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PoseitColors.backgroundCamera),
    ) {
        CameraPermissionGate(onBack = onBack) {
            // Only once the black camera UI shows: the permission rationale is on light paper and keeps dark icons.
            CameraWindowEffects()
            val context = LocalContext.current
            val vm: CameraViewModel = viewModel(
                factory = remember(initialPoseId, initialMirrored) { CameraViewModel.factory(context, initialPoseId, initialMirrored) },
            )
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
        // API 26-28 draw the app's own translucent canvas-coloured nav-bar scrim (API 29+ use the system's contrast scrim,
        // which follows the icon colour; API 24-25 already use a dark one): swap in a dark scrim for white icons.
        val navColor = if (Build.VERSION.SDK_INT in 26..28) window?.navigationBarColorCompat else null
        if (navColor != null) window?.navigationBarColorCompat = PoseitColors.backgroundCamera.copy(alpha = 0.5f).toArgb()
        val keepOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            controller?.let { c ->
                if (lightStatus != null) c.isAppearanceLightStatusBars = lightStatus
                if (lightNav != null) c.isAppearanceLightNavigationBars = lightNav
            }
            if (navColor != null) window?.navigationBarColorCompat = navColor
            view.keepScreenOn = keepOn
        }
    }
}

/** Window.navigationBarColor, which is deprecated (and ignored) on API 35+; only used on API 26-28 here. */
@Suppress("DEPRECATION")
private var Window.navigationBarColorCompat: Int
    get() = navigationBarColor
    set(value) {
        navigationBarColor = value
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
            showMessage("Allow storage access to save photos", "Open settings") { context.openAppSettings() }
        } else {
            shutterAfterStorageGrant = thenShoot
            requestStorage()
        }
    }
    LaunchedEffect(Unit) {
        if (context.needsLegacyStoragePermission() && !storage.askedBefore) askStorage(thenShoot = false)
    }
    // Without it the view model keeps auto-capture and the hand-raise trigger off; say so instead of failing silently.
    val storageBlocked = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !storage.granted

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
                // With the countdown, the camera's one orchestrated moment: a crisp white flash that decays fast.
                CameraEvent.Flash -> launch {
                    flashAlpha.snapTo(0.85f)
                    flashAlpha.animateTo(0f, tween(durationMillis = 280, easing = LinearOutSlowInEasing))
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
            // The viewfinder is a card like the bars around it: 8dp from the sides and below the top bar card.
            val edge = Space.edge
            val ratio = ui.aspect.ratio
            val previewWidth = min(maxWidth - edge * 2, (maxHeight - edge) * ratio)
            val previewHeight = previewWidth / ratio
            val free = maxHeight - edge - controlsHeight
            // Centre the preview above the dock when it fits; otherwise top-align it and let the dock overlap it.
            val previewTop = edge + if (previewHeight <= free) (free - previewHeight) / 2 else 0.dp
            val dockTop = maxHeight - controlsHeight + edge
            // A short screen gets the compact dock (smaller tiles and shutter), so more of the preview stays clear.
            val compactDock = maxHeight < CompactDockBelow
            val dockOverPreview = controlsHeightPx > 0 && previewTop + previewHeight > dockTop + 1.dp

            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = previewTop)
                    .width(previewWidth)
                    .aspectRatio(ratio)
                    .clip(PoseitShapes.Card)
                    .background(PoseitColors.backgroundCamera),
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
                    storageBlocked = storageBlocked,
                    onOpenTips = { showTips = true },
                    onAllowStorage = { askStorage(thenShoot = false) },
                    // Where the dock covers the bottom of the preview, the rail and notices centre above it.
                    bottomInset = if (dockOverPreview) previewTop + previewHeight - dockTop else 0.dp,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = flashAlpha.value }
                        .background(PoseitColors.cameraContent),
                )
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { controlsHeightPx = it.height }
                    // Makes the whole dock area a hit target (without consuming anything), so touches in its gaps and
                    // margins don't fall through to the preview below (tap-to-focus, dragging the guide).
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                    .navigationBarsPadding()
                    .padding(start = edge, end = edge, top = edge, bottom = edge),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CameraDock(overPreview = dockOverPreview) {
                    PoseStrip(
                        poses = stripPoses,
                        selectedId = ui.pose?.id,
                        favorites = favorites,
                        onSelect = vm::selectPose,
                        onOpenAll = { showAllPoses = true },
                        compact = compactDock,
                    )
                    ShutterRow(
                        lastPhoto = ui.lastPhoto,
                        sessionActive = session != null,
                        timer = ui.timer,
                        capturing = ui.capturing,
                        onGallery = onOpenGallery,
                        onShutter = onShutter,
                        onFlip = vm::flipLens,
                        compact = compactDock,
                    )
                }
            }

            PoseitSnackbarHost(
                snackbar,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = controlsHeight),
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
    storageBlocked: Boolean,
    onOpenTips: () -> Unit,
    onAllowStorage: () -> Unit,
    bottomInset: Dp = 0.dp,
) {
    // Collected here so hint / countdown changes only recompose the HUD.
    val coaching by vm.coaching.collectAsStateWithLifecycle()
    val countdown by vm.countdown.collectAsStateWithLifecycle()
    // The HUD is a dark area: shared components inside pick their on-dark colours.
    ProvideSurfaceTone(SurfaceTone.Dark) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = bottomInset)
                .padding(Space.m),
        ) {
            // The pose card and the top-left notices share the top edge, so they take turns: the photoshoot counter wins,
            // then the pose card (for a few seconds after a pose change), then the detector notice.
            val photoshoot = session != null && session.isPhotoshoot
            val showPill = pillVisible && !photoshoot
            when {
                photoshoot -> ShotCounter(session!!, vm::stopSession, Modifier.align(Alignment.TopStart))
                // In the corner, clear of the guide figure; the score badge shows a warning glyph too.
                ui.detectorError != null && ui.hasPose -> AnimatedVisibility(
                    visible = !showPill,
                    enter = fadeIn(PoseitMotion.standard(PoseitMotion.MEDIUM)),
                    exit = fadeOut(tween(PoseitMotion.FAST, easing = PoseitMotion.EaseExit)),
                    modifier = Modifier.align(Alignment.TopStart),
                ) { DetectorNotice() }
            }
            if (ui.hasPose) {
                ScoreBadge(vm, ui.detectorReady, ui.detectorError, Modifier.align(Alignment.TopEnd))
            }
            PosePill(
                pose = ui.pose,
                visible = showPill,
                onClick = onOpenTips,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = ScoreBadgeSize + Space.s),
            )
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = ScoreBadgeSize + Space.l),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                if (ui.hasPose && countdown == null) HintBanner(coaching.hint)
                LightingChip(hint = coaching.lighting.takeIf { countdown == null })
                if (storageBlocked) {
                    // Persistent, so kept clear of the ToolRail on the right edge (the hint banner above may overlap
                    // it briefly; this notice stays up for as long as storage is denied).
                    CameraErrorNotice(
                        STORAGE_BLOCKED_NOTICE,
                        Modifier
                            .padding(horizontal = CameraControlSize + Space.s)
                            .clip(PoseitShapes.Control)
                            .clickable(onClickLabel = "Allow storage access", onClick = onAllowStorage),
                    )
                }
            }
            RailSlot(clearTop = if (ui.hasPose) ScoreBadgeSize + Space.s else 0.dp) {
                ToolRail(
                    ui = ui,
                    onTips = onOpenTips,
                    onToggleLock = vm::toggleOverlayLock,
                    onToggleMirror = vm::togglePoseMirror,
                    onOverlayMode = vm::setOverlayMode,
                    onToggleSceneLock = vm::toggleSceneLock,
                )
            }
            // Kept clear of the tool rail on the right edge.
            ui.cameraError?.let {
                CameraErrorNotice(it, Modifier.align(Alignment.Center).padding(horizontal = CameraControlSize + Space.s))
            }
            CountdownNumber(countdown, Modifier.align(Alignment.Center))
        }
    }
}

/**
 * Places the tool rail on the right edge: centred on the HUD while that keeps it below the score badge
 * ([clearTop]), else centred in the space under the badge, else (a short screen) right under the badge.
 */
@Composable
private fun RailSlot(clearTop: Dp, content: @Composable () -> Unit) {
    Layout(content, Modifier.fillMaxSize()) { measurables, constraints ->
        val top = clearTop.roundToPx()
        val rail = measurables.first().measure(Constraints(maxWidth = constraints.maxWidth))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val height = constraints.maxHeight
            val centred = (height - rail.height) / 2
            val below = height - top
            val y = when {
                centred >= top -> centred
                rail.height <= below -> top + (below - rail.height) / 2
                else -> top
            }
            rail.place(constraints.maxWidth - rail.width, y)
        }
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

/** Below this height under the top bar, the dock goes compact. */
private val CompactDockBelow = 600.dp
private const val REAPPLY_DELAY_MILLIS = 600L
internal const val STORAGE_BLOCKED_NOTICE =
    "Allow storage access to save photos. Auto-capture and hand-raise are paused until then."
