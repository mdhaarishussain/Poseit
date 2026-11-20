package com.poseit.app.tools

import com.poseit.app.pose.BodyProportions
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.library.FloorPoses
import com.poseit.app.pose.library.LeaningPoses
import com.poseit.app.pose.library.SittingPoses
import com.poseit.app.pose.library.StandingPoses
import com.poseit.app.pose.library.WalkingPoses
import com.poseit.app.pose.render.FigureShapes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Renders and validates pose library part A (standing, sitting, leaning, walking, floor).
 *
 * The contact sheets land in `pose-core/build/pose-previews/poses-a-<category>.png` for visual review
 * (red joints = subject's LEFT side, blue = RIGHT side).
 */
class PosesARenderTest {

    private class Group(val category: PoseCategory, val prefix: String, val poses: List<PoseTemplate>)

    private val groups = listOf(
        Group(PoseCategory.STANDING, "stand_", StandingPoses.all),
        Group(PoseCategory.SITTING, "sit_", SittingPoses.all),
        Group(PoseCategory.LEANING, "lean_", LeaningPoses.all),
        Group(PoseCategory.WALKING, "walk_", WalkingPoses.all),
        Group(PoseCategory.FLOOR, "floor_", FloorPoses.all),
    )

    @Test
    fun renderContactSheets() {
        for (g in groups) {
            val file = File("build/pose-previews/poses-a-${g.category.name.lowercase()}.png")
            PosePreviewRenderer.renderSheet(g.poses, file, cellHeight = 420, columns = 4)
            if (g.poses.isNotEmpty()) assertTrue("missing sheet $file", file.isFile)
        }
    }

    @Test
    fun eachCategoryHasEnoughPoses() {
        for (g in groups) {
            assertTrue("${g.category} has only ${g.poses.size} poses", g.poses.size >= MIN_POSES_PER_CATEGORY)
        }
    }

    @Test
    fun idsAreUniqueAndPrefixed() {
        val all = groups.flatMap { it.poses }
        val dupes = all.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertTrue("duplicate ids: $dupes", dupes.isEmpty())
        for (g in groups) {
            for (p in g.poses) {
                assertTrue("${p.id} must start with ${g.prefix}", p.id.startsWith(g.prefix))
                assertTrue("${p.id} must be snake_case", p.id.matches(Regex("[a-z][a-z0-9_]*")))
                assertEquals("${p.id} category", g.category, p.category)
            }
        }
    }

    @Test
    fun metadataIsComplete() {
        for (p in groups.flatMap { it.poses }) {
            assertTrue("${p.id}: name '${p.name}' too long", p.name.isNotBlank() && p.name.length <= 22)
            assertTrue("${p.id}: description missing", p.description.length >= 30)
            assertTrue("${p.id}: needs 2-4 tips", p.tips.size in 2..4 && p.tips.all { it.isNotBlank() })
            assertTrue("${p.id}: camera note missing", !p.camera.note.isNullOrBlank())
            assertTrue("${p.id}: difficulty ${p.difficulty}", p.difficulty in 1..3)
            assertEquals("${p.id}: single-person pose", 1, p.personCount)
            val mood = p.tags.count { !it.isLocation }
            val location = p.tags.count { it.isLocation }
            assertTrue("${p.id}: needs 1-3 mood tags, has $mood", mood in 1..3)
            assertTrue("${p.id}: needs 1-3 location tags, has $location", location in 1..3)
        }
    }

    @Test
    fun allCoordinatesAreFiniteAndComplete() {
        for (p in groups.flatMap { it.poses }) {
            for (sk in p.figures) {
                assertEquals("${p.id}: every joint is authored", Joint.entries.toSet(), sk.points.keys)
                for ((j, v) in sk.points) {
                    assertTrue("${p.id}: $j = $v is not finite", v.x.isFinite() && v.y.isFinite())
                }
                assertTrue("${p.id}: unit ${sk.unit}", sk.unit.isFinite() && sk.unit > 0f)
            }
        }
    }

    @Test
    fun fullBodyPosesFitInsideTheFrame() {
        val problems = mutableListOf<String>()
        for (p in groups.flatMap { it.poses }.filter { it.camera.shot == ShotType.FULL_BODY }) {
            for (sk in p.figures) {
                for ((j, v) in sk.points) {
                    if (v.x !in EDGE..1f - EDGE || v.y !in EDGE..1f - EDGE) problems += "${p.id}: $j at $v is outside the frame"
                }
                // The head is drawn as a circle around the ears: it must be inside the frame too.
                val iso = sk.toIsotropic(p.frameAspect)
                val head = FigureShapes.headCenter(iso, sk.unit, null)
                if (head == null) {
                    problems += "${p.id}: no head"
                    continue
                }
                val r = BodyProportions.HEAD_RADIUS * sk.unit
                if (head.y - r < EDGE) problems += "${p.id}: head top cut off"
                if (head.x - r < EDGE * p.frameAspect || head.x + r > (1f - EDGE) * p.frameAspect) {
                    problems += "${p.id}: head cut off at the side"
                }
            }
        }
        assertTrue(problems.joinToString("\n", prefix = "\n"), problems.isEmpty())
    }

    private companion object {
        const val MIN_POSES_PER_CATEGORY = 8
        const val EDGE = 0.03f
    }
}
