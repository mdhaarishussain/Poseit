package com.poseit.app.ui.editor

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.border
import com.poseit.app.ui.components.ButtonHeight
import com.poseit.app.ui.components.TogglePill
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poseit.app.appContainer
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.ui.components.AccentButton
import com.poseit.app.ui.components.ButtonForm
import com.poseit.app.ui.components.ButtonSize
import com.poseit.app.ui.components.FillingCardStack
import com.poseit.app.ui.components.CapsLabel
import com.poseit.app.ui.components.DestructiveButton
import com.poseit.app.ui.components.HairlineDivider
import com.poseit.app.ui.components.IconSize
import com.poseit.app.ui.components.PlainIconButton
import com.poseit.app.ui.components.PoseitAlertDialog
import com.poseit.app.ui.components.PoseitBottomSheet
import com.poseit.app.ui.components.PoseitSnackbarHost
import com.poseit.app.ui.components.PoseitSpinner
import com.poseit.app.ui.components.SecondaryButton
import com.poseit.app.ui.components.SegmentedPill
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.components.TopBarContent
import com.poseit.app.ui.create.PoseDetailsForm
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import kotlinx.coroutines.launch

/**
 * Drag joints of a mannequin to create / edit a custom pose. [basePoseId] null = start from neutral.
 *
 * One stack of fused dark cards fills the screen: the top bar card (Back, "Pose editor", the lavender Save), the
 * canvas card (what the pose is based on, the dark stage with the figure, and at its foot the hint with undo and
 * redo), and the tools card (who is being edited, then the tools).
 */
@Composable
fun PoseEditorScreen(basePoseId: String?, onBack: () -> Unit, onSaved: (id: String, message: String) -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { context.appContainer.poseRepository }
    val viewModel: PoseEditorViewModel = viewModel(key = "editor:${basePoseId.orEmpty()}") {
        PoseEditorViewModel(repository, basePoseId, createSavedStateHandle())
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentOnSaved by rememberUpdatedState(onSaved)
    var showSaveSheet by rememberSaveable { mutableStateOf(false) }
    var showDiscard by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.Saved -> {
                    showSaveSheet = false
                    currentOnSaved(event.id, event.message)
                }
                is EditorEvent.Message -> scope.launch { snackbar.showSnackbar(event.text) }
            }
        }
    }

    // Derived so dragging (which changes the figures every frame) doesn't recompose the whole screen.
    val dirty by remember(viewModel) { derivedStateOf { viewModel.isDirty } }
    fun requestBack() {
        if (dirty) showDiscard = true else onBack()
    }
    BackHandler(enabled = dirty && !showSaveSheet) { showDiscard = true }

    // Snackbars sit just above the tools card.
    var toolsHeight by remember { mutableIntStateOf(0) }
    Box(
        Modifier
            .fillMaxSize()
            .background(PoseitColors.backgroundBody)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        FillingCardStack(
            modifier = Modifier
                .fillMaxSize()
                .padding(Space.edge),
            fillIndex = 1,
            cards = listOfNotNull<@Composable () -> Unit>(
                {
                    // Undo and redo live with the canvas, so the title keeps its room on a narrow phone.
                    TopBarContent(
                        title = "Pose editor",
                        onBack = ::requestBack,
                        actions = {
                            AccentButton(
                                text = "Save",
                                onClick = { showSaveSheet = true },
                                enabled = !viewModel.loading,
                                form = ButtonForm.Block,
                                // Small, so the bar keeps its air around the button, as around the badge on other screens.
                                size = ButtonSize.Small,
                                // The bar pads for icon glyphs; this puts the button's edge 16dp in from the card's.
                                modifier = Modifier.padding(start = Space.s, end = Space.xs),
                            )
                        },
                    )
                },
                { CanvasCard(viewModel) },
                if (viewModel.loading) null else { { ToolsCard(viewModel, Modifier.onSizeChanged { toolsHeight = it.height }) } },
            ),
        )
        val toolsDp = with(LocalDensity.current) { toolsHeight.toDp() }
        PoseitSnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = toolsDp + Space.edge),
        )
    }

    if (showSaveSheet) {
        SaveSheet(viewModel = viewModel, onDismiss = { showSaveSheet = false })
    }

    if (showDiscard) {
        PoseitAlertDialog(
            onDismissRequest = { showDiscard = false },
            title = "Discard your changes?",
            text = "Your edits to this pose haven't been saved.",
            confirmButton = {
                DestructiveButton("Discard", filled = true, onClick = {
                    showDiscard = false
                    onBack()
                })
            },
            dismissButton = { TertiaryButton("Keep editing", onClick = { showDiscard = false }) },
        )
    }
}

/**
 * The middle card: what the pose started from with the view options (Grid, and Photo for a pose made from a
 * photo) beside it, the stage, and (like the mockups' instruction at the foot of the figure card) the hint with undo
 * and redo.
 */
@Composable
private fun CanvasCard(viewModel: PoseEditorViewModel) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = Space.cardPadding, end = Space.l, top = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = viewModel.base?.name?.let { "Based on $it" } ?: "New pose",
                style = PoseitType.supporting,
                color = PoseitColors.textSecondaryInverted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!viewModel.loading) {
                // View options: they change what the stage shows, not the pose, so they live with the stage.
                Spacer(Modifier.width(Space.s))
                TogglePill("Grid", checked = viewModel.showGrid, onCheckedChange = { viewModel.showGrid = it })
                if (viewModel.referenceBitmap != null) {
                    Spacer(Modifier.width(Space.s))
                    TogglePill("Photo", checked = viewModel.showReference, onCheckedChange = { viewModel.showReference = it })
                }
            }
        }
        if (viewModel.loading) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { PoseitSpinner(size = 28.dp, color = PoseitColors.textPrimaryInverted) }
        } else {
            EditorCanvas(
                viewModel,
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            // The hint, with undo and redo at its end: their glyphs land on the card padding.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = Space.cardPadding, end = Space.cardPadding - Space.m, bottom = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (viewModel.figures.size > 1) {
                        "Tap a person to edit them, then drag the dots."
                    } else {
                        "Drag a dot to bend, the body to move. Pinch to zoom."
                    },
                    style = PoseitType.supporting,
                    color = PoseitColors.textSecondaryInverted,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = Space.xs),
                )
                Spacer(Modifier.width(Space.s))
                PlainIconButton(Icons.AutoMirrored.Outlined.Undo, "Undo", viewModel::undo, enabled = viewModel.canUndo)
                PlainIconButton(Icons.AutoMirrored.Outlined.Redo, "Redo", viewModel::redo, enabled = viewModel.canRedo)
            }
        }
    }
}

/** The bottom card: the person being edited (for a couple or group), then the tools. */
@Composable
private fun ToolsCard(viewModel: PoseEditorViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = Space.m),
    ) {
        if (viewModel.figures.size > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Space.l),
            ) {
                SegmentedPill(
                    options = List(viewModel.figures.size) { "Person ${it + 1}" },
                    selectedIndex = viewModel.selected,
                    onSelect = viewModel::select,
                )
            }
            Spacer(Modifier.height(Space.s))
        }
        EditorToolbar(viewModel)
    }
}

/**
 * The pose tools as the mockups' outlined pills, two by two in equal widths: Mirror and Reset, then Add person and
 * Remove.
 */
@Composable
private fun EditorToolbar(viewModel: PoseEditorViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.l),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            ToolPill(Icons.Outlined.Flip, "Mirror", Modifier.weight(1f), onClick = viewModel::mirrorSelected)
            ToolPill(Icons.Outlined.RestartAlt, "Reset", Modifier.weight(1f), onClick = viewModel::reset)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            ToolPill(Icons.Outlined.PersonAdd, "Add person", Modifier.weight(1f), enabled = viewModel.canAddPerson, onClick = viewModel::addPerson)
            ToolPill(Icons.Outlined.PersonRemove, "Remove", Modifier.weight(1f), enabled = viewModel.canRemovePerson, onClick = viewModel::removeSelected)
        }
    }
}

/**
 * A tool on the dark card: the outlined secondary pill (1dp mist outline, 48dp) with an 18dp glyph (dropped at
 * large text sizes) and the tiny capitals, one Role.Button target. Disabled tools fade their glyph, label and outline.
 */
@Composable
private fun ToolPill(icon: ImageVector, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val content = if (enabled) PoseitColors.textPrimaryInverted else PoseitColors.textDisabledInverted
    val edge = if (enabled) PoseitColors.borderEmphasizedInverted else PoseitColors.borderEmphasizedInverted.copy(alpha = 0.38f)
    Row(
        modifier
            .heightIn(min = ButtonHeight)
            .clip(PoseitShapes.Pill)
            .border(1.dp, edge, PoseitShapes.Pill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.m),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // At large text sizes the label needs the glyph's room (two pills share a phone's width).
        if (LocalDensity.current.fontScale <= 1.15f) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(IconSize.inline))
            Spacer(Modifier.width(Space.s))
        }
        CapsLabel(label, color = content, style = PoseitType.capsButton)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveSheet(viewModel: PoseEditorViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val base = viewModel.base
    // The draft lives in the view model, so "Keep editing" (or swiping the sheet away) keeps what was entered.
    val name = viewModel.draftName ?: viewModel.suggestedName
    val category = viewModel.draftCategory ?: base?.takeIf { it.source != PoseSource.BUILT_IN }?.category ?: PoseCategory.CUSTOM
    val tags = viewModel.draftTags ?: base?.tags ?: emptySet()
    val canSave = name.isNotBlank() && !viewModel.saving

    fun save(asNew: Boolean) = viewModel.save(name, category, tags, asNew)

    PoseitBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Full-height sheets would otherwise slide under the status bar (M3 only insets the bottom).
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
        ) {
            // The form scrolls; the actions stay pinned below it, so saving never needs a scroll.
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.cardPadding)
                    .padding(top = Space.s, bottom = Space.xl),
            ) {
                Text("Save pose", style = PoseitType.title, color = PoseitColors.textPrimary, modifier = Modifier.semantics { heading() })
                if (base != null && base.source == PoseSource.BUILT_IN) {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "Built-in poses stay as they are. Your version is saved to My poses.",
                        style = PoseitType.supporting,
                        color = PoseitColors.textSecondary,
                    )
                }
                Spacer(Modifier.height(Space.xl))
                PoseDetailsForm(
                    name = name,
                    onNameChange = viewModel::updateDraftName,
                    category = category,
                    onCategoryChange = viewModel::updateDraftCategory,
                    tags = tags,
                    onTagsChange = viewModel::updateDraftTags,
                )
            }
            HairlineDivider()
            SaveActions(
                canOverwrite = viewModel.canOverwriteBase,
                canSave = canSave,
                saving = viewModel.saving,
                onSave = ::save,
                onKeepEditing = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
            )
        }
    }
}

/**
 * The save sheet's pinned actions: one lavender main action ("Save pose", or "Save changes" next to an outlined
 * "Save as new"), then "Keep editing".
 */
@Composable
private fun SaveActions(
    canOverwrite: Boolean,
    canSave: Boolean,
    saving: Boolean,
    onSave: (asNew: Boolean) -> Unit,
    onKeepEditing: () -> Unit,
) {
    Column(Modifier.padding(horizontal = Space.l).padding(top = Space.l, bottom = Space.s)) {
        if (canOverwrite) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                SecondaryButton("Save as new", onClick = { onSave(true) }, enabled = canSave, modifier = Modifier.weight(1f))
                AccentButton(onClick = { onSave(false) }, enabled = canSave, modifier = Modifier.weight(1f)) {
                    SavingLabel(saving, "Save changes")
                }
            }
        } else {
            AccentButton(onClick = { onSave(true) }, enabled = canSave, modifier = Modifier.fillMaxWidth()) {
                SavingLabel(saving, "Save pose")
            }
        }
        Spacer(Modifier.height(Space.xs))
        TertiaryButton("Keep editing", onClick = onKeepEditing, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SavingLabel(saving: Boolean, label: String) {
    if (saving) PoseitSpinner(size = IconSize.inline) else CapsLabel(label, style = PoseitType.capsButton)
}
