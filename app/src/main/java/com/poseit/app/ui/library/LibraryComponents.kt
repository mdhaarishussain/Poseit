package com.poseit.app.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.theme.PoseitColors

/** Lets a full-width child (e.g. a chip row) ignore the parent's horizontal content padding. */
internal fun Modifier.bleedHorizontally(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = amount.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
        ),
    )
    val width = (placeable.width - extra).coerceAtLeast(0)
    layout(width, placeable.height) { placeable.place(-extra / 2, 0) }
}

/** Pill-shaped search field. Pressing the keyboard's search key just hides the keyboard (results are live). */
@Composable
internal fun LibrarySearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus: FocusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search poses, places, moods…", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            keyboard?.hide()
            focus.clearFocus()
        }),
    )
}

/** Segmented control with a sliding highlight. Labels shrink to fit on narrow phones. */
@Composable
internal fun LibraryTabs(selected: LibraryTab, onSelect: (LibraryTab) -> Unit, modifier: Modifier = Modifier) {
    val tabs = LibraryTab.entries
    val selectedIndex = tabs.indexOf(selected)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        val segment = maxWidth / tabs.size
        val indicatorOffset by animateDpAsState(segment * selectedIndex, spring(dampingRatio = 0.8f, stiffness = 500f), label = "tabIndicator")
        Box(
            Modifier
                .offset(x = indicatorOffset)
                .width(segment)
                .fillMaxHeight()
                .clip(RoundedCornerShape(19.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Row(Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val isSelected = tab == selected
                val textColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tabText",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(19.dp))
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge.copy(color = textColor, textAlign = TextAlign.Center),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp, stepSize = 0.5.sp),
                    )
                }
            }
        }
    }
}

/** A horizontally scrolling row of chips that starts with a small caption, edge to edge on screen. */
@Composable
internal fun ChipRow(
    caption: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    content: LazyListScope.() -> Unit,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .bleedHorizontally(horizontalPadding),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "caption") {
            Text(
                caption,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .semantics { heading() },
            )
        }
        content()
    }
}

@Composable
internal fun LibraryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            selectedContainerColor = PoseitColors.Violet,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = Color.Transparent,
        ),
    )
}

/** The three filter rows: people, category, place + mood. */
@Composable
internal fun LibraryFilters(
    filter: LibraryFilter,
    categories: List<PoseCategory>,
    locationTags: List<PoseTag>,
    moodTags: List<PoseTag>,
    onPeople: (PeopleFilter) -> Unit,
    onCategory: (PoseCategory?) -> Unit,
    onLocation: (PoseTag) -> Unit,
    onMood: (PoseTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ChipRow(caption = "Who's in the photo?") {
            items(PeopleFilter.entries.size, key = { "people_$it" }) { i ->
                val option = PeopleFilter.entries[i]
                LibraryChip(
                    label = option.label,
                    selected = filter.people == option,
                    onClick = { onPeople(option) },
                    leadingIcon = when (option) {
                        PeopleFilter.ANY -> null
                        PeopleFilter.SOLO -> peopleIcon(1)
                        PeopleFilter.COUPLE -> peopleIcon(2)
                        PeopleFilter.GROUP -> peopleIcon(3)
                    },
                )
            }
        }
        if (categories.size > 1) {
            ChipRow(caption = "Category") {
                item(key = "cat_all") {
                    LibraryChip(label = "All", selected = filter.category == null, onClick = { onCategory(null) })
                }
                items(categories.size, key = { "cat_${categories[it].name}" }) { i ->
                    val category = categories[i]
                    LibraryChip(
                        label = category.label,
                        selected = filter.category == category,
                        onClick = { onCategory(category) },
                    )
                }
            }
        }
        if (locationTags.isNotEmpty() || moodTags.isNotEmpty()) {
            ChipRow(caption = "Where are you?") {
                items(locationTags.size, key = { "loc_${locationTags[it].name}" }) { i ->
                    val tag = locationTags[i]
                    LibraryChip(
                        label = tag.label,
                        leadingIcon = Icons.Filled.Place,
                        selected = filter.location == tag,
                        onClick = { onLocation(tag) },
                    )
                }
                if (locationTags.isNotEmpty() && moodTags.isNotEmpty()) {
                    item(key = "divider") {
                        VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))
                    }
                }
                items(moodTags.size, key = { "mood_${moodTags[it].name}" }) { i ->
                    val tag = moodTags[i]
                    LibraryChip(
                        label = tag.label,
                        selected = filter.mood == tag,
                        onClick = { onMood(tag) },
                    )
                }
            }
        }
    }
}

/** Big gradient card with today's pose. Tapping the card opens it, "Try it" goes straight to the camera. */
@Composable
internal fun PoseOfTheDayCard(pose: PoseTemplate, onOpen: () -> Unit, onTry: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(196.dp)
                .background(HeroGradient)
                .clickable(onClickLabel = "Open pose of the day", onClick = onOpen),
        ) {
            Box(
                Modifier
                    .weight(0.44f)
                    .fillMaxHeight()
                    .padding(start = 8.dp, top = 12.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                )
                PoseThumbnail(template = pose, modifier = Modifier.fillMaxSize(), background = null)
            }
            Column(
                Modifier
                    .weight(0.56f)
                    .fillMaxHeight()
                    .padding(top = 18.dp, bottom = 16.dp, end = 16.dp, start = 4.dp),
            ) {
                Text(
                    "POSE OF THE DAY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    pose.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${pose.categoryText} · ${Difficulty.label(pose.difficulty)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onTry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PoseitColors.VioletDark),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Try it")
                }
            }
        }
    }
}

private val HeroGradient = Brush.linearGradient(
    listOf(PoseitColors.VioletDark, PoseitColors.Violet, Color(0xFFB765E8), PoseitColors.Coral),
)

/** Text + actions for empty lists. */
@Composable
internal fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            actions()
        }
    }
}
