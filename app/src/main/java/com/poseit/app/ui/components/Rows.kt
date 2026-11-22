package com.poseit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone
import com.poseit.app.ui.theme.toneColors

/*
 * Rows and lists (docs/DESIGN.md, "Rows"; Astryx Item / List). Two containers on the canvas:
 * - OutlinedRow: one cream row card with a 1dp dark outline and a chevron, standing alone (the mockups'
 *   "Rhythm match >"). Stack several 8dp apart for a short menu of places to go.
 * - RowGroup: one outlined cream card holding several rows (SettingRow, SwitchRow, SliderRow, RadioRow) separated
 *   by hairlines, for settings and option lists.
 * Rows are at least 56dp tall; the title is Body strong, the subtitle Supporting in the secondary colour, the
 * control at the end. No icon tiles.
 */

/** Default padding of a row inside a [RowGroup]: 24dp sides, so row text lines up with card text. */
val RowPadding = PaddingValues(horizontal = Space.cardPadding, vertical = Space.m)

/**
 * An outlined cream card holding rows, with hairline dividers between them (pass [divided] = false for none). Each
 * direct child is one row. For a group of settings: put a [SectionHeading] above it on [Space.textInset].
 */
@Composable
fun RowGroup(
    modifier: Modifier = Modifier,
    divided: Boolean = true,
    content: @Composable () -> Unit,
) {
    ProvideSurfaceTone(SurfaceTone.Light) {
        var lines by remember { mutableStateOf(emptyList<Int>()) }
        val color = PoseitColors.border
        Layout(
            content = content,
            modifier = modifier
                .fillMaxWidth()
                .clip(PoseitShapes.Row)
                .background(PoseitColors.backgroundSurface)
                .border(1.dp, PoseitColors.borderEmphasized, PoseitShapes.Row)
                .drawBehind {
                    val inset = Space.cardPadding.toPx()
                    val h = 1.dp.toPx()
                    lines.forEach { y -> drawRect(color, Offset(inset, y.toFloat()), Size(size.width - 2 * inset, h)) }
                },
        ) { measurables, constraints ->
            val line = if (divided) 1.dp.roundToPx() else 0
            val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
            val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeables.maxOfOrNull { it.width } ?: 0
            val height = placeables.sumOf { it.height } + line * (placeables.size - 1).coerceAtLeast(0)
            layout(width, height.coerceAtLeast(constraints.minHeight)) {
                var y = 0
                val ys = ArrayList<Int>()
                placeables.forEachIndexed { i, p ->
                    p.place(0, y)
                    y += p.height
                    if (i < placeables.lastIndex) {
                        if (divided) ys += y
                        y += line
                    }
                }
                if (ys != lines) lines = ys
            }
        }
    }
}

/**
 * A single outlined row card that opens something: cream, 1dp dark outline, 16dp corners, the title in Body large,
 * an optional subtitle and a chevron at the end (or your own [trailing]). The whole card is one button.
 */
@Composable
fun OutlinedRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    trailing: @Composable () -> Unit = { RowChevron() },
) {
    ProvideSurfaceTone(SurfaceTone.Light) {
        Row(
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(PoseitShapes.Row)
                .background(PoseitColors.backgroundSurface)
                .border(1.dp, PoseitColors.borderEmphasized, PoseitShapes.Row)
                .clickable(enabled = enabled, onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                .padding(start = Space.cardPadding, end = Space.l, top = Space.m, bottom = Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                val alpha = if (enabled) 1f else 0.38f
                Text(title, style = PoseitType.bodyLarge, color = PoseitColors.textPrimary.copy(alpha = alpha))
                if (subtitle != null) Text(subtitle, style = PoseitType.supporting, color = PoseitColors.textSecondary.copy(alpha = alpha))
            }
            Spacer(Modifier.width(Space.l))
            trailing()
        }
    }
}

/** The chevron at the end of a row that opens something. Decorative (the row itself is the button). */
@Composable
fun RowChevron(modifier: Modifier = Modifier) {
    Icon(PoseitIcons.chevronRight, contentDescription = null, tint = toneColors().textSecondary, modifier = modifier.size(IconSize.standalone))
}

/**
 * A setting or list row, usually inside a [RowGroup].
 * @param onClick makes the whole row a button (Role.Button); null for a row whose [trailing] control does the work
 * @param trailing the control at the end: a value label, a Switch, a [RowChevron], a SecondaryButton...
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = RowPadding,
    trailing: (@Composable () -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(clickModifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(title, subtitle, enabled, Modifier.weight(1f))
        if (trailing != null) {
            Spacer(Modifier.width(Space.l))
            trailing()
        }
    }
}

/** A row that toggles [checked] when tapped anywhere (one Role.Switch node), with a switch at the end. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = RowPadding,
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        contentPadding = contentPadding,
        modifier = modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, colors = poseitSwitchColors())
    }
}

/**
 * A title with its current value on the right ([valueLabel], on the title's baseline, also the slider's state
 * description) and a [ValueSlider] underneath.
 * @param sliderContentDescription the slider's name for screen readers; the [title] when null
 * @param above optional content between the title and the slider, e.g. a SegmentedPill of presets
 */
@Composable
fun SliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    sliderContentDescription: String? = null,
    above: (@Composable () -> Unit)? = null,
) {
    val colors = toneColors()
    Column(modifier.fillMaxWidth().padding(RowPadding)) {
        Row {
            RowText(title, subtitle, enabled, Modifier.weight(1f).alignByBaseline())
            Spacer(Modifier.width(Space.l))
            Text(
                valueLabel,
                style = PoseitType.numeral(PoseitType.label.fontSize),
                color = if (enabled) colors.textPrimary else colors.textDisabled,
                modifier = Modifier.alignByBaseline(),
            )
        }
        if (above != null) {
            Spacer(Modifier.height(Space.s))
            above()
        }
        ValueSlider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = sliderContentDescription ?: title
                stateDescription = valueLabel
            },
        )
    }
}

/**
 * One option of a single-choice group (put the options in a `Modifier.selectableGroup()` column): the whole row
 * selects it and the radio sits at the end like other row controls.
 */
@Composable
fun RadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    contentPadding: PaddingValues = RowPadding,
) {
    val colors = toneColors()
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(title, subtitle, enabled = true, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(Space.l))
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = colors.textPrimary, unselectedColor = colors.textSecondary),
        )
    }
}

/**
 * Switch colours. On: dark track with a lavender thumb (the accent marks "on"). Off: pale track with a dark
 * outline and a slate thumb.
 */
@Composable
fun poseitSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = PoseitColors.accent,
    checkedTrackColor = PoseitColors.textPrimary,
    checkedBorderColor = PoseitColors.textPrimary,
    checkedIconColor = PoseitColors.onAccent,
    uncheckedThumbColor = PoseitColors.textSecondary,
    uncheckedTrackColor = PoseitColors.backgroundMuted,
    uncheckedBorderColor = PoseitColors.textSecondary,
    uncheckedIconColor = PoseitColors.backgroundMuted,
    disabledCheckedThumbColor = PoseitColors.accent.copy(alpha = 0.6f),
    disabledCheckedTrackColor = PoseitColors.textPrimary.copy(alpha = 0.38f),
    disabledCheckedBorderColor = Color.Transparent,
    disabledUncheckedThumbColor = PoseitColors.textSecondary.copy(alpha = 0.38f),
    disabledUncheckedTrackColor = PoseitColors.backgroundMuted,
    disabledUncheckedBorderColor = PoseitColors.textSecondary.copy(alpha = 0.38f),
)

/**
 * Colours for a stock M3 Slider (whose default inactive track is lavender in this theme). Prefer [ValueSlider],
 * which also draws the v2 track and thumb.
 */
@Composable
fun poseitSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = PoseitColors.accent,
    activeTrackColor = PoseitColors.textPrimary,
    activeTickColor = PoseitColors.textPrimaryInverted,
    inactiveTrackColor = PoseitColors.track,
    inactiveTickColor = PoseitColors.textPrimary.copy(alpha = 0.38f),
    disabledThumbColor = PoseitColors.textPrimary.copy(alpha = 0.38f),
    disabledActiveTrackColor = PoseitColors.textPrimary.copy(alpha = 0.38f),
    disabledInactiveTrackColor = PoseitColors.track,
)

@Composable
private fun RowText(title: String, subtitle: String?, enabled: Boolean, modifier: Modifier) {
    val colors = toneColors()
    Column(modifier) {
        Text(title, style = PoseitType.bodyStrong, color = if (enabled) colors.textPrimary else colors.textDisabled)
        if (subtitle != null) {
            Text(
                subtitle,
                style = PoseitType.supporting,
                color = if (enabled) colors.textSecondary else colors.textDisabled,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
