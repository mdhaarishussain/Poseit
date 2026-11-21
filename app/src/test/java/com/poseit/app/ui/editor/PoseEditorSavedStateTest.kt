package com.poseit.app.ui.editor

import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.poseit.app.PoseitApp
import com.poseit.app.data.PoseRepository
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.library.BuiltInPoses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Unsaved editor work and the save-sheet draft survive the process being killed (a new view model, same handle). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PoseEditorSavedStateTest {

    private val repository: PoseRepository get() = ApplicationProvider.getApplicationContext<PoseitApp>().container.poseRepository

    private fun editor(baseId: String?, handle: SavedStateHandle): PoseEditorViewModel =
        PoseEditorViewModel(repository, baseId, handle).also {
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse("editor still loading", it.loading)
        }

    @Test
    fun unsavedFigures_andSelection_areRestored() {
        val base = BuiltInPoses.all.first { it.personCount == 1 }
        val handle = SavedStateHandle()
        val first = editor(base.id, handle)
        first.mirrorSelected()
        first.addPerson()
        val edited = first.figures
        assertTrue(first.isDirty)

        val restored = editor(base.id, handle)
        assertEquals(edited, restored.figures)
        assertEquals(first.selected, restored.selected)
        assertTrue("restored edits must still count as unsaved", restored.isDirty)
        // Reset still goes back to the base pose.
        restored.reset()
        assertEquals(base.figures.size, restored.figures.size)
    }

    @Test
    fun noStoredState_startsFromBase() {
        val base = BuiltInPoses.all.first { it.personCount == 1 }
        val vm = editor(base.id, SavedStateHandle())
        assertFalse(vm.isDirty)
        assertNull(vm.draftName)
    }

    @Test
    fun saveSheetDraft_isKept() {
        val handle = SavedStateHandle()
        val first = editor(null, handle)
        first.updateDraftName("Beach lean")
        first.updateDraftCategory(PoseCategory.SITTING)
        first.updateDraftTags(setOf(PoseTag.entries.first()))

        val restored = editor(null, handle)
        assertEquals("Beach lean", restored.draftName)
        assertEquals(PoseCategory.SITTING, restored.draftCategory)
        assertEquals(setOf(PoseTag.entries.first()), restored.draftTags)
    }
}
