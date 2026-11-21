package com.poseit.app.promo

import com.poseit.app.pose.Joint
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.coach.CaptureCoach
import com.poseit.app.pose.coach.CaptureState
import com.poseit.app.pose.coach.CoachSettings
import com.poseit.app.pose.match.FramingAudience
import com.poseit.app.pose.match.MatchConfig
import com.poseit.app.pose.match.PoseMatcher
import com.poseit.app.ui.camera.HintStabilizer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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
import kotlin.math.roundToInt

/**
 * The promo film's live match (promo/, act III): runs the app's own PoseMatcher, HintStabilizer and CaptureCoach,
 * frame by frame at 30 fps, over the keyframed performer and the Disco point guide that `promo/scripts/export-live.ts`
 * wrote to `promo/public/live.json` (scene space, 9:16, front camera), with the app's defaults: SUBJECT audience,
 * strictness 0.80, auto-capture on, 700 ms hold, 3 s countdown, mirroredView = true.
 *
 * Writes `promo/public/match.json`: per frame the score and the badge percent, the per-part scores (the guide's limb
 * colours come from these through Studio.forScore), the matcher's top hint, the hint the stabiliser would display,
 * and the capture state (Idle / Holding progress / Countdown seconds / Capture). The film shows exactly these.
 *
 * Run: cd promo && npm run match   (or ./gradlew :app:testDebugUnitTest --tests 'com.poseit.app.promo.PromoMatchExport')
 */
class PromoMatchExport {

    @Test
    fun exportMatch() {
        val input = File("../promo/public/live.json")
        if (!input.exists()) return // the film's export step has not run; nothing to do in a normal test run
        val root = Json.parseToJsonElement(input.readText()).jsonObject
        val aspect = root["aspect"]!!.jsonPrimitive.float
        val fps = root["fps"]!!.jsonPrimitive.int
        val guide = skeleton(root["guide"]!!.jsonObject)

        val stabilizer = HintStabilizer()
        val coach = CaptureCoach(CoachSettings(autoCapture = true, threshold = 0.80f, countdownSeconds = 3))
        val out = StringBuilder()
        out.append("{\n  \"source\": \"app PoseMatcher + HintStabilizer + CaptureCoach (PromoMatchExport.kt)\",\n")
        out.append("  \"aspect\": ").append(fmt(aspect)).append(",\n  \"frames\": [\n")
        val frames = root["frames"]!!.jsonArray
        var captured = false
        frames.forEachIndexed { i, el ->
            val fr = el.jsonObject
            val f = fr["f"]!!.jsonPrimitive.int
            val now = ((f - frames[0].jsonObject["f"]!!.jsonPrimitive.int) * 1000L) / fps
            val personEl: JsonElement = fr["person"] ?: JsonNull
            val people = if (personEl is JsonObject) listOf(skeleton(personEl)) else emptyList()
            val match = PoseMatcher.match(
                templateFigures = listOf(guide),
                detected = people,
                sceneAspect = aspect,
                audience = FramingAudience.SUBJECT,
                config = MatchConfig(perfectScore = 0.80f),
                mirroredView = true,
            )
            val top = match.hints.firstOrNull()
            val shown = stabilizer.update(top, now)
            val state = if (captured) CaptureState.Cooldown(0f) else coach.update(match.score, now)
            if (state == CaptureState.Capture) captured = true
            val percent = if (match.peopleDetected > 0 && match.figures.isNotEmpty()) {
                (match.score * 100f).roundToInt().coerceIn(0, 100)
            } else {
                null
            }
            val fig = match.figures.firstOrNull()
            out.append("    {\"f\":").append(f)
            out.append(",\"score\":").append(fmt(match.score))
            out.append(",\"percent\":").append(percent?.toString() ?: "null")
            out.append(",\"assigned\":").append(fig?.detectedIndex != null)
            out.append(",\"parts\":{")
            out.append(fig?.partScores.orEmpty().entries.joinToString(",") { (k, v) -> "\"${k.name}\":${fmt(v)}" })
            out.append("},\"segments\":{")
            out.append(fig?.segments.orEmpty().joinToString(",") { "\"${it.segment.name}\":${fmt(it.score)}" })
            out.append("},\"hints\":[")
            out.append(match.hints.joinToString(",") { "{\"text\":${str(it.text)},\"kind\":\"${it.kind.name}\",\"severity\":${fmt(it.severity)}}" })
            out.append("],\"shown\":")
            out.append(shown?.let { "{\"text\":${str(it.text)},\"kind\":\"${it.kind.name}\"}" } ?: "null")
            out.append(",\"capture\":").append(captureJson(state))
            out.append("}").append(if (i < frames.size - 1) ",\n" else "\n")
        }
        out.append("  ]\n}\n")
        val file = File("../promo/public/match.json")
        file.writeText(out.toString())
        assertTrue(file.length() > 1000)
    }

    private fun skeleton(o: JsonObject): Skeleton {
        val joints = o["joints"]!!.jsonObject
        val vis = o["visibility"]?.jsonObject
        val points = joints.entries.associate { (k, v) ->
            val a = v.jsonArray
            Joint.valueOf(k) to Vec2(a[0].jsonPrimitive.float, a[1].jsonPrimitive.float)
        }
        val visibility = vis?.entries?.associate { (k, v) -> Joint.valueOf(k) to v.jsonPrimitive.float }.orEmpty()
        return Skeleton(points = points, visibility = visibility, unit = o["unit"]!!.jsonPrimitive.float)
    }

    private fun captureJson(s: CaptureState): String = when (s) {
        CaptureState.Idle -> "{\"state\":\"Idle\"}"
        is CaptureState.Holding -> "{\"state\":\"Holding\",\"progress\":${fmt(s.progress)}}"
        is CaptureState.Countdown -> "{\"state\":\"Countdown\",\"seconds\":${s.secondsLeft},\"progress\":${fmt(s.progress)}}"
        CaptureState.Capture -> "{\"state\":\"Capture\"}"
        is CaptureState.Cooldown -> "{\"state\":\"Cooldown\",\"progress\":${fmt(s.progress)}}"
    }

    private fun fmt(v: Float) = String.format(Locale.US, "%.4f", v)

    private fun str(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
