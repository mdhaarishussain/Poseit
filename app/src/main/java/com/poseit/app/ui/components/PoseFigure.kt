package com.poseit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import com.poseit.app.pose.BodyPart
import com.poseit.app.pose.Bone
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.render.FigureShape
import com.poseit.app.pose.render.FigureShapes
import com.poseit.app.ui.theme.PoseitColors
import kotlin.math.max

enum class FigureStyle { SILHOUETTE, OUTLINE, STICK }

/**
 * How to paint a figure. The defaults are the v2 lay figure, which reads on both the dark stage (by its cream fill)
 * and the cream canvas (by its contour): [PoseitColors.figureFill] fill, [PoseitColors.figureContour] contour and a
 * [PoseitColors.figureFace] dot.
 * @param fill body colour (SILHOUETTE) / line colour (OUTLINE, STICK) when no part colour is given
 * @param outline colour of the contour drawn around a silhouette (Color.Transparent = none)
 * @param partColors optional per-body-part colours (e.g. match feedback); overrides [fill]
 * @param alpha opacity of the whole figure (applied once, so overlapping limbs don't get darker)
 * @param strokeWidthPx contour / line width in pixels (0 = automatic, relative to body size)
 * @param faceColor the dot that shows which way a SILHOUETTE faces (OUTLINE draws it in the head's line colour)
 */
data class FigurePaint(
    val style: FigureStyle = FigureStyle.SILHOUETTE,
    val fill: Color = PoseitColors.figureFill,
    val outline: Color = PoseitColors.figureContour,
    val partColors: Map<BodyPart, Color> = emptyMap(),
    val alpha: Float = 1f,
    val strokeWidthPx: Float = 0f,
    val showFaceMarker: Boolean = true,
    val faceColor: Color = PoseitColors.figureFace,
)

/**
 * Draws [skeleton] whose coordinates are normalized to a frame of aspect [aspect] (width/height) that is drawn
 * at [frameRect] (pixels). With [flipX] the figure is mirrored for display (front-camera preview) WITHOUT
 * changing which limbs are coloured as left/right.
 */
fun DrawScope.drawPoseFigure(
    skeleton: Skeleton,
    aspect: Float,
    frameRect: Rect,
    paint: FigurePaint,
    flipX: Boolean = false,
) {
    val shapes = FigureShapes.build(skeleton, aspect)
    if (shapes.isEmpty()) return
    val h = frameRect.height
    fun px(p: Vec2): Offset = if (flipX) Offset(frameRect.right - p.x * h, frameRect.top + p.y * h) else Offset(frameRect.left + p.x * h, frameRect.top + p.y * h)
    val unitPx = skeleton.estimatedUnit(aspect) * h
    // The contour grows with the figure but stays a fine line: at least 1.25dp so a small tile keeps a visible
    // edge, at most 2.25dp so a big figure (onboarding, detail, editor) looks crisp instead of outlined in marker.
    val autoStroke = if (paint.strokeWidthPx > 0f) paint.strokeWidthPx else (unitPx * 0.03f).coerceIn(1.25.dp.toPx(), 2.25.dp.toPx())

    fun colorFor(part: BodyPart): Color = paint.partColors[part] ?: paint.fill

    when (paint.style) {
        FigureStyle.STICK -> drawStick(skeleton, aspect, ::px, unitPx, paint, ::colorFor)
        FigureStyle.SILHOUETTE, FigureStyle.OUTLINE -> {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = paint.alpha.coerceIn(0f, 1f) })
                // 1) contour: every shape inflated by the stroke width. A translucent silhouette contour is drawn
                //    opaque into its own layer and faded as a whole, so overlapping shapes don't darken the joints.
                val contourColor = if (paint.style == FigureStyle.OUTLINE) null else paint.outline
                if (paint.style == FigureStyle.OUTLINE || paint.outline.alpha > 0f) {
                    val fadeContour = contourColor != null && contourColor.alpha < 1f
                    if (fadeContour) canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = contourColor!!.alpha })
                    val solid = contourColor?.copy(alpha = 1f)
                    for (s in shapes) drawShape(s, ::px, h, inflatePx = autoStroke, color = solid ?: colorFor(s.part))
                    if (fadeContour) canvas.restore()
                }
                // 2) body: fill (silhouette) or punch a hole (outline)
                for (s in shapes) {
                    if (paint.style == FigureStyle.OUTLINE) {
                        drawShape(s, ::px, h, inflatePx = 0f, color = PoseitColors.textPrimary, blendMode = BlendMode.Clear)
                    } else {
                        drawShape(s, ::px, h, inflatePx = 0f, color = colorFor(s.part))
                    }
                }
                if (paint.showFaceMarker) {
                    FigureShapes.noseMarker(skeleton, aspect)?.let { n ->
                        val c = if (paint.style == FigureStyle.OUTLINE) colorFor(BodyPart.HEAD) else paint.faceColor
                        drawCircle(c, radius = max(2f, n.radius * h), center = px(n.center))
                    }
                }
                canvas.restore()
            }
        }
    }
}

private fun DrawScope.drawShape(
    s: FigureShape,
    px: (Vec2) -> Offset,
    h: Float,
    inflatePx: Float,
    color: Color,
    blendMode: BlendMode = BlendMode.SrcOver,
) {
    when (s) {
        is FigureShape.Circle -> drawCircle(color, radius = s.radius * h + inflatePx, center = px(s.center), blendMode = blendMode)
        is FigureShape.Capsule -> {
            val a = px(s.a)
            val b = px(s.b)
            val ra = s.widthA * h / 2f + inflatePx
            val rb = s.widthB * h / 2f + inflatePx
            drawTaperedCapsule(a, b, ra, rb, color, blendMode)
        }
        is FigureShape.Polygon -> {
            val path = Path()
            s.points.forEachIndexed { i, p ->
                val o = px(p)
                if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            path.close()
            drawPath(path, color, blendMode = blendMode)
            if (inflatePx > 0f) {
                drawPath(path, color, style = Stroke(width = inflatePx * 2f, join = androidx.compose.ui.graphics.StrokeJoin.Round), blendMode = blendMode)
            }
        }
    }
}

/** A stick with round ends of radius [ra] at [a] and [rb] at [b]. */
private fun DrawScope.drawTaperedCapsule(a: Offset, b: Offset, ra: Float, rb: Float, color: Color, blendMode: BlendMode) {
    drawCircle(color, ra, a, blendMode = blendMode)
    drawCircle(color, rb, b, blendMode = blendMode)
    val d = b - a
    val len = d.getDistance()
    if (len < 0.5f) return
    val nx = -d.y / len
    val ny = d.x / len
    val path = Path().apply {
        moveTo(a.x + nx * ra, a.y + ny * ra)
        lineTo(b.x + nx * rb, b.y + ny * rb)
        lineTo(b.x - nx * rb, b.y - ny * rb)
        lineTo(a.x - nx * ra, a.y - ny * ra)
        close()
    }
    drawPath(path, color, blendMode = blendMode)
}

private fun DrawScope.drawStick(
    skeleton: Skeleton,
    aspect: Float,
    px: (Vec2) -> Offset,
    unitPx: Float,
    paint: FigurePaint,
    colorFor: (BodyPart) -> Color,
) {
    val iso = skeleton.toIsotropic(aspect)
    val w = if (paint.strokeWidthPx > 0f) paint.strokeWidthPx else (unitPx * 0.09f).coerceIn(1.5.dp.toPx(), 3.dp.toPx())
    val alpha = paint.alpha
    for (bone in Bone.entries) {
        val a = iso[bone.from] ?: continue
        val b = iso[bone.to] ?: continue
        drawLine(colorFor(bone.part).copy(alpha = alpha), px(a), px(b), strokeWidth = w, cap = StrokeCap.Round)
    }
    val ms = skeleton.midShoulder()?.let { Vec2(it.x * aspect, it.y) }
    val head = FigureShapes.headCenter(iso, skeleton.estimatedUnit(aspect), ms)
    if (head != null) {
        if (ms != null) drawLine(colorFor(BodyPart.HEAD).copy(alpha = alpha), px(ms), px(head), strokeWidth = w, cap = StrokeCap.Round)
        drawCircle(colorFor(BodyPart.HEAD).copy(alpha = alpha), radius = unitPx * 0.2f, center = px(head), style = Stroke(w))
    }
    for ((j, p) in iso) {
        if (j in Joint.FACE) continue
        val part = when {
            j.isLeft == null -> BodyPart.HEAD
            j.name.contains("SHOULDER") || j.name.contains("HIP") -> BodyPart.TORSO
            j.name.contains("ELBOW") || j.name.contains("WRIST") || j.name.contains("HAND") -> if (j.isLeft == true) BodyPart.LEFT_ARM else BodyPart.RIGHT_ARM
            else -> if (j.isLeft == true) BodyPart.LEFT_LEG else BodyPart.RIGHT_LEG
        }
        drawCircle(colorFor(part).copy(alpha = alpha), radius = w * 0.75f, center = px(p))
    }
}

/** Fits a frame of [aspect] (contain, centred) inside [size]. */
fun fitFrameRect(size: Size, aspect: Float): Rect {
    val viewAspect = size.width / size.height
    return if (viewAspect > aspect) {
        val w = size.height * aspect
        Rect(Offset((size.width - w) / 2f, 0f), Size(w, size.height))
    } else {
        val h = size.width / aspect
        Rect(Offset(0f, (size.height - h) / 2f), Size(size.width, h))
    }
}

/**
 * A whole pose template (all figures) standing on the stage, each figure on a flat floor spot. Respects the
 * template's frame aspect; letterboxed inside the given modifier bounds. People in a multi-person pose take
 * [PoseitColors.figureTones] in order.
 * @param background fill behind the figures, the dark stage ([PoseitColors.backgroundStage]) by default; null draws
 *   nothing (the caller paints it)
 * @param showGround draws the floor spots
 * @param floorColor the floor spot: [PoseitColors.figureFloorOnDark] on the stage, [PoseitColors.figureFloorOnLight]
 *   on cream
 */
@Composable
fun PoseThumbnail(
    template: PoseTemplate,
    modifier: Modifier = Modifier,
    style: FigureStyle = FigureStyle.SILHOUETTE,
    mirrored: Boolean = false,
    background: Brush? = StageBrush,
    showGround: Boolean = true,
    floorColor: Color = PoseitColors.figureFloorOnDark,
) {
    val figures = remember(template, mirrored) { if (mirrored) template.figures.map { it.mirrored() } else template.figures }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .aspectRatio(template.frameAspect, matchHeightConstraintsFirst = maxWidth / maxHeight > template.frameAspect)
                .let { if (background != null) it.background(background) else it },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val rect = Rect(Offset.Zero, size)
                clipRect {
                    if (showGround) figures.forEach { drawFloorSpot(it, floorColor) }
                    figures.forEachIndexed { i, sk ->
                        drawPoseFigure(
                            sk, template.frameAspect, rect,
                            FigurePaint(style = style, fill = PoseitColors.figureTones[i % PoseitColors.figureTones.size]),
                        )
                    }
                }
            }
        }
    }
}

private val StageBrush = SolidColor(PoseitColors.backgroundStage)

/** A flat oval under the feet that puts the figure on the floor (v2 has no shadows and no gradients). */
private fun DrawScope.drawFloorSpot(sk: Skeleton, color: Color) {
    val l = sk[Joint.LEFT_ANKLE] ?: return
    val r = sk[Joint.RIGHT_ANKLE] ?: return
    if (l.y !in 0f..1.02f) return
    val center = Offset((l.x + r.x) / 2f * size.width, max(l.y, r.y) * size.height + size.height * 0.014f)
    val rx = size.width * 0.17f
    val ry = rx * 0.16f
    drawOval(color, topLeft = Offset(center.x - rx, center.y - ry), size = Size(rx * 2f, ry * 2f))
}
