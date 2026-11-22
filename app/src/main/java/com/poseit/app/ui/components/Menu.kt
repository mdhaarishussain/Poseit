package com.poseit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone

/**
 * The one menu (docs/DESIGN.md, "Menus"; Astryx DropdownMenu): cream surface, 16dp corners and a hairline edge
 * instead of a shadow. The same on the canvas and over the camera. Fill it with [PoseitMenuItem]s; separate
 * groups with [PoseitMenuDivider].
 */
@Composable
fun PoseitMenu(
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
        shape = PoseitShapes.Row,
        containerColor = PoseitColors.backgroundSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, PoseitColors.border),
    ) {
        ProvideSurfaceTone(SurfaceTone.Light) { content() }
    }
}

/**
 * One item of a [PoseitMenu]: Label text in ink (signal red when [destructive]), an optional 24dp outlined
 * [leadingIcon] and, for a choice, a check at the end when [selected].
 * @param selected null for a plain action; true or false for one option of a choice
 */
@Composable
fun PoseitMenuItem(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    selected: Boolean? = null,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (destructive) PoseitColors.signalMiss else PoseitColors.textPrimary
    DropdownMenuItem(
        text = { Text(text, style = PoseitType.label) },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null, modifier = Modifier.size(IconSize.standalone)) } },
        trailingIcon = if (selected == true) {
            { Icon(PoseitIcons.check, contentDescription = "Selected", modifier = Modifier.size(IconSize.standalone)) }
        } else {
            null
        },
        colors = MenuDefaults.itemColors(
            textColor = color,
            leadingIconColor = color,
            trailingIconColor = PoseitColors.textPrimary,
        ),
    )
}

/** A hairline between two groups of items in a [PoseitMenu]. */
@Composable
fun PoseitMenuDivider() = HairlineDivider(Modifier.padding(vertical = Space.xs))
