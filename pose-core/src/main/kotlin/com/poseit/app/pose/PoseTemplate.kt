package com.poseit.app.pose

import kotlinx.serialization.Serializable

/**
 * A pose the user can try. Contains one figure per person (couples/groups have several).
 *
 * Figure coordinates are normalized to the template FRAME (x 0..1 across, y 0..1 down) whose shape is
 * [frameAspect] = width / height (default 3:4 portrait, the shape of the camera preview). The frame is
 * "what the final photo should look like": figures are in PHOTO orientation (not mirrored) with
 * anatomical left/right labels.
 */
@Serializable
data class PoseTemplate(
    val id: String,
    val name: String,
    val category: PoseCategory,
    val figures: List<Skeleton>,
    val frameAspect: Float = 3f / 4f,
    val description: String = "",
    val tips: List<String> = emptyList(),
    val camera: CameraAdvice = CameraAdvice(),
    val tags: Set<PoseTag> = emptySet(),
    /** 1 = easy, 2 = medium, 3 = advanced. */
    val difficulty: Int = 1,
    val source: PoseSource = PoseSource.BUILT_IN,
    /** Epoch millis; 0 for built-ins. */
    val createdAt: Long = 0L,
    /** For custom poses created from a photo: content URI or file path of the reference image. */
    val referenceImage: String? = null,
) {
    val personCount: Int get() = figures.size
}

@Serializable
enum class PoseSource { BUILT_IN, FROM_PHOTO, DRAWN }

@Serializable
enum class PoseCategory(val label: String, val emoji: String) {
    STANDING("Standing", "🧍"),
    SITTING("Sitting", "🪑"),
    LEANING("Leaning", "🧱"),
    WALKING("Walking & Action", "🚶"),
    PORTRAIT("Portrait", "🙂"),
    SELFIE("Selfie", "🤳"),
    PROFESSIONAL("Professional", "💼"),
    FUN("Fun & Jumping", "🤸"),
    FLOOR("Floor & Squat", "🧘"),
    COUPLE("Couple", "💑"),
    GROUP("Friends & Group", "👯"),
    CUSTOM("My Poses", "⭐"),
}

/** Mood / location tags used for filtering and "where are you?" suggestions. */
@Serializable
enum class PoseTag(val label: String, val isLocation: Boolean = false) {
    CASUAL("Casual"),
    CONFIDENT("Confident"),
    CANDID("Candid"),
    ELEGANT("Elegant"),
    PLAYFUL("Playful"),
    COOL("Cool"),
    ROMANTIC("Romantic"),
    SLIMMING("Slimming"),
    BEACH("Beach", true),
    CITY("City street", true),
    NATURE("Nature & park", true),
    CAFE("Café & restaurant", true),
    HOME("Home", true),
    OFFICE("Office", true),
    STAIRS("Stairs", true),
    WALL("Wall & door", true),
    TRAVEL("Travel & landmark", true),
    PARTY("Party & event", true),
}

@Serializable
data class CameraAdvice(
    val height: CameraHeight = CameraHeight.CHEST,
    val shot: ShotType = ShotType.FULL_BODY,
    val landscape: Boolean = false,
    val note: String? = null,
)

@Serializable
enum class CameraHeight(val label: String, val advice: String) {
    GROUND("Ground level", "Hold the phone close to the ground, tilted slightly up – it makes legs look longer."),
    KNEE("Knee height", "Hold the phone around knee height, tilted slightly up."),
    WAIST("Waist height", "Hold the phone at waist height and keep it vertical."),
    CHEST("Chest height", "Hold the phone at chest height, straight on."),
    EYE("Eye level", "Hold the phone at the subject's eye level."),
    HIGH("Above head", "Hold the phone a little above head height, angled down."),
}

@Serializable
enum class ShotType(val label: String) {
    FULL_BODY("Full body"),
    THREE_QUARTER("Knees up"),
    HALF_BODY("Waist up"),
    CLOSE_UP("Head & shoulders"),
}
