package com.poseit.app.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.LocalSurfaceTone
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.SurfaceTone

/*
 * Buttons (docs/DESIGN.md, "Buttons"; Astryx Button variants primary / secondary / ghost / destructive, plus Poseit's
 * accent). Pill-shaped, at least 48dp tall, labels in the tiny capitals (CapsLabel) except the text-only
 * TertiaryButton, which keeps sentence case. Labels say what happens, in sentence case in code ("Try this pose");
 * the capitals are display only.
 *
 * Colours follow the surface (LocalSurfaceTone): on cream a PrimaryButton is a dark pill, inside a dark card it is
 * a cream pill; SecondaryButton's outline is dark on cream and mist inside a dark card.
 *
 * One main action per screen: an AccentButton (lavender) when it is THE thing to do next ("Try this pose"), else a
 * PrimaryButton. Pair it with a SecondaryButton for the alternative, like the mockups' RESTART / PAUSE.
 *
 * Each button has a text form, `XButton(text, onClick, icon = ...)`, and a slot form, `XButton(onClick) { ... }`,
 * for anything else (a progress indicator). Full width with `Modifier.fillMaxWidth()`.
 */

/** The outline of a button: a pill on the canvas, a [Block] (12dp corners) for compact buttons inside cards. */
enum class ButtonForm(internal val shape: Shape) {
    Pill(PoseitShapes.Pill),

    /** Like the mockups' START: a rounded block inside a card. */
    Block(PoseitShapes.Control),
}

/** Minimum height of every button. */
val ButtonHeight: Dp = 48.dp

/** v1 name of [ButtonHeight]. */
@Deprecated("Use ButtonHeight", ReplaceWith("ButtonHeight"))
val StudioButtonHeight: Dp = ButtonHeight

private val ButtonPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
private val TextButtonPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
private const val DisabledAlpha = 0.38f

/** The main action: a dark pill on cream, a cream pill on a dark card. Capitals label. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    form: ButtonForm = ButtonForm.Pill,
) = PrimaryButton(onClick, modifier, enabled, form) { ButtonLabel(text, icon) }

/** [PrimaryButton] with a content slot. */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    form: ButtonForm = ButtonForm.Pill,
    content: @Composable RowScope.() -> Unit,
) {
    val dark = LocalSurfaceTone.current == SurfaceTone.Dark
    FilledButton(
        onClick, modifier, enabled, form,
        container = if (dark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary,
        content = if (dark) PoseitColors.textPrimary else PoseitColors.textPrimaryInverted,
        slot = content,
    )
}

/**
 * The accent call to action: lavender pill, dark capitals. The one thing to do next on a screen ("Try this pose",
 * "Get started"); never more than one per screen.
 */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    form: ButtonForm = ButtonForm.Pill,
) = AccentButton(onClick, modifier, enabled, form) { ButtonLabel(text, icon) }

/** [AccentButton] with a content slot. */
@Composable
fun AccentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    form: ButtonForm = ButtonForm.Pill,
    content: @Composable RowScope.() -> Unit,
) = FilledButton(onClick, modifier, enabled, form, container = PoseitColors.accent, content = PoseitColors.onAccent, slot = content)

/** The alternative: a 1dp outlined pill (dark outline on cream, mist outline on a dark card). Capitals label. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    form: ButtonForm = ButtonForm.Pill,
) = SecondaryButton(onClick, modifier, enabled, form) { ButtonLabel(text, icon) }

/** [SecondaryButton] with a content slot. */
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    form: ButtonForm = ButtonForm.Pill,
    content: @Composable RowScope.() -> Unit,
) {
    val dark = LocalSurfaceTone.current == SurfaceTone.Dark
    val fg = if (dark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary
    val edge = if (dark) PoseitColors.borderEmphasizedInverted else PoseitColors.borderEmphasized
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonHeight),
        enabled = enabled,
        shape = form.shape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = fg,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = fg.copy(alpha = DisabledAlpha),
        ),
        border = BorderStroke(1.dp, if (enabled) edge else edge.copy(alpha = DisabledAlpha)),
        contentPadding = ButtonPadding,
        content = content,
    )
}

/** Text only, sentence case (Astryx ghost): low-emphasis actions such as "Cancel", "Skip", "Show all". */
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) = TertiaryButton(onClick, modifier, enabled) { ButtonLabel(text, icon, caps = false) }

/** [TertiaryButton] with a content slot. */
@Composable
fun TertiaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val fg = if (LocalSurfaceTone.current == SurfaceTone.Dark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonHeight),
        enabled = enabled,
        shape = PoseitShapes.Pill,
        colors = textColors(fg),
        contentPadding = TextButtonPadding,
        content = content,
    )
}

/**
 * A destructive action: signal-red sentence-case text, or, when [filled], a red pill with cream capitals (the
 * confirming button of a "Delete pose?" dialog). Name the thing that goes: "Delete pose", "Delete photo".
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
    val dark = LocalSurfaceTone.current == SurfaceTone.Dark
    if (filled) {
        FilledButton(onClick, modifier, enabled, ButtonForm.Pill, container = PoseitColors.signalMiss, content = PoseitColors.onSignal) {
            ButtonLabel(text, icon)
        }
    } else {
        TextButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = ButtonHeight),
            enabled = enabled,
            shape = PoseitShapes.Pill,
            colors = textColors(if (dark) PoseitColors.signalMissBright else PoseitColors.signalMiss),
            contentPadding = TextButtonPadding,
        ) { ButtonLabel(text, icon, caps = false) }
    }
}

@Composable
private fun FilledButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    form: ButtonForm,
    container: Color,
    content: Color,
    slot: @Composable RowScope.() -> Unit,
) {
    val dark = LocalSurfaceTone.current == SurfaceTone.Dark
    val base = if (dark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonHeight),
        enabled = enabled,
        shape = form.shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = base.copy(alpha = 0.12f),
            disabledContentColor = base.copy(alpha = DisabledAlpha),
        ),
        elevation = null,
        contentPadding = ButtonPadding,
        content = slot,
    )
}

@Composable
private fun textColors(content: Color): ButtonColors = ButtonDefaults.textButtonColors(
    containerColor = Color.Transparent,
    contentColor = content,
    disabledContainerColor = Color.Transparent,
    disabledContentColor = content.copy(alpha = DisabledAlpha),
)

/** Optional 18dp leading icon, then the label on one line: capitals for pills, sentence case for text buttons. */
@Composable
private fun RowScope.ButtonLabel(text: String, icon: ImageVector?, caps: Boolean = true) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(IconSize.inline))
        Spacer(Modifier.width(8.dp))
    }
    if (caps) {
        CapsLabel(text)
    } else {
        Text(text, style = PoseitType.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
