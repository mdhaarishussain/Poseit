package com.poseit.app.tools

import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.dsl.pose
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.pose.render.FigureShape
import com.poseit.app.pose.render.FigureShapes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Exports the real pose library for the promo film (promo/, Remotion).
 *
 * Writes `promo/public/poses.json`: every [BuiltInPoses.all] template (id, name, category, frameAspect) and, per
 * figure, the joints in FRAME coordinates (x 0..1 across, y 0..1 down, photo orientation, anatomical labels), the
 * per-joint visibility, the body unit, and the [FigureShapes.build] primitives in isotropic height units, so the film
 * draws the app's own mannequin geometry. The promo's TypeScript port of FigureShapes is checked against these
 * shapes (promo/scripts/check-shapes.ts).
 *
 * `illustrative` holds stances built with the same pose DSL that are NOT library poses (the film's stiff "what do
 * I do with my hands" stance); the film declares them as illustrative.
 *
 * Run: ./gradlew :pose-core:test --tests 'com.poseit.app.tools.PromoPoseExport'
 */
class PromoPoseExport {

    private val illustrative: List<PoseTemplate> = listOf(
        // Standing like a plank: facing the camera, arms dead straight at the sides, feet together.
        pose("promo_stiff", "Stiff", PoseCategory.STANDING) {
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                leftArm(upper = 4f, fore = 3f, hand = 2f)
                rightArm(upper = -4f, fore = -3f, hand = -2f)
                leftLeg(thigh = 1.5f, shin = 0.5f)
                rightLeg(thigh = -1.5f, shin = -0.5f)
            }
        },
        // The same body, wrists flicked up to the waist on "hands?" (B1).
        pose("promo_stiff_flick", "Stiff, hands up", PoseCategory.STANDING) {
            figure(cx = 0.5f, bottom = 0.95f, bodyHeight = 0.84f) {
                leftArm(upper = 8f, fore = 70f, hand = 95f)
                rightArm(upper = -8f, fore = -70f, hand = -95f)
                leftLeg(thigh = 1.5f, shin = 0.5f)
                rightLeg(thigh = -1.5f, shin = -0.5f)
            }
        },
    )

    @Test
    fun exportPoses() {
        val poses = BuiltInPoses.all
        assertEquals("the film counts on 99 poses", 99, poses.size)
        val out = File("../promo/public/poses.json")
        out.parentFile.mkdirs()
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"source\": \"pose-core BuiltInPoses.all + FigureShapes.build (PromoPoseExport.kt)\",\n")
        sb.append("  \"joints\": [").append(Joint.entries.joinToString(",") { "\"${it.name}\"" }).append("],\n")
        sb.append("  \"categories\": [")
        sb.append(PoseCategory.entries.filter { it != PoseCategory.CUSTOM }.joinToString(",") { "{\"id\":\"${it.name}\",\"label\":${str(it.label)}}" })
        sb.append("],\n")
        sb.append("  \"poses\": [\n")
        poses.forEachIndexed { i, p -> sb.append("    ").append(template(p)).append(if (i < poses.size - 1) ",\n" else "\n") }
        sb.append("  ],\n")
        sb.append("  \"illustrative\": [\n")
        illustrative.forEachIndexed { i, p -> sb.append("    ").append(template(p)).append(if (i < illustrative.size - 1) ",\n" else "\n") }
        sb.append("  ]\n}\n")
        out.writeText(sb.toString())
        assertTrue(out.length() > 10_000)
    }

    private fun template(p: PoseTemplate): String = buildString {
        append("{\"id\":").append(str(p.id))
        append(",\"name\":").append(str(p.name))
        append(",\"category\":").append(str(p.category.name))
        append(",\"frameAspect\":").append(num(p.frameAspect))
        append(",\"figures\":[")
        append(p.figures.joinToString(",") { figure(it, p.frameAspect) })
        append("]}")
    }

    private fun figure(s: Skeleton, aspect: Float): String = buildString {
        append("{\"unit\":").append(num(s.estimatedUnit(aspect)))
        append(",\"joints\":{")
        append(Joint.entries.mapNotNull { j -> s[j]?.let { "\"${j.name}\":${vec(it)}" } }.joinToString(","))
        append("},\"visibility\":{")
        append(s.visibility.entries.joinToString(",") { (j, v) -> "\"${j.name}\":${num(v)}" })
        append("},\"shapes\":[")
        append(FigureShapes.build(s, aspect).joinToString(",") { shape(it) })
        append("]")
        val nose = FigureShapes.noseMarker(s, aspect)
        append(",\"nose\":").append(if (nose == null) "null" else "{\"c\":${vec(nose.center)},\"r\":${num(nose.radius)}}")
        append("}")
    }

    private fun shape(s: FigureShape): String {
        val part = str(s.part.name)
        val bone = s.bone?.let { str(it.name) } ?: "null"
        return when (s) {
            is FigureShape.Capsule ->
                "{\"t\":\"capsule\",\"a\":${vec(s.a)},\"b\":${vec(s.b)},\"wa\":${num(s.widthA)},\"wb\":${num(s.widthB)},\"part\":$part,\"bone\":$bone}"
            is FigureShape.Circle -> "{\"t\":\"circle\",\"c\":${vec(s.center)},\"r\":${num(s.radius)},\"part\":$part,\"bone\":$bone}"
            is FigureShape.Polygon ->
                "{\"t\":\"polygon\",\"pts\":[${s.points.joinToString(",") { vec(it) }}],\"part\":$part,\"bone\":$bone}"
        }
    }

    private fun vec(v: Vec2) = "[${num(v.x)},${num(v.y)}]"
    private fun num(f: Float): String = String.format(Locale.ROOT, "%.5f", f).trimEnd('0').trimEnd('.').let { if (it == "-0") "0" else it }
    private fun str(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
