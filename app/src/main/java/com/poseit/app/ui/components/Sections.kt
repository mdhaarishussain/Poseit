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
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioType

/**
 * The heading that starts a group or section: the heading style (Archivo SemiBold 17/24, normal width, so the wide
 * cut stays with the screen title), Ink, sentence case, marked as a heading for accessibility. Leave [Space.section][com.poseit.app.ui.theme.Space.section] (32dp) above it between
 * groups. No eyebrow labels above it, no caps.
 *
 * @param action optional trailing control on the same line, e.g. a TertiaryButton("Show all")
 */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = StudioType.heading,
            color = Studio.Ink,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (action != null) Box { action() }
    }
}

/**
 * A 1dp Hairline rule. Only between rows of the same group (setting rows, list items); never as decoration between
 * sections, which are separated by space. Inset it with padding to line up with the row text.
 */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 1.dp, color = Studio.Hairline)
}

/** A vertical 1dp Hairline rule, e.g. between two groups of chips in one row. Give it a height. */
@Composable
fun VerticalHairline(modifier: Modifier = Modifier) {
    VerticalDivider(modifier, thickness = 1.dp, color = Studio.Hairline)
}
