package com.poseit.app.pose.library

import com.poseit.app.pose.PoseCategory
import org.junit.Assert.assertTrue
import org.junit.Test

/** The writing rules of docs/DESIGN.md, checked over every built-in pose and the category labels. */
class PoseCopyTest {

    /** Words that keep their capital letters inside a sentence-case name. */
    private val properNouns = setOf("LinkedIn", "V")

    private fun sentenceCaseProblems(text: String): List<String> {
        val words = text.split(' ', '-').filter { it.isNotEmpty() }
        return words.drop(1).filter { w -> w !in properNouns && w.any { it.isUpperCase() } }
    }

    @Test
    fun poseNamesAreSentenceCase() {
        val problems = BuiltInPoses.all.mapNotNull { p ->
            val bad = sentenceCaseProblems(p.name)
            when {
                !p.name.first().isUpperCase() -> "${p.id}: \"${p.name}\" should start with a capital"
                bad.isNotEmpty() -> "${p.id}: \"${p.name}\" has capitalised $bad"
                else -> null
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun poseNamesAreUnique() {
        val dupes = BuiltInPoses.all.groupBy { it.name.lowercase() }.filterValues { it.size > 1 }
            .map { (name, poses) -> "$name: ${poses.map { it.id }}" }
        assertTrue(dupes.joinToString("\n"), dupes.isEmpty())
    }

    @Test
    fun categoryLabelsAreSentenceCase() {
        val problems = PoseCategory.entries.filter { sentenceCaseProblems(it.label).isNotEmpty() }.map { it.label }
        assertTrue(problems.toString(), problems.isEmpty())
    }

    @Test
    fun proseHasNoDashFragmentsOrExclamations() {
        val problems = BuiltInPoses.all.flatMap { p ->
            (listOf(p.description) + p.tips + listOfNotNull(p.camera.note))
                .filter { " – " in it || " — " in it || it.trimEnd().endsWith("!") }
                .map { "${p.id}: $it" }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
