package com.poseit.app.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.LocalSurfaceTone
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.SurfaceTone

/**
 * A filter pill (docs/DESIGN.md, "Filter pills"): 40dp tall, pill-shaped, sentence-case Label text. Unselected: a
 * 1dp outline (dark on cream, mist on a dark card); selected: filled dark with a cream label (cream with a dark
 * label on a dark card). The lavender accent is NOT used here: it belongs to the one selected segment of a
 * segmented pill.
 *
 * Built on M3 FilterChip, so it keeps its semantics (a checkable node with a selected state) and its 48dp touch
 * target. Lay pills out 8dp apart.
 *
 * @param leadingIcon outlined 18dp icon. Only for the people count (a person outline); no emoji, no decoration.
 */
@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val dark = LocalSurfaceTone.current == SurfaceTone.Dark
    val ink = if (dark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary
    val paper = if (dark) PoseitColors.textPrimary else PoseitColors.textPrimaryInverted
    val edge = if (dark) PoseitColors.borderEmphasizedInverted else PoseitColors.borderEmphasized
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = PoseitType.label, maxLines = 1) },
        modifier = modifier.heightIn(min = PillHeight),
        enabled = enabled,
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null, modifier = Modifier.size(IconSize.inline)) } },
        shape = PoseitShapes.Pill,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = ink,
            iconColor = ink,
            selectedContainerColor = ink,
            selectedLabelColor = paper,
            selectedLeadingIconColor = paper,
            disabledContainerColor = Color.Transparent,
            disabledLabelColor = ink.copy(alpha = 0.38f),
            disabledLeadingIconColor = ink.copy(alpha = 0.38f),
            disabledSelectedContainerColor = ink.copy(alpha = 0.12f),
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = edge,
            selectedBorderColor = Color.Transparent,
            disabledBorderColor = edge.copy(alpha = 0.38f),
            disabledSelectedBorderColor = Color.Transparent,
            borderWidth = 1.dp,
        ),
    )
}

/** Visible height of a [FilterPill] or [TogglePill] (each still answers touches across 48dp). */
private val PillHeight = 40.dp

/**
 * A view option that is on or off, drawn as a [FilterPill] (outlined when off, filled when on) but announced as a
 * switch (Role.Switch with its on/off state), like the editor's Grid and Photo. Use FilterPill for filters, this for
 * settings that show or hide something.
 */
@Composable
fun TogglePill(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dark = LocalSurfaceTone.current == SurfaceTone.Dark
    val ink = if (dark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary
    val paper = if (dark) PoseitColors.textPrimary else PoseitColors.textPrimaryInverted
    val edge = if (dark) PoseitColors.borderEmphasizedInverted else PoseitColors.borderEmphasized
    val alpha = if (enabled) 1f else 0.38f
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = PillHeight)
            .clip(PoseitShapes.Pill)
            .then(
                if (checked) {
                    Modifier.background(ink.copy(alpha = alpha))
                } else {
                    Modifier.border(1.dp, edge.copy(alpha = alpha), PoseitShapes.Pill)
                },
            )
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = PoseitType.label, color = (if (checked) paper else ink).copy(alpha = alpha), maxLines = 1)
    }
}
