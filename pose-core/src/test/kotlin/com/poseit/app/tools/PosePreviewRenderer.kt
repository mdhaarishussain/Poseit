package com.poseit.app.tools

import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.render.FigureShape
import com.poseit.app.pose.render.FigureShapes
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil

/**
 * JVM-only renderer that draws pose templates with Java2D so poses can be eyeballed as PNG contact
 * sheets without a device. Uses the same [FigureShapes] geometry as the in-app Compose renderer.
 */
object PosePreviewRenderer {

    fun renderSheet(poses: List<PoseTemplate>, file: File, cellHeight: Int = 360, columns: Int = 6, showJoints: Boolean = true) {
        if (poses.isEmpty()) return
        val labelH = 40
        val cells = poses.map { (it.frameAspect * cellHeight).toInt() to it }
        val cellW = cells.maxOf { it.first }
        val rows = ceil(poses.size / columns.toDouble()).toInt()
        val img = BufferedImage(columns * (cellW + 12) + 12, rows * (cellHeight + labelH + 12) + 12, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color(0x20, 0x22, 0x26)
        g.fillRect(0, 0, img.width, img.height)
        poses.forEachIndexed { i, pose ->
            val col = i % columns
            val row = i / columns
            val x0 = 12 + col * (cellW + 12)
            val y0 = 12 + row * (cellHeight + labelH + 12)
            val w = (pose.frameAspect * cellHeight).toInt()
            drawPose(g, pose, x0, y0, w, cellHeight, showJoints)
            g.color = Color.WHITE
            g.font = Font(Font.SANS_SERIF, Font.BOLD, 13)
            g.drawString(pose.name.take(32), x0, y0 + cellHeight + 16)
            g.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
            g.color = Color(0xB0, 0xB0, 0xB0)
            g.drawString(pose.id.take(40), x0, y0 + cellHeight + 32)
        }
        g.dispose()
        file.parentFile?.mkdirs()
        ImageIO.write(img, "png", file)
    }

    private val figureColors = listOf(Color(0xF5, 0xF5, 0xF5), Color(0xFF, 0xC8, 0x8A), Color(0x9A, 0xD0, 0xFF))

    fun drawPose(g: Graphics2D, pose: PoseTemplate, x0: Int, y0: Int, w: Int, h: Int, showJoints: Boolean) {
        // frame background (simulated photo) + rule-of-thirds
        g.color = Color(0x55, 0x6B, 0x7A)
        g.fillRect(x0, y0, w, h)
        g.color = Color(255, 255, 255, 40)
        g.stroke = BasicStroke(1f)
        for (k in 1..2) {
            g.drawLine(x0 + w * k / 3, y0, x0 + w * k / 3, y0 + h)
            g.drawLine(x0, y0 + h * k / 3, x0 + w, y0 + h * k / 3)
        }
        val clip = g.clip
        g.clipRect(x0, y0, w, h)
        pose.figures.forEachIndexed { idx, sk ->
            drawFigure(g, sk, pose.frameAspect, x0, y0, h, figureColors[idx % figureColors.size], showJoints)
        }
        g.clip = clip
    }

    fun drawFigure(g: Graphics2D, sk: Skeleton, aspect: Float, x0: Int, y0: Int, h: Int, color: Color, showJoints: Boolean) {
        val shapes = FigureShapes.build(sk, aspect)
        fun px(v: Float) = x0 + v * h
        fun py(v: Float) = y0 + v * h
        val fill = Color(color.red, color.green, color.blue, 215)
        for (s in shapes) {
            g.color = fill
            when (s) {
                is FigureShape.Capsule -> {
                    // approximate taper with average width
                    g.stroke = BasicStroke(((s.widthA + s.widthB) / 2f) * h, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                    g.draw(java.awt.geom.Line2D.Float(px(s.a.x), py(s.a.y), px(s.b.x), py(s.b.y)))
                }
                is FigureShape.Circle -> g.fill(Ellipse2D.Float(px(s.center.x - s.radius), py(s.center.y - s.radius), s.radius * 2 * h, s.radius * 2 * h))
                is FigureShape.Polygon -> {
                    val path = Path2D.Float()
                    s.points.forEachIndexed { i, p -> if (i == 0) path.moveTo(px(p.x), py(p.y)) else path.lineTo(px(p.x), py(p.y)) }
                    path.closePath()
                    g.fill(path)
                }
            }
        }
        FigureShapes.noseMarker(sk, aspect)?.let { n ->
            g.color = Color(0x30, 0x30, 0x30)
            g.fill(Ellipse2D.Float(px(n.center.x - n.radius), py(n.center.y - n.radius), n.radius * 2 * h, n.radius * 2 * h))
        }
        if (showJoints) {
            // Mark the subject's LEFT side joints red and RIGHT side blue so left/right mistakes are obvious.
            val iso = sk.toIsotropic(aspect)
            for ((j, p) in iso) {
                g.color = when (j.isLeft) { true -> Color(0xE5, 0x39, 0x35); false -> Color(0x1E, 0x88, 0xE5); null -> Color.DARK_GRAY }
                val r = 3.5f
                g.fill(Ellipse2D.Float(px(p.x) - r, py(p.y) - r, 2 * r, 2 * r))
            }
        }
    }
}
