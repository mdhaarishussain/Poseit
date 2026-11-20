package com.poseit.app.ui.library

import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class LibraryFilteringTest {

    private val figure = Skeleton(mapOf(Joint.NOSE to Vec2(0.5f, 0.2f), Joint.LEFT_HIP to Vec2(0.5f, 0.6f)))

    private fun pose(
        id: String,
        name: String = id,
        category: PoseCategory = PoseCategory.STANDING,
        people: Int = 1,
        tags: Set<PoseTag> = emptySet(),
        description: String = "",
        source: PoseSource = PoseSource.BUILT_IN,
    ) = PoseTemplate(
        id = id,
        name = name,
        category = category,
        figures = List(people) { figure },
        tags = tags,
        description = description,
        source = source,
    )

    private val beachSolo = pose("beach", "Sun Seeker", tags = setOf(PoseTag.BEACH, PoseTag.CASUAL))
    private val cafeCouple = pose("cafe", "Coffee Date", PoseCategory.COUPLE, people = 2, tags = setOf(PoseTag.CAFE, PoseTag.ROMANTIC))
    private val group = pose("group", "Squad Goals", PoseCategory.GROUP, people = 4, tags = setOf(PoseTag.PARTY, PoseTag.PLAYFUL))
    private val office = pose("office", "Power Stance", PoseCategory.PROFESSIONAL, description = "Arms crossed, chin up", tags = setOf(PoseTag.OFFICE, PoseTag.CONFIDENT))
    private val mine = pose("mine", "My Jump", PoseCategory.CUSTOM, source = PoseSource.DRAWN)
    private val all = listOf(mine, beachSolo, cafeCouple, group, office)

    @Test
    fun `people filter maps 1, 2 and 3+ persons`() {
        assertTrue(PeopleFilter.SOLO.matches(1))
        assertFalse(PeopleFilter.SOLO.matches(2))
        assertTrue(PeopleFilter.COUPLE.matches(2))
        assertFalse(PeopleFilter.COUPLE.matches(3))
        assertTrue(PeopleFilter.GROUP.matches(3))
        assertTrue(PeopleFilter.GROUP.matches(5))
        assertTrue(PeopleFilter.ANY.matches(7))

        val couples = LibraryFiltering.refine(all, LibraryFilter(people = PeopleFilter.COUPLE))
        assertEquals(listOf(cafeCouple), couples)
        val groups = LibraryFiltering.refine(all, LibraryFilter(people = PeopleFilter.GROUP))
        assertEquals(listOf(group), groups)
    }

    @Test
    fun `search matches name, description, tags and category, ignoring case and accents`() {
        assertEquals(listOf(beachSolo), LibraryFiltering.refine(all, LibraryFilter(query = "sun")))
        assertEquals(listOf(office), LibraryFiltering.refine(all, LibraryFilter(query = "CROSSED")))
        assertEquals(listOf(cafeCouple), LibraryFiltering.refine(all, LibraryFilter(query = "cafe")))
        assertEquals(listOf(cafeCouple), LibraryFiltering.refine(all, LibraryFilter(query = "café restaurant")))
        assertEquals(listOf(office), LibraryFiltering.refine(all, LibraryFilter(query = "professional")))
        assertEquals(listOf(group), LibraryFiltering.refine(all, LibraryFilter(query = "friends")))
        assertTrue(LibraryFiltering.refine(all, LibraryFilter(query = "sun office")).isEmpty())
        assertEquals(all, LibraryFiltering.refine(all, LibraryFilter(query = "   ")))
    }

    @Test
    fun `category, location and mood filters combine`() {
        val filter = LibraryFilter(category = PoseCategory.COUPLE, location = PoseTag.CAFE, mood = PoseTag.ROMANTIC)
        assertEquals(listOf(cafeCouple), LibraryFiltering.refine(all, filter))
        assertTrue(LibraryFiltering.refine(all, filter.copy(mood = PoseTag.PLAYFUL)).isEmpty())
        assertEquals(listOf(office), LibraryFiltering.refine(all, LibraryFilter(location = PoseTag.OFFICE)))
    }

    @Test
    fun `tabs select favourites, recents in recency order and custom poses`() {
        val favs = LibraryFiltering.posesForTab(LibraryTab.FAVORITES, all, setOf("group", "beach"), emptyList())
        assertEquals(listOf(beachSolo, group), favs)

        val recent = LibraryFiltering.posesForTab(LibraryTab.RECENT, all, emptySet(), listOf("office", "deleted", "beach", "office"))
        assertEquals(listOf(office, beachSolo), recent)

        assertEquals(listOf(mine), LibraryFiltering.posesForTab(LibraryTab.MINE, all, emptySet(), emptyList()))
        assertEquals(all, LibraryFiltering.posesForTab(LibraryTab.ALL, all, emptySet(), emptyList()))
    }

    @Test
    fun `hasRefinements ignores the tab and cleared keeps it`() {
        assertFalse(LibraryFilter(tab = LibraryTab.FAVORITES).hasRefinements)
        val busy = LibraryFilter(tab = LibraryTab.RECENT, query = "x", mood = PoseTag.COOL)
        assertTrue(busy.hasRefinements)
        assertEquals(LibraryFilter(tab = LibraryTab.RECENT), busy.cleared())
    }

    @Test
    fun `pose of the day is stable for a day, built-in only, and varies over days`() {
        assertNull(LibraryFiltering.poseOfTheDay(emptyList(), 100))
        assertNull(LibraryFiltering.poseOfTheDay(listOf(mine), 100))
        val first = LibraryFiltering.poseOfTheDay(all, 20_000)
        assertEquals(first, LibraryFiltering.poseOfTheDay(all, 20_000))
        val week = (20_000L until 20_014L).mapNotNull { LibraryFiltering.poseOfTheDay(all, it) }
        assertTrue(week.none { it.isCustom })
        assertTrue("expected some variety, got ${week.map { it.id }}", week.toSet().size > 1)
        // Negative days (clock before 1970) must not crash.
        assertNotEquals(null, LibraryFiltering.poseOfTheDay(all, -5))
    }

    @Test
    fun `local epoch day honours the time zone`() {
        val utc = TimeZone.getTimeZone("UTC")
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
        val lateEveningUtc = 86_400_000L * 10 + 23 * 3_600_000L // day 10, 23:00 UTC
        assertEquals(10L, LibraryFiltering.localEpochDay(lateEveningUtc, utc))
        assertEquals(11L, LibraryFiltering.localEpochDay(lateEveningUtc, tokyo))
    }

    @Test
    fun `available chips only list used categories and tags, plus active ones`() {
        val categories = LibraryFiltering.availableCategories(all, keep = PoseCategory.FUN)
        assertEquals(
            listOf(PoseCategory.STANDING, PoseCategory.PROFESSIONAL, PoseCategory.FUN, PoseCategory.COUPLE, PoseCategory.GROUP, PoseCategory.CUSTOM),
            categories,
        )
        val (locations, moods) = LibraryFiltering.availableTags(listOf(beachSolo), keep = listOf(PoseTag.CITY, null))
        assertEquals(listOf(PoseTag.BEACH, PoseTag.CITY), locations)
        assertEquals(listOf(PoseTag.CASUAL), moods)
    }
}
