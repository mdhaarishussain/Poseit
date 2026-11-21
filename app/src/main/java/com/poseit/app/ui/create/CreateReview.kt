package com.poseit.app.ui.create

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poseit.app.pose.Box as PoseBox
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.render.FigureShapes
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FigureStyle
import com.poseit.app.ui.components.HairlineDivider
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.StudioChip
import com.poseit.app.ui.components.StudioTabs
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType
import kotlin.math.max

/** Only the joints MediaPipe was reasonably sure about (what the saved template will contain). */
internal fun Skeleton.visibleOnly(): Skeleton {
    val kept = points.filterKeys { isVisible(it, CreateFromPhotoViewModel.MIN_VISIBILITY) }
    return copy(points = kept, visibility = visibility.filterKeys { it in kept })
}

private enum class GuidePreview(val label: String, val caption: String) {
    MANNEQUIN("Mannequin", "A clean figure to line up with."),
    PHOTO("Photo", "Your photo, shown see-through over the camera."),
    OUTLINE("Photo outline", "Just the person's outline, for exact framing."),
}

@Composable
internal fun ReviewContent(viewModel: CreateFromPhotoViewModel, onPickAnother: () -> Unit) {
    ReviewContent(
        state = viewModel.state,
        name = viewModel.name,
        onNameChange = { viewModel.name = it },
        category = viewModel.category,
        onCategoryChange = { viewModel.category = it },
        tags = viewModel.tags,
        onTagsChange = { viewModel.tags = it },
        onTogglePerson = viewModel::togglePerson,
        onPickAnother = onPickAnother,
    )
}

/** The review step: the traced photo, who to include, a preview of the guide styles and the pose's details. */
@Composable
internal fun ReviewContent(
    state: CreateUiState,
    name: String,
    onNameChange: (String) -> Unit,
    category: PoseCategory,
    onCategoryChange: (PoseCategory) -> Unit,
    tags: Set<PoseTag>,
    onTagsChange: (Set<PoseTag>) -> Unit,
    onTogglePerson: (Int) -> Unit,
    onPickAnother: () -> Unit,
) {
    val photo = state.photo ?: return
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = Space.s, bottom = Space.xl),
    ) {
        DetectedPhoto(
            photo = photo,
            aspect = state.imageAspect,
            people = state.people,
            selected = state.selected,
            onTogglePerson = onTogglePerson,
        )
        Spacer(Modifier.height(Space.s))
        PeopleSection(state.people.size, state.selected, onTogglePerson, onPickAnother)
        Spacer(Modifier.height(Space.section))
        GuidePreviewSection(state, name = name)
        Spacer(Modifier.height(Space.section))
        SectionHeading("Details")
        Spacer(Modifier.height(Space.m))
        PoseDetailsForm(
            name = name,
            onNameChange = onNameChange,
            category = category,
            onCategoryChange = onCategoryChange,
            tags = tags,
            onTagsChange = onTagsChange,
        )
    }
}

/**
 * The picked photo with the traced skeleton(s) on top; tap a person to include / exclude them. Skeletons are
 * Mannequin-white sticks over an Ink shadow line so they read on any photo; people left out are faded. With more
 * than one person, each carries a numbered tag that matches its chip.
 */
@Composable
private fun DetectedPhoto(
    photo: Bitmap,
    aspect: Float,
    people: List<Skeleton>,
    selected: Set<Int>,
    onTogglePerson: (Int) -> Unit,
) {
    val drawable = remember(people) { people.map { it.visibleOnly() } }
    val textMeasurer = rememberTextMeasurer()
    val currentToggle by rememberUpdatedState(onTogglePerson)
    val multi = people.size > 1
    PhotoStage(photo, aspect, heightLimit = 520.dp, contentDescription = "Your photo with the traced pose") {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(drawable, multi) {
                    if (!multi) return@pointerInput
                    detectTapGestures { tap ->
                        val p = Vec2(tap.x / size.width, tap.y / size.height)
                        // Smallest bounding box containing the tap wins (people can overlap).
                        drawable.indices
                            .mapNotNull { i -> PoseBox.around(drawable[i].points.values)?.let { i to it } }
                            .filter { (_, b) -> b.contains(p, margin = 0.04f) }
                            .minByOrNull { (_, b) -> b.width * b.height }
                            ?.let { currentToggle(it.first) }
                    }
                },
        ) {
            val rect = Rect(Offset.Zero, size)
            drawRect(Studio.LiveView.copy(alpha = 0.18f))
            drawable.forEachIndexed { i, person ->
                val isOn = i in selected
                val unitPx = person.estimatedUnit(aspect) * size.height
                val line = max(2.dp.toPx(), unitPx * 0.06f)
                // Shadow line first, then the white stick on top of it.
                drawPoseFigure(
                    person, aspect, rect,
                    FigurePaint(
                        style = FigureStyle.STICK,
                        fill = Studio.Ink,
                        alpha = if (isOn) 0.45f else 0.15f,
                        strokeWidthPx = line + 3.dp.toPx(),
                    ),
                )
                drawPoseFigure(
                    person, aspect, rect,
                    FigurePaint(style = FigureStyle.STICK, fill = Studio.Mannequin, alpha = if (isOn) 1f else 0.4f, strokeWidthPx = line),
                )
                if (multi) {
                    // Number tag above the head.
                    val iso = person.toIsotropic(aspect)
                    val unit = person.estimatedUnit(aspect)
                    val head = FigureShapes.headCenter(iso, unit, person.midShoulder()?.let { Vec2(it.x * aspect, it.y) })
                        ?: PoseBox.around(iso.values)?.let { Vec2(it.center.x, it.top) }
                    if (head != null) {
                        val half = 12.dp.toPx()
                        val c = Offset(
                            (head.x * size.height).coerceIn(half, size.width - half),
                            (head.y * size.height - unit * size.height * 0.45f - half).coerceIn(half, size.height - half),
                        )
                        drawRoundRect(
                            Studio.Sheet.copy(alpha = if (isOn) 1f else 0.6f),
                            topLeft = Offset(c.x - half, c.y - half),
                            size = Size(half * 2, half * 2),
                            cornerRadius = CornerRadius(StudioShapes.SmallRadius.toPx()),
                        )
                        val label = textMeasurer.measure(
                            "${i + 1}",
                            StudioType.numeral(15.sp).copy(color = if (isOn) Studio.Ink else Studio.Graphite, lineHeight = 15.sp),
                        )
                        drawText(label, topLeft = Offset(c.x - label.size.width / 2f, c.y - label.size.height / 2f))
                    }
                }
            }
        }
    }
}

/** What was found, the way back to the picker, and (for a group) a chip per person to include or leave out. */
@Composable
private fun PeopleSection(count: Int, selected: Set<Int>, onToggle: (Int) -> Unit, onPickAnother: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (count == 1) "Pose traced" else "Found $count people",
                style = StudioType.bodyStrong,
                color = Studio.Ink,
                modifier = Modifier.weight(1f),
            )
            // Nudged by its own padding so the label, not the ripple, ends on the gutter.
            TertiaryButton("Choose another photo", onClick = onPickAnother, modifier = Modifier.offset(x = Space.m))
        }
        if (count > 1) {
            Text(
                "Choose who to include. Tap a chip or a person in the photo.",
                style = StudioType.label,
                color = Studio.Graphite,
            )
            Spacer(Modifier.height(Space.xs))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                repeat(count) { i ->
                    StudioChip(label = "Person ${i + 1}", selected = i in selected, onClick = { onToggle(i) })
                }
            }
        }
    }
}

/** A preview of the three guide styles on the studio set. A preview only: the camera picks the style. */
@Composable
private fun GuidePreviewSection(state: CreateUiState, name: String) {
    var mode by rememberSaveable { mutableStateOf(GuidePreview.MANNEQUIN) }
    val photo = state.photo ?: return
    val aspect = state.imageAspect
    val previewTemplate = remember(state.people, state.selected, aspect) {
        PoseTemplate(
            id = "preview",
            name = name,
            category = PoseCategory.CUSTOM,
            figures = state.selectedPeople.map { it.visibleOnly() }.filter { it.points.isNotEmpty() },
            frameAspect = aspect,
        )
    }
    Column {
        SectionHeading("Guide styles")
        Spacer(Modifier.height(Space.xs))
        StudioTabs(
            labels = GuidePreview.entries.map { it.label },
            selectedIndex = mode.ordinal,
            onSelect = { mode = GuidePreview.entries[it] },
        )
        Spacer(Modifier.height(Space.l))
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(StudioShapes.Medium)
                .background(Studio.Set),
            contentAlignment = Alignment.Center,
        ) {
            when (mode) {
                GuidePreview.MANNEQUIN -> if (previewTemplate.figures.isNotEmpty()) {
                    PoseThumbnail(previewTemplate, Modifier.fillMaxSize().padding(Space.s), background = null)
                }
                GuidePreview.PHOTO -> PhotoPreview(photo, aspect, alpha = 0.75f)
                GuidePreview.OUTLINE -> when (state.outlineStatus) {
                    OutlineStatus.READY -> state.outline?.let { outline ->
                        Box(contentAlignment = Alignment.Center) {
                            PhotoPreview(photo, aspect, alpha = 0.25f)
                            // The outline image is white; tint it Ink so it reads on the light set.
                            PhotoPreview(outline, aspect, alpha = 1f, colorFilter = ColorFilter.tint(Studio.Ink, BlendMode.SrcIn))
                        }
                    }
                    OutlineStatus.UNAVAILABLE -> Text(
                        "No outline could be made for this photo. The mannequin and photo guides still work.",
                        style = StudioType.label,
                        color = Studio.Ink,
                        modifier = Modifier.padding(Space.gutter),
                    )
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Studio.Ink, strokeWidth = 2.dp)
                        Spacer(Modifier.width(Space.m))
                        Text("Drawing the outline…", style = StudioType.label, color = Studio.Ink)
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.m))
        Text(mode.caption, style = StudioType.label, color = Studio.Ink)
        Spacer(Modifier.height(Space.xs))
        Text(
            "The camera starts with the photo outline when there is one. Switch styles with its guide button.",
            style = StudioType.label,
            color = Studio.Graphite,
        )
    }
}

@Composable
private fun PhotoPreview(bitmap: Bitmap, aspect: Float, alpha: Float, colorFilter: ColorFilter? = null) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            image,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            alpha = alpha,
            colorFilter = colorFilter,
            modifier = Modifier.aspectRatio(aspect, matchHeightConstraintsFirst = maxWidth / maxHeight > aspect),
        )
    }
}

/** The one primary action of the review, pinned above the navigation bar and the keyboard. */
@Composable
internal fun ReviewBottomBar(saving: Boolean, canSave: Boolean, onSave: () -> Unit) {
    PinnedActionBar {
        PrimaryButton(onClick = onSave, enabled = canSave && !saving, modifier = Modifier.fillMaxWidth()) {
            if (saving) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                Spacer(Modifier.width(Space.m))
                Text("Saving…")
            } else {
                Text("Save pose")
            }
        }
    }
}

/** A screen's main action pinned under the content, on Paper with a Hairline above it, so it never scrolls away. */
@Composable
internal fun PinnedActionBar(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Studio.Paper),
    ) {
        HairlineDivider()
        Box(
            Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Space.gutter, vertical = Space.m),
        ) { content() }
    }
}
