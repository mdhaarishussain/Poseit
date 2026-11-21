package com.poseit.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.toneColors

/**
 * The heading that starts a group or section (Astryx Section heading): the Heading style (Medium 16/22), primary
 * colour for the surface, sentence case, marked as a heading for accessibility. On the canvas, start it on
 * [Space.textInset][com.poseit.app.ui.theme.Space.textInset] so it lines up with text inside cards, and leave
 * [Space.section][com.poseit.app.ui.theme.Space.section] above it. No eyebrow labels, no capitals.
 *
 * @param action optional trailing control on the same line, e.g. a TertiaryButton("Show all")
 */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = PoseitType.heading,
            color = toneColors().textPrimary,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (action != null) Box { action() }
    }
}

/**
 * A 1dp hairline in the surface's border colour. Only between rows of one group (RowGroup draws its own) or list
 * items; never as decoration between sections, which are separated by space.
 */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 1.dp, color = toneColors().border)
}

/** A vertical 1dp hairline, e.g. between two groups of filter pills in one row. Give it a height. */
@Composable
fun VerticalHairline(modifier: Modifier = Modifier) {
    VerticalDivider(modifier, thickness = 1.dp, color = toneColors().border)
}
