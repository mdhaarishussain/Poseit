package com.poseit.app.tools

import com.poseit.app.pose.Joint
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.detect.LandmarkMapping
import com.poseit.app.pose.detect.LandmarkSmoother
import com.poseit.app.pose.detect.RawLandmark
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
import kotlin.math.roundToLong

/**
 * The promo film's footage landmarks (promo/, STORYBOARD.md v2): what the app's live camera path does to each frame's
 * MediaPipe landmarks before it draws the skeleton and matches the pose, applied to the stock footage plates.
 *
 * Reads `promo/public/footage/plates.json` and every `promo/out/footage/<plate>.detect.json` (written by
 * `promo/scripts/detect.py` with the app's lite model, VIDEO mode, on the prepared 9:16 plate). For each frame, as
 * PoseDetector.toSkeletons and CameraViewModel.onDetection do on a phone:
 *  1. [LandmarkMapping.toScene] with the scene aspect 9:16 (the plate is the scene, so this is the identity crop):
 *     the 21 app joints, visibility = min(visibility, presence);
 *  2. [LandmarkSmoother] with the app's defaults (One Euro filter per joint coordinate, min cutoff 1.2 Hz, beta 0.03,
 *     d cutoff 1 Hz, speed scale 1000; exponential visibility, 120 ms; people tracked by torso centre), fed the plate
 *     frame timestamps at 30 fps.
 * The overlay then draws the joints with visibility >= 0.5 (CameraViewModel.visibleOnly) and the matcher reads the
 * smoothed skeletons.
 *
 * Writes `promo/public/footage/<plate>.landmarks.json`: per plate frame the 33 raw landmarks (x, y, z, visibility,
 * presence; normalized to the plate, not mirrored) and the smoothed skeletons (21 joints in Joint order as
 * [x, y, visibility], null where MediaPipe gave no point).
 *
 * Run: cd promo && npm run landmarks   (or ./gradlew :pose-core:test --tests 'com.poseit.app.tools.PromoFootageLandmarks')
 */
class PromoFootageLandmarks {

    @Test
    fun exportLandmarks() {
        val platesFile = File("../promo/public/footage/plates.json")
        if (!platesFile.exists()) return // the footage stage has not run; nothing to do in a normal test run
        val plates = Json.parseToJsonElement(platesFile.readText()).jsonObject["plates"]!!.jsonObject
        var written = 0
        for ((id, plateEl) in plates) {
            val detect = File("../promo/out/footage/$id.detect.json")
            if (!detect.exists()) continue
            val plate = plateEl.jsonObject
            val out = File("../promo/public/footage/$id.landmarks.json")
            out.writeText(export(id, plate, Json.parseToJsonElement(detect.readText()).jsonObject))
            written++
        }
        assertTrue(written >= 0)
    }

    private fun export(id: String, plate: JsonObject, det: JsonObject): String {
        val imageW = det["image_width"]!!.jsonPrimitive.int
        val imageH = det["image_height"]!!.jsonPrimitive.int
        val fps = det["fps"]!!.jsonPrimitive.float
        val sceneAspect = SCENE_ASPECT
        val smoother = LandmarkSmoother()
        val frames = det["frames_data"]!!.jsonArray
        val filmFrom = plate["filmFrom"]!!.jsonPrimitive.int
        val shotStart = plate["shotStart"]!!.jsonPrimitive.int
        val film = plate["film"]!!.jsonArray.map { it.jsonPrimitive.int }
        val sourceFrames = plate["sourceFrames"]!!.jsonArray.map { it.jsonPrimitive.int }
        check(frames.size == plate["frames"]!!.jsonPrimitive.int) { "$id: ${frames.size} detected frames, plate has ${plate["frames"]}" }
        check(kotlin.math.abs(fps - FPS) < 1e-3f) { "$id: detection ran at $fps fps" }

        var detected = 0
        var shotDetected = 0
        var peopleMax = 0
        var firstDetected = -1
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"source\": \"app LandmarkMapping.toScene + LandmarkSmoother (pose-core PromoFootageLandmarks.kt) over scripts/detect.py\",\n")
        sb.append("  \"plate\": ").append(str(id)).append(",\n")
        sb.append("  \"shot\": ").append(str(plate["shot"]!!.jsonPrimitive.content)).append(",\n")
        sb.append("  \"catalogue\": ").append(str(plate["catalogue"]!!.jsonPrimitive.content)).append(",\n")
        sb.append("  \"film\": [").append(film.joinToString(",")).append("],\n")
        sb.append("  \"filmFrom\": ").append(filmFrom).append(",\n")
        sb.append("  \"shotStart\": ").append(shotStart).append(",\n")
        sb.append("  \"fps\": ").append(FPS.toInt()).append(",\n")
        sb.append("  \"aspect\": ").append(fmt(sceneAspect)).append(",\n")
        sb.append("  \"image\": [").append(imageW).append(",").append(imageH).append("],\n")
        sb.append("  \"model\": ").append(str(det["model"]!!.jsonPrimitive.content)).append(",\n")
        sb.append("  \"runningMode\": ").append(str(det["running_mode"]!!.jsonPrimitive.content)).append(",\n")
        sb.append("  \"numPoses\": ").append(det["num_poses"]!!.jsonPrimitive.int).append(",\n")
        sb.append("  \"smoother\": {\"minCutoff\":").append(fmt(smoother.minCutoff)).append(",\"beta\":").append(fmt(smoother.beta))
            .append(",\"dCutoff\":").append(fmt(smoother.dCutoff)).append(",\"speedScale\":").append(fmt(smoother.speedScale)).append("},\n")
        sb.append("  \"joints\": [").append(Joint.entries.joinToString(",") { str(it.name) }).append("],\n")
        sb.append("  \"landmarkNames\": [").append(det["landmark_names"]!!.jsonArray.joinToString(",") { str(it.jsonPrimitive.content) }).append("],\n")
        val body = StringBuilder()
        frames.forEachIndexed { n, el ->
            val fr = el.jsonObject
            check(fr["i"]!!.jsonPrimitive.int == n) { "$id: frame $n out of order" }
            val rawPeople = fr["people"]!!.jsonArray.map { it.jsonArray }
            val timestampMs = (n * 1000.0 / FPS).roundToLong()
            val scene = rawPeople.map { person -> LandmarkMapping.toScene(person.toRaw(), imageW, imageH, sceneAspect) }
            val smoothed = smoother.smooth(scene, timestampMs)
            if (rawPeople.isNotEmpty()) {
                detected++
                if (firstDetected < 0) firstDetected = n
                if (filmFrom + n >= film[0] && filmFrom + n < film[1]) shotDetected++
            }
            if (rawPeople.size > peopleMax) peopleMax = rawPeople.size
            body.append("    {\"n\":").append(n).append(",\"f\":").append(filmFrom + n).append(",\"src\":").append(sourceFrames[n])
            body.append(",\"t\":").append(timestampMs)
            body.append(",\"raw\":[")
            body.append(rawPeople.joinToString(",") { person ->
                "[" + person.joinToString(",") { lm ->
                    val a = lm.jsonArray
                    "[" + (0 until 5).joinToString(",") { k -> fmt(a[k].jsonPrimitive.float) } + "]"
                } + "]"
            })
            body.append("],\"people\":[")
            body.append(smoothed.joinToString(",") { s -> skeletonJson(s) })
            body.append("]}").append(if (n < frames.size - 1) ",\n" else "\n")
        }
        val shotFrames = film[1] - film[0]
        sb.append("  \"summary\": {\"frames\":").append(frames.size)
            .append(",\"detectionRate\":").append(fmt(detected.toFloat() / frames.size))
            .append(",\"shotDetectionRate\":").append(fmt(shotDetected.toFloat() / shotFrames))
            .append(",\"peopleMax\":").append(peopleMax)
            .append(",\"firstDetected\":").append(firstDetected).append("},\n")
        sb.append("  \"frames\": [\n").append(body).append("  ]\n}\n")
        return sb.toString()
    }

    private fun JsonArray.toRaw(): List<RawLandmark> = map { el ->
        val a = el.jsonArray
        RawLandmark(
            x = a[0].jsonPrimitive.float,
            y = a[1].jsonPrimitive.float,
            visibility = a[3].jsonPrimitive.float,
            presence = a[4].jsonPrimitive.float,
        )
    }

    private fun skeletonJson(s: Skeleton): String = Joint.entries.joinToString(",", "[", "]") { j ->
        val p = s[j] ?: return@joinToString "null"
        "[" + fmt(p.x) + "," + fmt(p.y) + "," + fmt(s.visibility[j] ?: 1f) + "]"
    }

    private fun fmt(v: Float) = String.format(Locale.US, "%.5f", v).trimEnd('0').let { if (it.endsWith(".")) it + "0" else it }

    private fun str(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        const val FPS = 30f

        /** SceneAspect.RATIO_9_16 (app/.../camera/PoseCamera.kt): the film's frame and the plates. */
        const val SCENE_ASPECT = 9f / 16f
    }
}
