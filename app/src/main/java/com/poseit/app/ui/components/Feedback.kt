package com.poseit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.semantics.heading
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone

/*
 * Messages, confirmations and sheets (docs/DESIGN.md, "Sheets, dialogs, snackbars"; Astryx Toast, AlertDialog,
 * BottomSheet, Banner). Flat: no shadows, no tonal elevation.
 */

/**
 * Shows the snackbars of [state] as small dark cards (Astryx Toast): the dark card colour, 16dp corners, the message
 * in Body cream and the action in lavender sentence case. A 1dp cream hairline keeps the edge over the black camera.
 * Use it everywhere instead of the M3 SnackbarHost. Messages confirm with the button's own words ("Pose saved").
 */
@Composable
fun PoseitSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        ProvideSurfaceTone(SurfaceTone.Dark) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = Space.edge, vertical = Space.edge)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = PoseitShapes.Row,
                color = PoseitColors.backgroundCard,
                contentColor = PoseitColors.textPrimaryInverted,
                border = BorderStroke(1.dp, PoseitColors.borderInverted),
            ) {
                Row(
                    Modifier
                        .heightIn(min = 56.dp)
                        .padding(start = Space.xl, end = if (data.visuals.actionLabel != null) Space.s else Space.xl),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        data.visuals.message,
                        style = PoseitType.body,
                        color = PoseitColors.textPrimaryInverted,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = Space.m),
                    )
                    data.visuals.actionLabel?.let { label ->
                        TextButton(
                            onClick = data::performAction,
                            modifier = Modifier.heightIn(min = ButtonHeight),
                            colors = ButtonDefaults.textButtonColors(containerColor = Color.Transparent, contentColor = PoseitColors.accent),
                        ) { Text(label, style = PoseitType.label) }
                    }
                }
            }
        }
    }
}

/**
 * The dialog (Astryx AlertDialog): a [DialogCard] in a modal window. Cream surface, 28dp corners, no tonal
 * elevation, the title in the Title style and the message in Body, secondary. The confirming button names the
 * action ("Delete pose") and is a pill (PrimaryButton, or DestructiveButton(filled = true)); the dismiss button is
 * usually TertiaryButton("Cancel").
 * @param text the dialog's message, or use [content] for anything richer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoseitAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier) {
        DialogCard(title = title, confirmButton = confirmButton, text = text, dismissButton = dismissButton, content = content)
    }
}

/**
 * The body of a [PoseitAlertDialog], also usable inline (previews, the component gallery): title, message or
 * [content], and the buttons at the end of the last row, dismiss first.
 */
@Composable
fun DialogCard(
    title: String,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    ProvideSurfaceTone(SurfaceTone.Light) {
        Column(
            modifier
                .widthIn(min = 280.dp, max = 560.dp)
                .background(PoseitColors.backgroundSurface, PoseitShapes.Sheet)
                .padding(start = Space.xl, end = Space.l, top = Space.xl, bottom = Space.l),
        ) {
            Text(
                title,
                style = PoseitType.title,
                color = PoseitColors.textPrimary,
                modifier = Modifier
                    .padding(end = Space.s)
                    .semantics { heading() },
            )
            if (content != null || text != null) {
                Spacer(Modifier.height(Space.m))
                Box(Modifier.padding(end = Space.s)) {
                    CompositionLocalProvider(LocalContentColor provides PoseitColors.textSecondary) {
                        if (content != null) content() else Text(text!!, style = PoseitType.body, color = PoseitColors.textSecondary)
                    }
                }
            }
            Spacer(Modifier.height(Space.l))
            Row(
                Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}

/**
 * A modal bottom sheet (Astryx BottomSheet): cream surface, 28dp top corners, a slim dark drag handle, no tonal
 * elevation, the v2 scrim. Content gets the light surface tone. Give it a Title-style heading as its first line.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoseitBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = PoseitShapes.SheetTop,
        containerColor = PoseitColors.backgroundSurface,
        contentColor = PoseitColors.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = PoseitColors.overlay,
        dragHandle = { SheetHandle() },
    ) {
        ProvideSurfaceTone(SurfaceTone.Light) { Column(content = content) }
    }
}

/** The drag handle of a [PoseitBottomSheet]: 36x4dp, ink at 24%, 12dp from the top. */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(top = Space.m, bottom = Space.s)
            .size(width = 36.dp, height = 4.dp)
            .background(PoseitColors.textPrimary.copy(alpha = 0.24f), PoseitShapes.Pill),
    )
}

/** The intent of a [Banner]. */
enum class BannerStatus { Info, Error }

/**
 * An inline message about the screen's state (Astryx Banner): an outlined cream card with a status icon, an optional
 * title and the message; errors use the signal red for the icon and the outline, never colour alone (the icon and
 * the words carry it). For "Couldn't read that photo. Try another one." with an optional [action] button.
 */
@Composable
fun Banner(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    status: BannerStatus = BannerStatus.Info,
    icon: ImageVector = if (status == BannerStatus.Error) PoseitIcons.error else PoseitIcons.info,
    action: (@Composable () -> Unit)? = null,
) {
    val tint = if (status == BannerStatus.Error) PoseitColors.signalMiss else PoseitColors.textPrimary
    ProvideSurfaceTone(SurfaceTone.Light) {
        Row(
            modifier
                .fillMaxWidth()
                .background(PoseitColors.backgroundSurface, PoseitShapes.Row)
                .border(1.dp, if (status == BannerStatus.Error) PoseitColors.signalMiss else PoseitColors.borderEmphasized, PoseitShapes.Row)
                .padding(horizontal = Space.l + Space.xs, vertical = Space.l)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(IconSize.standalone))
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                if (title != null) Text(title, style = PoseitType.bodyStrong, color = PoseitColors.textPrimary)
                Text(message, style = PoseitType.body, color = if (title != null) PoseitColors.textSecondary else PoseitColors.textPrimary)
                if (action != null) {
                    Spacer(Modifier.height(Space.s))
                    action()
                }
            }
        }
    }
}

/** v1 name of [PoseitSnackbarHost]. */
@Deprecated("Use PoseitSnackbarHost", ReplaceWith("PoseitSnackbarHost(state, modifier)"))
@Composable
fun StudioSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) = PoseitSnackbarHost(state, modifier)

/** v1 name of [PoseitAlertDialog]. */
@Deprecated("Use PoseitAlertDialog", ReplaceWith("PoseitAlertDialog(onDismissRequest, title, confirmButton, modifier, text, dismissButton, content)"))
@Composable
fun StudioAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) = PoseitAlertDialog(onDismissRequest, title, confirmButton, modifier, text, dismissButton, content)
