package com.poseit.app.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
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
 * A filter pill (docs/DESIGN.md, "Filter pills"): 36dp tall, pill-shaped, sentence-case Label text. Unselected: a
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
        modifier = modifier.heightIn(min = 36.dp),
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
