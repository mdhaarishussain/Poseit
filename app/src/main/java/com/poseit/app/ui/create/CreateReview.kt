package com.poseit.app.ui.create

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import com.poseit.app.ui.components.AccentButton
import com.poseit.app.ui.components.CapsLabel
import com.poseit.app.ui.components.CardSegment
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FigureStyle
import com.poseit.app.ui.components.FilterPill
import com.poseit.app.ui.components.IconSize
import com.poseit.app.ui.components.PillTabs
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.PoseitCard
import com.poseit.app.ui.components.PoseitSpinner
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.detail.PinnedActions
import com.poseit.app.ui.detail.ScrollingStackScreen
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitRadius
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
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
internal fun ReviewContent(
    viewModel: CreateFromPhotoViewModel,
    onPickAnother: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState?,
    bottomBar: @Composable () -> Unit,
) {
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
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        bottomBar = bottomBar,
    )
}

/**
 * The review step. The top bar card ("Your new pose") is fused to the traced photo and to the card saying who was
 * found (with "Choose another photo"); below on the canvas, a preview of the guide styles on the dark stage and the
 * pose's details. [bottomBar] is usually [ReviewBottomBar].
 */
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
    onBack: () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: @Composable () -> Unit = {},
) {
    val photo = state.photo ?: return
    val photoLimit = LocalConfiguration.current.screenHeightDp.let { h -> (h * 0.55f).coerceIn(240f, 520f).dp }
    ScrollingStackScreen(
        pinnedTitle = "Your new pose",
        title = "Your new pose",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        bottomBar = bottomBar,
        joined = {
            CardSegment(contentPadding = PaddingValues(Space.m)) {
                DetectedPhoto(
                    photo = photo,
                    aspect = state.imageAspect,
                    people = state.people,
                    selected = state.selected,
                    onTogglePerson = onTogglePerson,
                    heightLimit = photoLimit,
                )
            }
            CardSegment(contentPadding = PaddingValues(start = Space.cardPadding, end = Space.m, top = Space.s, bottom = Space.l)) {
                PeopleSection(state.people.size, state.selected, onTogglePerson, onPickAnother)
            }
        },
    ) {
        Spacer(Modifier.height(Space.section))
        GuidePreviewSection(state, name = name)
        Spacer(Modifier.height(Space.section))
        SectionHeading("Details", Modifier.padding(horizontal = Space.textInset))
        Spacer(Modifier.height(Space.s))
        PoseDetailsForm(
            name = name,
            onNameChange = onNameChange,
            category = category,
            onCategoryChange = onCategoryChange,
            tags = tags,
            onTagsChange = onTagsChange,
            modifier = Modifier.padding(horizontal = Space.textInset),
        )
        Spacer(Modifier.height(Space.xl))
    }
}

/**
 * The picked photo with the traced skeleton(s) on top; tap a person to include / exclude them. Skeletons are cream
 * sticks over an ink shadow line so they read on any photo; people left out are faded. With more than one person,
 * each carries a numbered tag that matches its pill.
 */
@Composable
private fun DetectedPhoto(
    photo: Bitmap,
    aspect: Float,
    people: List<Skeleton>,
    selected: Set<Int>,
    onTogglePerson: (Int) -> Unit,
    heightLimit: androidx.compose.ui.unit.Dp,
) {
    val drawable = remember(people) { people.map { it.visibleOnly() } }
    val textMeasurer = rememberTextMeasurer()
    val currentToggle by rememberUpdatedState(onTogglePerson)
    val multi = people.size > 1
    PhotoStage(photo, aspect, heightLimit = heightLimit, contentDescription = "Your photo with the traced pose") {
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
            drawRect(PoseitColors.backgroundCamera.copy(alpha = 0.18f))
            drawable.forEachIndexed { i, person ->
                val isOn = i in selected
                val unitPx = person.estimatedUnit(aspect) * size.height
                val line = max(2.dp.toPx(), unitPx * 0.06f)
                // Shadow line first, then the cream stick on top of it.
                drawPoseFigure(
                    person, aspect, rect,
                    FigurePaint(
                        style = FigureStyle.STICK,
                        fill = PoseitColors.textPrimary,
                        alpha = if (isOn) 0.45f else 0.15f,
                        strokeWidthPx = line + 3.dp.toPx(),
                    ),
                )
                drawPoseFigure(
                    person, aspect, rect,
                    FigurePaint(style = FigureStyle.STICK, fill = PoseitColors.figureFill, alpha = if (isOn) 1f else 0.4f, strokeWidthPx = line),
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
                            (if (isOn) PoseitColors.accent else PoseitColors.backgroundSurface).copy(alpha = if (isOn) 1f else 0.6f),
                            topLeft = Offset(c.x - half, c.y - half),
                            size = Size(half * 2, half * 2),
                            cornerRadius = CornerRadius(PoseitRadius.inner.toPx()),
                        )
                        val label = textMeasurer.measure(
                            "${i + 1}",
                            PoseitType.numeral(15.sp).copy(color = if (isOn) PoseitColors.onAccent else PoseitColors.textSecondary, lineHeight = 15.sp),
                        )
                        drawText(label, topLeft = Offset(c.x - label.size.width / 2f, c.y - label.size.height / 2f))
                    }
                }
            }
        }
    }
}

/** What was found, the way back to the picker, and (for a group) a pill per person to include or leave out. */
@Composable
private fun PeopleSection(count: Int, selected: Set<Int>, onToggle: (Int) -> Unit, onPickAnother: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (count == 1) "Pose traced" else "Found $count people",
                style = PoseitType.bodyLarge,
                color = PoseitColors.textPrimaryInverted,
                modifier = Modifier.weight(1f),
            )
            TertiaryButton("Choose another photo", onClick = onPickAnother)
        }
        if (count > 1) {
            Text(
                "Choose who to include. Tap a pill or a person in the photo.",
                style = PoseitType.supporting,
                color = PoseitColors.textSecondaryInverted,
                modifier = Modifier.padding(end = Space.m),
            )
            Spacer(Modifier.height(Space.s))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                repeat(count) { i ->
                    FilterPill(label = "Person ${i + 1}", selected = i in selected, onClick = { onToggle(i) })
                }
            }
        }
    }
}

/** A preview of the three guide styles on the dark stage. A preview only: the camera picks the style. */
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
        SectionHeading("Guide styles", Modifier.padding(horizontal = Space.textInset))
        Spacer(Modifier.height(Space.s))
        PillTabs(
            labels = GuidePreview.entries.map { it.label },
            selectedIndex = mode.ordinal,
            onSelect = { mode = GuidePreview.entries[it] },
            edgePadding = Space.edge,
        )
        Spacer(Modifier.height(Space.s))
        PoseitCard(
            modifier = Modifier
                .padding(horizontal = Space.edge)
                .fillMaxWidth()
                .height(260.dp),
            contentPadding = PaddingValues(Space.m),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (mode) {
                    GuidePreview.MANNEQUIN -> if (previewTemplate.figures.isNotEmpty()) {
                        PoseThumbnail(previewTemplate, Modifier.fillMaxSize(), background = null)
                    }
                    GuidePreview.PHOTO -> PhotoPreview(photo, aspect, alpha = 0.75f)
                    GuidePreview.OUTLINE -> when (state.outlineStatus) {
                        OutlineStatus.READY -> state.outline?.let { outline ->
                            Box(contentAlignment = Alignment.Center) {
                                PhotoPreview(photo, aspect, alpha = 0.25f)
                                // The outline image is white; tint it cream so it reads on the dark stage.
                                PhotoPreview(
                                    outline,
                                    aspect,
                                    alpha = 1f,
                                    colorFilter = ColorFilter.tint(PoseitColors.textPrimaryInverted, BlendMode.SrcIn),
                                )
                            }
                        }
                        OutlineStatus.UNAVAILABLE -> Text(
                            "No outline could be made for this photo. The mannequin and photo guides still work.",
                            style = PoseitType.body,
                            color = PoseitColors.textSecondaryInverted,
                            modifier = Modifier.padding(Space.m),
                        )
                        else -> Row(verticalAlignment = Alignment.CenterVertically) {
                            PoseitSpinner(size = IconSize.inline, color = PoseitColors.textPrimaryInverted)
                            Spacer(Modifier.width(Space.m))
                            Text("Drawing the outline…", style = PoseitType.body, color = PoseitColors.textPrimaryInverted)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.m))
        Column(Modifier.padding(horizontal = Space.textInset)) {
            Text(mode.caption, style = PoseitType.body, color = PoseitColors.textPrimary)
            Spacer(Modifier.height(Space.xs))
            Text(
                "The camera starts with the photo outline when there is one. Switch styles with its guide button.",
                style = PoseitType.supporting,
                color = PoseitColors.textSecondary,
            )
        }
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

/** The review's one action, lavender, pinned above the navigation bar and the keyboard. */
@Composable
internal fun ReviewBottomBar(saving: Boolean, canSave: Boolean, onSave: () -> Unit) {
    PinnedActions {
        AccentButton(onClick = onSave, enabled = canSave && !saving, modifier = Modifier.fillMaxWidth()) {
            if (saving) {
                PoseitSpinner(size = IconSize.inline)
                Spacer(Modifier.width(Space.m))
                CapsLabel("Saving…", style = PoseitType.capsButton)
            } else {
                CapsLabel("Save pose", style = PoseitType.capsButton)
            }
        }
    }
}
