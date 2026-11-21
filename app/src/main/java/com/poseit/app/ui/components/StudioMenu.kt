package com.poseit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType

/**
 * The one Studio menu (docs/DESIGN.md): Sheet, Small radius and a Hairline edge instead of a shadow (Studio has no
 * elevation). The same on Paper and over the black camera. Fill it with [StudioMenuItem]s, and separate groups with
 * [StudioMenuDivider].
 */
@Composable
fun StudioMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = StudioShapes.Small,
        containerColor = Studio.Sheet,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, Studio.Hairline),
        content = content,
    )
}

/**
 * One item of a [StudioMenu]: Label large text in Ink (Miss when [destructive]), an optional 22dp outlined
 * [leadingIcon] and, for a choice, a check at the end when [selected].
 * @param selected null for a plain action; true or false for one option of a choice
 */
@Composable
fun StudioMenuItem(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    selected: Boolean? = null,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (destructive) Studio.Miss else Studio.Ink
    DropdownMenuItem(
        text = { Text(text, style = StudioType.labelLarge) },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null, modifier = Modifier.size(StudioIconSize)) } },
        trailingIcon = if (selected == true) {
            { Icon(Icons.Outlined.Check, contentDescription = "Selected", modifier = Modifier.size(StudioIconSize)) }
        } else {
            null
        },
        colors = MenuDefaults.itemColors(
            textColor = color,
            leadingIconColor = color,
            trailingIconColor = Studio.Ink,
        ),
    )
}

/** A Hairline between two groups of items in a [StudioMenu]. */
@Composable
fun StudioMenuDivider() = HairlineDivider(Modifier.padding(vertical = Space.xs))
