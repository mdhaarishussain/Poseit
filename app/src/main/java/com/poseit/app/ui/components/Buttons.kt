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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.LocalSurfaceTone
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
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

/** Minimum height of every [ButtonSize.Medium] button. */
val ButtonHeight: Dp = 48.dp

/**
 * Button heights (Astryx Button size). [Medium] (48dp) everywhere by default; [Small] (40dp, tighter sides) only
 * where a button shares a row with a title, as Save does in the editor's top bar card. A Small button still answers
 * touches across 48dp: Compose extends the hit area of a target smaller than the minimum touch size.
 */
enum class ButtonSize(internal val height: Dp, internal val padding: PaddingValues) {
    Medium(ButtonHeight, PaddingValues(horizontal = 20.dp, vertical = 12.dp)),
    Small(40.dp, PaddingValues(horizontal = 16.dp, vertical = 8.dp)),
}

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
    size: ButtonSize = ButtonSize.Medium,
) = AccentButton(onClick, modifier, enabled, form, size) { ButtonLabel(text, icon) }

/** [AccentButton] with a content slot. */
@Composable
fun AccentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    form: ButtonForm = ButtonForm.Pill,
    size: ButtonSize = ButtonSize.Medium,
    content: @Composable RowScope.() -> Unit,
) = FilledButton(onClick, modifier, enabled, form, container = PoseitColors.accent, content = PoseitColors.onAccent, size = size, slot = content)

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
        contentPadding = ButtonSize.Medium.padding,
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
    size: ButtonSize = ButtonSize.Medium,
    slot: @Composable RowScope.() -> Unit,
) {
    val dark = LocalSurfaceTone.current == SurfaceTone.Dark
    val base = if (dark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = size.height),
        enabled = enabled,
        shape = form.shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = base.copy(alpha = 0.12f),
            disabledContentColor = base.copy(alpha = DisabledAlpha),
        ),
        elevation = null,
        contentPadding = size.padding,
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

/**
 * Optional 18dp leading icon, then the label: capitals for pills, sentence case for text buttons. A label wraps to a
 * second line rather than being cut when it can't fit (large text sizes on a small phone), centred.
 */
@Composable
private fun RowScope.ButtonLabel(text: String, icon: ImageVector?, caps: Boolean = true) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(IconSize.inline))
        Spacer(Modifier.width(8.dp))
    }
    if (caps) {
        CapsLabel(text, maxLines = 2, textAlign = TextAlign.Center, style = PoseitType.capsButton)
    } else {
        Text(text, style = PoseitType.label, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/**
 * Two buttons side by side when both fit on one line at their natural width, else stacked full width with [second]
 * on top (docs/DESIGN.md, "Buttons"): the mockups' RESTART / PAUSE pair that still works at 200% text. [first] is
 * the alternative (a SecondaryButton or "Cancel"), [second] the main action.
 *
 * @param fill side by side, [second] takes the rest of the width (a pinned action bar); false keeps both at their
 *   natural width, packed to the end (a dialog's buttons)
 */
@Composable
fun ButtonPair(
    first: (@Composable () -> Unit)?,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    gap: Dp = Space.edge,
) {
    Layout(contents = listOf(first ?: {}, second), modifier = modifier) { (firsts, seconds), c ->
        val f = firsts.firstOrNull()
        val sec = seconds.first()
        val gapPx = gap.roundToPx()
        val max = if (c.hasBoundedWidth) c.maxWidth else Int.MAX_VALUE
        val wf = f?.maxIntrinsicWidth(Constraints.Infinity) ?: 0
        val ws = sec.maxIntrinsicWidth(Constraints.Infinity)
        val needed = if (f == null) ws else wf + gapPx + ws
        if (needed <= max) {
            val width = if (fill && max != Int.MAX_VALUE) max else needed
            val pf = f?.measure(Constraints.fixedWidth(wf))
            val secondWidth = if (f == null) (if (fill) width else ws) else width - wf - gapPx
            val ps = sec.measure(Constraints.fixedWidth(secondWidth))
            val h = maxOf(pf?.height ?: 0, ps.height)
            layout(width, h) {
                val start = if (fill) 0 else width - needed
                pf?.place(start, (h - pf.height) / 2)
                ps.place(if (pf == null) width - ps.width else start + wf + gapPx, (h - ps.height) / 2)
            }
        } else {
            val ps = sec.measure(Constraints.fixedWidth(max))
            val pf = f?.measure(Constraints.fixedWidth(max))
            layout(max, ps.height + (pf?.let { it.height + gapPx } ?: 0)) {
                ps.place(0, 0)
                pf?.place(0, ps.height + gapPx)
            }
        }
    }
}
