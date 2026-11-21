package com.poseit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes

/*
 * Studio buttons (docs/DESIGN.md, Components). All are at least 48dp tall with the Medium (12dp) radius and the
 * Label large text style (Archivo Medium 14/20). Use one PrimaryButton per screen. Labels say what happens, in
 * sentence case, without arrows: "Try this pose", "Save pose", "Delete".
 *
 * Each button has two forms: `XButton(text, onClick, icon = ...)` for the usual label with an optional leading
 * outlined icon, and `XButton(onClick) { ... }` with a content slot for anything else (a progress indicator, say).
 * Make a button full width with `Modifier.fillMaxWidth()`.
 */

/** Minimum height of every Studio button. */
val StudioButtonHeight = 48.dp

private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
private val TextButtonPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)

/** Ink fill, Sheet label. The main action of a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) = PrimaryButton(onClick, modifier, enabled) { ButtonLabel(text, icon) }

/** Ink fill, Sheet label, with a content slot. */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = Button(
    onClick = onClick,
    modifier = modifier.heightIn(min = StudioButtonHeight),
    enabled = enabled,
    shape = StudioShapes.Medium,
    colors = filledColors(Studio.Ink),
    elevation = null,
    contentPadding = ButtonPadding,
    content = content,
)

/** 1dp Hairline outline, Ink label. Alternatives to the primary action, or the main action of a secondary area. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) = SecondaryButton(onClick, modifier, enabled) { ButtonLabel(text, icon) }

/** 1dp Hairline outline, Ink label, with a content slot. */
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = OutlinedButton(
    onClick = onClick,
    modifier = modifier.heightIn(min = StudioButtonHeight),
    enabled = enabled,
    shape = StudioShapes.Medium,
    colors = ButtonDefaults.outlinedButtonColors(
        containerColor = Color.Transparent,
        contentColor = Studio.Ink,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = Studio.Ink.copy(alpha = DisabledAlpha),
    ),
    border = BorderStroke(1.dp, Studio.Hairline),
    contentPadding = ButtonPadding,
    content = content,
)

/** Text only, Ink. Low-emphasis actions: "Cancel", "Skip", "Show all". */
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) = TertiaryButton(onClick, modifier, enabled) { ButtonLabel(text, icon) }

/** Text only, Ink, with a content slot. */
@Composable
fun TertiaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = TextButton(
    onClick = onClick,
    modifier = modifier.heightIn(min = StudioButtonHeight),
    enabled = enabled,
    shape = StudioShapes.Medium,
    colors = textColors(Studio.Ink),
    contentPadding = TextButtonPadding,
    content = content,
)

/**
 * A destructive action: Miss text, or a Miss fill with a Sheet label when [filled] (the confirming button of a
 * "Delete pose?" dialog). Name the thing that goes: "Delete pose", "Delete photo".
 */
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    icon: ImageVector? = null,
) {
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = StudioButtonHeight),
            enabled = enabled,
            shape = StudioShapes.Medium,
            colors = filledColors(Studio.Miss),
            elevation = null,
            contentPadding = ButtonPadding,
        ) { ButtonLabel(text, icon) }
    } else {
        TextButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = StudioButtonHeight),
            enabled = enabled,
            shape = StudioShapes.Medium,
            colors = textColors(Studio.Miss),
            contentPadding = TextButtonPadding,
        ) { ButtonLabel(text, icon) }
    }
}

private const val DisabledAlpha = 0.38f

@Composable
private fun filledColors(container: Color): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = container,
    contentColor = Studio.Sheet,
    disabledContainerColor = Studio.Ink.copy(alpha = 0.12f),
    disabledContentColor = Studio.Ink.copy(alpha = DisabledAlpha),
)

@Composable
private fun textColors(content: Color): ButtonColors = ButtonDefaults.textButtonColors(
    containerColor = Color.Transparent,
    contentColor = content,
    disabledContainerColor = Color.Transparent,
    disabledContentColor = Studio.Ink.copy(alpha = DisabledAlpha),
)

/**
 * An icon-only button: a 22dp Material Icons Outlined glyph in a 48dp touch target, Ink by default (Graphite for
 * an inactive control). Use a filled icon only for an active toggle, e.g. the filled heart of a favourite.
 * @param contentDescription what the button does, e.g. "Back", "Settings", "Add Hands on hips to favourites"
 */
@Composable
fun StudioIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Studio.Ink,
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = DisabledAlpha),
            modifier = Modifier.size(StudioIconSize),
        )
    }
}

/**
 * An icon-only toggle for a view option (the detail screen's Mirror): a 22dp outlined glyph in a 48dp target that
 * inverts to a 40dp Ink patch with a Sheet glyph while on, like the editor's tool toggles.
 * @param contentDescription what it toggles, e.g. "Mirror"; the on/off state is announced separately
 */
@Composable
fun StudioIconToggle(
    icon: ImageVector,
    contentDescription: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(if (checked) Studio.Ink else Color.Transparent, StudioShapes.Medium),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (checked) Studio.Sheet else Studio.Ink, modifier = Modifier.size(StudioIconSize))
        }
    }
}

/** Size of every standalone Studio icon. */
val StudioIconSize = 22.dp

/** Size of an icon set inline with button, chip or Label text. */
val StudioInlineIconSize = 18.dp

/**
 * The air between a [StudioIconButton]'s 48dp touch target and its 22dp glyph. Pad a bar by
 * `Space.gutter - IconGlyphInset` so the glyphs (not the targets) land on the 20dp gutter.
 */
val IconGlyphInset = (48.dp - StudioIconSize) / 2

/** Optional 18dp leading icon, then the label on one line. */
@Composable
private fun RowScope.ButtonLabel(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(StudioInlineIconSize))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
