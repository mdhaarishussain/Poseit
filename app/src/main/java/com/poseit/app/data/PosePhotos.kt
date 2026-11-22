package com.poseit.app.data

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import com.poseit.app.BuildConfig
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.library.BuiltInPoses
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/*
 * Sample photos of the built-in poses (docs/POSE_PHOTOS.md): app/src/main/assets/pose_photos/<poseId>.webp with one
 * credit each in pose_photos/credits.json, written by tools/pose-photos/credits.py. Only built-in poses have photos;
 * poses people make never do. The photos are shown only in the pose guide, beside the pose's figure, always with
 * their credit; the app never offers them to share, save or export.
 *
 * Whether a build shows the Mixkit stills is BuildConfig.MIXKIT_PHOTOS: always in debug builds; in release builds it
 * follows the Gradle property poseit.releaseMixkitPhotos, true since 1.2.0 at the owner's request with the two open
 * questions in docs/POSE_PHOTOS.md ("Mixkit in release builds") accepted. Setting it to false leaves them out again.
 */

/**
 * A pose's sample photo and its credit.
 *
 * @property attribution the credit line to show with the photo ("Photo: Mixkit", "Photo: <author>, CC0, Wikimedia
 *   Commons (cropped)")
 * @property source "Mixkit" or "Wikimedia Commons"
 * @property title the clip's or file's title as published. Shown only where the licence requires it ([titleRequired]:
 *   CC BY and BY-SA 2.x and 3.0), as "Original title" in the full credit; elsewhere it is not shown, because a stock
 *   title can describe the people
 * @property author as the source states it
 * @property pageUrl the clip page or the Commons file page
 * @property licenceTextPath the full licence text in the assets ("licenses/CC0-1.0.txt")
 * @property changes what was done to the source, as recorded by the tools (see [plainChanges] for display)
 * @property mirrored flipped left to right so the person matches the figure as drawn
 * @property stillFromVideo a still frame cut from a stock video clip
 */
@Immutable
data class PosePhoto(
    val poseId: String,
    val poseName: String,
    val people: Int,
    val assetPath: String,
    val width: Int,
    val height: Int,
    val attribution: String,
    val source: String,
    val title: String?,
    val author: String,
    val pageUrl: String,
    val licence: String,
    val licenceUrl: String,
    val licenceTextPath: String,
    val changes: List<String>,
    val mirrored: Boolean,
    val stillFromVideo: Boolean,
) {
    /** Width over height: 0.75 for the portrait photos, 1.33 for the one landscape group photo. */
    val aspect: Float get() = width.toFloat() / height

    val isLandscape: Boolean get() = width > height

    /** What the photo shows, for screen readers: "Photo of a person in the Hand on hip pose". */
    val contentDescription: String
        get() {
            val who = when (people) {
                1 -> "a person"
                2 -> "two people"
                else -> "a group of people"
            }
            return "Photo of $who in the $poseName pose"
        }

    /** The author for display; Mixkit names none ("Mixkit (no author named on the clip page)" in the credits). */
    val authorForDisplay: String
        get() = if (source == SOURCE_MIXKIT && author.startsWith("Mixkit (")) "Not named on the Mixkit clip page" else author

    /**
     * The work's title as its source publishes it, for the full credit: Commons' "File:" prefix, the file extension
     * and the Flickr photo id that Commons' upload tool appends are left out ("Woman in a red dress"). Null if none.
     */
    val titleForDisplay: String? get() = title?.let(PhotoCredits::displayTitle)?.takeIf { it.isNotBlank() }

    /**
     * True when the photo's licence requires the work's title in its credit if the licensor supplies one: CC BY and
     * CC BY-SA 2.0, 2.5 and 3.0 ("the title of the Work if supplied"). CC BY 4.0, CC0 and the Mixkit licence do not.
     */
    val titleRequired: Boolean get() = PhotoCredits.licenceRequiresTitle(licence)

    /** The changes in plain words: "A still frame from a video", "Cropped", "Flipped left to right", "Resized". */
    val plainChanges: List<String>
        get() = buildList {
            if (stillFromVideo) add("A still frame from a video")
            if (changes.any { it.startsWith("cropped", ignoreCase = true) }) add("Cropped")
            if (mirrored) add("Flipped left to right")
            if (changes.any { it.startsWith("resized", ignoreCase = true) }) add("Resized")
        }

    /** [plainChanges] as one sentence: "A still frame from a video, cropped and resized." */
    val changesSentence: String
        get() {
            val parts = plainChanges.mapIndexed { i, s -> if (i == 0) s else s.replaceFirstChar { it.lowercaseChar() } }
            return when (parts.size) {
                0 -> "Shown as published."
                1 -> parts[0] + "."
                else -> parts.dropLast(1).joinToString(", ") + " and " + parts.last() + "."
            }
        }

    companion object {
        const val SOURCE_MIXKIT = "Mixkit"
        const val SOURCE_COMMONS = "Wikimedia Commons"
    }
}

/**
 * One entry of pose_photos/credits.json, as tools/pose-photos/credits.py writes it: only what the app reads (the
 * review notes, scores and download links stay in tools/pose-photos/credits-full.json). R8 keeps the generated
 * serializer (proguard-rules.pro, "kotlinx.serialization").
 */
@Serializable
internal data class PhotoCreditRecord(
    val photo: String,
    val source: String,
    val title: String? = null,
    val pageUrl: String,
    val author: String,
    val licence: String,
    val licenceUrl: String,
    val licenceText: String,
    val attribution: String,
    val changes: List<String> = emptyList(),
    val mirrored: Boolean = false,
    val frameTimeS: Double? = null,
)

/**
 * Bundled licence texts are hard-wrapped at about 75 characters for reading in an editor; on a phone those breaks
 * fall mid-line. [reflow] joins the wrapped lines of each paragraph and keeps blank lines, list items ("a.", "1.",
 * "iv.", "(b)", "-") and short lines such as headings as they are.
 */
internal object LicenceText {
    private val listItem = Regex("""^\s*(\(?([0-9]{1,2}|[a-z]|[ivx]{1,5})[.)]|[-*\u2022])\s""")
    private val label = Regex("""^[A-Z][A-Za-z ]{0,24}:\s""")
    private const val WRAPPED = 50

    fun reflow(text: String): String {
        val out = StringBuilder()
        var previous: String? = null
        for (raw in text.replace("\r\n", "\n").split('\n')) {
            val line = raw.trimEnd()
            val prev = previous
            val joins = prev != null && prev.isNotBlank() && line.isNotBlank() && prev.length >= WRAPPED &&
                !listItem.containsMatchIn(line) && !label.containsMatchIn(line)
            if (joins) {
                out.append(' ').append(line.trimStart())
            } else {
                if (prev != null) out.append('\n')
                out.append(line)
            }
            previous = line
        }
        return out.toString()
    }
}

/** Parsing of pose_photos/credits.json, kept free of Android so plain JVM tests can check the shipped file. */
internal object PhotoCredits {
    private val json = Json { ignoreUnknownKeys = true }
    private val resized = Regex("""resized to (\d+)x(\d+)""")

    /** Pose id to credit, in file order. */
    fun parse(text: String): Map<String, PhotoCreditRecord> = json.decodeFromString(text)

    /**
     * The credits a build shows: every one, or without the Mixkit stills when [includeMixkit] is false (a release
     * built with poseit.releaseMixkitPhotos=false; docs/POSE_PHOTOS.md, "Mixkit in release builds").
     */
    fun shown(records: Map<String, PhotoCreditRecord>, includeMixkit: Boolean): Map<String, PhotoCreditRecord> =
        if (includeMixkit) records else records.filterValues { it.source != PosePhoto.SOURCE_MIXKIT }

    private val fileExtension = Regex("""\.(jpe?g|png|webp|tiff?|gif)$""", RegexOption.IGNORE_CASE)
    private val flickrId = Regex("""\s*\(\d{6,}\)$""")

    /** "File:Ladies Grey Loungewear Set (51618413959).jpg" -> "Ladies Grey Loungewear Set". */
    fun displayTitle(title: String): String =
        title.trim().removePrefix("File:").replace(fileExtension, "").replace(flickrId, "").replace('_', ' ').trim()

    private val titleLicence = Regex("""^CC BY(-SA)? [23](\.\d)?$""")

    /** CC BY and BY-SA 2.x and 3.0 ask for the title of the work in the credit, if one is supplied. */
    fun licenceRequiresTitle(licence: String): Boolean = titleLicence.matches(licence.trim())

    /** The shipped photo's size from its "resized to WxH" change, or null when the credit does not record it. */
    fun recordedSize(record: PhotoCreditRecord): Pair<Int, Int>? =
        record.changes.firstNotNullOfOrNull { resized.find(it) }?.let { it.groupValues[1].toInt() to it.groupValues[2].toInt() }

    fun toPosePhoto(poseId: String, record: PhotoCreditRecord, pose: PoseTemplate, width: Int, height: Int) = PosePhoto(
        poseId = poseId,
        poseName = pose.name,
        people = pose.personCount,
        assetPath = "$PHOTO_DIR/${record.photo}",
        width = width,
        height = height,
        attribution = record.attribution,
        source = record.source,
        title = record.title,
        author = record.author,
        pageUrl = record.pageUrl,
        licence = record.licence,
        licenceUrl = record.licenceUrl,
        licenceTextPath = record.licenceText,
        changes = record.changes,
        mirrored = record.mirrored,
        stillFromVideo = record.frameTimeS != null || record.source == PosePhoto.SOURCE_MIXKIT,
    )

    const val PHOTO_DIR = "pose_photos"
    const val CREDITS_ASSET = "$PHOTO_DIR/credits.json"
}

/**
 * The sample photos of the built-in poses: their credits (credits.json, parsed once) and their pixels (decoded off the
 * main thread at the size they are shown, kept in a small memory cache). Get it from `context.appContainer.posePhotos`
 * or, in a composable, `rememberPosePhotos()`.
 */
class PosePhotos(context: Context, private val includeMixkit: Boolean = BuildConfig.MIXKIT_PHOTOS) {

    private val assets = context.applicationContext.assets
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val photos: Map<String, PosePhoto> by lazy { load() }

    /**
     * Poses whose photo could not be decoded (a missing or damaged file). Compose state, so a screen that showed the
     * photo's card recomposes without it: [photoFor] gives null for them and the pose falls back to its figure-only
     * layout, never an empty frame.
     */
    private val broken = mutableStateMapOf<String, Boolean>()

    /** Decoded bitmaps keyed by pose id and sample size, sized in KB. */
    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    /** Every pose that has a photo, in library order (read in a composable, it follows [isBroken]). */
    val all: List<PosePhoto> get() = photos.values.filterNot { isBroken(it.poseId) }

    /** The photo of the built-in pose [poseId], or null (no photo yet, not a built-in pose, or it failed to decode). */
    fun photoFor(poseId: String): PosePhoto? = photos[poseId]?.takeUnless { isBroken(poseId) }

    /** The photo of [pose], or null: poses people make never have one, even when copied from a built-in pose. */
    fun photoFor(pose: PoseTemplate): PosePhoto? = if (pose.source == PoseSource.BUILT_IN) photoFor(pose.id) else null

    /** The credit of [poseId] whether or not its pixels decode; for `rememberPosePhoto`, which checks [isBroken]. */
    internal fun recordFor(poseId: String): PosePhoto? = photos[poseId]

    /** True once [poseId]'s photo has failed to decode. Snapshot state: a composable that reads it recomposes. */
    fun isBroken(poseId: String): Boolean = broken[poseId] == true

    /** Forgets decode failures (tests). */
    @VisibleForTesting
    fun forgetFailures() = broken.clear()

    /**
     * Frees decoded photos when the system asks ([ComponentCallbacks2.onTrimMemory], from PoseitApp): half the cache
     * once the app's screens are hidden or memory runs low, all of it once the app is in the background list.
     */
    fun trim(level: Int) {
        @Suppress("DEPRECATION")
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> cache.evictAll()
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> cache.trimToSize(cache.maxSize() / 2)
        }
    }

    /** Frees every decoded photo (onLowMemory). */
    fun clear() = cache.evictAll()

    /** Parses the credits on a background thread now, so the first [photoFor] on the main thread finds them ready. */
    fun warmUp() {
        scope.launch { photos }
    }

    /** The bitmap for a box of [widthPx] x [heightPx] if it is already decoded, else null. Never decodes. */
    fun cachedBitmap(photo: PosePhoto, widthPx: Int, heightPx: Int): Bitmap? =
        cache.get(key(photo, sampleSizeFor(photo.width, photo.height, widthPx, heightPx)))

    /**
     * The photo decoded for a box of [widthPx] x [heightPx] (filled and cropped, so it is never smaller than the box
     * where the source allows): downsampled by a power of two with inSampleSize, decoded on [ioDispatcher] and
     * cached. Null if the file cannot be read; the pose is then marked [isBroken] and loses its photo.
     */
    suspend fun bitmap(photo: PosePhoto, widthPx: Int, heightPx: Int): Bitmap? {
        val sample = sampleSizeFor(photo.width, photo.height, widthPx, heightPx)
        val key = key(photo, sample)
        cache.get(key)?.let { return it }
        val bitmap = withContext(ioDispatcher) {
            cache.get(key) ?: decode(photo, sample)?.also { cache.put(key, it) }
        }
        if (bitmap == null) broken[photo.poseId] = true
        return bitmap
    }

    /**
     * The full text of a bundled licence ([PosePhoto.licenceTextPath]), read off the main thread, its hard-wrapped
     * lines joined into paragraphs for a phone screen.
     */
    suspend fun licenceText(path: String): String = withContext(ioDispatcher) {
        LicenceText.reflow(assets.open(path).bufferedReader().use { it.readText() })
    }

    private fun decode(photo: PosePhoto, sample: Int): Bitmap? = try {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        assets.open(photo.assetPath).use { BitmapFactory.decodeStream(it, null, options) }
            .also { if (it == null) Log.w(TAG, "Could not decode ${photo.assetPath}") }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Could not decode ${photo.assetPath}", e)
        null
    }

    private fun load(): Map<String, PosePhoto> {
        val records = try {
            PhotoCredits.shown(PhotoCredits.parse(assets.open(PhotoCredits.CREDITS_ASSET).bufferedReader().use { it.readText() }), includeMixkit)
        } catch (e: Exception) {
            Log.e(TAG, "Could not read ${PhotoCredits.CREDITS_ASSET}", e)
            return emptyMap()
        }
        // Library order, built-in poses only.
        return BuiltInPoses.all.mapNotNull { pose ->
            val record = records[pose.id] ?: return@mapNotNull null
            val (w, h) = PhotoCredits.recordedSize(record) ?: boundsOf("${PhotoCredits.PHOTO_DIR}/${record.photo}") ?: return@mapNotNull null
            pose.id to PhotoCredits.toPosePhoto(pose.id, record, pose, w, h)
        }.toMap()
    }

    private fun boundsOf(path: String): Pair<Int, Int>? = try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
    } catch (e: Exception) {
        Log.w(TAG, "Could not read $path", e)
        null
    }

    private fun key(photo: PosePhoto, sample: Int) = "${photo.poseId}@$sample"

    companion object {
        private const val TAG = "PosePhotos"

        /**
         * Where photos are decoded and licence texts read: Dispatchers.IO in the app. Screenshot tests set
         * `Dispatchers.Unconfined`, so a photo is decoded inside the composition's own frame and `waitForIdle()`
         * covers it (docs/DESIGN.md, "Sample photos").
         */
        @VisibleForTesting
        @Volatile
        var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

        /**
         * The largest power-of-two inSampleSize that still leaves the decoded photo at least as large as the cover
         * size for a [reqW] x [reqH] box (a filled, cropped image), so photos are never scaled up by the sampling.
         */
        fun sampleSizeFor(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
            if (reqW <= 0 || reqH <= 0 || srcW <= 0 || srcH <= 0) return 1
            var sample = 1
            while (srcW / (sample * 2) >= reqW && srcH / (sample * 2) >= reqH) sample *= 2
            return sample
        }

        /** A sixteenth of the heap, at most 24 MB: about ten full-size photos, or every thumbnail. */
        private fun cacheSizeKb(): Int = (Runtime.getRuntime().maxMemory() / 1024 / 16).toInt().coerceIn(4 * 1024, 24 * 1024)
    }
}
