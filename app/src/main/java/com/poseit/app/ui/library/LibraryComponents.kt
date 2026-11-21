package com.poseit.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

/**
 * Every filter in one horizontally scrolling chip row, edge to edge with the first chip on the gutter: who's in the
 * photo, then the category, the place and the mood, each group set off by a short vertical Hairline. The chips say
 * what they filter, so the groups need no captions. A selected chip is cleared by tapping it again.
 */
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
    // Couple and Group are already people chips, and My poses is a tab, so they aren't offered as categories too.
    val shownCategories = categories.filter { it !in RedundantCategories || it == filter.category }
    val people = PeopleFilter.entries.filter { it != PeopleFilter.ANY }
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .bleedHorizontally(Space.gutter),
        contentPadding = PaddingValues(horizontal = Space.gutter),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(people.size, key = { "people_${people[it].name}" }) { i ->
            val option = people[i]
            val selected = filter.people == option
            StudioChip(
                label = option.label,
                selected = selected,
                onClick = { onPeople(if (selected) PeopleFilter.ANY else option) },
                leadingIcon = when (option) {
                    PeopleFilter.SOLO -> peopleIcon(1)
                    PeopleFilter.COUPLE -> peopleIcon(2)
                    else -> peopleIcon(3)
                },
            )
        }
        if (shownCategories.size > 1) {
            item(key = "divider_category") { GroupDivider() }
            items(shownCategories.size, key = { "cat_${shownCategories[it].name}" }) { i ->
                val category = shownCategories[i]
                // The view model clears the category when the selected one is tapped again.
                StudioChip(label = category.label, selected = filter.category == category, onClick = { onCategory(category) })
            }
        }
        if (locationTags.isNotEmpty()) {
            item(key = "divider_location") { GroupDivider() }
            items(locationTags.size, key = { "loc_${locationTags[it].name}" }) { i ->
                val tag = locationTags[i]
                StudioChip(label = tag.label, selected = filter.location == tag, onClick = { onLocation(tag) })
            }
        }
        if (moodTags.isNotEmpty()) {
            item(key = "divider_mood") { GroupDivider() }
            items(moodTags.size, key = { "mood_${moodTags[it].name}" }) { i ->
                val tag = moodTags[i]
                StudioChip(label = tag.label, selected = filter.mood == tag, onClick = { onMood(tag) })
            }
        }
    }
}

private val RedundantCategories = setOf(PoseCategory.COUPLE, PoseCategory.GROUP, PoseCategory.CUSTOM)

@Composable
private fun GroupDivider() {
    VerticalHairline(
        Modifier
            .height(20.dp)
            .padding(horizontal = Space.xs),
    )
}

/**
 * Today's pose: a larger figure on its patch of studio paper, the name in the Title cut, its facts as one short
 * phrase and "Try this pose" right under them. Tapping anywhere else opens the pose.
 */
@Composable
internal fun TodaysPose(pose: PoseTemplate, onOpen: () -> Unit, onTry: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = Space.l
        val tileWidth = (maxWidth - gap) * 0.44f
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
            Column(Modifier.weight(1f)) {
                Text("Today's pose", style = StudioType.label, color = Studio.Graphite)
                Spacer(Modifier.height(Space.xs))
                Text(pose.name, style = StudioType.title, color = Studio.Ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Space.xs))
                Text(pose.factsPhrase(), style = StudioType.label, color = Studio.Graphite)
                Spacer(Modifier.height(Space.l))
                // A secondary button: the floating Pose camera button is the screen's one primary action.
                SecondaryButton("Try this pose", onClick = onTry)
            }
        }
    }
}
