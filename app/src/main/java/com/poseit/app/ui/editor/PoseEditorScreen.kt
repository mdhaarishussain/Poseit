package com.poseit.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poseit.app.appContainer
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.ui.create.PoseDetailsForm
import com.poseit.app.ui.theme.PoseitColors
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
            TopAppBar(
                title = {
                    Column {
                        Text("Pose editor", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = viewModel.base?.name?.let { "Based on $it" } ?: "New pose",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = ::requestBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = viewModel.canUndo) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = viewModel::redo, enabled = viewModel.canRedo) {
                        Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = "Redo")
                    }
                    Button(
                        onClick = { showSaveSheet = true },
                        enabled = !viewModel.loading,
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        // In the bottom-bar slot so snackbars sit above the tools and the toolbar runs under the navigation bar.
        bottomBar = { if (!viewModel.loading) EditorBottomBar(viewModel) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (viewModel.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            EditorCanvas(viewModel, Modifier.fillMaxSize().padding(padding))
        }
    }

    if (showSaveSheet) {
        SaveSheet(viewModel = viewModel, onDismiss = { showSaveSheet = false })
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard your changes?") },
            text = { Text("Your edits to this pose haven't been saved.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    onBack()
                }) { Text("Discard", color = PoseitColors.Red) }
            },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@Composable
private fun EditorBottomBar(viewModel: PoseEditorViewModel) {
    Column(Modifier.fillMaxWidth()) {
        if (viewModel.figures.size > 1) {
            PersonChips(
                count = viewModel.figures.size,
                selected = viewModel.selected,
                onSelect = viewModel::select,
            )
        }
        Text(
            text = if (viewModel.figures.size > 1) {
                "Tap a person to edit them · drag the dots to bend joints"
            } else {
                "Drag the dots to bend · drag the body to move · pinch to zoom"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
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
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { i ->
            FilterChip(
                selected = i == selected,
                onClick = { onSelect(i) },
                label = { Text("Person ${i + 1}") },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(PoseitColors.FigureTints[i % PoseitColors.FigureTints.size]),
                    )
                },
            )
        }
    }
}

@Composable
private fun EditorToolbar(viewModel: PoseEditorViewModel) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ToolButton(Icons.Rounded.Flip, "Mirror", onClick = viewModel::mirrorSelected)
            ToolButton(Icons.Rounded.RestartAlt, "Reset", onClick = viewModel::reset)
            ToolToggle(Icons.Rounded.GridOn, "Grid", checked = viewModel.showGrid, onCheckedChange = { viewModel.showGrid = it })
            if (viewModel.referenceBitmap != null) {
                ToolToggle(Icons.Rounded.Image, "Photo", checked = viewModel.showReference, onCheckedChange = { viewModel.showReference = it })
            }
            ToolButton(Icons.Rounded.PersonAdd, "Add person", enabled = viewModel.canAddPerson, onClick = viewModel::addPerson)
            ToolButton(Icons.Rounded.PersonRemove, "Remove", enabled = viewModel.canRemovePerson, onClick = viewModel::removeSelected)
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Column(
        Modifier
            .widthIn(min = 64.dp)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

@Composable
private fun ToolToggle(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val color = if (checked) PoseitColors.Violet else LocalContentColor.current
    Column(
        Modifier
            .widthIn(min = 64.dp)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) PoseitColors.Violet.copy(alpha = 0.14f) else Color.Transparent)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        // Full-height sheets would otherwise slide under the status bar (M3 1.3 only insets the bottom).
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Save pose", style = MaterialTheme.typography.titleLarge)
            if (base != null && base.source == PoseSource.BUILT_IN) {
                Text(
                    "Built-in poses stay as they are – your version is saved to My Poses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PoseDetailsForm(
                name = name,
                onNameChange = viewModel::updateDraftName,
                category = category,
                onCategoryChange = viewModel::updateDraftCategory,
                tags = tags,
                onTagsChange = viewModel::updateDraftTags,
            )
            if (viewModel.canOverwriteBase) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { save(asNew = true) }, enabled = canSave, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text("Save as new")
                    }
                    Button(onClick = { save(asNew = false) }, enabled = canSave, modifier = Modifier.weight(1f).height(52.dp)) {
                        SavingLabel(viewModel.saving, "Save changes")
                    }
                }
            } else {
                Button(onClick = { save(asNew = true) }, enabled = canSave, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    SavingLabel(viewModel.saving, "Save pose")
                }
            }
            TextButton(
                onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Keep editing") }
        }
    }
}

@Composable
private fun SavingLabel(saving: Boolean, label: String) {
    if (saving) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
    } else {
        Text(label)
    }
}
