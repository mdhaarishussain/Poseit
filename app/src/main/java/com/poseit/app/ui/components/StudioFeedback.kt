package com.poseit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType

/*
 * Messages and confirmations: the Studio snackbar and dialog. Both sit on Sheet with no shadow and no tonal
 * elevation (the stock M3 snackbar casts a 6dp shadow that cannot be turned off).
 */

/**
 * Shows the snackbars of [state] as Studio cards: Sheet, Medium radius and a Hairline edge (it separates the card
 * from Paper without a shadow and does no harm over the camera), the message in Body Ink and the action as a
 * [TertiaryButton]. Use it everywhere instead of the M3 SnackbarHost, on light screens and over the camera.
 */
@Composable
fun StudioSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Surface(
            modifier = Modifier
                .padding(horizontal = Space.m, vertical = Space.m)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
            shape = StudioShapes.Medium,
            color = Studio.Sheet,
            contentColor = Studio.Ink,
            border = BorderStroke(1.dp, Studio.Hairline),
        ) {
            Row(
                Modifier
                    .heightIn(min = 48.dp)
                    .padding(start = Space.l, end = if (data.visuals.actionLabel != null) Space.xs else Space.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    data.visuals.message,
                    style = StudioType.body,
                    color = Studio.Ink,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = Space.m),
                )
                data.visuals.actionLabel?.let { label -> TertiaryButton(label, onClick = data::performAction) }
            }
        }
    }
}

/**
 * The Studio dialog: Sheet, Medium radius, no tonal elevation, the title in the Title style and the text in Body
 * Graphite. The confirming button names the action ("Delete pose", "Set photoshoot"); the dismiss button is usually
 * TertiaryButton("Cancel").
 * @param text the dialog's message, or use [content] for anything richer
 */
@Composable
fun StudioAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = StudioShapes.Medium,
        containerColor = Studio.Sheet,
        titleContentColor = Studio.Ink,
        textContentColor = Studio.Graphite,
        tonalElevation = 0.dp,
        title = { Text(title, style = StudioType.title) },
        text = when {
            content != null -> content
            text != null -> { { Text(text, style = StudioType.body) } }
            else -> null
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}
