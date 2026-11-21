package com.poseit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.indication
import androidx.compose.runtime.remember
import com.poseit.app.ui.theme.LocalSurfaceTone
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.SurfaceTone

/*
 * Icon-only controls (docs/DESIGN.md, "Icon buttons"; Astryx IconButton). Every one has a content description that
 * says what it does ("Settings", "Pose camera", "Add Hand on hip to favourites") and a 48dp touch target.
 */

/** The fill of a [CircleIconButton]. */
enum class CircleVariant {
    /** Dark disc, cream glyph: the floating pill bar's circles, round actions on cream. */
    Dark,

    /** Cream disc, dark glyph: a round action inside a dark card. */
    Light,

    /** Lavender disc, dark glyph: a round main action (use sparingly; it competes with an AccentButton). */
    Accent,

    /** 1dp outline, no fill: a quiet round action on cream. */
    Outlined,

    /** Black at 45% with a cream glyph: camera controls over the live view. */
    Camera,
}

/**
 * A round icon button (the mockups' bottom-bar circles). [size] is the disc's diameter: 48dp in content, [PillBarItemSize]
 * (52dp) in the [FloatingPillBar]; the touch target is never smaller than 48dp.
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CircleVariant = CircleVariant.Dark,
    size: Dp = 48.dp,
    enabled: Boolean = true,
) {
    val (fill, glyph) = when (variant) {
        CircleVariant.Dark -> PoseitColors.backgroundCard to PoseitColors.textPrimaryInverted
        CircleVariant.Light -> PoseitColors.backgroundSurface to PoseitColors.textPrimary
        CircleVariant.Accent -> PoseitColors.accent to PoseitColors.onAccent
        CircleVariant.Outlined -> Color.Transparent to PoseitColors.textPrimary
        CircleVariant.Camera -> PoseitColors.cameraControlScrim to PoseitColors.cameraContent
    }
    Box(
        modifier
            .size(maxOf(size, 48.dp))
            .clip(PoseitShapes.Circle)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .background(fill, PoseitShapes.Circle)
                .then(if (variant == CircleVariant.Outlined) Modifier.border(1.dp, PoseitColors.borderEmphasized, PoseitShapes.Circle) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) glyph else glyph.copy(alpha = 0.38f),
                modifier = Modifier.size(IconSize.standalone),
            )
        }
    }
}

/**
 * A round toggle (camera flash, grid, mirror over the live view; a view option on a dark card): the
 * [CircleVariant.Camera] disc while off, inverting to a cream disc with a dark glyph while on.
 * @param contentDescription what it toggles ("Grid"); the on/off state is announced separately (Role.Switch)
 */
@Composable
fun CircleIconToggle(
    icon: ImageVector,
    contentDescription: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    enabled: Boolean = true,
) {
    val fill by animateColorAsState(
        if (checked) PoseitColors.cameraControlActive else PoseitColors.cameraControlScrim,
        PoseitMotion.standard(PoseitMotion.FAST),
        label = "circleToggleFill",
    )
    val glyph = if (checked) PoseitColors.textPrimary else PoseitColors.cameraContent
    Box(
        modifier
            .size(maxOf(size, 48.dp))
            .clip(PoseitShapes.Circle)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size).background(fill, PoseitShapes.Circle), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (enabled) glyph else glyph.copy(alpha = 0.38f), modifier = Modifier.size(IconSize.standalone))
        }
    }
}

/**
 * A bare glyph in a 48dp touch target (top bar actions, the favourite heart on a tile, row ends). The tint follows
 * the surface: dark on cream, cream on a dark card.
 * @param contentDescription what the button does, e.g. "Back", "Settings", "Add Hand on hip to favourites"
 */
@Composable
fun PlainIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = if (LocalSurfaceTone.current == SurfaceTone.Dark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary,
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(IconSize.standalone),
        )
    }
}

/**
 * An icon toggle for a view option (the detail screen's Mirror, the editor's tools): a bare glyph while off,
 * a 40dp disc while on (dark disc with a cream glyph on cream; lavender disc with a dark glyph on a dark card).
 * @param contentDescription what it toggles, e.g. "Mirror"; the on/off state is announced separately
 */
@Composable
fun IconToggle(
    icon: ImageVector,
    contentDescription: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dark = LocalSurfaceTone.current == SurfaceTone.Dark
    val onFill = if (dark) PoseitColors.accent else PoseitColors.textPrimary
    val onGlyph = if (dark) PoseitColors.onAccent else PoseitColors.textPrimaryInverted
    val offGlyph = if (dark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary
    val fill by animateColorAsState(if (checked) onFill else Color.Transparent, PoseitMotion.standard(PoseitMotion.FAST), label = "iconToggleFill")
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, interactionSource = source, indication = null, onValueChange = onCheckedChange)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(PoseitShapes.Circle)
                .background(fill)
                .indication(source, ripple()),
            contentAlignment = Alignment.Center,
        ) {
            val glyph = if (checked) onGlyph else offGlyph
            Icon(icon, contentDescription = null, tint = if (enabled) glyph else glyph.copy(alpha = 0.38f), modifier = Modifier.size(IconSize.standalone))
        }
    }
}

/** v1 name of [PlainIconButton]. */
@Deprecated("Use PlainIconButton (or CircleIconButton)", ReplaceWith("PlainIconButton(icon, contentDescription, onClick, modifier, enabled, tint)"))
@Composable
fun StudioIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = if (LocalSurfaceTone.current == SurfaceTone.Dark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary,
) = PlainIconButton(icon, contentDescription, onClick, modifier, enabled, tint)

/** v1 name of [IconToggle]. */
@Deprecated("Use IconToggle", ReplaceWith("IconToggle(icon, contentDescription, checked, onCheckedChange, modifier)"))
@Composable
fun StudioIconToggle(
    icon: ImageVector,
    contentDescription: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) = IconToggle(icon, contentDescription, checked, onCheckedChange, modifier)
