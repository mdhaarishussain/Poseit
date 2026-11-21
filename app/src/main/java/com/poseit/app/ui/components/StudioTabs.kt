package com.poseit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioType
import kotlinx.coroutines.launch

/**
 * Text tabs (docs/DESIGN.md): the selected tab is Ink with a 2dp Ink underline, the others Graphite, all on a
 * Hairline track (inset to [edgePadding] while the tabs fit, edge to edge when the row scrolls). No pill backgrounds. Tabs sit left-aligned, 24dp apart, and the row scrolls
 * horizontally when they don't fit (the selected tab is scrolled into view).
 *
 * Each tab is a selectable node with [Role.Tab] and its selected state, inside a selectable group.
 *
 * @param labels tab labels, sentence case
 * @param edgePadding space before the first and after the last tab; pass [Space.gutter] for an edge-to-edge row
 *   whose first tab lines up with the screen content
 */
@Composable
fun StudioTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 0.dp,
) {
    // x and width (px) of each tab, measured on placement, so the underline can slide between them.
    val bounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val target = bounds[selectedIndex]
    val indicatorX = remember { Animatable(0f) }
    val indicatorW = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        val (x, w) = target ?: return@LaunchedEffect
        if (!placed) {
            // First layout: put the underline in place without sliding in from the left.
            indicatorX.snapTo(x)
            indicatorW.snapTo(w)
            placed = true
        } else {
            val spec = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)
            launch { indicatorX.animateTo(x, spec) }
            indicatorW.animateTo(w, spec)
        }
    }

    val scroll = rememberScrollState()
    Box(
        modifier
            .fillMaxWidth()
            .drawBehind {
                // The track stops at the edge padding (the gutters) while all tabs fit, and runs edge to edge once
                // the row scrolls, so it doesn't end in mid-air under a cut-off tab.
                val track = 1.dp.toPx()
                val inset = if (scroll.maxValue == 0) edgePadding.toPx() else 0f
                drawRect(Studio.Hairline, Offset(inset, size.height - track), Size(size.width - 2 * inset, track))
            },
    ) {
        Row(
            Modifier
                .horizontalScroll(scroll)
                .padding(horizontal = edgePadding)
                .selectableGroup()
                .drawBehind {
                    if (placed) {
                        val line = 2.dp.toPx()
                        drawRect(Studio.Ink, Offset(indicatorX.value, size.height - line), Size(indicatorW.value, line))
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(Space.xl),
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val requester = remember { BringIntoViewRequester() }
                LaunchedEffect(selected) { if (selected) requester.bringIntoView() }
                val color by animateColorAsState(
                    if (selected) Studio.Ink else Studio.Graphite,
                    tween(durationMillis = 200),
                    label = "tabColor",
                )
                Box(
                    Modifier
                        .height(48.dp)
                        .onPlaced { bounds[index] = it.positionInParent().x to it.size.width.toFloat() }
                        .bringIntoViewRequester(requester)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = StudioType.labelLarge, color = color, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
