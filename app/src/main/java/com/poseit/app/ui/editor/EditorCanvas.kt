package com.poseit.app.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.poseit.app.pose.Joint
import com.poseit.app.pose.Vec2
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FigureStyle
import com.poseit.app.ui.components.drawFrameMarks
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.components.fitFrameRect
import com.poseit.app.ui.components.IconSize
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_VIEW_ZOOM = 4f

/** Space between the canvas edge and the photo frame at zoom 1. */
private val FramePadding = Space.xl

/** Maps isotropic frame units (x * aspect, y; see [SkeletonEditing]) to canvas pixels and back. */
internal class EditorViewport(val frame: Rect) {
    fun toPx(p: Vec2) = Offset(frame.left + p.x * frame.height, frame.top + p.y * frame.height)
    fun toIso(o: Offset) = Vec2((o.x - frame.left) / frame.height, (o.y - frame.top) / frame.height)
}

/** The frame rectangle inside a canvas of [size], letterboxed with [paddingPx], then zoomed / panned by the view. */
internal fun editorViewport(size: Size, aspect: Float, paddingPx: Float, zoom: Float, pan: Offset): EditorViewport {
    val inner = Size(max(1f, size.width - 2 * paddingPx), max(1f, size.height - 2 * paddingPx))
    val base = fitFrameRect(inner, aspect).translate(paddingPx, paddingPx)
    val center = Offset(size.width / 2f, size.height / 2f)
    val topLeft = center + (base.topLeft - center) * zoom + pan
    return EditorViewport(Rect(topLeft, base.size * zoom))
}

/** Human label of what a drag manipulates. */
internal fun DragTarget.label(): String = when (this) {
    DragTarget.Head -> "Head"
    DragTarget.Lean -> "Upper body"
    DragTarget.Move -> "Whole figure"
    is DragTarget.JointTarget -> when (joint) {
        Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER -> "Shoulders"
        Joint.LEFT_HIP, Joint.RIGHT_HIP -> "Hips"
        else -> joint.label.replaceFirstChar { it.uppercase() }
    }
}

/**
 * The editing surface, drawn straight onto the dark stage of the card it sits in (no fill of its own): the photo
 * frame marked by cream viewfinder frame marks, the figures drawn large, draggable joint handles for the selected
 * figure, tap-to-select for multi-person poses, and two-finger zoom / pan of the view for precise work.
 */
@Composable
internal fun EditorCanvas(viewModel: PoseEditorViewModel, modifier: Modifier = Modifier) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val paddingPx = with(density) { FramePadding.toPx() }
    val rotatePainter = rememberVectorPainter(Icons.Outlined.Sync)

    Box(modifier.clipToBounds()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Pose editing canvas. Drag the dots to bend joints, drag the body to move it." }
                .pointerInput(viewModel) {
                    val handleRadiusPx = 24.dp.toPx()
                    fun viewport() = editorViewport(size.toSize(), viewModel.aspect, paddingPx, zoom, pan)
                    fun clampPan(p: Offset, z: Float) = Offset(
                        p.x.coerceIn(-size.width * z / 2f, size.width * z / 2f),
                        p.y.coerceIn(-size.height * z / 2f, size.height * z / 2f),
                    )
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startViewport = viewport()
                        var editing = viewModel.onGestureStart(startViewport.toIso(down.position), handleRadiusPx / startViewport.frame.height)
                        if (editing) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            down.consume()
                        }
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            when {
                                pressed.size >= 2 -> {
                                    // Two fingers zoom / pan the view; any edit in progress is abandoned.
                                    if (editing) {
                                        viewModel.onGestureCancel()
                                        editing = false
                                    }
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    if (centroid.isSpecified) {
                                        val newZoom = (zoom * event.calculateZoom()).coerceIn(1f, MAX_VIEW_ZOOM)
                                        val c = centroid - Offset(size.width / 2f, size.height / 2f)
                                        pan = clampPan(c - (c - pan) * (newZoom / zoom) + event.calculatePan(), newZoom)
                                        zoom = newZoom
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                                editing -> {
                                    val change = pressed.firstOrNull { it.id == down.id } ?: pressed.first()
                                    viewModel.onGestureMove(viewport().toIso(change.position))
                                    change.consume()
                                }
                                zoom > 1f -> {
                                    val change = pressed.first()
                                    pan = clampPan(pan + change.positionChange(), zoom)
                                    change.consume()
                                }
                            }
                        }
                        if (editing) viewModel.onGestureEnd()
                        if (zoom < 1.03f) {
                            zoom = 1f
                            pan = Offset.Zero
                        }
                    }
                },
        ) {
            val vp = editorViewport(size, viewModel.aspect, paddingPx, zoom, pan)
            drawFrame(vp, viewModel)
            drawFigures(vp, viewModel)
            fadeOutsideFrame(vp.frame)
            drawFrameMarks(vp.frame)
            drawHandles(vp, viewModel, rotatePainter)
        }

        // What is being dragged (helps with left/right, which is anatomical).
        val active = viewModel.activeTarget
        AnimatedVisibility(
            visible = active != null,
            enter = fadeIn(PoseitMotion.standard(PoseitMotion.FAST)),
            exit = fadeOut(PoseitMotion.standard(PoseitMotion.FAST)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = Space.s),
        ) {
            // A small cream cue card, legible over the figure and the stage alike.
            Text(
                text = active?.label().orEmpty(),
                style = PoseitType.label,
                color = PoseitColors.textPrimary,
                modifier = Modifier
                    .background(PoseitColors.backgroundSurface, PoseitShapes.Inner)
                    .padding(horizontal = Space.m, vertical = Space.xs),
            )
        }
        AnimatedVisibility(
            visible = zoom > 1f,
            enter = fadeIn(PoseitMotion.standard(PoseitMotion.FAST)),
            exit = fadeOut(PoseitMotion.standard(PoseitMotion.FAST)),
            modifier = Modifier.align(Alignment.TopEnd).padding(end = Space.s),
        ) {
            // A cream pill on the stage: the way back to the whole frame after zooming in.
            Surface(
                onClick = {
                    zoom = 1f
                    pan = Offset.Zero
                },
                shape = PoseitShapes.Pill,
                color = PoseitColors.backgroundSurface,
                contentColor = PoseitColors.textPrimary,
            ) {
                Row(
                    Modifier
                        .heightIn(min = 32.dp)
                        .padding(horizontal = Space.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.ZoomOutMap, contentDescription = null, modifier = Modifier.size(IconSize.inline))
                    Spacer(Modifier.width(Space.s))
                    Text("Fit", style = PoseitType.label)
                }
            }
        }
    }
}

/** Rule-of-thirds lines: cream at 16%, quiet on the dark stage. */
private val GridLine = PoseitColors.borderInverted

/** The reference photo (when shown) and the optional rule-of-thirds grid, inside the frame. */
private fun DrawScope.drawFrame(vp: EditorViewport, viewModel: PoseEditorViewModel) {
    val frame = vp.frame
    val reference = viewModel.referenceBitmap
    if (reference != null && viewModel.showReference) {
        drawImage(
            reference,
            dstOffset = IntOffset(frame.left.roundToInt(), frame.top.roundToInt()),
            dstSize = IntSize(frame.width.roundToInt().coerceAtLeast(1), frame.height.roundToInt().coerceAtLeast(1)),
            alpha = 0.45f,
        )
    }
    if (viewModel.showGrid) {
        clipRect(frame.left, frame.top, frame.right, frame.bottom) {
            val thin = 1.dp.toPx()
            for (i in 1..2) {
                val x = frame.left + frame.width * i / 3f
                val y = frame.top + frame.height * i / 3f
                drawLine(GridLine, Offset(x, frame.top), Offset(x, frame.bottom), thin)
                drawLine(GridLine, Offset(frame.left, y), Offset(frame.right, y), thin)
            }
        }
    }
}

/** Lay figures in their tones; with several people, the ones not being edited are faded. */
private fun DrawScope.drawFigures(vp: EditorViewport, viewModel: PoseEditorViewModel) {
    val figures = viewModel.figures
    val selected = viewModel.selected
    val multi = figures.size > 1
    figures.forEachIndexed { i, skeleton ->
        val isSelected = i == selected
        drawPoseFigure(
            skeleton,
            viewModel.aspect,
            vp.frame,
            FigurePaint(
                style = FigureStyle.SILHOUETTE,
                fill = PoseitColors.figureTones[i % PoseitColors.figureTones.size],
                alpha = if (isSelected || !multi) 1f else 0.45f,
                // No face dot here: the head's drag handle already shows where the head points, and a dot beside it
                // reads as a stray handle.
                showFaceMarker = false,
            ),
        )
    }
}

/** Washes the stage back over whatever lies outside the frame, so cropped-off limbs read as outside the photo. */
private fun DrawScope.fadeOutsideFrame(frame: Rect) {
    val wash = PoseitColors.backgroundStage.copy(alpha = 0.72f)
    val w = size.width
    val h = size.height
    if (frame.top > 0f) drawRect(wash, Offset.Zero, Size(w, frame.top))
    if (frame.bottom < h) drawRect(wash, Offset(0f, frame.bottom), Size(w, h - frame.bottom))
    val top = frame.top.coerceAtLeast(0f)
    val bottom = frame.bottom.coerceAtMost(h)
    if (bottom > top) {
        if (frame.left > 0f) drawRect(wash, Offset(0f, top), Size(frame.left, bottom - top))
        if (frame.right < w) drawRect(wash, Offset(frame.right, top), Size(w - frame.right, bottom - top))
    }
}

/**
 * Joint handles of the selected figure, told apart by tone rather than hue, so they read on the cream figure and on
 * the dark stage alike: the figure's left joints are ink dots with a cream ring, its right (and centre) joints cream
 * dots with an ink ring. The handle being dragged turns lavender (ink ring) inside a soft lavender halo. The hips
 * handle (rotate the upper body) is a cream disc with an ink ring and rotate glyph, lavender while dragged.
 */
private fun DrawScope.drawHandles(
    vp: EditorViewport,
    viewModel: PoseEditorViewModel,
    rotatePainter: Painter,
) {
    val skeleton = viewModel.figures.getOrNull(viewModel.selected) ?: return
    val active = viewModel.activeTarget
    val ring = 1.5.dp.toPx()
    val handles = SkeletonEditing.handles(skeleton, viewModel.aspect)
    // Joints first, then head and the hips badge on top.
    val ordered = handles.sortedBy { if (it.target is DragTarget.JointTarget) 0 else 1 }
    for (handle in ordered) {
        val center = vp.toPx(handle.position)
        val isActive = handle.target == active
        if (isActive) drawCircle(PoseitColors.accent.copy(alpha = 0.35f), radius = 20.dp.toPx(), center = center)
        if (handle.target == DragTarget.Lean) {
            val r = 11.dp.toPx()
            drawCircle(if (isActive) PoseitColors.accent else PoseitColors.figureFill, radius = r, center = center)
            drawCircle(PoseitColors.textPrimary, radius = r - ring / 2f, center = center, style = Stroke(ring))
            val icon = 14.dp.toPx()
            translate(center.x - icon / 2f, center.y - icon / 2f) {
                with(rotatePainter) {
                    draw(Size(icon, icon), colorFilter = ColorFilter.tint(PoseitColors.textPrimary))
                }
            }
        } else {
            val r = if (handle.target == DragTarget.Head) 6.dp.toPx() else 5.dp.toPx()
            val left = (handle.target as? DragTarget.JointTarget)?.joint?.isLeft == true
            val dot = when {
                isActive -> PoseitColors.accent
                left -> PoseitColors.textPrimary
                else -> PoseitColors.figureFill
            }
            val outer = if (left && !isActive) PoseitColors.figureFill else PoseitColors.textPrimary
            drawCircle(outer, radius = r + ring, center = center)
            drawCircle(dot, radius = r, center = center)
        }
    }
}
