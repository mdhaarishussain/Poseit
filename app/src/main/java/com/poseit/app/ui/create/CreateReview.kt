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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poseit.app.pose.Box as PoseBox
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.render.FigureShapes
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FigureStyle
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.theme.PoseitColors

/** Vivid per-person colours for skeletons drawn over a photo. */
private val PersonColors = listOf(PoseitColors.Mint, PoseitColors.Amber, PoseitColors.Coral, Color(0xFF6EC8FF))

private fun personColor(index: Int) = PersonColors[index % PersonColors.size]

/** Only the joints MediaPipe was reasonably sure about (what the saved template will contain). */
internal fun Skeleton.visibleOnly(): Skeleton {
    val kept = points.filterKeys { isVisible(it, CreateFromPhotoViewModel.MIN_VISIBILITY) }
    return copy(points = kept, visibility = visibility.filterKeys { it in kept })
}

private enum class GuidePreview(val label: String, val caption: String) {
    MANNEQUIN("Mannequin", "A clean figure to line up with."),
    PHOTO("Photo", "Your photo, shown see-through over the camera."),
    OUTLINE("Outline", "Just the person's outline – great for exact framing."),
}

@Composable
internal fun ReviewContent(viewModel: CreateFromPhotoViewModel) {
    val state = viewModel.state
    val photo = state.photo ?: return
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(0.dp))
        DetectedPhoto(
            photo = photo,
            aspect = state.imageAspect,
            people = state.people,
            selected = state.selected,
            onTogglePerson = viewModel::togglePerson,
        )
        PeopleSection(state.people.size, state.selected, viewModel::togglePerson)
        GuidePreviewCard(state, name = viewModel.name)
        Text("Details", style = MaterialTheme.typography.titleMedium)
        PoseDetailsForm(
            name = viewModel.name,
            onNameChange = { viewModel.name = it },
            category = viewModel.category,
            onCategoryChange = { viewModel.category = it },
            tags = viewModel.tags,
            onTagsChange = { viewModel.tags = it },
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** The picked photo with the traced skeleton(s) on top; tap a person to include / exclude them. */
@Composable
private fun DetectedPhoto(
    photo: Bitmap,
    aspect: Float,
    people: List<Skeleton>,
    selected: Set<Int>,
    onTogglePerson: (Int) -> Unit,
) {
    val image = remember(photo) { photo.asImageBitmap() }
    val drawable = remember(people) { people.map { it.visibleOnly() } }
    val textMeasurer = rememberTextMeasurer()
    val currentToggle by rememberUpdatedState(onTogglePerson)
    val multi = people.size > 1
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val maxH = 460.dp
        Box(
            Modifier
                .heightIn(max = maxH)
                .aspectRatio(aspect, matchHeightConstraintsFirst = maxWidth / maxH > aspect)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Image(image, contentDescription = "Your photo with the traced pose", contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
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
                drawRect(Color.Black.copy(alpha = 0.18f))
                drawable.forEachIndexed { i, person ->
                    val isOn = i in selected
                    drawPoseFigure(
                        person, aspect, rect,
                        FigurePaint(
                            style = FigureStyle.STICK,
                            fill = if (isOn) personColor(i) else Color.White,
                            alpha = if (isOn) 0.9f else 0.35f,
                        ),
                    )
                    if (multi) {
                        // Number badge above the head.
                        val iso = person.toIsotropic(aspect)
                        val unit = person.estimatedUnit(aspect)
                        val head = FigureShapes.headCenter(iso, unit, person.midShoulder()?.let { Vec2(it.x * aspect, it.y) })
                            ?: PoseBox.around(iso.values)?.let { Vec2(it.center.x, it.top) }
                        if (head != null) {
                            val r = 13.dp.toPx()
                            val c = Offset(
                                (head.x * size.height).coerceIn(r, size.width - r),
                                (head.y * size.height - unit * size.height * 0.45f - r).coerceIn(r, size.height - r),
                            )
                            drawCircle(if (isOn) personColor(i) else Color.Gray, radius = r, center = c)
                            val label = textMeasurer.measure("${i + 1}", TextStyle(color = Color(0xFF15161A), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                            drawText(label, topLeft = Offset(c.x - label.size.width / 2f, c.y - label.size.height / 2f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeopleSection(count: Int, selected: Set<Int>, onToggle: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PoseitColors.Mint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = PoseitColors.Mint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (count == 1) "Pose traced" else "Found $count people",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (count > 1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Choose who to include – tap the chips or the people in the photo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(count) { i ->
                    val isOn = i in selected
                    FilterChip(
                        selected = isOn,
                        onClick = { onToggle(i) },
                        label = { Text("Person ${i + 1}") },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(FilterChipDefaults.IconSize)
                                    .clip(CircleShape)
                                    .background(if (isOn) personColor(i) else Color.Gray),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isOn) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF15161A), modifier = Modifier.size(12.dp))
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuidePreviewCard(state: CreateUiState, name: String) {
    var mode by rememberSaveable { mutableStateOf(GuidePreview.MANNEQUIN) }
    val photo = state.photo ?: return
    val aspect = state.imageAspect
    val previewTemplate = remember(state.people, state.selected, aspect) {
        PoseTemplate(
            id = "preview",
            name = name,
            category = com.poseit.app.pose.PoseCategory.CUSTOM,
            figures = state.selectedPeople.map { it.visibleOnly() }.filter { it.points.isNotEmpty() },
            frameAspect = aspect,
        )
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("How the guide looks", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                GuidePreview.entries.forEachIndexed { i, option ->
                    SegmentedButton(
                        selected = mode == option,
                        onClick = { mode = option },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = GuidePreview.entries.size),
                        // No icons: three labels have to fit on a 360 dp phone.
                        icon = {},
                        label = { Text(option.label, maxLines = 1) },
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2B2E3A), Color(0xFF1C1E25)))),
                contentAlignment = Alignment.Center,
            ) {
                when (mode) {
                    GuidePreview.MANNEQUIN -> if (previewTemplate.figures.isNotEmpty()) {
                        PoseThumbnail(previewTemplate, Modifier.fillMaxSize(), background = null)
                    }
                    GuidePreview.PHOTO -> PhotoPreview(photo, aspect, alpha = 0.75f)
                    GuidePreview.OUTLINE -> when (state.outlineStatus) {
                        OutlineStatus.READY -> state.outline?.let { outline ->
                            Box(contentAlignment = Alignment.Center) {
                                PhotoPreview(photo, aspect, alpha = 0.25f)
                                PhotoPreview(outline, aspect, alpha = 1f)
                            }
                        }
                        OutlineStatus.UNAVAILABLE -> Text(
                            "No outline could be made for this photo.\nThe mannequin and photo guides still work.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            Spacer(Modifier.height(10.dp))
                            Text("Drawing the outline…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text(mode.caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PhotoPreview(bitmap: Bitmap, aspect: Float, alpha: Float) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            image,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            alpha = alpha,
            modifier = Modifier.aspectRatio(aspect, matchHeightConstraintsFirst = maxWidth / maxHeight > aspect),
        )
    }
}

@Composable
internal fun ReviewBottomBar(saving: Boolean, canSave: Boolean, onPickAnother: () -> Unit, onSave: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onPickAnother, enabled = !saving) { Text("Other photo") }
            Button(
                onClick = onSave,
                enabled = canSave && !saving,
                modifier = Modifier.weight(1f).widthIn(min = 160.dp).height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                    Spacer(Modifier.width(10.dp))
                    Text("Saving…")
                } else {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save pose")
                }
            }
        }
    }
}
