package com.poseit.app.ui.library

import androidx.compose.runtime.remember
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.AccentButton
import com.poseit.app.ui.components.ButtonForm
import com.poseit.app.ui.components.CardSegment
import com.poseit.app.ui.components.FilterPill
import com.poseit.app.ui.components.PhotoCredit
import com.poseit.app.ui.components.PosePhoto
import com.poseit.app.ui.components.rememberPosePhoto
import com.poseit.app.data.PosePhoto
import com.poseit.app.ui.components.IconSize
import com.poseit.app.ui.components.PillTabs
import com.poseit.app.ui.components.PlainIconButton
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.PoseitIcons
import com.poseit.app.ui.components.VerticalHairline
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space

/** Lets a full-width child (e.g. a pill row) ignore the parent's horizontal content padding. */
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
 * The search field (Astryx Field): a 48dp cream field with 16dp corners and the 1dp ink outline of the mockups'
 * cream rows (2dp while focused), an outlined search glyph and a clear button while there is text. 48dp like the
 * tab track under it, so the header has two heights: the field and tabs, and the 40dp filter pills. The keyboard's
 * search key just hides the keyboard (results are live).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibrarySearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus: FocusManager = LocalFocusManager.current
    val interaction = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = PoseitColors.backgroundSurface,
        unfocusedContainerColor = PoseitColors.backgroundSurface,
        focusedBorderColor = PoseitColors.borderEmphasized,
        unfocusedBorderColor = PoseitColors.borderEmphasized,
        focusedTextColor = PoseitColors.textPrimary,
        unfocusedTextColor = PoseitColors.textPrimary,
        cursorColor = PoseitColors.textPrimary,
        focusedPlaceholderColor = PoseitColors.textSecondary,
        unfocusedPlaceholderColor = PoseitColors.textSecondary,
        focusedLeadingIconColor = PoseitColors.textPrimary,
        unfocusedLeadingIconColor = PoseitColors.textPrimary,
    )
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SearchFieldHeight),
        textStyle = PoseitType.bodyLarge.copy(color = PoseitColors.textPrimary),
        cursorBrush = SolidColor(PoseitColors.textPrimary),
        singleLine = true,
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            keyboard?.hide()
            focus.clearFocus()
        }),
    ) { inner ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = query,
            innerTextField = inner,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interaction,
            placeholder = {
                Text("Search poses, places, moods…", style = PoseitType.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            leadingIcon = { Icon(PoseitIcons.search, contentDescription = null, modifier = Modifier.size(IconSize.standalone)) },
            trailingIcon = if (query.isNotEmpty()) {
                { PlainIconButton(PoseitIcons.close, "Clear search", onClick = { onQueryChange("") }, tint = PoseitColors.textSecondary) }
            } else {
                null
            },
            colors = colors,
            contentPadding = PaddingValues(vertical = Space.m),
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = true,
                    isError = false,
                    interactionSource = interaction,
                    colors = colors,
                    shape = PoseitShapes.Row,
                    focusedBorderThickness = 2.dp,
                    unfocusedBorderThickness = 1.dp,
                )
            },
        )
    }
}

/** Height of the search field, the same as the tab track under it. */
private val SearchFieldHeight = 48.dp

/**
 * All / Favourites / Recent / My poses as pill tabs: a dark track with the lavender selection sliding to the chosen
 * tab. It scrolls edge to edge, the track starting on the card edge.
 */
@Composable
internal fun LibraryTabs(selected: LibraryTab, onSelect: (LibraryTab) -> Unit, modifier: Modifier = Modifier) {
    val tabs = LibraryTab.entries
    PillTabs(
        labels = tabs.map { it.label },
        selectedIndex = tabs.indexOf(selected),
        onSelect = { onSelect(tabs[it]) },
        modifier = modifier.bleedHorizontally(Space.edge),
        edgePadding = Space.edge,
    )
}

/**
 * Every filter in one horizontally scrolling row of filter pills, edge to edge with the first pill on the card
 * edge: who's in the photo, then the category, the place and the mood, each group set off by a short vertical
 * hairline. The pills say what they filter, so the groups need no captions. A selected pill is cleared by tapping
 * it again.
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
    // Couple and Group are already people pills, and My poses is a tab, so they aren't offered as categories too.
    val shownCategories = categories.filter { it !in RedundantCategories || it == filter.category }
    val people = PeopleFilter.entries.filter { it != PeopleFilter.ANY }
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .bleedHorizontally(Space.edge),
        contentPadding = PaddingValues(horizontal = Space.edge),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(people.size, key = { "people_${people[it].name}" }) { i ->
            val option = people[i]
            val selected = filter.people == option
            FilterPill(
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
                FilterPill(label = category.label, selected = filter.category == category, onClick = { onCategory(category) })
            }
        }
        if (locationTags.isNotEmpty()) {
            item(key = "divider_location") { GroupDivider() }
            items(locationTags.size, key = { "loc_${locationTags[it].name}" }) { i ->
                val tag = locationTags[i]
                FilterPill(label = tag.label, selected = filter.location == tag, onClick = { onLocation(tag) })
            }
        }
        if (moodTags.isNotEmpty()) {
            item(key = "divider_mood") { GroupDivider() }
            items(moodTags.size, key = { "mood_${moodTags[it].name}" }) { i ->
                val tag = moodTags[i]
                FilterPill(label = tag.label, selected = filter.mood == tag, onClick = { onMood(tag) })
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

/** The Today's pose card's least height: the figure needs room to stand. */
private val TodaysPoseMinHeight = 224.dp

/**
 * Today's pose, the card fused under the top bar card (docs/DESIGN.md, "Home"): "Today's pose" in mist, the name
 * in the headline style, its facts as one short phrase and "Try this pose" at the foot, with the lay figure
 * standing on the right of the dark card. The whole card opens the pose; the button opens the camera. When the pose
 * has a sample photo, a small print of it and its credit line sit above the button, so the figure keeps the stage
 * and the photo stays its companion; without one the card is the figure and the words alone.
 *
 * It is one card of a connected stack, so put it in `TopBarCard(joined = { ... })`.
 */
@Composable
internal fun TodaysPoseCard(pose: PoseTemplate, onOpen: () -> Unit, onTry: () -> Unit, modifier: Modifier = Modifier) {
    val photo = rememberPosePhoto(pose)
    // With a large font the words get more of the card, so the button's label still fits on one line.
    val largeFont = LocalDensity.current.fontScale > 1.15f
    val textShare = if (largeFont) 0.66f else 0.58f
    CardSegment(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        onClick = onOpen,
        onClickLabel = "Open today's pose",
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = TodaysPoseMinHeight),
        ) {
            Box(Modifier.matchParentSize()) {
                PoseThumbnail(
                    template = pose,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(1f - textShare + 0.04f)
                        .fillMaxHeight()
                        .padding(top = Space.l, end = Space.m, bottom = Space.m),
                    background = null,
                )
            }
            Column(
                Modifier
                    .fillMaxWidth(textShare)
                    .heightIn(min = TodaysPoseMinHeight)
                    .padding(start = Space.cardPadding, top = Space.cardPadding - Space.xs, bottom = Space.cardPadding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Today's pose", style = PoseitType.supporting, color = PoseitColors.textSecondaryInverted)
                    Spacer(Modifier.height(Space.xs))
                    // The display cut, Light and tightly tracked, like every other hero name and the mockups' headline.
                    Text(
                        pose.name,
                        style = PoseitType.display,
                        color = PoseitColors.textPrimaryInverted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(pose.factsPhrase(), style = PoseitType.supporting, color = PoseitColors.textSecondaryInverted)
                }
                if (photo != null) {
                    Spacer(Modifier.height(Space.l))
                    TodaysPhoto(photo)
                }
                Column {
                    Spacer(Modifier.height(Space.l))
                    // The screen's one accent: the thing to do next. A block, like every button inside a card.
                    AccentButton("Try this pose", onClick = onTry, form = ButtonForm.Block)
                }
            }
        }
    }
}

/**
 * Today's pose's sample photo: a small print (the whole photo, never cropped) with its credit line beside its foot,
 * or under it when the credit is a long one (a Wikimedia Commons author and licence) that would need more than two
 * lines beside the print. The card around it already names the pose, so the print has no spoken label of its own;
 * the credit is a button.
 */
@Composable
private fun TodaysPhoto(photo: PosePhoto) {
    val printWidth = if (photo.isLandscape) TodaysPhotoWidth * 4f / 3f else TodaysPhotoWidth
    val print = @Composable {
        PosePhoto(
            photo,
            Modifier
                .width(printWidth)
                .aspectRatio(photo.aspect),
            shape = PoseitShapes.Inner,
            contentDescription = null,
        )
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val besidePx = with(density) { (maxWidth - printWidth - Space.s - CreditGlyphRoom).roundToPx() }.coerceAtLeast(1)
        val besideLines = remember(photo.attribution, besidePx, density) {
            measurer.measure(photo.attribution, PoseitType.supporting, constraints = Constraints(maxWidth = besidePx)).lineCount
        }
        if (besideLines <= 2) {
            Row(verticalAlignment = Alignment.Bottom) {
                print()
                Spacer(Modifier.width(Space.s))
                PhotoCredit(photo, Modifier.weight(1f, fill = false))
            }
        } else {
            Column {
                print()
                PhotoCredit(photo)
            }
        }
    }
}

/** Width of the portrait print on the Today's pose card (a landscape one is a third wider). */
private val TodaysPhotoWidth = 60.dp

/** The credit line's info glyph and the gap after it. */
private val CreditGlyphRoom = 22.dp
