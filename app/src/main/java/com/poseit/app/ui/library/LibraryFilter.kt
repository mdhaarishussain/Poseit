package com.poseit.app.ui.library

import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import java.text.Normalizer
import java.util.Locale
import java.util.TimeZone

/** The segmented tabs at the top of the library. */
enum class LibraryTab(val label: String) {
    ALL("All"),
    FAVORITES("Favourites"),
    RECENT("Recent"),
    MINE("My poses"),
}

/** "Who's in the photo?" filter. */
enum class PeopleFilter(val label: String) {
    ANY("Any"),
    SOLO("Solo"),
    COUPLE("Couple"),
    GROUP("Group");

    fun matches(personCount: Int): Boolean = when (this) {
        ANY -> true
        SOLO -> personCount <= 1
        COUPLE -> personCount == 2
        GROUP -> personCount >= 3
    }
}

/**
 * Everything the user can narrow the library down with. [location] and [mood] are single-select (tap again to
 * clear) so combining them never leads to surprising "no results".
 */
data class LibraryFilter(
    val query: String = "",
    val tab: LibraryTab = LibraryTab.ALL,
    val people: PeopleFilter = PeopleFilter.ANY,
    val category: PoseCategory? = null,
    val location: PoseTag? = null,
    val mood: PoseTag? = null,
) {
    /** True when anything besides the tab narrows the results. */
    val hasRefinements: Boolean
        get() = query.isNotBlank() || people != PeopleFilter.ANY || category != null || location != null || mood != null

    /** Same tab, every other refinement cleared. */
    fun cleared(): LibraryFilter = LibraryFilter(tab = tab)
}

/** A user-made pose (from a photo or drawn in the editor) rather than one shipped with the app. */
val PoseTemplate.isCustom: Boolean get() = source != PoseSource.BUILT_IN

/** Pure, unit-testable library filtering. */
object LibraryFiltering {

    /** The poses that belong to [tab], before any refinement. Recent keeps most-recent-first order. */
    fun posesForTab(
        tab: LibraryTab,
        all: List<PoseTemplate>,
        favorites: Set<String>,
        recent: List<String>,
    ): List<PoseTemplate> = when (tab) {
        LibraryTab.ALL -> all
        LibraryTab.FAVORITES -> all.filter { it.id in favorites }
        LibraryTab.RECENT -> {
            val byId = all.associateBy { it.id }
            recent.distinct().mapNotNull { byId[it] }
        }
        LibraryTab.MINE -> all.filter { it.isCustom }
    }

    /** Applies people / category / location / mood / search refinements, keeping the input order. */
    fun refine(poses: List<PoseTemplate>, filter: LibraryFilter): List<PoseTemplate> {
        val tokens = tokenize(filter.query)
        return poses.filter { pose ->
            filter.people.matches(pose.personCount) &&
                (filter.category == null || pose.category == filter.category) &&
                (filter.location == null || filter.location in pose.tags) &&
                (filter.mood == null || filter.mood in pose.tags) &&
                matchesTokens(pose, tokens)
        }
    }

    /**
     * True when every word of [query] appears in the pose's name, description, tags or category
     * (case- and accent-insensitive, so "cafe" finds "Café & restaurant").
     */
    fun matchesQuery(pose: PoseTemplate, query: String): Boolean = matchesTokens(pose, tokenize(query))

    private fun matchesTokens(pose: PoseTemplate, tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return true
        val haystack = searchText(pose)
        return tokens.all { it in haystack }
    }

    private fun searchText(pose: PoseTemplate): String = buildString {
        append(pose.name).append(' ')
        append(pose.description).append(' ')
        append(pose.category.label).append(' ')
        pose.tags.forEach { append(it.label).append(' ') }
        when (pose.personCount) {
            1 -> append("solo single ")
            2 -> append("couple duo pair two ")
            else -> if (pose.personCount >= 3) append("group friends ")
        }
    }.let(::normalize)

    private fun tokenize(query: String): List<String> =
        normalize(query).split(' ', '\t', '\n', ',').filter { it.isNotBlank() }

    /** Lower-case, accents stripped. */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase(Locale.ROOT)

    /**
     * A deterministic "pose of the day": the same pose all day long (in the user's time zone), a different one
     * tomorrow. Only built-in poses are candidates so the hero card never shows a half-finished custom pose.
     */
    fun poseOfTheDay(poses: List<PoseTemplate>, epochDay: Long): PoseTemplate? {
        val pool = poses.filter { !it.isCustom }.ifEmpty { return null }
        // Knuth multiplicative hash spreads consecutive days across the whole library instead of walking it in order.
        val mixed = (epochDay * 2_654_435_761L) xor (epochDay ushr 7)
        return pool[Math.floorMod(mixed, pool.size.toLong()).toInt()]
    }

    /** Days since 1970-01-01 in [timeZone] (works on API 24 without java.time). */
    fun localEpochDay(nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long =
        Math.floorDiv(nowMillis + timeZone.getOffset(nowMillis), MILLIS_PER_DAY)

    /** Categories that actually contain poses, in enum order. [keep] is always included (an active filter). */
    fun availableCategories(all: List<PoseTemplate>, keep: PoseCategory?): List<PoseCategory> {
        val used = all.mapTo(HashSet()) { it.category }
        return PoseCategory.entries.filter { it in used || it == keep }
    }

    /** Tags used by at least one pose, split into (locations, moods). Active filters are always included. */
    fun availableTags(all: List<PoseTemplate>, keep: Collection<PoseTag?>): Pair<List<PoseTag>, List<PoseTag>> {
        val used = all.flatMapTo(HashSet()) { it.tags }
        val visible = PoseTag.entries.filter { it in used || it in keep }
        return visible.partition { it.isLocation }
    }

    private const val MILLIS_PER_DAY = 86_400_000L
    private val DIACRITICS = Regex("\\p{Mn}+")
}
