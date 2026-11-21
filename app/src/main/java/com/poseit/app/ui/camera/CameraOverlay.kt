package com.poseit.app.ui.camera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.poseit.app.camera.DeviceTilt
import com.poseit.app.camera.Lens
import com.poseit.app.data.AppSettings
import com.poseit.app.data.GridType
import com.poseit.app.data.OverlayStyle
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.match.MatchResult
import com.poseit.app.pose.scene.OverlayTransform
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FigureStyle
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.components.fitFrameRect
import com.poseit.app.ui.components.drawFrameMarks
import com.poseit.app.ui.theme.PoseitColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/** |roll| below this counts as level (the horizon joins its side marks). */
private const val LEVEL_TOLERANCE_DEG = 1.5f

/** Opacity of the background-match snapshot. */
private const val SCENE_LOCK_ALPHA = 0.4f

/** The detected body as thin white lines, quieter than the guide figure. */
private const val LIVE_SKELETON_ALPHA = 0.7f

/** Viewfinder cream for the grid, level and focus marks, with a faint black line under it for bright scenes. */
private val MarkWhite = PoseitColors.cameraContent
private val MarkShade = PoseitColors.backgroundCamera.copy(alpha = 0.18f)

internal fun OverlayStyle.toFigureStyle(): FigureStyle = when (this) {
    OverlayStyle.SILHOUETTE -> FigureStyle.SILHOUETTE
    OverlayStyle.OUTLINE -> FigureStyle.OUTLINE
    OverlayStyle.STICK -> FigureStyle.STICK
}

/**
 * Everything drawn over the camera preview: background-match snapshot, grid, the pose guide (mannequin or
 * photo), the live skeleton, the level and the tap-to-focus ring. Also handles the overlay gestures:
 * drag / pinch / two-finger rotate the guide, double-tap resets it, a single tap focuses the camera.
 * With no pose (or a locked overlay) a pinch zooms the camera instead.
 *
 * High-frequency state (detections, transform, tilt) is only read inside the draw lambda, so new camera
 * frames redraw the canvas without recomposing.
 */
@Composable
internal fun PoseOverlay(
    vm: CameraViewModel,
    ui: CameraUiState,
    settings: AppSettings,
    onTapFocus: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val live = vm.live.collectAsState()
    val transform = vm.transform.collectAsState()
    val tilt = vm.tilt.collectAsState()
    // The lens actually bound (PoseCamera falls back to the other lens when the requested one doesn't exist):
    // the preview is mirrored according to this one, so the overlay must be too.
    val activeLens by vm.camera.lens.collectAsState()
    val onFocus by rememberUpdatedState(onTapFocus)

    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusToken by remember { mutableIntStateOf(0) }
    // 0..1 = ring shrinks onto the point, 1..2 = ring fades out.
    val focusAnim = remember { Animatable(0f) }
    LaunchedEffect(focusToken) {
        if (focusToken == 0) return@LaunchedEffect
        focusAnim.snapTo(0f)
        focusAnim.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
        delay(600)
        focusAnim.animateTo(2f, tween(durationMillis = 180))
        focusPoint = null
    }

    val flip = activeLens == Lens.FRONT
    val adjustable = ui.hasPose && !ui.overlayLocked

    Canvas(
        modifier
            .fillMaxSize()
            .pointerInput(adjustable) {
                detectTapGestures(
                    onTap = { p ->
                        focusPoint = p
                        focusToken++
                        onFocus(p)
                    },
                    onDoubleTap = if (adjustable) {
                        { vm.resetTransform() }
                    } else {
                        null
                    },
                )
            }
            .pointerInput(adjustable, flip) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (adjustable) {
                        vm.updateTransform {
                            OverlayMath.applyGesture(
                                it, pan.x, pan.y, zoom, rotation,
                                size.width.toFloat(), size.height.toFloat(), flip,
                            )
                        }
                    } else if (zoom != 1f) {
                        vm.zoomBy(zoom)
                    }
                }
            }
            .semantics {
                contentDescription = if (adjustable) {
                    "Camera preview with pose guide. Drag, pinch or twist with two fingers to move the guide, " +
                        "double-tap to reset it, tap to focus."
                } else {
                    "Camera preview. Tap to focus, pinch to zoom."
                }
            },
    ) {
        drawOverlay(
            vm = vm,
            ui = ui,
            settings = settings,
            flip = flip,
            live = live,
            transform = transform,
            tilt = tilt,
            focus = focusPoint,
            focusProgress = focusAnim.value,
        )
    }
}

private fun DrawScope.drawOverlay(
    vm: CameraViewModel,
    ui: CameraUiState,
    settings: AppSettings,
    flip: Boolean,
    live: State<LiveFrame>,
    transform: State<OverlayTransform>,
    tilt: State<DeviceTilt?>,
    focus: Offset?,
    focusProgress: Float,
) {
    ui.sceneLock?.let { drawSceneLock(it, flip) }
    drawGrid(settings.grid)

    // State reads below happen in the draw phase only (no recomposition per frame).
    val frame = live.value
    val pose = ui.pose
    if (pose != null && ui.hasPose) {
        val t = transform.value
        when (ui.overlayMode) {
            OverlayMode.PHOTO_GHOST -> ui.referenceImage?.let {
                drawGuideImage(it, pose, t, ui.poseMirrored, flip, alpha = settings.overlayOpacity)
            }
            OverlayMode.PHOTO_OUTLINE -> ui.outlineImage?.let {
                drawGuideImage(it, pose, t, ui.poseMirrored, flip, alpha = (settings.overlayOpacity * 1.5f).coerceIn(0.5f, 1f))
            }
            OverlayMode.MANNEQUIN -> {
                val figures = vm.templateScene(pose, t, ui.poseMirrored, ui.aspect)
                val match = frame.match.takeIf { frame.aspect == ui.aspect }
                drawTemplateFigures(figures, ui.aspect.ratio, match, settings.overlayStyle.toFigureStyle(), settings.overlayOpacity, flip)
            }
        }
    }

    if (settings.showLiveSkeleton && frame.aspect == ui.aspect) {
        drawLiveSkeletons(frame.displayPeople, ui.aspect.ratio, flip)
    }

    tilt.value?.let { drawLevel(it) }
    focus?.let { drawFocusRing(it, focusProgress) }
}

private fun DrawScope.drawTemplateFigures(
    figures: List<Skeleton>,
    aspect: Float,
    match: MatchResult?,
    style: FigureStyle,
    opacity: Float,
    flipX: Boolean,
) {
    val rect = Rect(Offset.Zero, size)
    figures.forEachIndexed { i, figure ->
        val figureMatch = match?.figures?.getOrNull(i)
        val partColors = if (figureMatch?.detectedIndex != null) {
            figureMatch.partScores.mapValues { (_, score) -> PoseitColors.forScore(score, onDark = true) }
        } else {
            emptyMap()
        }
        val neutral = PoseitColors.figureTones[i % PoseitColors.figureTones.size]
        drawPoseFigure(
            skeleton = figure,
            aspect = aspect,
            frameRect = rect,
            paint = FigurePaint(style = style, fill = neutral, partColors = partColors, alpha = opacity.coerceIn(0.05f, 1f)),
            flipX = flipX,
        )
    }
}

private fun DrawScope.drawLiveSkeletons(people: List<Skeleton>, aspect: Float, flipX: Boolean) {
    if (people.isEmpty()) return
    val rect = Rect(Offset.Zero, size)
    val paint = FigurePaint(
        style = FigureStyle.STICK,
        fill = PoseitColors.figureFill,
        alpha = LIVE_SKELETON_ALPHA,
        strokeWidthPx = 2.5.dp.toPx(),
        showFaceMarker = false,
    )
    people.forEach { drawPoseFigure(it, aspect, rect, paint, flipX = flipX) }
}

/**
 * Draws a reference photo / outline image into the template's fitted frame rectangle with the same overlay
 * transform the mannequin gets (mirror -> scale & rotate about the frame centre -> translate), then flips
 * everything for the mirrored front-camera preview.
 */
private fun DrawScope.drawGuideImage(
    image: ImageBitmap,
    pose: PoseTemplate,
    transform: OverlayTransform,
    mirrored: Boolean,
    flipX: Boolean,
    alpha: Float,
) {
    if (image.width <= 0 || image.height <= 0) return
    val frame = fitFrameRect(size, pose.frameAspect)
    val pivot = frame.center
    withTransform({
        if (flipX) scale(-1f, 1f, pivot = center)
        translate(transform.offsetX * size.width, transform.offsetY * size.height)
        rotate(transform.rotation, pivot)
        scale(transform.scale, transform.scale, pivot)
        if (mirrored) scale(-1f, 1f, pivot)
    }) {
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset(frame.left.roundToInt(), frame.top.roundToInt()),
            dstSize = IntSize(frame.width.roundToInt().coerceAtLeast(1), frame.height.roundToInt().coerceAtLeast(1)),
            alpha = alpha.coerceIn(0f, 1f),
            filterQuality = FilterQuality.Low,
        )
    }
}

/** The background-match snapshot, centre-cropped to the preview and mirrored like the preview. */
private fun DrawScope.drawSceneLock(image: ImageBitmap, flipX: Boolean) {
    if (image.width <= 0 || image.height <= 0 || size.width <= 0f || size.height <= 0f) return
    val srcAspect = image.width.toFloat() / image.height
    val dstAspect = size.width / size.height
    val (srcW, srcH) = if (srcAspect > dstAspect) {
        (image.height * dstAspect).roundToInt() to image.height
    } else {
        image.width to (image.width / dstAspect).roundToInt()
    }
    val srcOffset = IntOffset((image.width - srcW) / 2, (image.height - srcH) / 2)
    withTransform({ if (flipX) scale(-1f, 1f, pivot = center) }) {
        drawImage(
            image = image,
            srcOffset = srcOffset,
            srcSize = IntSize(srcW.coerceAtLeast(1), srcH.coerceAtLeast(1)),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            alpha = SCENE_LOCK_ALPHA,
            filterQuality = FilterQuality.Low,
        )
    }
}

private fun DrawScope.drawGrid(type: GridType) {
    if (type == GridType.NONE) return
    val line = MarkWhite.copy(alpha = 0.42f)
    val shadow = MarkShade
    val w = 1.dp.toPx()
    fun vertical(x: Float) {
        drawLine(shadow, Offset(x + w, 0f), Offset(x + w, size.height), w)
        drawLine(line, Offset(x, 0f), Offset(x, size.height), w)
    }
    fun horizontal(y: Float) {
        drawLine(shadow, Offset(0f, y + w), Offset(size.width, y + w), w)
        drawLine(line, Offset(0f, y), Offset(size.width, y), w)
    }
    when (type) {
        GridType.NONE -> Unit
        GridType.THIRDS -> {
            vertical(size.width / 3f); vertical(size.width * 2f / 3f)
            horizontal(size.height / 3f); horizontal(size.height * 2f / 3f)
        }
        GridType.GOLDEN -> {
            val g = 0.382f
            vertical(size.width * g); vertical(size.width * (1f - g))
            horizontal(size.height * g); horizontal(size.height * (1f - g))
        }
        GridType.CENTER -> {
            vertical(size.width / 2f)
            horizontal(size.height / 2f)
            drawCircle(line, radius = 20.dp.toPx(), center = center, style = Stroke(w))
        }
    }
}

/**
 * Horizon line rotated against the device roll, between two fixed side marks. When the phone is level the line
 * snaps straight and joins the marks into one solid line, so the state is shown by shape, not by colour.
 */
private fun DrawScope.drawLevel(tilt: DeviceTilt) {
    // Lying flat (camera pointing at the floor/sky) the roll is meaningless.
    if (abs(tilt.pitchDegrees) > 70f || !tilt.rollDegrees.isFinite()) return
    val roll = tilt.rollDegrees
    val level = abs(roll) < LEVEL_TOLERANCE_DEG
    val half = size.minDimension * 0.15f
    val gap = 8.dp.toPx()
    val tick = 14.dp.toPx()
    val stroke = 2.dp.toPx()
    val shade = stroke + 2.dp.toPx()
    val c = center
    val outer = half + gap + tick
    if (level) {
        drawLine(MarkShade, Offset(c.x - outer, c.y), Offset(c.x + outer, c.y), shade, cap = StrokeCap.Round)
        drawLine(MarkWhite, Offset(c.x - outer, c.y), Offset(c.x + outer, c.y), stroke, cap = StrokeCap.Round)
    } else {
        val ref = MarkWhite.copy(alpha = 0.55f)
        drawLine(ref, Offset(c.x - outer, c.y), Offset(c.x - half - gap, c.y), stroke, cap = StrokeCap.Round)
        drawLine(ref, Offset(c.x + half + gap, c.y), Offset(c.x + outer, c.y), stroke, cap = StrokeCap.Round)
        rotate(degrees = -roll.coerceIn(-45f, 45f), pivot = c) {
            drawLine(MarkShade, Offset(c.x - half, c.y), Offset(c.x + half, c.y), shade, cap = StrokeCap.Round)
            drawLine(MarkWhite, Offset(c.x - half, c.y), Offset(c.x + half, c.y), stroke, cap = StrokeCap.Round)
        }
    }
}

/**
 * Tap-to-focus mark: viewfinder corner brackets (the frame marks) that close in on the point, hold, then
 * fade out.
 */
private fun DrawScope.drawFocusRing(point: Offset, progress: Float) {
    val shrink = progress.coerceIn(0f, 1f)
    val alpha = (2f - progress).coerceIn(0f, 1f)
    if (alpha <= 0f) return
    val from = 44.dp.toPx()
    val half = from + (30.dp.toPx() - from) * shrink
    val box = Rect(point.x - half, point.y - half, point.x + half, point.y + half)
    val length = 12.dp.toPx()
    drawFrameMarks(box.inflate(1.dp.toPx()), MarkShade.copy(alpha = MarkShade.alpha * alpha), 3.5.dp.toPx(), length + 2.dp.toPx())
    drawFrameMarks(box, MarkWhite.copy(alpha = alpha), 1.5.dp.toPx(), length)
}
