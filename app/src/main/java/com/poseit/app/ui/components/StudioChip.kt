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
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType

/**
 * A filter chip (docs/DESIGN.md): 32dp tall, Small (6dp) radius, 1dp Hairline outline with an Ink label; selected
 * chips are Ink-filled with a Sheet label. Label style, sentence case.
 *
 * Built on M3 FilterChip, so it keeps its semantics (a checkable node with a selected state) and its 48dp touch
 * target. Lay chips out 8dp apart.
 *
 * @param leadingIcon outlined 18dp icon. Only for the people count (a person outline); no emoji, no decoration.
 */
@Composable
fun StudioChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = StudioType.label, maxLines = 1) },
        modifier = modifier.heightIn(min = 32.dp),
        enabled = enabled,
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null, modifier = Modifier.size(StudioInlineIconSize)) } },
        shape = StudioShapes.Small,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = Studio.Ink,
            iconColor = Studio.Ink,
            selectedContainerColor = Studio.Ink,
            selectedLabelColor = Studio.Sheet,
            selectedLeadingIconColor = Studio.Sheet,
            disabledContainerColor = Color.Transparent,
            disabledLabelColor = Studio.Ink.copy(alpha = 0.38f),
            disabledLeadingIconColor = Studio.Ink.copy(alpha = 0.38f),
            disabledSelectedContainerColor = Studio.Ink.copy(alpha = 0.12f),
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = Studio.Hairline,
            selectedBorderColor = Color.Transparent,
            disabledBorderColor = Studio.Hairline,
            disabledSelectedBorderColor = Color.Transparent,
            borderWidth = 1.dp,
        ),
    )
}
