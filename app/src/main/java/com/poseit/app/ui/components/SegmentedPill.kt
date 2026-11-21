package com.poseit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.LocalSurfaceTone
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.SurfaceTone
import kotlinx.coroutines.launch

/*
 * The segmented pill (docs/DESIGN.md, "Segmented pill"; Astryx SegmentedControl): a dark pill track holding two to
 * four options in the tiny capitals; the selected option sits on a lavender pill with dark capitals, the others are
 * cream. It is the one place the accent marks a selection. The selection slides between options in 200ms.
 *
 * Every option fills the track's full height, so each one is a full 48dp touch target in the accessibility tree too;
 * the lavender pill is only drawn 4dp inside its option.
 */

/**
 * Heights of a [SegmentedPill]: [Medium] (48dp, a full touch target per option) in content and sheets, [Large]
 * (52dp) in the [FloatingPillBar].
 */
enum class SegmentedPillSize(internal val height: Dp) {
    Medium(48.dp),
    Large(52.dp),
}

/** Inset between the track's edge and the selected option's lavender pill (drawn; the options fill the track). */
private val TrackInset = 4.dp

/**
 * A segmented pill with equal-width options (two to five short ones). It hugs its widest option times the count, or
 * fills the width it is given (`Modifier.fillMaxWidth()` / `weight(1f)`).
 *
 * Semantics: a selectable group; each option is a selectable node with [role] and its selected state, and its
 * sentence-case label as text. An option whose entry in [optionRoles] is [Role.Button] is a plain button instead
 * (no selected state): use it for an option that leaves the screen rather than switching a view in place, like
 * Photos in the home screen's floating bar.
 *
 * @param options sentence-case labels ("Poses", "Photos"); shown in capitals
 * @param role [Role.Tab] when the options switch views (the floating bar), [Role.RadioButton] when they choose a
 *   value (a countdown of "Off", "3 s", "10 s")
 * @param size the track's height: [SegmentedPillSize.Medium] (48dp) or Large (52dp)
 * @param descriptions optional spoken form per option, for labels that don't read well aloud ("3 seconds" for
 *   "3 s"; Astryx SegmentedControlItem's label); null entries keep the visible label
 * @param optionRoles optional role per option, overriding [role]; [Role.Button] makes that option a button
 * @param onClickLabels optional action label per option ("Open your photos"), read as "double-tap to ..."
 * @param optionPadding side padding inside each option; lower it (to 6dp) so five short options fit a 360dp phone
 *   at 130% text
 */
@Composable
fun SegmentedPill(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: SegmentedPillSize = SegmentedPillSize.Medium,
    role: Role = Role.RadioButton,
    enabled: Boolean = true,
    descriptions: List<String?>? = null,
    optionRoles: List<Role?>? = null,
    onClickLabels: List<String?>? = null,
    optionPadding: Dp = 16.dp,
) {
    if (options.isEmpty()) return
    val track = if (LocalSurfaceTone.current == SurfaceTone.Dark) PoseitColors.backgroundCardRaised else PoseitColors.backgroundCard
    val position by animateFloatAsState(selectedIndex.coerceIn(0, options.lastIndex).toFloat(), PoseitMotion.standard(PoseitMotion.MEDIUM), label = "segmentPosition")
    Row(
        modifier
            .width(IntrinsicSize.Max)
            .height(size.height)
            .clip(PoseitShapes.Pill)
            .background(track)
            .drawBehind {
                if (selectedIndex in options.indices) {
                    val inset = TrackInset.toPx()
                    val w = this.size.width / options.size
                    val h = this.size.height - inset * 2
                    drawRoundRect(PoseitColors.accent, Offset(w * position + inset, inset), Size(w - inset * 2, h), CornerRadius(h / 2f))
                }
            }
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val color by animateColorAsState(
                if (selected) PoseitColors.onAccent else PoseitColors.textPrimaryInverted,
                PoseitMotion.standard(PoseitMotion.FAST),
                label = "segmentLabel",
            )
            val optionRole = optionRoles?.getOrNull(index) ?: role
            val clickLabel = onClickLabels?.getOrNull(index)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(PoseitShapes.Pill)
                    .then(
                        if (optionRole == Role.Button) {
                            Modifier.clickable(enabled = enabled, onClickLabel = clickLabel, role = Role.Button, onClick = { onSelect(index) })
                        } else {
                            Modifier.selectable(selected = selected, enabled = enabled, role = optionRole, onClick = { onSelect(index) })
                        },
                    )
                    .then(descriptions?.getOrNull(index)?.let { d -> Modifier.semantics { contentDescription = d } } ?: Modifier)
                    .padding(horizontal = optionPadding),
                contentAlignment = Alignment.Center,
            ) {
                CapsLabel(label, color = if (enabled) color else color.copy(alpha = 0.38f))
            }
        }
    }
}

/** Side padding of a tab's label inside its option (12dp to the lavender pill's edge). */
private val TabPadding = 16.dp

/** The padding tabs close up to (large text) before the row has to scroll. */
private val TightTabPadding = 8.dp

/**
 * Tabs as a segmented pill (docs/DESIGN.md, "Tabs"), for three to five views of one list (All / Favourites /
 * Recent / My poses). When the tabs fit, the track fills the width it is given and the spare space is shared out
 * between the tabs; when they don't (long labels, large text), the labels first close up, and only if that isn't
 * enough does each tab hug its label and the row scroll horizontally, keeping the selected tab in view. The lavender selection slides to the chosen tab.
 *
 * Each tab is a selectable node with [role] and its selected state, inside a selectable group, and fills the
 * 48dp track height. Also the shape for a value choice whose options differ a lot in length ("3", "5", "10",
 * "Until I stop"): pass [Role.RadioButton].
 *
 * @param edgePadding space before the first and after the last tab, so the pill lines up with content when the
 *   row scrolls edge to edge
 * @param descriptions optional spoken form per option ("3 seconds" for "3 s"); null entries keep the label
 */
@Composable
fun PillTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 0.dp,
    role: Role = Role.Tab,
    descriptions: List<String?>? = null,
) {
    // x and width (px) of each tab, measured on placement, so the selection can slide between them.
    val bounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val target = bounds[selectedIndex]
    val indicatorX = remember { Animatable(0f) }
    val indicatorW = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        val (x, w) = target ?: return@LaunchedEffect
        if (!placed) {
            indicatorX.snapTo(x)
            indicatorW.snapTo(w)
            placed = true
        } else {
            val spec = PoseitMotion.standard<Float>(PoseitMotion.MEDIUM)
            launch { indicatorX.animateTo(x, spec) }
            indicatorW.animateTo(w, spec)
        }
    }
    val track = if (LocalSurfaceTone.current == SurfaceTone.Dark) PoseitColors.backgroundCardRaised else PoseitColors.backgroundCard
    BoxWithConstraints(modifier) {
        val available = if (constraints.hasBoundedWidth) constraints.maxWidth - 2 * with(LocalDensity.current) { edgePadding.roundToPx() } else Int.MAX_VALUE
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            Layout(
                modifier = Modifier
                    .padding(horizontal = edgePadding)
                    .height(SegmentedPillSize.Medium.height)
                    .clip(PoseitShapes.Pill)
                    .background(track)
                    .drawBehind {
                        if (placed) {
                            val inset = TrackInset.toPx()
                            val h = size.height - inset * 2
                            drawRoundRect(
                                PoseitColors.accent,
                                Offset(indicatorX.value + inset, inset),
                                Size((indicatorW.value - inset * 2).coerceAtLeast(0f), h),
                                CornerRadius(h / 2f),
                            )
                        }
                    }
                    .selectableGroup(),
                content = {
                    labels.forEachIndexed { index, label ->
                        val selected = index == selectedIndex
                        val requester = remember { BringIntoViewRequester() }
                        LaunchedEffect(selected) { if (selected) requester.bringIntoView() }
                        val color by animateColorAsState(
                            if (selected) PoseitColors.onAccent else PoseitColors.textPrimaryInverted,
                            PoseitMotion.standard(PoseitMotion.FAST),
                            label = "tabLabel",
                        )
                        Box(
                            Modifier
                                .onPlaced { bounds[index] = it.positionInParent().x to it.size.width.toFloat() }
                                .bringIntoViewRequester(requester)
                                .clip(PoseitShapes.Pill)
                                .selectable(
                                    selected = selected,
                                    role = role,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = androidx.compose.material3.ripple(),
                                    onClick = { onSelect(index) },
                                )
                                .then(descriptions?.getOrNull(index)?.let { d -> Modifier.semantics { contentDescription = d } } ?: Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            CapsLabel(label, color = color)
                        }
                    }
                },
            ) { measurables, c ->
                val h = c.maxHeight
                val labels = measurables.map { it.maxIntrinsicWidth(h) }
                fun padded(side: Dp) = labels.map { it + 2 * side.roundToPx() }
                fun fill(widths: List<Int>): List<Int> {
                    // They fit: share the spare width out so the track spans the whole row.
                    val spare = available - widths.sum()
                    return widths.mapIndexed { i, w -> w + spare / widths.size + if (i < spare % widths.size) 1 else 0 }
                }
                val roomy = padded(TabPadding)
                val tight = padded(TightTabPadding)
                val widths = when {
                    available == Int.MAX_VALUE -> roomy
                    roomy.sum() <= available -> fill(roomy)
                    // Large text: closer labels keep the whole track in view before falling back to scrolling.
                    tight.sum() <= available -> fill(tight)
                    else -> roomy
                }
                val placeables = measurables.mapIndexed { i, m -> m.measure(Constraints.fixed(widths[i], h)) }
                layout(widths.sum(), h) {
                    var x = 0
                    placeables.forEach { p ->
                        p.place(x, 0)
                        x += p.width
                    }
                }
            }
        }
    }
}
