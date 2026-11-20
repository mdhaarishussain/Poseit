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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
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
import com.poseit.app.pose.BodyPart
import com.poseit.app.pose.Joint
import com.poseit.app.pose.Vec2
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FigureStyle
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.components.fitFrameRect
import com.poseit.app.ui.theme.PoseitColors
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_VIEW_ZOOM = 4f

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
 * The editing surface: the frame with the figures drawn large, draggable joint handles for the selected figure,
 * tap-to-select for multi-person poses, and two-finger zoom / pan of the view for precise work.
 */
@Composable
internal fun EditorCanvas(viewModel: PoseEditorViewModel, modifier: Modifier = Modifier) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val paddingPx = with(density) { 18.dp.toPx() }
    val rotatePainter = rememberVectorPainter(Icons.Rounded.Sync)

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
            dimOutsideFrame(vp.frame)
            drawHandles(vp, viewModel, rotatePainter)
        }

        // What is being dragged (helps with left/right, which is anatomical).
        val active = viewModel.activeTarget
        AnimatedVisibility(
            visible = active != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
        ) {
            Text(
                text = active?.label().orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        AnimatedVisibility(
            visible = zoom > 1f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            AssistChip(
                onClick = {
                    zoom = 1f
                    pan = Offset.Zero
                },
                label = { Text("Fit") },
                leadingIcon = { Icon(Icons.Rounded.ZoomOutMap, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            )
        }
    }
}

private val FrameGradient = Brush.verticalGradient(listOf(Color(0xFF2B2E3A), Color(0xFF1C1E25)))

private fun DrawScope.drawFrame(vp: EditorViewport, viewModel: PoseEditorViewModel) {
    val frame = vp.frame
    drawRect(FrameGradient, topLeft = frame.topLeft, size = frame.size)
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
            val fine = Color.White.copy(alpha = 0.05f)
            val thirds = Color.White.copy(alpha = 0.16f)
            val thin = 1.dp.toPx()
            for (i in 1 until 12) {
                if (i % 4 == 0) continue
                val x = frame.left + frame.width * i / 12f
                val y = frame.top + frame.height * i / 12f
                drawLine(fine, Offset(x, frame.top), Offset(x, frame.bottom), thin)
                drawLine(fine, Offset(frame.left, y), Offset(frame.right, y), thin)
            }
            for (i in 1..2) {
                val x = frame.left + frame.width * i / 3f
                val y = frame.top + frame.height * i / 3f
                drawLine(thirds, Offset(x, frame.top), Offset(x, frame.bottom), thin)
                drawLine(thirds, Offset(frame.left, y), Offset(frame.right, y), thin)
            }
        }
    }
}

private fun DrawScope.drawFigures(vp: EditorViewport, viewModel: PoseEditorViewModel) {
    val figures = viewModel.figures
    val selected = viewModel.selected
    val multi = figures.size > 1
    figures.forEachIndexed { i, skeleton ->
        val isSelected = i == selected
        val base = PoseitColors.FigureTints[i % PoseitColors.FigureTints.size]
        // Tint left / right limbs of the figure being edited like its handles (left = coral, right = violet).
        val sideTints = if (isSelected) {
            val left = lerp(base, PoseitColors.Coral, 0.30f)
            val right = lerp(base, PoseitColors.Violet, 0.30f)
            mapOf(BodyPart.LEFT_ARM to left, BodyPart.LEFT_LEG to left, BodyPart.RIGHT_ARM to right, BodyPart.RIGHT_LEG to right)
        } else {
            emptyMap()
        }
        drawPoseFigure(
            skeleton,
            viewModel.aspect,
            vp.frame,
            FigurePaint(
                style = FigureStyle.SILHOUETTE,
                fill = base,
                partColors = sideTints,
                alpha = if (isSelected || !multi) 1f else 0.45f,
            ),
        )
    }
}

private fun DrawScope.dimOutsideFrame(frame: Rect) {
    val dim = Color.Black.copy(alpha = 0.45f)
    val w = size.width
    val h = size.height
    if (frame.top > 0f) drawRect(dim, Offset.Zero, Size(w, frame.top))
    if (frame.bottom < h) drawRect(dim, Offset(0f, frame.bottom), Size(w, h - frame.bottom))
    val top = frame.top.coerceAtLeast(0f)
    val bottom = frame.bottom.coerceAtMost(h)
    if (bottom > top) {
        if (frame.left > 0f) drawRect(dim, Offset(0f, top), Size(frame.left, bottom - top))
        if (frame.right < w) drawRect(dim, Offset(frame.right, top), Size(w - frame.right, bottom - top))
    }
    drawRect(Color.White.copy(alpha = 0.22f), frame.topLeft, frame.size, style = Stroke(1.dp.toPx()))
}

private fun DrawScope.drawHandles(
    vp: EditorViewport,
    viewModel: PoseEditorViewModel,
    rotatePainter: androidx.compose.ui.graphics.painter.Painter,
) {
    val skeleton = viewModel.figures.getOrNull(viewModel.selected) ?: return
    val active = viewModel.activeTarget
    val ring = Color(0xFF15161A)
    val handles = SkeletonEditing.handles(skeleton, viewModel.aspect)
    // Joints first, then head and the hips badge on top.
    val ordered = handles.sortedBy { if (it.target is DragTarget.JointTarget) 0 else 1 }
    for (handle in ordered) {
        val center = vp.toPx(handle.position)
        val color = when (val t = handle.target) {
            is DragTarget.JointTarget -> when (t.joint.isLeft) {
                true -> PoseitColors.Coral
                false -> PoseitColors.Violet
                null -> Color.White
            }
            DragTarget.Lean -> PoseitColors.Mint
            else -> Color.White
        }
        val isActive = handle.target == active
        if (isActive) drawCircle(color.copy(alpha = 0.3f), radius = 22.dp.toPx(), center = center)
        if (handle.target == DragTarget.Lean) {
            val r = 10.dp.toPx()
            drawCircle(ring.copy(alpha = 0.85f), radius = r, center = center)
            drawCircle(color, radius = r, center = center, style = Stroke(1.5.dp.toPx()))
            val icon = 13.dp.toPx()
            translate(center.x - icon / 2f, center.y - icon / 2f) {
                with(rotatePainter) { draw(Size(icon, icon), colorFilter = ColorFilter.tint(color)) }
            }
        } else {
            val r = if (handle.target == DragTarget.Head) 7.dp.toPx() else 6.dp.toPx()
            drawCircle(ring, radius = r + 2.dp.toPx(), center = center)
            drawCircle(color, radius = r, center = center)
        }
    }
}
