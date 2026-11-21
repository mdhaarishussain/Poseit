package com.poseit.app.review

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.poseit.app.data.SavedPhoto
import com.poseit.app.ui.gallery.PhotoViewer
import com.poseit.app.ui.theme.PoseitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The full-screen review viewer with a shrinking photo list (quick delete / confirmed delete / keep). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class PhotoViewerReviewTest {

    @get:Rule
    val compose = createComposeRule()

    private fun photo(i: Int) = SavedPhoto(
        uri = Uri.parse("content://media/external/images/media/$i"),
        displayName = "Poseit_$i.jpg",
        dateTakenMillis = 1_700_000_000_000L - i * 60_000L,
        width = 3000,
        height = 4000,
    )

    @Test
    fun keep_quickDelete_confirmDelete_untilEmpty() {
        var photos by mutableStateOf(List(3) { photo(it + 1) })
        val initial = photos[1]
        var finished = 0
        val removed = mutableListOf<Uri>()
        compose.setContent {
            PoseitTheme {
                val snackbar = remember { SnackbarHostState() }
                PhotoViewer(
                    photos = photos,
                    initialPhoto = initial,
                    snackbarHostState = snackbar,
                    onClose = {},
                    onCurrentPhotoChanged = {},
                    onShare = {},
                    onOpenExternally = {},
                    onDeleteConfirmed = { p -> removed += p.uri; photos = photos - p },
                    onQuickDelete = { p -> removed += p.uri; photos = photos - p },
                    onReviewFinished = { finished++ },
                )
            }
        }
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithText("2 / 3", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        compose.onAllNodes(hasText("Keep") and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()
        assertTrue("Keep should move to photo 3", compose.onAllNodesWithText("3 / 3", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        compose.onAllNodes(hasText("Keep · done") and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()
        assertEquals(1, finished)

        // Quick delete the last photo: the pager has to clamp to the new last page.
        compose.onAllNodes(hasText("Delete") and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()
        assertEquals(2, photos.size)
        compose.onRoot().captureRoboImage("build/screenshots/review-viewer-after-quick-delete.png")
        assertTrue(compose.onAllNodesWithText("2 / 2", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())

        // Confirmed delete from the top bar.
        compose.onAllNodes(hasContentDescription("Delete") and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasText("Delete") and hasClickAction()).fetchSemanticsNodes() // dialog shown
        compose.onAllNodes(hasText("Delete this photo?")).onFirst().assertExists()
        // The dialog's confirm button is the last "Delete" text button.
        val deleteButtons = compose.onAllNodes(hasText("Delete") and hasClickAction())
        deleteButtons[deleteButtons.fetchSemanticsNodes().size - 1].performClick()
        compose.waitForIdle()
        assertEquals(1, photos.size)

        compose.onAllNodes(hasText("Delete") and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()
        assertEquals(0, photos.size)
        compose.onRoot().captureRoboImage("build/screenshots/review-viewer-empty.png")
    }
}
