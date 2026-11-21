package com.poseit.app.screens

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.poseit.app.data.SavedPhoto
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.ui.components.StudioTopBar
import com.poseit.app.ui.create.CreateStep
import com.poseit.app.ui.create.CreateUiState
import com.poseit.app.ui.create.OutlineStatus
import com.poseit.app.ui.create.ProblemContent
import com.poseit.app.ui.create.ReviewBottomBar
import com.poseit.app.ui.create.ReviewContent
import com.poseit.app.ui.create.WorkingContent
import com.poseit.app.ui.gallery.GalleryHeader
import com.poseit.app.ui.gallery.PhotoGrid
import com.poseit.app.ui.gallery.PhotoViewer
import com.poseit.app.ui.gallery.ReviewCard
import com.poseit.app.ui.theme.PoseitTheme
import com.poseit.app.ui.theme.Studio
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screens of the photo flows that the other screenshot tests can't reach (detection needs MediaPipe, the grid needs
 * MediaStore photos): the create-from-photo review, its working and problem states, the gallery grid and the
 * photo viewer's delete dialog. Rendered into app/build/screenshots/photo-*.png for design review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class PhotoScreensScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    /** A stand-in photo: a cool wall over a darker floor. */
    private fun fakePhoto(w: Int = 600, h: Int = 800): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val wall = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h * 0.7f, 0xFF9AA7B0.toInt(), 0xFF7D8A93.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.72f, wall)
        canvas.drawRect(0f, h * 0.72f, w.toFloat(), h.toFloat(), Paint().apply { color = 0xFF5E5A55.toInt() })
        return bmp
    }

    /** A stand-in segmentation outline: a white contour on transparent, like PersonSegmenter's output. */
    private fun fakeOutline(w: Int = 600, h: Int = 800): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val stroke = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawOval(w * 0.42f, h * 0.10f, w * 0.58f, h * 0.24f, stroke)
        canvas.drawRoundRect(w * 0.36f, h * 0.25f, w * 0.64f, h * 0.88f, 40f, 40f, stroke)
        return bmp
    }

    private fun reviewState(people: Int, selected: Set<Int>, outline: Boolean = false): CreateUiState {
        val pose = BuiltInPoses.all.first { it.personCount == people }
        val photo = fakePhoto()
        return CreateUiState(
            step = CreateStep.REVIEW,
            photo = photo,
            people = pose.figures,
            selected = selected,
            outline = if (outline) fakeOutline() else null,
            outlineStatus = if (outline) OutlineStatus.READY else OutlineStatus.WORKING,
        )
    }

    private fun show(content: @Composable ColumnScope.() -> Unit) {
        compose.setContent {
            PoseitTheme {
                Column(Modifier.fillMaxSize().background(Studio.Paper)) { content() }
            }
        }
        compose.waitForIdle()
    }

    private fun capture(name: String) = compose.onRoot().captureRoboImage("build/screenshots/photo-$name.png")

    @Composable
    private fun ColumnScope.Review(state: CreateUiState) {
        var name by remember { mutableStateOf("My pose 1") }
        var category by remember { mutableStateOf(PoseCategory.CUSTOM) }
        var tags by remember { mutableStateOf(setOf(PoseTag.CASUAL)) }
        // The review draws its own top bar card ("Your new pose"), fused to the traced photo.
        Column(Modifier.weight(1f)) {
            ReviewContent(
                state = state,
                name = name,
                onNameChange = { name = it },
                category = category,
                onCategoryChange = { category = it },
                tags = tags,
                onTagsChange = { tags = it },
                onTogglePerson = {},
                onPickAnother = {},
                bottomBar = { ReviewBottomBar(saving = false, canSave = true, onSave = {}) },
            )
        }
    }

    @Test
    fun createReviewOnePerson() {
        show { Review(reviewState(people = 1, selected = setOf(0))) }
        capture("create-review")
    }

    @Test
    @Config(qualifiers = "w400dp-h2400dp-xxhdpi")
    fun createReviewWholePage() {
        show { Review(reviewState(people = 2, selected = setOf(0), outline = true)) }
        compose.onAllNodes(hasText("Photo outline") and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()
        capture("create-review-whole")
    }

    @Test
    fun createWorking() {
        show { WorkingContent(CreateUiState(step = CreateStep.DETECTING, photo = fakePhoto())) }
        capture("create-working")
    }

    @Test
    fun createNoPerson() {
        show {
            ProblemContent(
                photo = fakePhoto(800, 600),
                aspect = 800f / 600f,
                title = "No one found in this photo",
                message = "Try a photo where the person is clearly visible and not too small, ideally head to toe in good light.",
                onPick = {},
            )
        }
        capture("create-no-person")
    }

    private fun photo(i: Int, dayOffset: Int) = SavedPhoto(
        uri = Uri.parse("content://media/external/images/media/$i"),
        displayName = "Poseit_$i.jpg",
        dateTakenMillis = System.currentTimeMillis() - dayOffset * 86_400_000L - i * 60_000L,
        width = 3000,
        height = 4000,
    )

    @Test
    fun galleryGrid() {
        val photos = List(5) { photo(it, 0) } + List(4) { photo(10 + it, 1) } + List(3) { photo(20 + it, 9) }
        show {
            PhotoGrid(
                photos = photos,
                permissionMissing = true,
                onGrantPermission = {},
                onOpen = {},
                header = { GalleryHeader(photoCount = photos.size, onBack = {}) { ReviewCard(onReview = {}) } },
            )
        }
        capture("gallery-grid")
    }

    @Test
    fun viewerDeleteDialog() {
        var photos by mutableStateOf(List(3) { photo(it + 1, 0) })
        show {
            PhotoViewer(
                photos = photos,
                initialPhoto = photos[0],
                snackbarHostState = remember { SnackbarHostState() },
                onClose = {},
                onCurrentPhotoChanged = {},
                onShare = {},
                onOpenExternally = {},
                onDeleteConfirmed = { p -> photos = photos - p },
                onQuickDelete = { p -> photos = photos - p },
                onReviewFinished = {},
            )
        }
        capture("viewer")
        compose.onAllNodes(hasContentDescription("Delete") and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()
        captureScreenRoboImage("build/screenshots/photo-viewer-delete-dialog.png")
    }
}
