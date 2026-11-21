package com.poseit.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.PoseTile
import com.poseit.app.ui.components.SecondaryButton
import com.poseit.app.ui.components.StudioChip
import com.poseit.app.ui.components.StudioIconButton
import com.poseit.app.ui.components.StudioIconSize
import com.poseit.app.ui.components.StudioTabs
import com.poseit.app.ui.components.VerticalHairline
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType

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

/**
 * The search field: a Sheet patch with the Small radius and an outlined search glyph, Ink outline only while
 * focused. Pressing the keyboard's search key just hides the keyboard (results are live).
 */
@Composable
internal fun LibrarySearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus: FocusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = {
            Text("Search poses, places, moods…", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(StudioIconSize)) },
        trailingIcon = if (query.isNotEmpty()) {
            { StudioIconButton(Icons.Outlined.Close, "Clear search", onClick = { onQueryChange("") }, tint = Studio.Graphite) }
        } else {
            null
        },
        singleLine = true,
        shape = StudioShapes.Small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Studio.Sheet,
            unfocusedContainerColor = Studio.Sheet,
            focusedBorderColor = Studio.Ink,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = Studio.Ink,
            unfocusedTextColor = Studio.Ink,
            cursorColor = Studio.Ink,
            focusedPlaceholderColor = Studio.Graphite,
            unfocusedPlaceholderColor = Studio.Graphite,
            focusedLeadingIconColor = Studio.Ink,
            unfocusedLeadingIconColor = Studio.Graphite,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            keyboard?.hide()
            focus.clearFocus()
        }),
    )
}

/** All / Favourites / Recent / My poses as Studio text tabs, edge to edge with the first tab on the gutter. */
@Composable
internal fun LibraryTabs(selected: LibraryTab, onSelect: (LibraryTab) -> Unit, modifier: Modifier = Modifier) {
    val tabs = LibraryTab.entries
    StudioTabs(
        labels = tabs.map { it.label },
        selectedIndex = tabs.indexOf(selected),
        onSelect = { onSelect(tabs[it]) },
        modifier = modifier.bleedHorizontally(Space.gutter),
        edgePadding = Space.gutter,
    )
}

/** A horizontally scrolling row of chips that starts with a short caption, edge to edge on screen. */
@Composable
internal fun ChipRow(
    caption: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = Space.gutter,
    content: LazyListScope.() -> Unit,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .bleedHorizontally(horizontalPadding),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "caption") {
            Text(
                caption,
                style = StudioType.label,
                color = Studio.Graphite,
                modifier = Modifier
                    .padding(end = Space.xs)
                    .semantics { heading() },
            )
        }
        content()
    }
}

/** The three filter rows: people, category, place + mood. Category, place and mood chips are text only. */
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
    Column(modifier) {
        ChipRow(caption = "Who's in the photo?") {
            items(PeopleFilter.entries.size, key = { "people_$it" }) { i ->
                val option = PeopleFilter.entries[i]
                StudioChip(
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
                    StudioChip(label = "All", selected = filter.category == null, onClick = { onCategory(null) })
                }
                items(categories.size, key = { "cat_${categories[it].name}" }) { i ->
                    val category = categories[i]
                    StudioChip(
                        label = category.displayLabel,
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
                    StudioChip(
                        label = tag.label,
                        selected = filter.location == tag,
                        onClick = { onLocation(tag) },
                    )
                }
                if (locationTags.isNotEmpty() && moodTags.isNotEmpty()) {
                    item(key = "divider") {
                        VerticalHairline(
                            Modifier
                                .height(20.dp)
                                .padding(horizontal = Space.xs),
                        )
                    }
                }
                items(moodTags.size, key = { "mood_${moodTags[it].name}" }) { i ->
                    val tag = moodTags[i]
                    StudioChip(
                        label = tag.label,
                        selected = filter.mood == tag,
                        onClick = { onMood(tag) },
                    )
                }
            }
        }
    }
}

/** The name in the featured row: the wide display cut, a step below the detail screen's 28sp. */
private val FeaturedName = StudioType.display.copy(fontSize = 22.sp, lineHeight = 28.sp)

/**
 * Today's pose: a larger figure on its patch of studio paper, the name in the wide cut, its category and
 * difficulty as two quiet words, and "Try it". Tapping anywhere else opens the pose.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TodaysPose(pose: PoseTemplate, onOpen: () -> Unit, onTry: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = Space.l
        val tileWidth = (maxWidth - gap) * 0.44f
        // The text column is at least as tall as the tile, so "Try it" sits level with the tile's foot; with a
        // large font it grows past it instead of squeezing the button.
        val tileHeight = tileWidth * (4f / 3f)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(interactionSource = source, indication = null, onClickLabel = "Open today's pose", onClick = onOpen),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            PoseTile(
                pose = pose,
                name = null,
                detail = null,
                interactionSource = source,
                modifier = Modifier.width(tileWidth),
            )
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = tileHeight),
            ) {
                Text("Today's pose", style = StudioType.label, color = Studio.Graphite)
                Spacer(Modifier.height(Space.xs))
                Text(pose.name, style = FeaturedName, color = Studio.Ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Space.s))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    Text(pose.categoryText, style = StudioType.label, color = Studio.Graphite)
                    Text(Difficulty.label(pose.difficulty), style = StudioType.label, color = Studio.Graphite)
                }
                Spacer(Modifier.height(Space.l))
                Spacer(Modifier.weight(1f))
                SecondaryButton("Try it", onClick = onTry)
            }
        }
    }
}
