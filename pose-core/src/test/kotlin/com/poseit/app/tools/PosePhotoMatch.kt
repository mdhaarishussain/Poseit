package com.poseit.app.tools

import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.detect.LandmarkMapping
import com.poseit.app.pose.detect.RawLandmark
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.pose.match.FramingAudience
import com.poseit.app.pose.match.MatchConfig
import com.poseit.app.pose.match.MatchResult
import com.poseit.app.pose.match.PoseMatcher
import com.poseit.app.pose.match.Segment
import com.poseit.app.pose.scene.OverlayTransform
import com.poseit.app.pose.scene.SceneMapping
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

/**
 * The sample photos of the pose library (`app/src/main/assets/pose_photos/<poseId>.webp`), checked with the app's own
 * PoseMatcher: for every chosen photo, the landmarks of the SHIPPED file (`tools/pose-photos/landmarks/<poseId>.json`:
 * the app's full pose model, IMAGE mode, up to 4 people, on the cropped and, where the credit says so, already
 * mirrored WebP) are scored against the photo's own library pose, and against every other library pose to find the
 * one it is nearest to.
 *
 * The photo is shown beside the figure as drawn, and a mirrored photo was flipped before it was saved, so the app
 * score is the match with the pose AS DRAWN (no second flip). The score with the flipped pose (the app's flip toggle)
 * is reported too: when it beats the as-drawn score, the photo reads the other way round from the figure.
 *
 * Scene = the whole photo (LandmarkMapping.toScene with sceneAspect = width / height). People with fewer than 6 joints
 * of visibility >= 0.5 are dropped, the rest ordered by the area of their visible-joint box (largest first, at most
 * 6); "people" counts those at least 20 % of the largest one's area. The guide is placed as a user would drag and
 * pinch it (PromoChorusMatch.solveTransform, for several figures the means over figures and over the largest
 * people): fitted into the photo (SceneMapping.fitFrame), scaled by person torso / figure torso (0.15..4, as
 * tools/pose-photos/score.py; the photo is already cropped to the pose's frame, so the scale is near 1) and moved so
 * the mid-hips (mid-shoulders when a hip is not visible) coincide. Score = PoseMatcher.match (BACK camera,
 * mirroredView = false, PHOTOGRAPHER audience, strictness 0.80; strictness does not change the score).
 *
 * Also per pose: fit (PromoChorusMatch.fitOf: share of what the person shows that the pose requires), tcov (share by
 * weight of the segments the pose needs in its own frame that the photo shows and the matcher scored; the app's
 * coverage only counts segments inside the scene) and sit (mean joint distance in torso lengths). "Nearest" ranks
 * every library pose whose people count fits the photo (as score.py: people >= figures, at most figures + 1, + 2 for
 * groups) by rank = score - 0.3 max(0, 0.85 - fit) - 0.25 |people - figures| - 0.2 max(0, 0.85 - cropInside)
 * - max(0, 0.95 - tcov) - max(0, 0.95 - inFrame), best of as drawn and flipped: the rank score.py used to find the
 * candidates (without its text bonus), plus inFrame = the share of the person's joints shown in the photo that lie
 * inside the pose's own frame placed with the same drag and pinch (margin 3 % of the frame). The matcher alone
 * over-rates poses that need few segments (a selfie or portrait template on any waist-up photo with the arms down),
 * which fit and tcov correct; and a template's figure has joints outside its own frame (a headshot's legs), which a
 * strong pinch-out brings into the photo, so without inFrame a headshot template shrunk onto a full-body photo is
 * scored as a full standing figure (the app clamps a pinch to 0.4..2.5 and shows only the frame).
 *
 * Writes `tools/pose-photos/app-match.json` (committed; read by tools/pose-photos/credits.py into the credits).
 *
 * Run: ./gradlew :pose-core:test --tests 'com.poseit.app.tools.PosePhotoMatch' --rerun (the landmark files are not
 * declared task inputs, so without --rerun Gradle can call the test up to date and write nothing)
 */
class PosePhotoMatch {

    private val tools = File("../tools/pose-photos")
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun scorePosePhotos() {
        val dir = File(tools, "landmarks")
        val files = dir.listFiles { f -> f.name.endsWith(".json") }?.sortedBy { it.name } ?: return
        if (files.isEmpty()) return
        val poses = BuiltInPoses.all.associateBy { it.id }
        val rows = mutableListOf<String>()
        for (f in files) {
            val poseId = f.name.removeSuffix(".json")
            val pose = poses[poseId] ?: error("no library pose $poseId for ${f.name}")
            val photo = loadPhoto(f)
            rows += photoJson(pose, photo, evaluate(pose, photo), nearest(photo, BuiltInPoses.all))
        }
        val sb = StringBuilder()
        sb.append("{\n  \"source\": \"pose-core PosePhotoMatch.kt: the app's PoseMatcher on the landmarks of each shipped sample photo (tools/pose-photos/landmarks/<poseId>.json)\",\n")
        sb.append("  \"camera\": {\"lens\":\"BACK\",\"mirroredView\":false,\"audience\":\"PHOTOGRAPHER\",\"strictness\":").append(fmt(STRICTNESS)).append("},\n")
        sb.append("  \"appScore\": \"score of the shipped photo (already mirrored where its credit says so) against its own pose as drawn\",\n")
        sb.append("  \"rank\": \"score - 0.3 max(0, 0.85 - fit) - 0.25 |people - figures| - 0.2 max(0, 0.85 - cropInside) - max(0, 0.95 - tcov) - max(0, 0.95 - inFrame); nearest = best rank over the library poses whose people count fits, as drawn or flipped\",\n")
        sb.append("  \"photos\": [\n").append(rows.joinToString(",\n")).append("\n  ]\n}\n")
        File(tools, "app-match.json").writeText(sb.toString())
        assertTrue(rows.size == files.size)
    }

    // ------------------------------------------------------------------------------------------------ data

    private class Photo(val image: String, val w: Int, val h: Int, val people: List<Skeleton>, val significant: Int) {
        val aspect get() = w.toFloat() / h
    }

    private fun loadPhoto(f: File): Photo {
        val o = json.parseToJsonElement(f.readText()).jsonObject
        val w = o["w"]!!.jsonPrimitive.int
        val h = o["h"]!!.jsonPrimitive.int
        val all = o["people"]!!.jsonArray.map { p ->
            val raw = p.jsonArray.map { el ->
                if (el is JsonNull) RawLandmark(Float.NaN, Float.NaN, 0f, 0f) else {
                    val a = el.jsonArray
                    RawLandmark(
                        a[0].jsonPrimitive.float,
                        a[1].jsonPrimitive.float,
                        a.getOrNull(3)?.jsonPrimitive?.float ?: 1f,
                        a.getOrNull(4)?.jsonPrimitive?.float ?: 1f,
                    )
                }
            }
            LandmarkMapping.toScene(raw, w, h, w.toFloat() / h)
        }
        val usable = all.filter { s -> s.points.keys.count { s.isVisible(it, MIN_VIS) } >= 6 }
        val sorted = usable.sortedByDescending { area(it) }.take(MAX_PEOPLE)
        val big = sorted.firstOrNull()?.let { area(it) } ?: 0f
        val significant = sorted.count { area(it) >= 0.2f * big }
        return Photo(o["image"]!!.jsonPrimitive.content, w, h, sorted, significant)
    }

    private fun area(s: Skeleton): Float {
        val pts = s.points.filterKeys { s.isVisible(it, MIN_VIS) }.values
        if (pts.size < 2) return 0f
        return (pts.maxOf { it.x } - pts.minOf { it.x }) * (pts.maxOf { it.y } - pts.minOf { it.y })
    }

    // ------------------------------------------------------------------------------------------- scoring

    private class Eval(
        val template: PoseTemplate,
        val mirrored: Boolean,
        val transform: OverlayTransform,
        val result: MatchResult,
        val fit: Float,
        val tcov: Float,
        val sit: Float,
        val cropInside: Float,
        /** share of the assigned people's joints shown in the photo that lie inside the pose's own frame, placed */
        val inFrame: Float,
        val peopleMismatch: Int,
    ) {
        val score get() = result.score
        val rank get() = score - 0.3f * maxOf(0f, 0.85f - fit) - 0.25f * peopleMismatch - 0.2f * maxOf(0f, 0.85f - cropInside) -
            maxOf(0f, 0.95f - tcov) - maxOf(0f, 0.95f - inFrame)
    }

    /** The pose as drawn and flipped on the photo (null when the guide cannot be placed: no torso or shoulders). */
    private fun evaluate(template: PoseTemplate, photo: Photo): Map<Boolean, Eval?> =
        listOf(false, true).associateWith { m -> evalOne(template, m, photo) }

    private fun evalOne(template: PoseTemplate, mirrored: Boolean, photo: Photo): Eval? {
        val n = template.personCount
        val transform = solveTransform(template, mirrored, photo.people.take(n), photo.aspect) ?: return null
        val scene = SceneMapping.templateToScene(template, photo.aspect, transform, mirrored)
        val result = PoseMatcher.match(
            templateFigures = scene,
            detected = photo.people,
            sceneAspect = photo.aspect,
            audience = FramingAudience.PHOTOGRAPHER,
            config = MatchConfig(perfectScore = STRICTNESS),
            mirroredView = false,
        )
        return Eval(
            template, mirrored, transform, result,
            fit = fitOf(result, photo.people),
            tcov = templateCoverage(template, mirrored, result),
            sit = sitOf(scene, result, photo),
            cropInside = cropInside(template, photo.aspect, transform),
            inFrame = inFrame(template, photo, transform, result),
            peopleMismatch = abs(photo.significant - n),
        )
    }

    /** Every library pose whose people count fits the photo, best of as drawn and flipped, by rank. */
    private fun nearest(photo: Photo, library: List<PoseTemplate>): List<Eval> {
        val out = mutableListOf<Eval>()
        for (t in library) {
            val n = t.personCount
            if (photo.significant < n || photo.significant > n + (if (n >= 3) 1 else 0) + 1) continue
            val best = listOfNotNull(evalOne(t, false, photo), evalOne(t, true, photo)).maxByOrNull { it.rank } ?: continue
            out += best
        }
        return out.sortedByDescending { it.rank }
    }

    // --------------------------------------------------------------------- the guide fit (as score.py)

    private fun solveTransform(template: PoseTemplate, mirrored: Boolean, people: List<Skeleton>, aspect: Float): OverlayTransform? {
        val figs = SceneMapping.templateToScene(template, aspect, OverlayTransform.IDENTITY, mirrored)
        if (figs.isEmpty() || people.isEmpty()) return null
        val centre = SceneMapping.fitFrame(template.frameAspect, aspect).center
        val vis = people.map { it.visibleOnly() }
        val hips = vis.all { it.midHip() != null } && figs.all { it.midHip() != null }
        fun key(s: Skeleton) = if (hips) s.midHip() else s.midShoulder()
        val ta = mean(figs.mapNotNull { key(it) }) ?: return null
        val pa = mean(vis.mapNotNull { key(it) }) ?: return null
        val pt = vis.map { torso(it, aspect, hips) }.average().toFloat()
        val ft = figs.map { torso(it, aspect, hips) }.average().toFloat()
        val scale = (pt / ft).takeIf { ft > 0f && it.isFinite() }?.coerceIn(MIN_SCALE, MAX_SCALE) ?: 1f
        val offsetX = (pa.x - centre.x - (ta.x - centre.x) * scale).coerceIn(-MAX_OFFSET, MAX_OFFSET)
        val offsetY = (pa.y - centre.y - (ta.y - centre.y) * scale).coerceIn(-MAX_OFFSET, MAX_OFFSET)
        return OverlayTransform(offsetX = offsetX, offsetY = offsetY, scale = scale, rotation = 0f)
    }

    private fun mean(ps: List<Vec2>): Vec2? =
        if (ps.isEmpty()) null else Vec2(ps.map { it.x }.average().toFloat(), ps.map { it.y }.average().toFloat())

    private fun torso(s: Skeleton, aspect: Float, hips: Boolean): Float {
        val ms = s.midShoulder()
        val mh = s.midHip()
        if (hips && ms != null && mh != null) return hypot((ms.x - mh.x) * aspect, ms.y - mh.y)
        return s.copy(unit = 0f).estimatedUnit(aspect)
    }

    private fun Skeleton.visibleOnly(): Skeleton =
        copy(points = points.filterKeys { isVisible(it, MIN_VIS) }, visibility = visibility.filter { it.value >= MIN_VIS }, unit = 0f)

    // --------------------------------------------------------------------- fit, coverage, sit (as score.py)

    private fun fitOf(result: MatchResult, people: List<Skeleton>): Float {
        val fits = result.figures.map { fig ->
            val person = fig.detectedIndex?.let { people.getOrNull(it) }?.visibleOnly() ?: return 0f
            val required = fig.segments.filter { it.required }.map { it.segment }.toSet()
            var shown = 0f
            var covered = 0f
            for (seg in Segment.entries) {
                if (!person.showsInScene(seg)) continue
                shown += seg.weight
                if (seg in required) covered += seg.weight
            }
            if (shown > 0f) covered / shown else 0f
        }
        return fits.minOrNull() ?: 0f
    }

    /** Segments the pose needs in its OWN frame (as drawn or flipped), by figure. */
    private fun ownRequired(template: PoseTemplate, mirrored: Boolean): List<Set<Segment>> {
        val own = SceneMapping.templateToScene(template, template.frameAspect, OverlayTransform.IDENTITY, mirrored)
        val self = PoseMatcher.match(own, own, template.frameAspect)
        return self.figures.map { fm -> fm.segments.filter { it.required }.map { it.segment }.toSet() }
    }

    private fun templateCoverage(template: PoseTemplate, mirrored: Boolean, result: MatchResult): Float {
        val req = ownRequired(template, mirrored)
        val vals = req.mapIndexed { k, segs ->
            val total = segs.sumOf { it.weight.toDouble() }.toFloat()
            val fig = result.figures.getOrNull(k)
            if (fig == null || fig.detectedIndex == null || total <= 0f) 0f else {
                val scored = fig.segments.filter { it.required && it.actualAngle != null }.map { it.segment }.toSet()
                segs.filter { it in scored }.sumOf { it.weight.toDouble() }.toFloat() / total
            }
        }
        return vals.minOrNull() ?: 0f
    }

    private fun sitOf(scene: List<Skeleton>, result: MatchResult, photo: Photo): Float {
        val vals = result.figures.mapIndexedNotNull { k, fig ->
            val person = fig.detectedIndex?.let { photo.people.getOrNull(it) }?.visibleOnly() ?: return@mapIndexedNotNull null
            val f = scene.getOrNull(k) ?: return@mapIndexedNotNull null
            val tl = torso(person, photo.aspect, person.midHip() != null).takeIf { it > 1e-4f } ?: return@mapIndexedNotNull null
            val ds = person.points.mapNotNull { (j, p) ->
                if (p.x !in 0f..1f || p.y !in 0f..1f) null else f[j]?.let { q -> hypot((q.x - p.x) * photo.aspect, q.y - p.y) }
            }
            if (ds.size >= 6) ds.average().toFloat() / tl else null
        }
        return if (vals.isEmpty()) 9f else vals.average().toFloat()
    }

    /** The pose's own frame placed on the photo with the drag and pinch [t] (scene units: x0, y0, x1, y1). */
    private fun placedFrame(template: PoseTemplate, aspect: Float, t: OverlayTransform): FloatArray {
        val b = SceneMapping.fitFrame(template.frameAspect, aspect)
        val c = b.center
        return floatArrayOf(
            c.x + (b.left - c.x) * t.scale + t.offsetX, c.y + (b.top - c.y) * t.scale + t.offsetY,
            c.x + (b.right - c.x) * t.scale + t.offsetX, c.y + (b.bottom - c.y) * t.scale + t.offsetY,
        )
    }

    private fun inFrame(template: PoseTemplate, photo: Photo, t: OverlayTransform, result: MatchResult): Float {
        val (x0, y0, x1, y1) = placedFrame(template, photo.aspect, t).toList()
        val mx = FRAME_MARGIN * (x1 - x0)
        val my = FRAME_MARGIN * (y1 - y0)
        var shown = 0
        var inside = 0
        for (fig in result.figures) {
            val person = fig.detectedIndex?.let { photo.people.getOrNull(it) }?.visibleOnly() ?: continue
            for (p in person.points.values) {
                if (p.x !in 0f..1f || p.y !in 0f..1f) continue
                shown++
                if (p.x in (x0 - mx)..(x1 + mx) && p.y in (y0 - my)..(y1 + my)) inside++
            }
        }
        return if (shown > 0) inside.toFloat() / shown else 0f
    }

    /** Share of the pose's own frame, placed with the same drag and pinch, that lies inside the photo. */
    private fun cropInside(template: PoseTemplate, aspect: Float, t: OverlayTransform): Float {
        val (x0, y0, x1, y1) = placedFrame(template, aspect, t).toList()
        val iw = maxOf(0f, minOf(x1, 1f) - maxOf(x0, 0f))
        val ih = maxOf(0f, minOf(y1, 1f) - maxOf(y0, 0f))
        return if (x1 > x0 && y1 > y0) iw * ih / ((x1 - x0) * (y1 - y0)) else 0f
    }

    private fun Skeleton.showsInScene(seg: Segment): Boolean {
        fun ok(j: Joint) = this[j]?.let { it.x in 0f..1f && it.y in 0f..1f } == true
        val shoulder = ok(Joint.LEFT_SHOULDER) || ok(Joint.RIGHT_SHOULDER)
        val hip = ok(Joint.LEFT_HIP) || ok(Joint.RIGHT_HIP)
        return when (seg) {
            Segment.TORSO -> shoulder && hip
            Segment.HEAD -> shoulder && ok(Joint.NOSE)
            Segment.SHOULDER_LINE -> ok(Joint.LEFT_SHOULDER) && ok(Joint.RIGHT_SHOULDER)
            Segment.HIP_LINE -> ok(Joint.LEFT_HIP) && ok(Joint.RIGHT_HIP)
            Segment.LEFT_UPPER_ARM -> ok(Joint.LEFT_SHOULDER) && ok(Joint.LEFT_ELBOW)
            Segment.LEFT_FOREARM -> ok(Joint.LEFT_ELBOW) && ok(Joint.LEFT_WRIST)
            Segment.RIGHT_UPPER_ARM -> ok(Joint.RIGHT_SHOULDER) && ok(Joint.RIGHT_ELBOW)
            Segment.RIGHT_FOREARM -> ok(Joint.RIGHT_ELBOW) && ok(Joint.RIGHT_WRIST)
            Segment.LEFT_THIGH -> ok(Joint.LEFT_HIP) && ok(Joint.LEFT_KNEE)
            Segment.LEFT_SHIN -> ok(Joint.LEFT_KNEE) && ok(Joint.LEFT_ANKLE)
            Segment.RIGHT_THIGH -> ok(Joint.RIGHT_HIP) && ok(Joint.RIGHT_KNEE)
            Segment.RIGHT_SHIN -> ok(Joint.RIGHT_KNEE) && ok(Joint.RIGHT_ANKLE)
        }
    }

    // ------------------------------------------------------------------------------------------------ json

    private fun photoJson(pose: PoseTemplate, photo: Photo, own: Map<Boolean, Eval?>, near: List<Eval>): String {
        val poseId = pose.id
        val drawn = own[false]
        val flipped = own[true]
        val ownRank = near.indexOfFirst { it.template.id == poseId }.let { if (it < 0) null else it + 1 }
        val top = near.firstOrNull()
        val sb = StringBuilder()
        sb.append("    {\"poseId\":").append(str(poseId)).append(",\"image\":").append(str(photo.image))
            .append(",\"w\":").append(photo.w).append(",\"h\":").append(photo.h)
            .append(",\"people\":").append(photo.significant).append(",\"figures\":").append(pose.personCount)
        sb.append(",\n      \"appScore\":").append(drawn?.let { fmt(it.score) } ?: "null")
        sb.append(",\"asDrawn\":").append(evalJson(drawn))
        sb.append(",\n      \"flipped\":").append(evalJson(flipped))
        sb.append(",\n      \"ownRankAmongPoses\":").append(ownRank ?: "null").append(",\"posesRanked\":").append(near.size)
        sb.append(",\"nearestPose\":").append(top?.let { str(it.template.id) } ?: "null")
        sb.append(",\"nearestIsOwn\":").append(top?.template?.id == poseId)
        sb.append(",\n      \"nearest\":[").append(near.take(NEAREST).joinToString(",") {
            "{\"poseId\":${str(it.template.id)},\"mirrored\":${it.mirrored},\"score\":${fmt(it.score)},\"rank\":${fmt(it.rank)},\"fit\":${fmt(it.fit)},\"tcov\":${fmt(it.tcov)},\"inFrame\":${fmt(it.inFrame)}}"
        }).append("]}")
        return sb.toString()
    }

    private fun evalJson(e: Eval?): String {
        if (e == null) return "null"
        return "{\"score\":${fmt(e.score)},\"rank\":${fmt(e.rank)},\"fit\":${fmt(e.fit)},\"tcov\":${fmt(e.tcov)},\"sit\":${fmt(e.sit)}," +
            "\"cropInside\":${fmt(e.cropInside)},\"inFrame\":${fmt(e.inFrame)},\"assigned\":${e.result.figures.count { it.detectedIndex != null }}," +
            "\"figureScores\":[${e.result.figures.joinToString(",") { fmt(it.score) }}]," +
            "\"parts\":[${e.result.figures.joinToString(",") { f -> f.partScores.entries.joinToString(",", "{", "}") { (k, v) -> "\"${k.name}\":${fmt(v)}" } }}]," +
            "\"transform\":{\"scale\":${fmt(e.transform.scale)},\"offsetX\":${fmt(e.transform.offsetX)},\"offsetY\":${fmt(e.transform.offsetY)}}}"
    }

    private companion object {
        const val STRICTNESS = 0.80f
        const val MIN_VIS = 0.5f
        const val MAX_PEOPLE = 6
        const val MIN_SCALE = 0.15f
        const val MAX_SCALE = 4f
        const val MAX_OFFSET = 1.5f
        const val NEAREST = 5
        const val FRAME_MARGIN = 0.03f

        fun fmt(v: Float) = if (v.isFinite()) String.format(Locale.US, "%.4f", v) else "null"
        fun str(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
}
