package com.poseit.app.ui.settings

import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseBackupTest {

    private val figure = Skeleton(mapOf(Joint.NOSE to Vec2(0.5f, 0.2f)))

    private fun pose(
        id: String,
        name: String = id,
        source: PoseSource = PoseSource.DRAWN,
        createdAt: Long = 1_000L,
        reference: String? = null,
        outline: String? = null,
    ) = PoseTemplate(
        id = id,
        name = name,
        category = PoseCategory.CUSTOM,
        figures = listOf(figure),
        source = source,
        createdAt = createdAt,
        referenceImage = reference,
        outlineImage = outline,
    )

    private val noFiles: (String) -> Boolean = { false }

    @Test
    fun `new poses are kept as they are`() {
        val incoming = listOf(pose("a"), pose("b"))
        val plan = PoseBackup.planImport(incoming, existingCustom = emptyList(), reservedIds = emptySet(), fileExists = noFiles, now = 5L)
        assertEquals(incoming, plan.toSave)
        assertEquals(0, plan.alreadyPresent)
    }

    @Test
    fun `identical poses already in the library are skipped`() {
        val existing = pose("a", reference = "/data/a.jpg")
        val plan = PoseBackup.planImport(
            incoming = listOf(existing, pose("b")),
            existingCustom = listOf(existing),
            reservedIds = emptySet(),
            fileExists = { it == "/data/a.jpg" },
            now = 5L,
        )
        assertEquals(listOf("b"), plan.toSave.map { it.id })
        assertEquals(1, plan.alreadyPresent)
    }

    @Test
    fun `clashing ids are renamed without clashing with each other or built-ins`() {
        val plan = PoseBackup.planImport(
            incoming = listOf(pose("a", name = "Other A"), pose("standing_1", name = "Mine")),
            existingCustom = listOf(pose("a"), pose("a-imported")),
            reservedIds = setOf("standing_1"),
            fileExists = noFiles,
            now = 5L,
        )
        assertEquals(listOf("a-imported-2", "standing_1-imported"), plan.toSave.map { it.id })
        assertEquals(listOf("Other A", "Mine"), plan.toSave.map { it.name })
    }

    @Test
    fun `unique id never stacks suffixes`() {
        assertEquals("x-imported", PoseBackup.uniqueId("x", setOf("x")))
        assertEquals("x-imported-3", PoseBackup.uniqueId("x-imported-2", setOf("x", "x-imported", "x-imported-2")))
    }

    @Test
    fun `missing images are cleared, existing ones kept`() {
        val plan = PoseBackup.planImport(
            incoming = listOf(pose("p", source = PoseSource.FROM_PHOTO, reference = "/sdcard/gone.jpg", outline = "/data/ok.png")),
            existingCustom = emptyList(),
            reservedIds = emptySet(),
            fileExists = { it == "/data/ok.png" },
            now = 5L,
        )
        val saved = plan.toSave.single()
        assertNull(saved.referenceImage)
        assertEquals("/data/ok.png", saved.outlineImage)
        assertEquals(PoseSource.FROM_PHOTO, saved.source)
    }

    @Test
    fun `built-in claims become user poses with a creation date and blank ids get one`() {
        val plan = PoseBackup.planImport(
            incoming = listOf(pose("q", source = PoseSource.BUILT_IN, createdAt = 0L), pose("  ", createdAt = 0L)),
            existingCustom = emptyList(),
            reservedIds = emptySet(),
            fileExists = noFiles,
            now = 1_000L,
        )
        val (first, second) = plan.toSave
        assertEquals(PoseSource.DRAWN, first.source)
        assertEquals(1_000L, first.createdAt)
        assertEquals(999L, second.createdAt)
        assertTrue(second.id.startsWith("imported-"))
    }

    @Test
    fun `import messages read naturally`() {
        assertEquals("Imported 1 pose", PoseBackup.importMessage(1, 0))
        assertEquals("Imported 3 poses (2 already in your library)", PoseBackup.importMessage(3, 2))
        assertEquals("That pose is already in your library", PoseBackup.importMessage(0, 1))
        assertEquals("All 4 poses are already in your library", PoseBackup.importMessage(0, 4))
        assertEquals("No poses found in that file", PoseBackup.importMessage(0, 0))
    }
}
