package com.poseit.app.pose

import com.poseit.app.pose.dsl.FigureBuilder
import com.poseit.app.pose.dsl.pose
import com.poseit.app.pose.scene.SceneMapping

/** Helpers shared by the engine tests. Templates are authored with the DSL in a 3:4 frame = 3:4 scene. */
object TestFixtures {
    const val ASPECT = 3f / 4f

    /** One DSL figure mapped into a 3:4 scene (identity mapping). */
    fun figure(
        cx: Float = 0.5f,
        bottom: Float? = null,
        top: Float? = null,
        bodyHeight: Float = 0.82f,
        block: FigureBuilder.() -> Unit = {},
    ): Skeleton {
        val template = pose("test", "Test", PoseCategory.STANDING) {
            figure(cx = cx, bottom = bottom, top = top, bodyHeight = bodyHeight, block = block)
        }
        return SceneMapping.templateToScene(template, ASPECT).single()
    }

    /** What the detector would report for exactly this pose: no unit, high confidence (far-side head points stay low). */
    fun detected(s: Skeleton, confidence: Float = 0.95f): Skeleton = Skeleton(
        points = s.points,
        visibility = s.points.keys.associateWith { j -> minOf(confidence, s.visibility[j] ?: 1f) },
        unit = 0f,
    )
}

fun Skeleton.shifted(dx: Float, dy: Float): Skeleton = mapPoints { Vec2(it.x + dx, it.y + dy) }

fun Skeleton.without(vararg joints: Joint): Skeleton =
    Skeleton(points - joints.toSet(), visibility - joints.toSet(), unit)

/** Uniform scale about [center] (the same factor on both normalized axes keeps proportions). */
fun Skeleton.scaledAbout(center: Vec2, factor: Float): Skeleton = mapPoints { p ->
    Vec2(center.x + (p.x - center.x) * factor, center.y + (p.y - center.y) * factor)
}

/** Centre of the four torso joints (scene units). */
fun Skeleton.torsoCentre(): Vec2 {
    val pts = listOf(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER, Joint.LEFT_HIP, Joint.RIGHT_HIP).map { points.getValue(it) }
    return Vec2(pts.map { it.x }.average().toFloat(), pts.map { it.y }.average().toFloat())
}
