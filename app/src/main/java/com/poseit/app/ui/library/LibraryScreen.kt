package com.poseit.app.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.poseit.app.appContainer
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.ui.components.EmptyState
import com.poseit.app.ui.components.HairlineDivider
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.SecondaryButton
import com.poseit.app.ui.components.SettingRow
import com.poseit.app.ui.components.StudioButtonHeight
import com.poseit.app.ui.components.StudioIconButton
import com.poseit.app.ui.components.StudioLargeTopBar
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioType
import kotlinx.coroutines.launch

/** Home screen: browse / search / filter the pose library. */
@Composable
fun LibraryScreen(
    onOpenPose: (String) -> Unit,
    onOpenCamera: (String?) -> Unit,
    onOpenGallery: () -> Unit,
    onCreateFromPhoto: () -> Unit,
    onOpenEditor: (String?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    val vm: LibraryViewModel = viewModel(
        factory = viewModelFactory { initializer { LibraryViewModel(appContext.appContainer.poseRepository, savedState = createSavedStateHandle()) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    LibraryContent(
        state = state,
        query = vm.query,
        onQueryChange = vm::onQueryChange,
        onSelectTab = vm::selectTab,
        onSelectPeople = vm::selectPeople,
        onToggleCategory = vm::toggleCategory,
        onToggleLocation = vm::toggleLocation,
        onToggleMood = vm::toggleMood,
        onClearFilters = vm::clearRefinements,
        onToggleFavorite = vm::toggleFavorite,
        onOpenPose = onOpenPose,
        onOpenCamera = onOpenCamera,
        onOpenGallery = onOpenGallery,
        onCreateFromPhoto = onCreateFromPhoto,
        onOpenEditor = onOpenEditor,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun LibraryContent(
    state: LibraryUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelectTab: (LibraryTab) -> Unit,
    onSelectPeople: (PeopleFilter) -> Unit,
    onToggleCategory: (PoseCategory?) -> Unit,
    onToggleLocation: (PoseTag) -> Unit,
    onToggleMood: (PoseTag) -> Unit,
    onClearFilters: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenPose: (String) -> Unit,
    onOpenCamera: (String?) -> Unit,
    onOpenGallery: () -> Unit,
    onCreateFromPhoto: () -> Unit,
    onOpenEditor: (String?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val fabExpanded by remember { derivedStateOf { gridState.firstVisibleItemIndex < 2 } }
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Studio.Paper,
        topBar = {
            StudioLargeTopBar("Poseit") {
                StudioIconButton(Icons.Outlined.Add, "Create a pose", onClick = { showCreateSheet = true })
                StudioIconButton(Icons.Outlined.PhotoLibrary, "Your photos", onClick = onOpenGallery)
                StudioIconButton(Icons.Outlined.Settings, "Settings", onClick = onOpenSettings)
            }
        },
        floatingActionButton = {
            PoseCameraButton(
                expanded = fabExpanded,
                onClick = { onOpenCamera(null) },
                // Scaffold keeps the FAB 16dp from the edges; this puts it on the 20dp gutter.
                modifier = Modifier.padding(end = Space.xs, bottom = Space.xs),
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = Studio.Ink, strokeWidth = 2.dp) }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = PoseGridCells,
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Space.gutter,
                end = Space.gutter,
                top = padding.calculateTopPadding() + Space.xs,
                // Room for the camera button over the last row.
                bottom = padding.calculateBottomPadding() + StudioButtonHeight + Space.xxl,
            ),
            horizontalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            fullWidth("search") {
                LibrarySearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.padding(bottom = Space.xl),
                )
            }
            if (state.showHero) {
                val hero = state.poseOfTheDay
                if (hero != null) {
                    fullWidth("hero") {
                        TodaysPose(
                            pose = hero,
                            onOpen = { onOpenPose(hero.id) },
                            onTry = { onOpenCamera(hero.id) },
                            modifier = Modifier
                                .animateItem()
                                .padding(bottom = Space.section),
                        )
                    }
                }
            }
            fullWidth("tabs") {
                LibraryTabs(selected = state.filter.tab, onSelect = onSelectTab)
            }
            fullWidth("filters") {
                LibraryFilters(
                    filter = state.filter,
                    categories = state.categories,
                    locationTags = state.locationTags,
                    moodTags = state.moodTags,
                    onPeople = onSelectPeople,
                    onCategory = onToggleCategory,
                    onLocation = onToggleLocation,
                    onMood = onToggleMood,
                    modifier = Modifier.padding(top = Space.s),
                )
            }
            if (state.poses.isNotEmpty()) {
                fullWidth("results_header") {
                    ResultsHeader(
                        count = state.poses.size,
                        showClear = state.filter.hasRefinements,
                        onClear = onClearFilters,
                        modifier = Modifier
                            .animateItem()
                            .padding(top = Space.xs, bottom = Space.xs),
                    )
                }
            }
            if (state.filter.tab == LibraryTab.MINE && state.tabTotal > 0) {
                item(key = "create_card", contentType = "create") {
                    CreatePoseCard(
                        onClick = { showCreateSheet = true },
                        modifier = Modifier
                            .animateItem()
                            .padding(bottom = Space.xl),
                    )
                }
            }
            items(state.poses, key = { it.id }, contentType = { "pose" }) { pose ->
                PoseCard(
                    pose = pose,
                    isFavorite = pose.id in state.favorites,
                    onOpen = { onOpenPose(pose.id) },
                    onToggleFavorite = { onToggleFavorite(pose.id) },
                    onTry = { onOpenCamera(pose.id) },
                    onCustomize = { onOpenEditor(pose.id) },
                    modifier = Modifier
                        .animateItem()
                        .padding(bottom = Space.xl),
                )
            }
            if (state.poses.isEmpty()) {
                fullWidth("empty") {
                    LibraryEmptyState(
                        modifier = Modifier
                            .animateItem()
                            // EmptyState brings its own 20dp gutter; the grid already has one.
                            .bleedHorizontally(Space.gutter),
                        state = state,
                        onClearFilters = onClearFilters,
                        onShowAll = { onSelectTab(LibraryTab.ALL) },
                        onOpenCamera = { onOpenCamera(null) },
                        onCreate = { showCreateSheet = true },
                        onDraw = { onOpenEditor(null) },
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        CreatePoseSheet(
            onDismiss = { showCreateSheet = false },
            onFromPhoto = onCreateFromPhoto,
            onDraw = { onOpenEditor(null) },
        )
    }
}

/**
 * The screen's one primary action, floating bottom-end: Ink, Medium radius, 48dp, flat. It folds to the camera
 * glyph once the grid scrolls, so it covers less of the poses.
 */
@Composable
private fun PoseCameraButton(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PrimaryButton(onClick = onClick, modifier = modifier) {
        // The label can fold away, so the glyph carries the name while it is hidden.
        Icon(
            Icons.Outlined.CameraAlt,
            contentDescription = if (expanded) null else "Pose camera",
            modifier = Modifier.size(18.dp),
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + expandHorizontally(tween(200)),
            exit = fadeOut(tween(150)) + shrinkHorizontally(tween(200)),
        ) {
            Row {
                Spacer(Modifier.width(Space.s))
                Text("Pose camera", maxLines = 1)
            }
        }
    }
}

/**
 * Like `GridCells.Adaptive(160.dp)` (as many ~160 dp columns as fit) but never fewer than two, so small and
 * mid-size phones (320–400 dp wide) still get a two-column grid instead of one huge card per row.
 */
private object PoseGridCells : GridCells {
    private val minCellSize = 160.dp

    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val count = maxOf(2, (availableSize + spacing) / (minCellSize.roundToPx() + spacing))
        val usable = (availableSize - spacing * (count - 1)).coerceAtLeast(0)
        val cell = usable / count
        val remainder = usable % count
        return List(count) { index -> cell + if (index < remainder) 1 else 0 }
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable LazyGridItemScope.() -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = key) { content() }
}

/** "24 poses" in Label Graphite, and "Clear filters" while anything narrows the list. */
@Composable
private fun ResultsHeader(count: Int, showClear: Boolean, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = StudioButtonHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (count == 1) "1 pose" else "$count poses",
            style = StudioType.label,
            color = Studio.Graphite,
            modifier = Modifier.weight(1f),
        )
        AnimatedVisibility(visible = showClear, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
            TertiaryButton("Clear filters", onClick = onClear)
        }
    }
}

@Composable
private fun LibraryEmptyState(
    modifier: Modifier,
    state: LibraryUiState,
    onClearFilters: () -> Unit,
    onShowAll: () -> Unit,
    onOpenCamera: () -> Unit,
    onCreate: () -> Unit,
    onDraw: () -> Unit,
) {
    // The camera button is the screen's primary action, so each empty state offers one secondary button.
    val tab = state.filter.tab
    when {
        state.tabTotal == 0 && tab == LibraryTab.FAVORITES -> EmptyState(
            modifier = modifier,
            title = "No favourites yet",
            message = "Tap the heart on any pose to keep it here for your next shoot.",
            action = { SecondaryButton("Browse poses", onClick = onShowAll) },
        )

        state.tabTotal == 0 && tab == LibraryTab.RECENT -> EmptyState(
            modifier = modifier,
            title = "Nothing tried yet",
            message = "Poses you use in the camera show up here, so you can find them again quickly.",
            action = { SecondaryButton("Open the pose camera", onClick = onOpenCamera) },
        )

        state.tabTotal == 0 && tab == LibraryTab.MINE -> EmptyState(
            modifier = modifier,
            title = "Make your first pose",
            message = "Turn any photo you love into a pose guide, or drag the mannequin into a pose of your own.",
            action = { SecondaryButton("Create a pose", onClick = onCreate) },
        )

        state.tabTotal == 0 -> EmptyState(
            modifier = modifier,
            title = "No poses here",
            message = "Create your own pose from a photo or draw one with the mannequin.",
            action = { SecondaryButton("Draw a pose", onClick = onDraw) },
        )

        else -> EmptyState(
            modifier = modifier,
            title = "No poses match",
            message = if (state.filter.query.isNotBlank()) {
                "Nothing found for “${state.filter.query.trim()}”. Try another word or clear the filters."
            } else {
                "Try a different combination of filters."
            },
            action = { SecondaryButton("Clear filters", onClick = onClearFilters) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePoseSheet(onDismiss: () -> Unit, onFromPhoto: () -> Unit, onDraw: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Studio.Sheet,
        contentColor = Studio.Ink,
        tonalElevation = 0.dp,
        // Full-height sheets would otherwise slide under the status bar (M3 1.3 only insets the bottom).
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = Space.l),
        ) {
            Text(
                "Create a pose",
                style = StudioType.title,
                color = Studio.Ink,
                modifier = Modifier
                    .padding(horizontal = Space.gutter)
                    .semantics { heading() },
            )
            Spacer(Modifier.height(Space.s))
            Text(
                "Your poses work in the camera just like the built-in ones.",
                style = StudioType.body,
                color = Studio.Graphite,
                modifier = Modifier.padding(horizontal = Space.gutter),
            )
            Spacer(Modifier.height(Space.m))
            SettingRow(
                title = "From a photo",
                subtitle = "Pick a photo you like. Poseit traces the pose and turns it into a guide.",
                onClick = { closeThen(onFromPhoto) },
            )
            HairlineDivider(Modifier.padding(horizontal = Space.gutter))
            SettingRow(
                title = "Draw your own",
                subtitle = "Drag the mannequin's joints into exactly the pose you have in mind.",
                onClick = { closeThen(onDraw) },
            )
        }
    }
}
