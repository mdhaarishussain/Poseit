package com.poseit.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poseit.app.appContainer
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.ui.components.DestructiveButton
import com.poseit.app.ui.components.HairlineDivider
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.SecondaryButton
import com.poseit.app.ui.components.StudioChip
import com.poseit.app.ui.components.StudioIconButton
import com.poseit.app.ui.components.StudioIconSize
import com.poseit.app.ui.components.StudioTopBar
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.create.PoseDetailsForm
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType
import kotlinx.coroutines.launch

/** Drag joints of a mannequin to create / edit a custom pose. [basePoseId] null = start from neutral. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoseEditorScreen(basePoseId: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
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
                    currentOnSaved(event.id)
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

    Scaffold(
        topBar = {
            StudioTopBar(title = "Pose editor", onBack = ::requestBack) {
                StudioIconButton(Icons.AutoMirrored.Outlined.Undo, "Undo", viewModel::undo, enabled = viewModel.canUndo)
                StudioIconButton(Icons.AutoMirrored.Outlined.Redo, "Redo", viewModel::redo, enabled = viewModel.canRedo)
                PrimaryButton(
                    text = "Save",
                    onClick = { showSaveSheet = true },
                    enabled = !viewModel.loading,
                    // Compact: 40dp tall inside the 56dp bar (the touch target stays 48dp). The bar pads 7dp for
                    // icon glyphs, so the button's edge sits on the 20dp gutter.
                    modifier = Modifier
                        .padding(start = Space.xs, end = 13.dp)
                        .height(40.dp),
                )
            }
        },
        // In the bottom-bar slot so snackbars sit above the tools and the toolbar runs under the navigation bar.
        bottomBar = { if (!viewModel.loading) EditorBottomBar(viewModel) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Studio.Paper,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = viewModel.base?.name?.let { "Based on $it" } ?: "New pose",
                style = StudioType.label,
                color = Studio.Graphite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = Space.gutter),
            )
            if (viewModel.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = Studio.Ink, strokeWidth = 2.dp)
                }
            } else {
                EditorCanvas(
                    viewModel,
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = Space.gutter)
                        .padding(top = Space.s, bottom = Space.xs),
                )
            }
        }
    }

    if (showSaveSheet) {
        SaveSheet(viewModel = viewModel, onDismiss = { showSaveSheet = false })
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            shape = StudioShapes.Medium,
            containerColor = Studio.Sheet,
            tonalElevation = 0.dp,
            title = { Text("Discard your changes?", style = StudioType.title, color = Studio.Ink) },
            text = { Text("Your edits to this pose haven't been saved.", style = StudioType.body, color = Studio.Graphite) },
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

@Composable
private fun EditorBottomBar(viewModel: PoseEditorViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Studio.Paper),
    ) {
        if (viewModel.figures.size > 1) {
            PersonChips(
                count = viewModel.figures.size,
                selected = viewModel.selected,
                onSelect = viewModel::select,
            )
        }
        Text(
            text = if (viewModel.figures.size > 1) {
                "Tap a person to edit them, then drag the dots."
            } else {
                "Drag a dot to bend, the body to move. Pinch to zoom."
            },
            style = StudioType.label,
            color = Studio.Graphite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter, vertical = Space.s),
        )
        EditorToolbar(viewModel)
    }
}

@Composable
private fun PersonChips(count: Int, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        repeat(count) { i ->
            StudioChip(label = "Person ${i + 1}", selected = i == selected, onClick = { onSelect(i) })
        }
    }
}

@Composable
private fun EditorToolbar(viewModel: PoseEditorViewModel) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .widthIn(min = maxWidth)
                .padding(horizontal = Space.s, vertical = Space.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ToolButton(Icons.Outlined.Flip, "Mirror", onClick = viewModel::mirrorSelected)
            ToolButton(Icons.Outlined.RestartAlt, "Reset", onClick = viewModel::reset)
            ToolToggle(Icons.Outlined.GridOn, "Grid", checked = viewModel.showGrid, onCheckedChange = { viewModel.showGrid = it })
            if (viewModel.referenceBitmap != null) {
                ToolToggle(Icons.Outlined.Image, "Photo", checked = viewModel.showReference, onCheckedChange = { viewModel.showReference = it })
            }
            ToolButton(Icons.Outlined.PersonAdd, "Add person", enabled = viewModel.canAddPerson, onClick = viewModel::addPerson)
            ToolButton(Icons.Outlined.PersonRemove, "Remove", enabled = viewModel.canRemovePerson, onClick = viewModel::removeSelected)
        }
    }
}

/** An outlined icon over its Label caption, in a 56dp-tall Medium-radius target. */
@Composable
private fun ToolLayout(icon: ImageVector, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .widthIn(min = 56.dp)
            .heightIn(min = 56.dp)
            .padding(horizontal = Space.xs, vertical = Space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(StudioIconSize))
        Spacer(Modifier.height(Space.xs))
        Text(label, style = StudioType.label, color = color, maxLines = 1)
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val color = if (enabled) Studio.Ink else Studio.Ink.copy(alpha = 0.38f)
    ToolLayout(
        icon, label, color,
        Modifier
            .clip(StudioShapes.Medium)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    )
}

/** A tool that stays on: inverted (Ink fill, Sheet icon and caption) while checked. */
@Composable
private fun ToolToggle(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ToolLayout(
        icon, label, if (checked) Studio.Sheet else Studio.Ink,
        Modifier
            .clip(StudioShapes.Medium)
            .background(if (checked) Studio.Ink else Color.Transparent)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Studio.Sheet,
        tonalElevation = 0.dp,
        // Full-height sheets would otherwise slide under the status bar (M3 1.3 only insets the bottom).
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
                    .padding(horizontal = Space.gutter)
                    .padding(bottom = Space.xl),
            ) {
                Text("Save pose", style = StudioType.title, color = Studio.Ink)
                if (base != null && base.source == PoseSource.BUILT_IN) {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "Built-in poses stay as they are. Your version is saved to My poses.",
                        style = StudioType.label,
                        color = Studio.Graphite,
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

/** The save sheet's pinned actions: one primary ("Save pose", or "Save changes" next to "Save as new"). */
@Composable
private fun SaveActions(
    canOverwrite: Boolean,
    canSave: Boolean,
    saving: Boolean,
    onSave: (asNew: Boolean) -> Unit,
    onKeepEditing: () -> Unit,
) {
    Column(Modifier.padding(horizontal = Space.gutter).padding(top = Space.l, bottom = Space.s)) {
        if (canOverwrite) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                SecondaryButton("Save as new", onClick = { onSave(true) }, enabled = canSave, modifier = Modifier.weight(1f))
                PrimaryButton(onClick = { onSave(false) }, enabled = canSave, modifier = Modifier.weight(1f)) {
                    SavingLabel(saving, "Save changes")
                }
            }
        } else {
            PrimaryButton(onClick = { onSave(true) }, enabled = canSave, modifier = Modifier.fillMaxWidth()) {
                SavingLabel(saving, "Save pose")
            }
        }
        Spacer(Modifier.height(Space.xs))
        TertiaryButton("Keep editing", onClick = onKeepEditing, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SavingLabel(saving: Boolean, label: String) {
    if (saving) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
    } else {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
