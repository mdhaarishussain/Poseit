package com.poseit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.Space

/** Diameter of the circles in a [FloatingPillBar], and the height of its segmented pill. */
val PillBarItemSize: Dp = 52.dp

/** Insets the floating pill bar pads itself by: the navigation bar and any side system bars. */
val PillBarInsets: WindowInsets
    @Composable get() = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)

/**
 * The floating bottom bar (docs/DESIGN.md, "Floating pill bar"; Astryx MobileNav's role on a phone): separate
 * pills on a canvas band at the bottom of the screen, [Space.edge] from the sides and above the navigation bar.
 * The usual content is the mockups' three pieces: a [CircleIconButton] (size [PillBarItemSize]), a [SegmentedPill]
 * (size Large, role Tab, `Modifier.weight(1f)`) and another [CircleIconButton]. Use [PillBarSegments] for the
 * middle piece.
 *
 * Put it at the bottom of a Box or in a Scaffold's bottomBar; content scrolls under the band, never through it.
 *
 * @param containerColor the band; the canvas by default, transparent over a full-bleed view
 */
@Composable
fun FloatingPillBar(
    modifier: Modifier = Modifier,
    containerColor: Color = PoseitColors.backgroundBody,
    windowInsets: WindowInsets = PillBarInsets,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(containerColor)
            .windowInsetsPadding(windowInsets)
            .padding(horizontal = Space.edge, vertical = Space.edge),
        horizontalArrangement = Arrangement.spacedBy(Space.edge),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * The middle piece of a [FloatingPillBar]: a Large [SegmentedPill] of views (Role.Tab) that takes the remaining
 * width, e.g. "Poses" / "Photos".
 */
@Composable
fun RowScope.PillBarSegments(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = SegmentedPill(
    options = options,
    selectedIndex = selectedIndex,
    onSelect = onSelect,
    modifier = modifier.weight(1f),
    size = SegmentedPillSize.Large,
    role = Role.Tab,
)
