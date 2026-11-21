package com.poseit.app.promo

import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.coach.CaptureCoach
import com.poseit.app.pose.coach.CaptureState
import com.poseit.app.pose.coach.CoachSettings
import com.poseit.app.pose.detect.LandmarkMapping
import com.poseit.app.pose.detect.RawLandmark
import com.poseit.app.pose.io.CustomPoseFactory
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.pose.match.FramingAudience
import com.poseit.app.pose.match.MatchConfig
import com.poseit.app.pose.match.MatchResult
import com.poseit.app.pose.match.PoseMatcher
import com.poseit.app.pose.match.Segment
import com.poseit.app.pose.scene.OverlayTransform
import com.poseit.app.pose.scene.SceneMapping
import com.poseit.app.ui.camera.HintStabilizer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
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
import kotlin.math.roundToInt

/**
 * The promo film's matches on real footage (promo/, STORYBOARD.md v2): the app's PoseMatcher, HintStabilizer and
 * CaptureCoach run frame by frame at 30 fps over the smoothed detections of each prepared stock plate
 * (`promo/public/footage/<plate>.landmarks.json`, written by PromoFootageLandmarks with the app's LandmarkMapping and
 * LandmarkSmoother), as CameraViewModel.onDetection / matchPose / updateHint / tick do on a phone:
 *  - the back camera: mirroredView = false, the PHOTOGRAPHER audience (the app's AUTO choice on the back lens, so the
 *    cards read "Ask them to ..."), the scene is the 9:16 plate;
 *  - the app's defaults: strictness 0.80 (MatchConfig.perfectScore and the coach threshold), auto-capture on, 700 ms
 *    hold, 3 s countdown, abort margin 0.12; HintStabilizer 400 ms on top / 1500 ms shown;
 *  - the guide is a library template mapped with SceneMapping.templateToScene and an OverlayTransform (the drag and
 *    pinch a user makes) solved so the template's mid-hip and torso length meet the person's at one frame; scale and
 *    offsets are clamped like OverlayMath (0.4-2.5, +-0.9).
 *
 * Writes, next to the landmarks:
 *  - `nearest.json`: for every manifest snap (and the prepared alternates) the library poses ranked by the matcher's
 *    score for that person at the key frame (every template with the right person count, as drawn and flipped), the
 *    pick under the manifest snap rule, and the best frame inside the shot's in-point window;
 *  - `<plate>.match.json` for every plate with a guide: the act III live shot (b5-live and its real-time variant
 *    b5-live.1x, guide = the manifest's stand_hands_in_pockets fixed at its defining frame) and every chorus plate
 *    (guide = its snap). Same per-frame records as public/match.json (src/scenes/viewfinder/match.ts reads them),
 *    plus the guide in scene space and the derived events;
 *  - `b5-live.photo-guide.match.json`: the same live run with the guide made by the app's "pose from a photo" path
 *    (CustomPoseFactory.fromDetection on the full-model detection of the defining frame), for comparison.
 *
 * Run: cd promo && npm run footage-match
 *      (or ./gradlew :app:testDebugUnitTest --tests 'com.poseit.app.promo.PromoFootageMatch')
 */
class PromoFootageMatch {

    private val dir = File("../promo/public/footage")
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun exportFootageMatch() {
        val platesFile = File(dir, "plates.json")
        val manifestFile = File("../promo/footage/manifest.json")
        if (!platesFile.exists() || !manifestFile.exists()) return // the footage stage has not run
        val manifest = json.parseToJsonElement(manifestFile.readText()).jsonObject
        val platesJson = json.parseToJsonElement(platesFile.readText()).jsonObject["plates"]!!.jsonObject
        val plates = platesJson.keys.filter { File(dir, "$it.landmarks.json").exists() }.associateWith { loadPlate(it, platesJson[it]!!.jsonObject) }
        if (plates.isEmpty()) return
        val shots = manifest["shots"]!!.jsonArray.map { it.jsonObject }.associateBy { it.str("id") }

        // ---- 1. nearest library poses at every snap
        val snapResults = mutableListOf<SnapResult>()
        for (snap in manifest["snaps"]!!.jsonArray.map { it.jsonObject }) {
            val shotId = snap.str("shot")
            val shot = shots[shotId] ?: continue
            val plate = plates[shotId] ?: continue
            val couple = snap.str("pose").contains("couple", ignoreCase = true)
            snapResults += evaluateSnap(plate, shot, snap.float("sourceTime"), if (couple) 2 else 1, snap.str("pose"), alternateOf = null)
        }
        for ((id, plate) in plates) {
            if (plate.alternateOf == null) continue
            val shot = shots[plate.alternateOf] ?: continue
            val k = id.substringAfter("-alt").toInt() - 1
            val alt = shot["alternates"]!!.jsonArray[k].jsonObject
            val pose = alt.str("pose")
            val couple = pose.contains("couple", ignoreCase = true)
            snapResults += evaluateSnap(plate, shot, alt.float("keyTime"), if (couple) 2 else 1, pose, alternateOf = plate.alternateOf)
        }
        // the act III guide's defining frame: is the manifest's guide the nearest library pose there?
        val live = shots["b5-live"]
        val liveRef = plates["b5-live.1x"] ?: plates["b5-live"]
        val liveDefining = live?.get("guide")?.jsonObject?.get("definingFrame")?.jsonObject?.float("sourceTime")
        val liveNearest = if (live != null && liveRef != null && liveDefining != null) {
            evaluateSnap(liveRef, live, liveDefining, 1, live["guide"]!!.jsonObject.str("poseId"), alternateOf = null, window = false)
        } else {
            null
        }
        File(dir, "nearest.json").writeText(nearestJson(manifest, snapResults, liveNearest, distinctPicks(snapResults)))

        // ---- 2. per-plate match runs
        var runs = 0
        if (live != null && liveRef != null && liveDefining != null) {
            val guideSpec = live["guide"]!!.jsonObject
            val defining = liveRef.frameAtSource(liveDefining)
            fun guideFor(poseId: String): Guide {
                val template = BuiltInPoses.byId(poseId) ?: error("no library pose $poseId")
                val transform = solveTransform(template, mirrored = false, people = defining.people.take(1))
                return Guide(template, mirrored = false, transform, definedAt = defining, definedOn = liveRef.id, origin = "library")
            }
            // The manifest's acceptance procedure: the planned guide at the default strictness; if it never locks (the
            // 700 ms hold never completes, so no countdown starts), the listed fallbacks in order: the Pocket stroll
            // guide at the same placement (fitted the same way at the same frame), then the next real strictness,
            // Balanced 0.75 (never below). The real-time plate decides: the time remap is set from its countdown start
            // (scripts/footage-match.mjs), and only the remapped plate can show the capture (at 1x her hold ends
            // inside the countdown).
            val attempts = listOf(
                guideSpec.str("poseId") to DEFAULT_STRICTNESS,
                "walk_pockets" to DEFAULT_STRICTNESS,
                guideSpec.str("poseId") to BALANCED_STRICTNESS,
                "walk_pockets" to BALANCED_STRICTNESS,
            )
            val measure = plates["b5-live.1x"] ?: plates["b5-live"]!!
            val tried = attempts.map { (poseId, threshold) ->
                val guide = guideFor(poseId)
                Attempt(guide, threshold, measure.id, runCoach(measure, guide, threshold).second)
            }
            val chosen = tried.firstOrNull { it.events.digits.isNotEmpty() } ?: tried.first()
            val attemptsJson = tried.joinToString(",\n    ", "[\n    ", "\n  ]") { a ->
                "{\"poseId\":${str(a.guide.template.id)},\"threshold\":${fmt4(a.threshold)},\"plate\":${str(a.plate)},\"transform\":${transformJson(a.guide.transform)}," +
                    "\"chosen\":${a === chosen},\"events\":${a.events.json()}}"
            }
            for (id in listOf("b5-live.1x", "b5-live")) {
                val plate = plates[id] ?: continue
                File(dir, "$id.match.json").writeText(runCoach(plate, chosen.guide, chosen.threshold, attemptsJson).first)
                runs++
            }
            // the app's "pose from a photo" path on the defining frame, for comparison
            val photoDetect = File(dir, "b5-live.guide-photo.json")
            val plate = plates["b5-live"]
            if (photoDetect.exists() && plate != null) {
                val photoTemplate = photoTemplate(photoDetect)
                if (photoTemplate != null) {
                    val photoGuide = Guide(photoTemplate, mirrored = false, OverlayTransform.IDENTITY, definedAt = defining, definedOn = liveRef.id, origin = "photo")
                    File(dir, "b5-live.photo-guide.match.json").writeText(runCoach(plate, photoGuide, DEFAULT_STRICTNESS).first)
                    runs++
                }
            }
        }
        for (snap in snapResults) {
            val plate = plates[snap.plate] ?: continue
            val pick = snap.pick
            if (pick == null) {
                File(dir, "${plate.id}.match.json").delete() // no snap: no guide, so no match file (drop a stale one)
                continue
            }
            val guide = Guide(pick.template, pick.mirrored, pick.transform, definedAt = snap.frame, definedOn = plate.id, origin = "snap")
            File(dir, "${plate.id}.match.json").writeText(runCoach(plate, guide, DEFAULT_STRICTNESS).first)
            runs++
        }
        assertTrue(runs > 0)
    }

    // ------------------------------------------------------------------------------------------------ data

    private class FrameLm(val n: Int, val f: Int, val src: Int, val people: List<Skeleton>)

    private class PlateLm(
        val id: String,
        val shot: String,
        val film: List<Int>,
        val filmFrom: Int,
        val shotStart: Int,
        val sourceFps: Float,
        val alternateOf: String?,
        val frames: List<FrameLm>,
    ) {
        fun sourceTime(fr: FrameLm) = fr.src / sourceFps

        /** The plate frame inside the shot showing the source frame nearest [t] (the first such frame). */
        fun frameAtSource(t: Float, inShot: Boolean = false): FrameLm {
            val pool = if (inShot) frames.filter { it.f >= film[0] && it.f < film[1] } else frames
            val target = (t * sourceFps).roundToInt()
            return pool.minWith(compareBy<FrameLm> { abs(it.src - target) }.thenBy { it.n })
        }
    }

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
            FrameLm(fr.int("n"), fr.int("f"), fr.int("src"), people)
        }
        return PlateLm(
            id = id,
            shot = plate.str("shot"),
            film = plate["film"]!!.jsonArray.map { it.jsonPrimitive.int },
            filmFrom = plate.int("filmFrom"),
            shotStart = plate.int("shotStart"),
            sourceFps = plate["source"]!!.jsonObject.float("fps"),
            alternateOf = plate["alternateOf"]?.jsonPrimitive?.content,
            frames = frames,
        )
    }

    // ------------------------------------------------------------------------------------------- the guide fit

    /**
     * The OverlayTransform a user would drag and pinch to: the template's mid-hip (mid-shoulder where the person's hips
     * are not visible) onto the person's, the template's torso length onto the person's. For two figures the pair's
     * mean anchor and mean torso length (figures and people both taken left to right). No rotation.
     */
    private fun solveTransform(template: PoseTemplate, mirrored: Boolean, people: List<Skeleton>): OverlayTransform {
        val figures = SceneMapping.templateToScene(template, ASPECT, OverlayTransform.IDENTITY, mirrored)
        if (figures.isEmpty() || people.isEmpty()) return OverlayTransform.IDENTITY
        val centre = SceneMapping.fitFrame(template.frameAspect, ASPECT).center
        val figs = figures.sortedBy { it.centreX() }
        val persons = people.map { it.visibleOnly() }.sortedBy { it.centreX() }
        val pairs = figs.zip(persons)
        if (pairs.isEmpty()) return OverlayTransform.IDENTITY
        var tx = 0f; var ty = 0f; var px = 0f; var py = 0f; var tu = 0f; var pu = 0f
        for ((fig, person) in pairs) {
            val hips = person.midHip() != null && fig.midHip() != null
            val ta = (if (hips) fig.midHip() else fig.midShoulder()) ?: fig.points.values.first()
            val pa = (if (hips) person.midHip() else person.midShoulder()) ?: person.points.values.first()
            tx += ta.x; ty += ta.y; px += pa.x; py += pa.y
            tu += torso(fig, hips); pu += torso(person, hips)
        }
        val k = pairs.size.toFloat()
        val scale = ((pu / k) / (tu / k)).takeIf { it.isFinite() && it > 0f }?.coerceIn(OverlayTransform.MIN_SCALE, OverlayTransform.MAX_SCALE) ?: 1f
        val ax = tx / k; val ay = ty / k
        val offsetX = (px / k - centre.x - (ax - centre.x) * scale).coerceIn(-MAX_OFFSET, MAX_OFFSET)
        val offsetY = (py / k - centre.y - (ay - centre.y) * scale).coerceIn(-MAX_OFFSET, MAX_OFFSET)
        return OverlayTransform(offsetX = offsetX, offsetY = offsetY, scale = scale, rotation = 0f)
    }

    /** Torso length in isotropic scene heights: mid-hip to mid-shoulder, or the app's estimate without hips. */
    private fun torso(s: Skeleton, hips: Boolean): Float {
        val ms = s.midShoulder()
        val mh = s.midHip()
        if (hips && ms != null && mh != null) return hypot((ms.x - mh.x) * ASPECT, ms.y - mh.y)
        return s.copy(unit = 0f).estimatedUnit(ASPECT)
    }

    private fun Skeleton.centreX(): Float = (midHip() ?: midShoulder() ?: points.values.firstOrNull())?.x ?: 0.5f

    /** CameraViewModel.visibleOnly: the joints the overlay draws (visibility >= 0.5). */
    private fun Skeleton.visibleOnly(): Skeleton = copy(points = points.filterKeys { isVisible(it, LIVE_MIN_VISIBILITY) })

    private fun match(scene: List<Skeleton>, people: List<Skeleton>, threshold: Float): MatchResult = PoseMatcher.match(
        templateFigures = scene,
        detected = people,
        sceneAspect = ASPECT,
        audience = FramingAudience.PHOTOGRAPHER,
        config = MatchConfig(perfectScore = threshold),
        mirroredView = false,
    )

    // ------------------------------------------------------------------------------------------------- snaps

    /**
     * @param fit the share (by segment weight) of what the person shows inside the frame that the template also
     *   requires: a template mostly pushed out of the frame scores well on the few segments left (the matcher only
     *   compares what the template has inside the scene), but it does not sit on the body, so the snap rule needs it
     */
    private class Candidate(
        val template: PoseTemplate,
        val mirrored: Boolean,
        val transform: OverlayTransform,
        val result: MatchResult,
        val fit: Float,
    ) {
        val score get() = result.score
        val usable get() = fit >= SNAP_MIN_FIT
    }

    /** Segments the person shows inside the scene, and how many of them (by weight) the template requires. */
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

    private class SnapResult(
        val shot: String,
        val plate: String,
        val alternateOf: String?,
        val keyTime: Float,
        val frame: FrameLm,
        val sourceTime: Float,
        val people: Int,
        val needed: Int,
        val planned: String,
        val ranking: List<Candidate>,
        val pick: Candidate?,
        val windowBest: Pair<FrameLm, Candidate>?,
        val windowSourceTime: Float?,
    )

    private fun rank(people: List<Skeleton>, personCount: Int): List<Candidate> {
        if (people.isEmpty()) return emptyList()
        val out = mutableListOf<Candidate>()
        for (template in BuiltInPoses.all) {
            if (template.personCount != personCount) continue
            for (mirrored in listOf(false, true)) {
                val transform = solveTransform(template, mirrored, people)
                val scene = SceneMapping.templateToScene(template, ASPECT, transform, mirrored)
                val result = match(scene, people, DEFAULT_STRICTNESS)
                out += Candidate(template, mirrored, transform, result, fitOf(result, people))
            }
        }
        return out.sortedByDescending { it.score }
    }

    private fun evaluateSnap(
        plate: PlateLm,
        shot: JsonObject,
        keyTime: Float,
        personCount: Int,
        planned: String,
        alternateOf: String?,
        window: Boolean = true,
    ): SnapResult {
        val frame = plate.frameAtSource(keyTime, inShot = false)
        val ranking = rank(frame.people, personCount)
        val threshold = if (personCount > 1) SNAP_MIN_COUPLE else SNAP_MIN
        val pick = ranking.firstOrNull { it.usable && (it.template.id !in CHORUS_EXCLUDED || alternateOf == null && shot.str("id") == "b5-live") }
            ?.takeIf { it.score >= threshold }
        // the best frame inside the in-point window (the snap rule lets keyTime move there)
        var best: Pair<FrameLm, Candidate>? = null
        val win = shot["source"]?.jsonObject?.get("window")?.jsonArray?.map { it.jsonPrimitive.float }
        if (window && win != null && alternateOf == null) {
            val lo = (win[0] * plate.sourceFps).roundToInt()
            val hi = (win[1] * plate.sourceFps).roundToInt()
            val seen = HashSet<Int>()
            for (fr in plate.frames) {
                if (fr.src < lo || fr.src > hi || !seen.add(fr.src)) continue
                val c = rank(fr.people, personCount).firstOrNull { it.usable && it.template.id !in CHORUS_EXCLUDED } ?: continue
                if (best == null || c.score > best.second.score) best = fr to c
            }
        }
        return SnapResult(
            shot = alternateOf ?: shot.str("id"),
            plate = plate.id,
            alternateOf = alternateOf,
            keyTime = keyTime,
            frame = frame,
            sourceTime = plate.sourceTime(frame),
            people = frame.people.size,
            needed = personCount,
            planned = planned,
            ranking = ranking,
            pick = pick,
            windowBest = best,
            windowSourceTime = best?.let { plate.sourceTime(it.first) },
        )
    }

    /**
     * The chorus with no pose twice: in film order (main snaps only), each snap takes its best usable pose at or above
     * the snap threshold that no earlier snap took (a pose and its flip count as one pose).
     */
    private fun distinctPicks(snaps: List<SnapResult>): Map<String, Candidate?> {
        val used = HashSet<String>()
        val out = LinkedHashMap<String, Candidate?>()
        for (s in snaps.filter { it.alternateOf == null }.sortedBy { it.frame.f }) {
            val threshold = if (s.needed > 1) SNAP_MIN_COUPLE else SNAP_MIN
            val c = s.ranking.firstOrNull { it.usable && it.score >= threshold && it.template.id !in CHORUS_EXCLUDED && it.template.id !in used }
            if (c != null) used += c.template.id
            out[s.plate] = c
        }
        return out
    }

    private fun nearestJson(manifest: JsonObject, snaps: List<SnapResult>, live: SnapResult?, distinct: Map<String, Candidate?>): String {
        val sb = StringBuilder()
        sb.append("{\n  \"source\": \"app PoseMatcher over every library template (PromoFootageMatch.kt), smoothed detections from <plate>.landmarks.json\",\n")
        sb.append("  \"camera\": {\"lens\":\"BACK\",\"mirroredView\":false,\"audience\":\"PHOTOGRAPHER\",\"strictness\":").append(fmt(DEFAULT_STRICTNESS)).append("},\n")
        sb.append("  \"fit\": \"each template is mapped with SceneMapping.templateToScene and an OverlayTransform solved to the person (mid-hip, torso length), as drawn and flipped (the app's flip toggle)\",\n")
        sb.append("  \"rule\": ").append(str(manifest["snapRule"]?.jsonPrimitive?.content ?: "")).append(",\n")
        sb.append("  \"snapMin\": ").append(fmt(SNAP_MIN)).append(", \"snapMinCouple\": ").append(fmt(SNAP_MIN_COUPLE)).append(", \"snapMinFit\": ").append(fmt(SNAP_MIN_FIT)).append(",\n")
        sb.append("  \"fitNote\": \"fit = share (by matcher segment weight) of the segments the person shows inside the frame that the fitted template also requires; a pick needs fit >= snapMinFit, so a template pushed mostly out of the frame cannot win on the few segments left\",\n")
        sb.append("  \"excludedFromChorus\": [").append(CHORUS_EXCLUDED.joinToString(",") { str(it) }).append("],\n")
        sb.append("  \"snaps\": [\n")
        sb.append(snaps.joinToString(",\n") { snapJson(it, "    ") })
        sb.append("\n  ],\n")
        sb.append("  \"distinct\": {\"note\": \"the chorus with no pose twice: in film order each snap takes its best usable pose >= the snap threshold that no earlier snap took\",\n    ")
        sb.append(distinct.entries.joinToString(",\n    ") { (k, c) -> str(k) + ":" + (c?.let { candidateJson(it, full = true) } ?: "null") })
        sb.append("}")
        if (live != null) sb.append(",\n  \"liveGuide\": ").append(snapJson(live, "  ").trimStart())
        sb.append("\n}\n")
        return sb.toString()
    }

    private fun snapJson(s: SnapResult, indent: String): String {
        val sb = StringBuilder()
        sb.append(indent).append("{\"shot\":").append(str(s.shot)).append(",\"plate\":").append(str(s.plate))
        if (s.alternateOf != null) sb.append(",\"alternateOf\":").append(str(s.alternateOf))
        sb.append(",\"keyTime\":").append(fmt(s.keyTime))
        sb.append(",\"frame\":{\"n\":").append(s.frame.n).append(",\"f\":").append(s.frame.f).append(",\"src\":").append(s.frame.src)
            .append(",\"sourceTime\":").append(fmt(s.sourceTime)).append("}")
        sb.append(",\"peopleDetected\":").append(s.people).append(",\"peopleNeeded\":").append(s.needed)
        val plannedRank = s.ranking.indexOfFirst { it.template.id == s.planned }
        val plannedBest = s.ranking.getOrNull(plannedRank)
        sb.append(",\n").append(indent).append(" \"planned\":{\"poseId\":").append(str(s.planned))
        if (plannedBest != null) {
            sb.append(",\"score\":").append(fmt(plannedBest.score)).append(",\"mirrored\":").append(plannedBest.mirrored)
                .append(",\"rank\":").append(plannedRank + 1)
        }
        sb.append("}")
        sb.append(",\n").append(indent).append(" \"pick\":")
        sb.append(s.pick?.let { candidateJson(it, full = true) } ?: "null")
        sb.append(",\n").append(indent).append(" \"ranking\":[")
        sb.append(s.ranking.take(8).joinToString(",") { candidateJson(it, full = false) })
        sb.append("]")
        val wb = s.windowBest
        if (wb != null) {
            sb.append(",\n").append(indent).append(" \"windowBest\":{\"n\":").append(wb.first.n).append(",\"f\":").append(wb.first.f)
                .append(",\"src\":").append(wb.first.src).append(",\"sourceTime\":").append(fmt(s.windowSourceTime!!))
                .append(",\"pose\":").append(candidateJson(wb.second, full = false)).append("}")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun candidateJson(c: Candidate, full: Boolean): String {
        val sb = StringBuilder("{\"poseId\":").append(str(c.template.id)).append(",\"name\":").append(str(c.template.name))
            .append(",\"mirrored\":").append(c.mirrored).append(",\"score\":").append(fmt(c.score)).append(",\"fit\":").append(fmt(c.fit))
        if (full) {
            sb.append(",\"transform\":").append(transformJson(c.transform))
            val fig = c.result.figures
            sb.append(",\"parts\":[").append(fig.joinToString(",") { f -> "{" + f.partScores.entries.joinToString(",") { (k, v) -> "\"${k.name}\":${fmt(v)}" } + "}" }).append("]")
            sb.append(",\"coverage\":[").append(fig.joinToString(",") { fmt(it.coverage) }).append("]")
            sb.append(",\"topHint\":").append(c.result.hints.firstOrNull()?.let { str(it.text) } ?: "null")
        }
        return sb.append("}").toString()
    }

    // ------------------------------------------------------------------------------------------ the coach run

    private class Guide(
        val template: PoseTemplate,
        val mirrored: Boolean,
        val transform: OverlayTransform,
        val definedAt: FrameLm,
        val definedOn: String,
        val origin: String,
    )

    /**
     * One live session from the shot's first film frame: every plate frame's smoothed people -> PoseMatcher ->
     * HintStabilizer (the top hint) -> CaptureCoach (the score), with the act's clock (frame - film[0]) * 1000 / 30.
     */
    private class Attempt(val guide: Guide, val threshold: Float, val plate: String, val events: Events)

    private fun runCoach(plate: PlateLm, guide: Guide, threshold: Float, attempts: String? = null): Pair<String, Events> {
        val scene = SceneMapping.templateToScene(guide.template, ASPECT, guide.transform, guide.mirrored)
        val stabilizer = HintStabilizer()
        val coach = CaptureCoach(CoachSettings(autoCapture = true, threshold = threshold, countdownSeconds = 3))
        val film0 = plate.film[0]
        val records = StringBuilder()
        var captured = false
        val frames = plate.frames.filter { it.f >= film0 }
        val events = Events()
        var prevShown: String? = null
        frames.forEachIndexed { i, fr ->
            val now = ((fr.f - film0) * 1000L) / FPS
            val m = match(scene, fr.people, threshold)
            val top = m.hints.firstOrNull()
            val shown = stabilizer.update(top, now)
            val state = if (captured) CaptureState.Cooldown(0f) else coach.update(m.score, now)
            if (state == CaptureState.Capture) captured = true
            val percent = if (m.peopleDetected > 0 && m.figures.isNotEmpty()) (m.score * 100f).roundToInt().coerceIn(0, 100) else null
            val fig = m.figures.firstOrNull()
            events.observe(fr.f, m.score, percent, threshold, shown?.text, shown?.kind?.name, prevShown, state)
            prevShown = shown?.text
            records.append("    {\"f\":").append(fr.f).append(",\"n\":").append(fr.n).append(",\"src\":").append(fr.src)
            records.append(",\"score\":").append(fmt4(m.score))
            records.append(",\"percent\":").append(percent?.toString() ?: "null")
            records.append(",\"assigned\":").append(fig?.detectedIndex != null)
            records.append(",\"parts\":{")
            records.append(fig?.partScores.orEmpty().entries.joinToString(",") { (k, v) -> "\"${k.name}\":${fmt4(v)}" })
            records.append("},\"segments\":{")
            records.append(fig?.segments.orEmpty().joinToString(",") { "\"${it.segment.name}\":${fmt4(it.score)}" })
            records.append("},\"hints\":[")
            records.append(m.hints.joinToString(",") { "{\"text\":${str(it.text)},\"kind\":\"${it.kind.name}\",\"severity\":${fmt4(it.severity)}}" })
            records.append("],\"shown\":")
            records.append(shown?.let { "{\"text\":${str(it.text)},\"kind\":\"${it.kind.name}\"}" } ?: "null")
            records.append(",\"capture\":").append(captureJson(state))
            records.append("}").append(if (i < frames.size - 1) ",\n" else "\n")
        }
        val sb = StringBuilder()
        sb.append("{\n  \"source\": \"app PoseMatcher + HintStabilizer + CaptureCoach (PromoFootageMatch.kt) over ").append(plate.id).append(".landmarks.json\",\n")
        sb.append("  \"plate\": ").append(str(plate.id)).append(",\n  \"shot\": ").append(str(plate.shot)).append(",\n")
        sb.append("  \"aspect\": ").append(fmt4(ASPECT)).append(",\n  \"fps\": ").append(FPS).append(",\n")
        sb.append("  \"camera\": {\"lens\":\"BACK\",\"mirroredView\":false,\"audience\":\"PHOTOGRAPHER\"},\n")
        sb.append("  \"coach\": {\"threshold\":").append(fmt4(threshold)).append(",\"holdMillis\":").append(coach.settings.holdMillis)
            .append(",\"countdownSeconds\":").append(coach.settings.countdownSeconds).append(",\"abortMargin\":").append(fmt4(coach.settings.abortMargin))
            .append(",\"clock\":\"(f - ").append(film0).append(") * 1000 / 30 ms\"},\n")
        sb.append("  \"guide\": {\"origin\":").append(str(guide.origin)).append(",\"poseId\":").append(str(guide.template.id))
            .append(",\"name\":").append(str(guide.template.name)).append(",\"mirrored\":").append(guide.mirrored)
            .append(",\"frameAspect\":").append(fmt4(guide.template.frameAspect))
            .append(",\"transform\":").append(transformJson(guide.transform))
            .append(",\"definedAt\":{\"plate\":").append(str(guide.definedOn)).append(",\"n\":").append(guide.definedAt.n)
            .append(",\"f\":").append(guide.definedAt.f).append(",\"src\":").append(guide.definedAt.src).append("}")
            .append(",\n    \"figures\":[").append(scene.joinToString(",") { skeletonJson(it) }).append("]},\n")
        sb.append("  \"events\": ").append(events.json()).append(",\n")
        if (attempts != null) sb.append("  \"attempts\": ").append(attempts).append(",\n")
        sb.append("  \"frames\": [\n").append(records).append("  ]\n}\n")
        return sb.toString() to events
    }

    private class Events {
        var found: Int? = null
        var matchFrom: Int? = null
        val cards = mutableListOf<Triple<Int, String, String>>()
        var hold: Int? = null
        val digits = sortedMapOf<Int, Int>(compareByDescending { it })
        var capture: Int? = null
        val crossings = mutableListOf<Pair<Int, Boolean>>()
        private var above = false

        fun observe(f: Int, score: Float, percent: Int?, threshold: Float, shown: String?, kind: String?, prevShown: String?, state: CaptureState) {
            if (found == null && percent != null) found = f
            val nowAbove = score >= threshold
            if (nowAbove != above) {
                crossings += f to nowAbove
                above = nowAbove
                if (nowAbove && matchFrom == null) matchFrom = f
            }
            if (shown != null && shown != prevShown) cards += Triple(f, shown, kind ?: "")
            if (hold == null && state is CaptureState.Holding) hold = f
            if (state is CaptureState.Countdown && state.secondsLeft !in digits) digits[state.secondsLeft] = f
            if (capture == null && state == CaptureState.Capture) capture = f
        }

        fun json(): String = buildString {
            append("{\"found\":").append(found ?: "null")
            append(",\"matchFrom\":").append(matchFrom ?: "null")
            append(",\"crossings\":[").append(crossings.joinToString(",") { "{\"f\":${it.first},\"match\":${it.second}}" }).append("]")
            append(",\"cards\":[").append(cards.joinToString(",") { "{\"f\":${it.first},\"text\":${str(it.second)},\"kind\":\"${it.third}\"}" }).append("]")
            append(",\"hold\":").append(hold ?: "null")
            append(",\"countdown\":{").append(digits.entries.joinToString(",") { "\"${it.key}\":${it.value}" }).append("}")
            append(",\"capture\":").append(capture ?: "null").append("}")
        }
    }

    // ------------------------------------------------------------------------------ the photo-guide comparison

    /** CreateFromPhotoViewModel: the photo's people (image-normalized) -> CustomPoseFactory.fromDetection. */
    private fun photoTemplate(file: File): PoseTemplate? {
        val d = json.parseToJsonElement(file.readText()).jsonObject
        val w = d.int("image_width")
        val h = d.int("image_height")
        val people = d["frames_data"]!!.jsonArray.firstOrNull()?.jsonObject?.get("people")?.jsonArray ?: return null
        val aspect = w.toFloat() / h
        val skeletons = people.map { p ->
            val raw = p.jsonArray.map { lm ->
                val a = lm.jsonArray
                RawLandmark(a[0].jsonPrimitive.float, a[1].jsonPrimitive.float, a[3].jsonPrimitive.floatOrNull ?: 1f, a[4].jsonPrimitive.floatOrNull ?: 1f)
            }
            LandmarkMapping.toScene(raw, w, h, aspect)
        }
        val t = CustomPoseFactory.fromDetection(
            id = "promo_photo_guide",
            name = "From her photo",
            category = PoseCategory.STANDING,
            people = skeletons,
            imageAspect = aspect,
            referenceImage = null,
            createdAt = 0L,
        )
        return t.takeIf { it.figures.isNotEmpty() }
    }

    // ---------------------------------------------------------------------------------------------- helpers

    private fun captureJson(s: CaptureState): String = when (s) {
        CaptureState.Idle -> "{\"state\":\"Idle\"}"
        is CaptureState.Holding -> "{\"state\":\"Holding\",\"progress\":${fmt4(s.progress)}}"
        is CaptureState.Countdown -> "{\"state\":\"Countdown\",\"seconds\":${s.secondsLeft},\"progress\":${fmt4(s.progress)}}"
        CaptureState.Capture -> "{\"state\":\"Capture\"}"
        is CaptureState.Cooldown -> "{\"state\":\"Cooldown\",\"progress\":${fmt4(s.progress)}}"
    }

    private fun transformJson(t: OverlayTransform) =
        "{\"scale\":${fmt4(t.scale)},\"offsetX\":${fmt4(t.offsetX)},\"offsetY\":${fmt4(t.offsetY)},\"rotation\":${fmt4(t.rotation)}}"

    private fun skeletonJson(s: Skeleton): String {
        val joints = s.points.entries.joinToString(",") { (j, p) -> "\"${j.name}\":[${fmt5(p.x)},${fmt5(p.y)}]" }
        val vis = s.visibility.entries.joinToString(",") { (j, v) -> "\"${j.name}\":${fmt4(v)}" }
        return "{\"joints\":{$joints},\"visibility\":{$vis},\"unit\":${fmt5(s.unit)}}"
    }

    private fun JsonObject.str(k: String) = this[k]!!.jsonPrimitive.content
    private fun JsonObject.int(k: String) = this[k]!!.jsonPrimitive.int
    private fun JsonObject.float(k: String) = this[k]!!.jsonPrimitive.float

    private companion object {
        /** SceneAspect.RATIO_9_16: the film's frame and every plate. */
        const val ASPECT = 9f / 16f
        const val FPS = 30
        /** AppSettings.matchThreshold default ("strictness"): MatchConfig.perfectScore and the coach threshold. */
        const val DEFAULT_STRICTNESS = 0.80f
        /** The next real strictness setting ("Balanced"), the manifest's last fallback (never below). */
        const val BALANCED_STRICTNESS = 0.75f
        /** CameraViewModel.LIVE_MIN_VISIBILITY */
        const val LIVE_MIN_VISIBILITY = 0.5f
        /** OverlayMath.MAX_OFFSET */
        const val MAX_OFFSET = 0.9f
        /** The manifest snap rule: a snap needs this best score (0.75 for the couple). */
        const val SNAP_MIN = 0.80f
        const val SNAP_MIN_COUPLE = 0.75f
        /** "the drawn figure visibly sits on the body": the template requires at least this share of what the person shows */
        const val SNAP_MIN_FIT = 0.8f
        /** "Never show 'Hands in pockets' in the chorus: it is the punchline at f288." */
        val CHORUS_EXCLUDED = setOf("stand_hands_in_pockets")

        fun fmt4(v: Float) = String.format(Locale.US, "%.4f", v)
        fun fmt5(v: Float) = String.format(Locale.US, "%.5f", v)
        fun fmt(v: Float) = fmt4(v)
        fun str(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
}
