package com.poseit.app.tools

import com.poseit.app.pose.BodyProportions
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.pose.library.CouplePoses
import com.poseit.app.pose.library.FunPoses
import com.poseit.app.pose.library.GroupPoses
import com.poseit.app.pose.library.PortraitPoses
import com.poseit.app.pose.library.ProfessionalPoses
import com.poseit.app.pose.library.SelfiePoses
import com.poseit.app.pose.render.FigureShapes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Renders and validates pose library part B (portraits, selfies, professional, fun, couples, groups).
 *
 * The contact sheets land in `pose-core/build/pose-previews/poses-b-<category>.png` for visual review
 * (red joints = subject's LEFT side, blue = RIGHT side).
 */
class PosesBRenderTest {

    private class Group(
        val category: PoseCategory,
        val prefix: String,
        val poses: List<PoseTemplate>,
        val minPoses: Int,
        val people: IntRange,
    )

    private val groups = listOf(
        Group(PoseCategory.PORTRAIT, "port_", PortraitPoses.all, minPoses = 8, people = 1..1),
        Group(PoseCategory.SELFIE, "selfie_", SelfiePoses.all, minPoses = 7, people = 1..1),
        Group(PoseCategory.PROFESSIONAL, "pro_", ProfessionalPoses.all, minPoses = 7, people = 1..1),
        Group(PoseCategory.FUN, "fun_", FunPoses.all, minPoses = 8, people = 1..1),
        Group(PoseCategory.COUPLE, "couple_", CouplePoses.all, minPoses = 8, people = 2..2),
        Group(PoseCategory.GROUP, "group_", GroupPoses.all, minPoses = 6, people = 3..4),
    )

    private val allPoses get() = groups.flatMap { it.poses }

    @Test
    fun renderContactSheets() {
        for (g in groups) {
            val file = File("build/pose-previews/poses-b-${g.category.name.lowercase()}.png")
            PosePreviewRenderer.renderSheet(g.poses, file, cellHeight = 420, columns = 4)
            assertTrue("missing sheet $file", file.isFile)
        }
    }

    @Test
    fun eachCategoryHasEnoughPoses() {
        for (g in groups) {
            assertTrue("${g.category} has only ${g.poses.size} poses (needs ${g.minPoses})", g.poses.size >= g.minPoses)
        }
    }

    @Test
    fun idsAreUniqueAndPrefixed() {
        val dupes = allPoses.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertTrue("duplicate ids: $dupes", dupes.isEmpty())
        // The aggregated library (which also rejects duplicates across ALL categories) must expose every pose.
        for (g in groups) {
            assertEquals("${g.category} in BuiltInPoses", g.poses, BuiltInPoses.byCategory(g.category))
        }
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
        for (g in groups) {
            for (p in g.poses) {
                assertTrue("${p.id}: name '${p.name}' too long", p.name.isNotBlank() && p.name.length <= 22)
                assertTrue("${p.id}: description missing", p.description.length >= 30)
                assertTrue("${p.id}: needs 2-4 tips", p.tips.size in 2..4 && p.tips.all { it.isNotBlank() })
                assertTrue("${p.id}: camera note missing", !p.camera.note.isNullOrBlank())
                assertTrue("${p.id}: difficulty ${p.difficulty}", p.difficulty in 1..3)
                assertTrue("${p.id}: ${p.personCount} people, expected ${g.people}", p.personCount in g.people)
                val mood = p.tags.count { !it.isLocation }
                val location = p.tags.count { it.isLocation }
                assertTrue("${p.id}: needs 1-3 mood tags, has $mood", mood in 1..3)
                assertTrue("${p.id}: needs 1-3 location tags, has $location", location in 1..3)
                if (p.camera.landscape) assertTrue("${p.id}: landscape frame", p.frameAspect > 1f)
            }
        }
        // Partners should not be identical twins: give them different heights.
        for (p in CouplePoses.all) {
            assertTrue("${p.id}: partners have the same body size", p.figures[0].unit != p.figures[1].unit)
        }
    }

    @Test
    fun allCoordinatesAreFiniteAndComplete() {
        for (p in allPoses) {
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
        for (p in allPoses.filter { it.camera.shot == ShotType.FULL_BODY }) {
            p.figures.forEachIndexed { i, sk ->
                for ((j, v) in sk.points) {
                    if (v.x !in EDGE..1f - EDGE || v.y !in EDGE..1f - EDGE) {
                        problems += "${p.id}[$i]: $j at $v is outside the frame"
                    }
                }
                headProblem(p, sk)?.let { problems += "${p.id}[$i]: $it" }
            }
        }
        assertTrue(problems.joinToString("\n", prefix = "\n"), problems.isEmpty())
    }

    /** Cropped shots (portraits, selfies, knees-up) may cut limbs, but never the face or the shoulders. */
    @Test
    fun croppedPosesKeepFaceAndShouldersInFrame() {
        val problems = mutableListOf<String>()
        for (p in allPoses.filter { it.camera.shot != ShotType.FULL_BODY }) {
            p.figures.forEachIndexed { i, sk ->
                headProblem(p, sk)?.let { problems += "${p.id}[$i]: $it" }
                for (j in listOf(Joint.NOSE, Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER)) {
                    val v = sk[j] ?: continue
                    if (v.x !in 0f..1f || v.y !in 0f..1f) problems += "${p.id}[$i]: $j at $v is outside the frame"
                }
            }
        }
        assertTrue(problems.joinToString("\n", prefix = "\n"), problems.isEmpty())
    }

    /** Standing full-body figures must have at least one foot near the bottom of their bounding box (no floating). */
    @Test
    fun figuresInOnePoseShareAGroundLine() {
        val problems = mutableListOf<String>()
        for (p in (CouplePoses.all + GroupPoses.all).filter { it.camera.shot == ShotType.FULL_BODY }) {
            if (p.id in AIRBORNE_OR_STAGGERED) continue
            val grounds = p.figures.map { sk -> lowestFoot(sk) }
            val spread = grounds.max() - grounds.min()
            if (spread > 0.03f) problems += "${p.id}: feet are ${"%.3f".format(spread)} apart vertically"
        }
        assertTrue(problems.joinToString("\n", prefix = "\n"), problems.isEmpty())
    }

    private fun lowestFoot(sk: Skeleton): Float =
        listOf(Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE).mapNotNull { sk[it]?.y }.max()

    private fun headProblem(p: PoseTemplate, sk: Skeleton): String? {
        // The head is drawn as a circle around the ears: it must be inside the frame too.
        val iso: Map<Joint, Vec2> = sk.toIsotropic(p.frameAspect)
        val head = FigureShapes.headCenter(iso, sk.unit, null) ?: return "no head"
        val r = BodyProportions.HEAD_RADIUS * sk.unit
        return when {
            head.y - r < EDGE -> "head top cut off"
            head.y + r > 1f - EDGE -> "head bottom cut off"
            head.x - r < EDGE * p.frameAspect || head.x + r > (1f - EDGE) * p.frameAspect -> "head cut off at the side"
            else -> null
        }
    }

    private companion object {
        const val EDGE = 0.03f

        /** Poses whose figures are intentionally not on one ground line (jumping, sitting vs. crouching, depth). */
        val AIRBORNE_OR_STAGGERED = setOf(
            "group_jump",
            "group_v_formation",
            "group_levels",
            "couple_piggyback",
            "couple_dip",
        )
    }
}
