package com.poseit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
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
 */

/** Heights of a [SegmentedPill]: [Medium] (40dp) in content, [Large] (52dp) in the [FloatingPillBar]. */
enum class SegmentedPillSize(internal val height: Dp) {
    Medium(40.dp),
    Large(52.dp),
}

/** Inset between the track and the selected option's pill. */
private val TrackInset = 4.dp

/**
 * A segmented pill with equal-width options (two or three; four if the words are short). It hugs its widest option
 * times the count, or fills the width it is given (`Modifier.fillMaxWidth()` / `weight(1f)`).
 *
 * Semantics: a selectable group; each option is a selectable node with [role] and its selected state, and its
 * sentence-case label as text.
 *
 * @param options sentence-case labels ("Poses", "Photos"); shown in capitals
 * @param role [Role.Tab] when the options switch views (the floating bar), [Role.RadioButton] when they choose a
 *   value (a countdown of "Off", "3 s", "10 s")
 * @param size the track's height: [SegmentedPillSize.Medium] (40dp) or Large (52dp)
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
            .padding(TrackInset)
            .drawBehind {
                if (selectedIndex in options.indices) {
                    val w = this.size.width / options.size
                    drawRoundRect(PoseitColors.accent, Offset(w * position, 0f), Size(w, this.size.height), CornerRadius(this.size.height / 2f))
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
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(PoseitShapes.Pill)
                    .selectable(selected = selected, enabled = enabled, role = role, onClick = { onSelect(index) })
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CapsLabel(label, color = if (enabled) color else color.copy(alpha = 0.38f))
            }
        }
    }
}

/**
 * Tabs as a segmented pill (docs/DESIGN.md, "Tabs"): options hug their labels, the row scrolls horizontally when
 * they don't fit (the selected tab is scrolled into view), and the lavender selection slides to the chosen tab.
 * For three to five views of one list (All / Favourites / Recent / My poses).
 *
 * Each tab is a selectable node with [Role.Tab] and its selected state, inside a selectable group.
 *
 * @param edgePadding space before the first and after the last tab, so the pill lines up with content when the
 *   row scrolls edge to edge
 */
@Composable
fun PillTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 0.dp,
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
    Box(modifier.horizontalScroll(rememberScrollState())) {
        Row(
            Modifier
                .padding(horizontal = edgePadding)
                .height(SegmentedPillSize.Medium.height)
                .clip(PoseitShapes.Pill)
                .background(track)
                .padding(TrackInset)
                .drawBehind {
                    if (placed) {
                        drawRoundRect(
                            PoseitColors.accent,
                            Offset(indicatorX.value, 0f),
                            Size(indicatorW.value, size.height),
                            CornerRadius(size.height / 2f),
                        )
                    }
                }
                .selectableGroup(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                        .fillMaxHeight()
                        .onPlaced { bounds[index] = it.positionInParent().x to it.size.width.toFloat() }
                        .bringIntoViewRequester(requester)
                        .clip(PoseitShapes.Pill)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = androidx.compose.material3.ripple(),
                            onClick = { onSelect(index) },
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CapsLabel(label, color = color)
                }
            }
        }
    }
}
