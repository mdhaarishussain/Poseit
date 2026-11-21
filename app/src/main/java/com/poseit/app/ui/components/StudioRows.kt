package com.poseit.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioType

/*
 * Rows for settings and lists (docs/DESIGN.md): title in Body strong, subtitle in Label Graphite, the control at
 * the end, at least 56dp tall. No icon tiles. Separate rows of one group with HairlineDivider; start each group
 * with a SectionHeading and leave 32dp between groups.
 */

/** Default padding of a row: the 20dp screen gutter left and right. */
val RowPadding = PaddingValues(horizontal = Space.gutter, vertical = Space.m)

/**
 * A setting or list row.
 * @param onClick makes the whole row a button (Role.Button); null for a row whose [trailing] control does the work
 * @param trailing the control at the end: a value label, a Switch, a chevron, a SecondaryButton...
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

/** A row that toggles [checked] when tapped anywhere (one Role.Switch node), with a Studio switch at the end. */
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
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, colors = studioSwitchColors())
    }
}

/**
 * A title with its current value on the right ([valueLabel], also the slider's state description) and a
 * [StudioSlider] underneath.
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
) {
    Column(modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowText(title, subtitle, enabled, Modifier.weight(1f))
            Spacer(Modifier.width(Space.l))
            Text(valueLabel, style = StudioType.label, color = if (enabled) Studio.Ink else Studio.Ink.copy(alpha = 0.38f))
        }
        StudioSlider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.semantics { stateDescription = valueLabel },
        )
    }
}

/** Checked: Ink track, Sheet thumb. Unchecked: Paper track with a Graphite outline and a Graphite thumb. */
@Composable
fun studioSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Studio.Sheet,
    checkedTrackColor = Studio.Ink,
    checkedBorderColor = Studio.Ink,
    checkedIconColor = Studio.Ink,
    uncheckedThumbColor = Studio.Graphite,
    uncheckedTrackColor = Studio.Paper,
    uncheckedBorderColor = Studio.Graphite,
    uncheckedIconColor = Studio.Paper,
    disabledCheckedThumbColor = Studio.Sheet,
    disabledCheckedTrackColor = Studio.Ink.copy(alpha = 0.38f),
    disabledCheckedBorderColor = Color.Transparent,
    disabledUncheckedThumbColor = Studio.Graphite.copy(alpha = 0.38f),
    disabledUncheckedTrackColor = Studio.Paper,
    disabledUncheckedBorderColor = Studio.Graphite.copy(alpha = 0.38f),
)

/**
 * Ink thumb and active track, Hairline inactive track, for a stock M3 Slider (whose default inactive track is Ink
 * in this theme). Prefer [StudioSlider], which also draws the thin Studio track.
 */
@Composable
fun studioSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = Studio.Ink,
    activeTrackColor = Studio.Ink,
    activeTickColor = Studio.Sheet,
    inactiveTrackColor = Studio.Hairline,
    inactiveTickColor = Studio.Ink.copy(alpha = 0.38f),
    disabledThumbColor = Studio.Ink.copy(alpha = 0.38f),
    disabledActiveTrackColor = Studio.Ink.copy(alpha = 0.38f),
    disabledInactiveTrackColor = Studio.Hairline,
)

@Composable
private fun RowText(title: String, subtitle: String?, enabled: Boolean, modifier: Modifier) {
    val alpha = if (enabled) 1f else 0.38f
    Column(modifier) {
        Text(title, style = StudioType.bodyStrong, color = Studio.Ink.copy(alpha = alpha))
        if (subtitle != null) {
            Text(subtitle, style = StudioType.label, color = Studio.Graphite.copy(alpha = alpha))
        }
    }
}
