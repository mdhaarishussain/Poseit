package com.poseit.app.pose.io

import com.poseit.app.pose.CameraAdvice
import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.Skeleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * JSON (de)serialization of user-created poses. Must be lenient: ignore unknown keys, never crash on bad input.
 *
 * Format: a versioned wrapper `{"version":1,"poses":[...]}` where each pose is a serialized [PoseTemplate].
 * Decoding is element-wise, so one corrupt pose does not lose the others; a bare array of poses is accepted too.
 */
object PoseJson {
    const val FORMAT_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        // Write every field so stored poses keep their meaning even if a default changes in a later version.
        encodeDefaults = true
    }

    @Serializable
    private data class PoseFile(
        @SerialName("version") val version: Int = FORMAT_VERSION,
        @SerialName("poses") val poses: List<PoseTemplate> = emptyList(),
    )

    fun encode(poses: List<PoseTemplate>): String =
        json.encodeToString(PoseFile.serializer(), PoseFile(FORMAT_VERSION, poses.map(::sanitize)))

    /** Returns an empty list for blank/corrupt input. */
    fun decode(json: String): List<PoseTemplate> {
        if (json.isBlank()) return emptyList()
        // Untrusted file content: any parse failure means "no poses", never a crash.
        val root = try {
            this.json.parseToJsonElement(json)
        } catch (e: RuntimeException) {
            return emptyList()
        } catch (e: StackOverflowError) {
            return emptyList()
        }
        val entries: List<JsonElement> = when (root) {
            is JsonObject -> root["poses"] as? JsonArray ?: return emptyList()
            is JsonArray -> root
            else -> return emptyList()
        }
        return entries
            .mapNotNull(::decodeEntry)
            .distinctBy { it.id }
    }

    private fun decodeEntry(element: JsonElement): PoseTemplate? {
        val pose = try {
            json.decodeFromJsonElement(PoseTemplate.serializer(), element)
        } catch (e: RuntimeException) {
            return null // corrupt entry (SerializationException, bad number, unknown enum…): skip just this one
        }
        return sanitize(pose).takeIf { it.id.isNotBlank() && it.figures.isNotEmpty() }
    }

    /** Removes values that JSON can't hold or that would break rendering (NaN/∞ points, bad aspects, empty figures). */
    private fun sanitize(p: PoseTemplate): PoseTemplate {
        val figures = p.figures.map(::sanitize).filter { it.points.isNotEmpty() }
        val aspect = p.frameAspect.takeIf { it.isFinite() && it > 0f } ?: DEFAULT_ASPECT
        val difficulty = p.difficulty.coerceIn(1, 3)
        return if (figures == p.figures && aspect == p.frameAspect && difficulty == p.difficulty) p
        else p.copy(figures = figures, frameAspect = aspect, difficulty = difficulty)
    }

    private fun sanitize(s: Skeleton): Skeleton {
        val points = s.points.filterValues { it.x.isFinite() && it.y.isFinite() }
        val visibility = s.visibility.filter { (j, v) -> j in points && v.isFinite() }.mapValues { (_, v) -> v.coerceIn(0f, 1f) }
        val unit = s.unit.takeIf { it.isFinite() && it > 0f } ?: 0f
        return if (points.size == s.points.size && visibility == s.visibility && unit == s.unit) s
        else Skeleton(points, visibility, unit)
    }

    private const val DEFAULT_ASPECT = 3f / 4f
}

object CustomPoseFactory {

    /** Detections below this visibility are not part of the template. */
    const val MIN_VISIBILITY = 0.5f

    /** People with fewer confidently detected joints than this are ignored (stray partial detections). */
    private const val MIN_JOINTS = 4

    /**
     * Builds a custom template from people detected in a still photo.
     * @param people skeletons in IMAGE-normalized coordinates (LandmarkMapping.toScene with sceneAspect = image aspect)
     * @param imageAspect image width / height; becomes the template frameAspect
     * Joints with low visibility (< 0.5) are dropped so the template only contains what was actually seen.
     * The body [Skeleton.unit] is estimated from the points.
     *
     * Figures are ordered left to right. If nobody survives the filtering, [PoseTemplate.figures] is empty: callers
     * should check that before saving (such a template is useless and is skipped when decoded).
     */
    fun fromDetection(
        id: String,
        name: String,
        category: PoseCategory,
        people: List<Skeleton>,
        imageAspect: Float,
        referenceImage: String?,
        createdAt: Long,
        source: PoseSource = PoseSource.FROM_PHOTO,
    ): PoseTemplate {
        val aspect = imageAspect.takeIf { it.isFinite() && it > 0f } ?: (3f / 4f)
        val figures = people
            .map { person ->
                val kept = person.points.filter { (j, p) ->
                    p.x.isFinite() && p.y.isFinite() && person.isVisible(j, MIN_VISIBILITY)
                }
                val seen = Skeleton(points = kept)
                seen.copy(unit = seen.estimatedUnit(aspect))
            }
            .filter { it.points.size >= MIN_JOINTS }
            .sortedBy { fig -> fig.points.values.map { it.x }.average() }

        val count = figures.size
        val hasAnkles = figures.any { Joint.LEFT_ANKLE in it.points || Joint.RIGHT_ANKLE in it.points }
        val hasKnees = figures.any { Joint.LEFT_KNEE in it.points || Joint.RIGHT_KNEE in it.points }
        val shot = when {
            hasAnkles -> ShotType.FULL_BODY
            hasKnees -> ShotType.THREE_QUARTER
            else -> ShotType.HALF_BODY
        }
        val height = when (shot) {
            ShotType.FULL_BODY -> CameraHeight.WAIST
            ShotType.THREE_QUARTER -> CameraHeight.CHEST
            else -> CameraHeight.EYE
        }

        val tips = buildList {
            add("Match the outline from your photo")
            if (count > 1) add("Each person takes the place of one outline")
            add("Line up your shoulders and hips first, then your arms and legs")
            add("Hold still for a moment once the pose matches")
        }
        val description = if (count > 1) "Created from your photo with $count people." else "Created from your photo."

        return PoseTemplate(
            id = id,
            name = name,
            category = category,
            figures = figures,
            frameAspect = aspect,
            description = description,
            tips = tips,
            camera = CameraAdvice(
                height = height,
                shot = shot,
                landscape = aspect > 1f,
                note = "Stand where the photo was taken from and frame it the same way.",
            ),
            difficulty = 1,
            source = source,
            createdAt = createdAt,
            referenceImage = referenceImage,
        )
    }
}
