package com.poseit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.toneColors

/**
 * One figure with its name (docs/DESIGN.md, "Stats"), like the mockups' "Best score / Expert mode / 7,593": the
 * [label] in Body large, an optional [caption] in Supporting, secondary, and the [value] in numerals below. Colours
 * follow the surface; usually inside a dark card. Read as one node ("Best match, 87%").
 *
 * @param valueStyle [PoseitType.numeralMedium] by default; [PoseitType.numeralLarge] for a headline figure
 * @param spacing the space above the value (pushes it to the card's foot when the card is taller)
 * @param minValueSize when set, a value too wide for the card steps down towards this size instead of being cut
 *   (a word such as "Advanced" in half a phone's width at large text sizes)
 */
@Composable
fun Stat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    valueStyle: TextStyle = PoseitType.numeralMedium,
    spacing: Dp = Space.l,
    minValueSize: TextUnit = TextUnit.Unspecified,
) {
    val colors = toneColors()
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = PoseitType.bodyLarge, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (caption != null) {
            Text(caption, style = PoseitType.supporting, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(spacing))
        if (minValueSize.isSpecified) {
            BasicText(
                value,
                style = valueStyle.copy(color = colors.textPrimary),
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = minValueSize, maxFontSize = valueStyle.fontSize, stepSize = 1.sp),
            )
        } else {
            Text(value, style = valueStyle, color = colors.textPrimary, maxLines = 1)
        }
    }
}

/**
 * Two or three [Stat]s side by side in one card (the mockups' "Accuracy / Score / Speed"): equal columns, labels
 * in Supporting and values in numerals. For two stats in two fused cards, use a [ConnectedCardRow] of [Stat]s.
 */
@Composable
fun StatRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.l), content = content)
}

/** A compact [Stat] for a [StatRow]: Supporting label above a 24sp numeral, no caption. */
@Composable
fun RowScope.CompactStat(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = toneColors()
    Column(modifier.weight(1f).semantics(mergeDescendants = true) {}) {
        Text(label, style = PoseitType.supporting, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = PoseitType.numeral(PoseitType.numeralMedium.fontSize * 0.92f, androidx.compose.ui.text.font.FontWeight.Normal), color = colors.textPrimary, maxLines = 1)
    }
}
