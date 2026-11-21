package com.poseit.app.tools

import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.math.hypot

/**
 * The promo film's chorus (promo/, STORYBOARD.md v2 B3): for each chorus shot, the library pose the app's matcher
 * finds for the person on screen, the frame it is fitted on, and the guide in scene space.
 *
 * For every prepared chorus plate (`promo/public/footage/<plate>.landmarks.json`: the app's lite model, LandmarkMapping
 * and LandmarkSmoother on the 9:16 plate) and every plate frame, every single-person library template, as drawn and
 * flipped (the app's flip toggle), is mapped with SceneMapping.templateToScene and an OverlayTransform solved like a
 * drag and pinch (mid-hip and torso length, clamped like OverlayMath) and scored by the app's PoseMatcher (back camera:
 * mirroredView = false, PHOTOGRAPHER audience, strictness 0.80). The manifest snap rule decides what is usable: score
 * >= 0.80, the template covers what the person shows (fit >= 0.8), never "Hands in pockets" (the punchline).
 *
 * The film then holds that guide still while the person moves under it (as the app's guide does), so each candidate
 * is also scored on every frame the shot shows after the guide has landed ("hold"): the plate may start anywhere in its
 * 15-frame handles (the manifest's in-point window), so the key frame is placed on the frame the guide lands. In film
 * order each shot takes the best candidate (key score and hold) whose pose no earlier shot took.
 *
 * Writes `promo/public/footage/chorus.json` (committed): per shot the plate, the in-point offset (plate frames), the
 * key frame, the pose, its score, fit and hold, the part scores, the transform, the guide's figures in scene space and
 * the top of the ranking at the key frame.
 *
 * A manifest shot may set `guideAnchor.x` to "head": the drag then puts the guide's head and shoulders (the mean of
 * mid-shoulder and nose) over the person's horizontally instead of the mid-hip (height and size still from the
 * mid-hip and torso). For a waist-up shot whose person leans their head, that is the drag a user would make, since the
 * frame shows little below the chest (c9, review 2).
 *
 * Run: ./gradlew :pose-core:test --tests 'com.poseit.app.tools.PromoChorusMatch' --rerun (the JSON it reads is not a
 * declared task input, so without --rerun Gradle can call the test up to date and write nothing)
 */
class PromoChorusMatch {

    private val dir = File("../promo/public/footage")
    private val json = Json { ignoreUnknownKeys = true }

    /** The chorus slots in film order and the plates each may use (the manifest's alternates; c1-alt1 repeats c8's take). */
    private val slots = listOf(
        "c1" to listOf("c1"),
        "c2" to listOf("c2"),
        "c3" to listOf("c3"),
        "c4" to listOf("c4"),
        "c5" to listOf("c5"),
        "c6" to listOf("c6"),
        "c7" to listOf("c7"),
        "c8" to listOf("c8"),
        "c9" to listOf("c9", "c9-alt1", "c9-alt2"),
    )

    @Test
    fun exportChorus() {
        val platesFile = File(dir, "plates.json")
        val manifestFile = File("../promo/footage/manifest.json")
        if (!platesFile.exists() || !manifestFile.exists()) return // the footage stage has not run
        val manifest = json.parseToJsonElement(manifestFile.readText()).jsonObject
        val shots = manifest["shots"]!!.jsonArray.map { it.jsonObject }.associateBy { it["id"]!!.jsonPrimitive.content }
        val platesJson = json.parseToJsonElement(platesFile.readText()).jsonObject["plates"]!!.jsonObject

        // every slot's usable candidates (best per pose), then the assignment with no pose twice that maximises the
        // summed rank (exhaustive, branch and bound: nine slots, a few poses each)
        val perSlot = slots.mapNotNull { (slot, plateIds) ->
            val shot = shots[slot] ?: return@mapNotNull null
            val film = shot["film"]!!.jsonArray.map { it.jsonPrimitive.int }
            val land = if (film[1] - film[0] >= 16) LAND_BEAT else LAND_EIGHTH
            val headAnchor = (shot["guideAnchor"] as? JsonObject)?.get("x")?.jsonPrimitive?.content == "head"
            val all = mutableListOf<Choice>()
            for (pid in plateIds) {
                val pj = platesJson[pid]?.jsonObject ?: continue
                if (!File(dir, "$pid.landmarks.json").exists()) continue
                all += candidates(loadPlate(pid, pj), film, land, headAnchor)
            }
            // the slot's own plate first; the manifest's alternates only when it has nothing usable (c9-alt2 repeats
            // c4's person; the couple plate keeps a new face and both people in frame, with one figure on the one the
            // app's model finds)
            val usable = all.filter { it.usable }
            val plate = plateIds.firstOrNull { pid -> usable.any { it.plate.id == pid } }
            val options = usable.filter { it.plate.id == plate }.sortedByDescending { it.rank }.distinctBy { it.c.template.id }.take(OPTIONS)
            Slot(slot, plateIds, film, land, all, options)
        }
        val picks = assign(perSlot)
        val results = mutableListOf<String>()
        val report = StringBuilder()
        perSlot.forEachIndexed { i, sl ->
            val pick = picks[i]
            report.append("${sl.id}: ").append(pick?.let { "${it.plate.id} ${it.c.template.id}${if (it.c.mirrored) " (flipped)" else ""} key n${it.key.n} f${it.key.f} score ${fmt(it.c.score)} sit ${fmt(it.c.sit)}/${fmt(it.holdSit)} hold ${fmt(it.holdMean)}/${fmt(it.holdMin)} offset ${it.offset}" } ?: "no usable pose")
                .append('\n')
            for (pid in sl.plates) {
                val best = sl.all.filter { it.plate.id == pid }.sortedByDescending { it.rank }.distinctBy { it.c.template.id }.take(6)
                report.append("    $pid (${sl.all.count { it.plate.id == pid }} candidates): ")
                    .append(best.joinToString { "${it.c.template.id}${if (it.c.mirrored) "~" else ""} f${it.key.f} ${fmt(it.c.score)} sit ${fmt(it.c.sit)}/${fmt(it.holdSit)} hold ${fmt(it.holdMean)}/${fmt(it.holdMin)}${if (it.usable) "" else " (not usable)"}" }).append('\n')
            }
            val alternatives = sl.plates.flatMap { pid -> sl.options.filter { it.plate.id == pid && it !== pick }.take(3) }
            results += shotJson(sl.id, sl.film, sl.land, pick, alternatives)
        }
        val sb = StringBuilder()
        sb.append("{\n  \"source\": \"app PoseMatcher over every single-person library template on every chorus plate frame (pose-core PromoChorusMatch.kt), smoothed detections from <plate>.landmarks.json\",\n")
        sb.append("  \"camera\": {\"lens\":\"BACK\",\"mirroredView\":false,\"audience\":\"PHOTOGRAPHER\",\"strictness\":").append(fmt(STRICTNESS)).append("},\n")
        sb.append("  \"rule\": \"usable = score >= ").append(fmt(SNAP_MIN)).append(" at the key frame, fit >= ").append(fmt(SNAP_MIN_FIT))
            .append(", sit <= ").append(fmt(SIT_MAX)).append(", hold mean >= ").append(fmt(HOLD_MEAN)).append(", hold sit <= ").append(fmt(HOLD_SIT_MAX)).append(", not ").append(EXCLUDED.joinToString()).append("; rank = (score + hold mean) / 2 - ").append(fmt(SIT_WEIGHT)).append(" (sit + hold sit) / 2; a slot uses its own plate when it has a usable pose, else its alternates; the shots take the assignment with no pose twice that maximises the summed rank\",\n")
        sb.append("  \"timing\": \"the guide lands over `land` frames from the cut; the plate is offset by `offset` plate frames (inside its 15-frame handles) so the key frame is on screen on the frame the guide has landed (film frame `film[0] + land`); hold = the fixed guide's score on every frame shown after it has landed\",\n")
        sb.append("  \"shots\": [\n").append(results.joinToString(",\n")).append("\n  ]\n}\n")
        File(dir, "chorus.json").writeText(sb.toString())
        File("../promo/out").takeIf { it.isDirectory }?.let { File(it, "chorus-report.txt").writeText(report.toString()) }
        assertTrue(results.isNotEmpty())
    }

    // ------------------------------------------------------------------------------------------------ data

    private class FrameLm(val n: Int, val f: Int, val src: Int, val people: List<Skeleton>)

    private class PlateLm(val id: String, val clip: String, val filmFrom: Int, val sourceFps: Float, val frames: List<FrameLm>)

    private fun loadPlate(id: String, plate: JsonObject): PlateLm {
        val lm = json.parseToJsonElement(File(dir, "$id.landmarks.json").readText()).jsonObject
        val joints = lm["joints"]!!.jsonArray.map { Joint.valueOf(it.jsonPrimitive.content) }
        val frames = lm["frames"]!!.jsonArray.map { el ->
            val fr = el.jsonObject
            val people = fr["people"]!!.jsonArray.map { p ->
                val points = LinkedHashMap<Joint, Vec2>()
                val vis = LinkedHashMap<Joint, Float>()
                p.jsonArray.forEachIndexed { k, j ->
                    if (j is JsonNull) return@forEachIndexed
                    val a = j.jsonArray
                    points[joints[k]] = Vec2(a[0].jsonPrimitive.float, a[1].jsonPrimitive.float)
                    vis[joints[k]] = a[2].jsonPrimitive.float
                }
                Skeleton(points = points, visibility = vis)
            }
            FrameLm(fr["n"]!!.jsonPrimitive.int, fr["f"]!!.jsonPrimitive.int, fr["src"]!!.jsonPrimitive.int, people)
        }
        return PlateLm(
            id = id,
            clip = plate["catalogue"]!!.jsonPrimitive.content,
            filmFrom = plate["filmFrom"]!!.jsonPrimitive.int,
            sourceFps = plate["source"]!!.jsonObject["fps"]!!.jsonPrimitive.float,
            frames = frames,
        )
    }

    // ------------------------------------------------------------------------------------------- candidates

    private class Candidate(
        val template: PoseTemplate,
        val mirrored: Boolean,
        val transform: OverlayTransform,
        val scene: List<Skeleton>,
        val result: MatchResult,
        val fit: Float,
        /** mean distance from each drawn joint to the person's (isotropic), in the person's torso lengths */
        val sit: Float,
    ) {
        val score get() = result.score
    }

    private class Slot(val id: String, val plates: List<String>, val film: List<Int>, val land: Int, val all: List<Choice>, val options: List<Choice>)

    /** The pick per slot with no pose used twice that maximises the summed rank (a slot with no option gets null). */
    private fun assign(slots: List<Slot>): List<Choice?> {
        var best: List<Choice?> = slots.map { null }
        var bestSum = Float.NEGATIVE_INFINITY
        val bound = FloatArray(slots.size + 1)
        for (i in slots.indices.reversed()) bound[i] = bound[i + 1] + (slots[i].options.firstOrNull()?.rank ?: 0f)
        val cur = arrayOfNulls<Choice>(slots.size)
        val used = HashSet<String>()
        fun dfs(i: Int, sum: Float) {
            if (sum + bound[i] <= bestSum) return
            if (i == slots.size) {
                bestSum = sum
                best = cur.toList()
                return
            }
            var any = false
            for (o in slots[i].options) {
                if (o.c.template.id in used) continue
                any = true
                used += o.c.template.id
                cur[i] = o
                dfs(i + 1, sum + o.rank)
                used -= o.c.template.id
            }
            if (!any || slots[i].options.isEmpty()) {
                cur[i] = null
                dfs(i + 1, sum)
            }
            cur[i] = null
        }
        dfs(0, 0f)
        return best
    }

    private class Choice(
        val plate: PlateLm,
        val key: FrameLm,
        val offset: Int,
        val c: Candidate,
        val holdMean: Float,
        val holdMin: Float,
        /** sit (see sitOf) averaged over the same frames: the fixed figure must stay on the person, not only in shape */
        val holdSit: Float,
    ) {
        val usable get() = c.score >= SNAP_MIN && c.fit >= SNAP_MIN_FIT && c.sit <= SIT_MAX && holdMean >= HOLD_MEAN &&
            holdSit <= HOLD_SIT_MAX && c.template.id !in EXCLUDED
        val rank get() = (c.score + holdMean) / 2f - SIT_WEIGHT * (c.sit + holdSit) / 2f
    }

    /**
     * Every key frame the shot can show on its landing frame (in-point offset within the handles), the top usable
     * templates there, each scored as a fixed guide on every frame shown after the landing.
     */
    private fun candidates(plate: PlateLm, film: List<Int>, land: Int, headAnchor: Boolean): List<Choice> {
        val (a, b) = film[0] to film[1]
        val out = mutableListOf<Choice>()
        for (key in plate.frames) {
            if (key.people.size != 1) continue
            val offset = key.f - (a + land)
            if (offset < -HANDLE || offset > HANDLE) continue
            val standing = !key.people.first().showsKnees()
            val top = rank(key.people, headAnchor)
                .filter { it.fit >= SNAP_MIN_FIT && it.score >= SNAP_MIN && it.sit <= SIT_MAX && it.template.id !in EXCLUDED }
                .filter { !standing || it.template.category !in NEEDS_LEGS }
                .take(TOP_PER_FRAME)
            for (c in top) {
                val shown = (a + land until b).map { f -> plate.frames.firstOrNull { it.f == f + offset } }
                val scores = shown.map { fr -> if (fr == null || fr.people.isEmpty()) 0f else match(c.scene, fr.people).score }
                val sits = shown.map { fr -> if (fr == null || fr.people.isEmpty()) 9f else sitOf(c.scene, fr.people) }
                out += Choice(plate, key, offset, c, scores.average().toFloat(), scores.minOrNull() ?: 0f, sits.average().toFloat())
            }
        }
        return out
    }

    private fun rank(people: List<Skeleton>, headAnchor: Boolean = false): List<Candidate> {
        val out = mutableListOf<Candidate>()
        for (template in BuiltInPoses.all) {
            if (template.personCount != 1) continue
            for (mirrored in listOf(false, true)) {
                val transform = solveTransform(template, mirrored, people, headAnchor)
                val scene = SceneMapping.templateToScene(template, ASPECT, transform, mirrored)
                val result = match(scene, people)
                out += Candidate(template, mirrored, transform, scene, result, fitOf(result, people), sitOf(scene, people))
            }
        }
        return out.sortedByDescending { it.score }
    }

    private fun match(scene: List<Skeleton>, people: List<Skeleton>): MatchResult = PoseMatcher.match(
        templateFigures = scene,
        detected = people,
        sceneAspect = ASPECT,
        audience = FramingAudience.PHOTOGRAPHER,
        config = MatchConfig(perfectScore = STRICTNESS),
        mirroredView = false,
    )

    // --------------------------------------------------------------------- the guide fit (as PromoFootageMatch)

    private fun solveTransform(template: PoseTemplate, mirrored: Boolean, people: List<Skeleton>, headAnchor: Boolean = false): OverlayTransform {
        val figures = SceneMapping.templateToScene(template, ASPECT, OverlayTransform.IDENTITY, mirrored)
        if (figures.isEmpty() || people.isEmpty()) return OverlayTransform.IDENTITY
        val centre = SceneMapping.fitFrame(template.frameAspect, ASPECT).center
        val fig = figures.first()
        val person = people.first().visibleOnly()
        val hips = person.midHip() != null && fig.midHip() != null
        val ta = (if (hips) fig.midHip() else fig.midShoulder()) ?: fig.points.values.first()
        val pa = (if (hips) person.midHip() else person.midShoulder()) ?: person.points.values.firstOrNull() ?: return OverlayTransform.IDENTITY
        val scale = (torso(person, hips) / torso(fig, hips)).takeIf { it.isFinite() && it > 0f }
            ?.coerceIn(OverlayTransform.MIN_SCALE, OverlayTransform.MAX_SCALE) ?: 1f
        // headAnchor: x from the head and shoulders (mean of mid-shoulder and nose) when both show them
        val ha = if (headAnchor) fig.headAndShoulders() else null
        val hp = if (headAnchor) person.headAndShoulders() else null
        val (ax, px) = if (ha != null && hp != null) ha.x to hp.x else ta.x to pa.x
        val offsetX = (px - centre.x - (ax - centre.x) * scale).coerceIn(-MAX_OFFSET, MAX_OFFSET)
        val offsetY = (pa.y - centre.y - (ta.y - centre.y) * scale).coerceIn(-MAX_OFFSET, MAX_OFFSET)
        return OverlayTransform(offsetX = offsetX, offsetY = offsetY, scale = scale, rotation = 0f)
    }

    private fun Skeleton.headAndShoulders(): Vec2? {
        val ms = midShoulder() ?: return null
        val nose = this[Joint.NOSE] ?: return null
        return Vec2((ms.x + nose.x) / 2f, (ms.y + nose.y) / 2f)
    }

    private fun torso(s: Skeleton, hips: Boolean): Float {
        val ms = s.midShoulder()
        val mh = s.midHip()
        if (hips && ms != null && mh != null) return hypot((ms.x - mh.x) * ASPECT, ms.y - mh.y)
        return s.copy(unit = 0f).estimatedUnit(ASPECT)
    }

    /**
     * "The drawn figure visibly sits on the body": the matcher scores limb directions, so a figure can score high while
     * its torso leans off the person (both share the mid-hip by construction). Mean isotropic distance from each joint
     * the person shows inside the frame to the same joint of the drawn figure, in the person's torso lengths.
     */
    private fun sitOf(scene: List<Skeleton>, people: List<Skeleton>): Float {
        val fig = scene.firstOrNull() ?: return 9f
        val person = people.firstOrNull()?.visibleOnly() ?: return 9f
        val torso = torso(person, person.midHip() != null).takeIf { it > 1e-4f } ?: return 9f
        var sum = 0f
        var k = 0
        for ((j, p) in person.points) {
            if (p.x !in 0f..1f || p.y !in 0f..1f) continue
            val q = fig[j] ?: continue
            sum += hypot((q.x - p.x) * ASPECT, q.y - p.y)
            k++
        }
        return if (k >= 6) sum / k / torso else 9f
    }

    /** Both knees visible inside the scene: without them the matcher cannot tell sitting from standing. */
    private fun Skeleton.showsKnees(): Boolean = listOf(Joint.LEFT_KNEE, Joint.RIGHT_KNEE).all { j ->
        isVisible(j, LIVE_MIN_VISIBILITY) && this[j]?.let { it.x in 0f..1f && it.y in 0f..1f } == true
    }

    private fun Skeleton.visibleOnly(): Skeleton = copy(points = points.filterKeys { isVisible(it, LIVE_MIN_VISIBILITY) })

    private fun fitOf(result: MatchResult, people: List<Skeleton>): Float {
        val fits = result.figures.mapNotNull { fig ->
            val person = fig.detectedIndex?.let { people.getOrNull(it) }?.visibleOnly() ?: return@mapNotNull null
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
        return if (fits.size == result.figures.size && fits.isNotEmpty()) fits.min() else 0f
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

    private fun shotJson(slot: String, film: List<Int>, land: Int, pick: Choice?, others: List<Choice>): String {
        val sb = StringBuilder()
        sb.append("    {\"id\":").append(str(slot)).append(",\"film\":[").append(film[0]).append(',').append(film[1]).append("],\"land\":").append(land)
        if (pick == null) return sb.append(",\"plate\":null,\"pose\":null}").toString()
        val c = pick.c
        val fig = c.result.figures.firstOrNull()
        sb.append(",\"plate\":").append(str(pick.plate.id)).append(",\"clip\":").append(str(pick.plate.clip))
        sb.append(",\"offset\":").append(pick.offset)
        sb.append(",\"key\":{\"n\":").append(pick.key.n).append(",\"f\":").append(pick.key.f).append(",\"src\":").append(pick.key.src)
            .append(",\"sourceTime\":").append(fmt(pick.key.src / pick.plate.sourceFps)).append("}")
        sb.append(",\n      \"pose\":{\"poseId\":").append(str(c.template.id)).append(",\"name\":").append(str(c.template.name))
            .append(",\"mirrored\":").append(c.mirrored).append(",\"frameAspect\":").append(fmt(c.template.frameAspect))
            .append(",\"score\":").append(fmt(c.score)).append(",\"fit\":").append(fmt(c.fit)).append(",\"sit\":").append(fmt(c.sit))
            .append(",\"hold\":{\"mean\":").append(fmt(pick.holdMean)).append(",\"min\":").append(fmt(pick.holdMin)).append(",\"sit\":").append(fmt(pick.holdSit)).append("}")
            .append(",\"parts\":{").append(fig?.partScores.orEmpty().entries.joinToString(",") { (k, v) -> "\"${k.name}\":${fmt(v)}" }).append("}")
            .append(",\"transform\":{\"scale\":").append(fmt(c.transform.scale)).append(",\"offsetX\":").append(fmt(c.transform.offsetX))
            .append(",\"offsetY\":").append(fmt(c.transform.offsetY)).append(",\"rotation\":").append(fmt(c.transform.rotation)).append("}}")
        sb.append(",\n      \"figures\":[").append(c.scene.joinToString(",") { skeletonJson(it) }).append("]")
        sb.append(",\n      \"alternatives\":[").append(others.joinToString(",") {
            "\n        {\"plate\":${str(it.plate.id)},\"offset\":${it.offset},\"keyF\":${it.key.f},\"poseId\":${str(it.c.template.id)},\"mirrored\":${it.c.mirrored}," +
                "\"score\":${fmt(it.c.score)},\"hold\":${fmt(it.holdMean)},\"figures\":[${it.c.scene.joinToString(",") { f -> skeletonJson(f) }}]}"
        }).append("]}")
        return sb.toString()
    }

    private fun skeletonJson(s: Skeleton): String {
        val joints = s.points.entries.joinToString(",") { (j, p) -> "\"${j.name}\":[${fmt5(p.x)},${fmt5(p.y)}]" }
        val vis = s.visibility.entries.joinToString(",") { (j, v) -> "\"${j.name}\":${fmt(v)}" }
        return "{\"joints\":{$joints},\"visibility\":{$vis},\"unit\":${fmt5(s.unit)}}"
    }

    private companion object {
        const val ASPECT = 9f / 16f
        const val STRICTNESS = 0.80f
        const val LIVE_MIN_VISIBILITY = 0.5f
        const val MAX_OFFSET = 0.9f
        const val SNAP_MIN = 0.80f
        const val SNAP_MIN_FIT = 0.8f
        /** the fixed guide must still sit on the person, on average, over the frames shown after it lands (the person
         *  may move under it, as under the app's guide) */
        const val HOLD_MEAN = 0.55f
        /** sit: at most this far off the body on average (torso lengths), and it costs rank */
        const val SIT_MAX = 0.25f
        const val SIT_WEIGHT = 0.5f
        const val HOLD_SIT_MAX = 0.3f
        const val HANDLE = 15
        const val LAND_BEAT = 5
        const val LAND_EIGHTH = 4
        const val TOP_PER_FRAME = 4
        /** poses per slot the assignment considers */
        const val OPTIONS = 6
        val EXCLUDED = setOf("stand_hands_in_pockets")
        /**
         * Categories whose figure sits, kneels, lies or leans on something: a half-body shot (no knees in frame) shows a
         * standing torso, so these are not offered there (the matcher only sees the upper body and ties them).
         */
        val NEEDS_LEGS = setOf(PoseCategory.SITTING, PoseCategory.FLOOR, PoseCategory.LEANING)

        fun fmt(v: Float) = String.format(Locale.US, "%.4f", v)
        fun fmt5(v: Float) = String.format(Locale.US, "%.5f", v)
        fun str(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
}
