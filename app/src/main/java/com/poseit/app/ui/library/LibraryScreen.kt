package com.poseit.app.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.poseit.app.appContainer
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.ui.components.AccentButton
import com.poseit.app.ui.components.ButtonForm
import com.poseit.app.ui.components.Badge
import com.poseit.app.ui.components.ButtonHeight
import com.poseit.app.ui.components.CircleIconButton
import com.poseit.app.ui.components.EmptyState
import com.poseit.app.ui.components.FloatingPillBar
import com.poseit.app.ui.components.OutlinedRow
import com.poseit.app.ui.components.PillBarItemSize
import com.poseit.app.ui.components.PillBarSegments
import com.poseit.app.ui.components.PlainIconButton
import com.poseit.app.ui.components.PoseitBottomSheet
import com.poseit.app.ui.components.PoseitCard
import com.poseit.app.ui.components.PoseitIcons
import com.poseit.app.ui.components.PoseitSnackbarHost
import com.poseit.app.ui.components.PoseitSpinner
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.components.TopBarCard
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
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
    message: String? = null,
    onMessageShown: () -> Unit = {},
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
        message = message,
        onMessageShown = onMessageShown,
    )
}

/*
 * Layout (docs/DESIGN.md, "Home"): the top bar card fused to Today's pose, then on the canvas the search field, the
 * pill tabs and the filter pills, and the grid of dark pose tiles, all 8dp from the sides and 8dp apart. The
 * floating pill bar sits on a canvas band at the bottom; the grid scrolls under the band, never through it.
 */
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
    message: String?,
    onMessageShown: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val messageScope = rememberCoroutineScope()
    LaunchedEffect(message) {
        if (message != null) {
            // Shown in its own scope: clearing the message restarts this effect, which must not cut the snackbar off.
            onMessageShown()
            messageScope.launch { snackbar.showSnackbar(message) }
        }
    }

    Scaffold(
        containerColor = PoseitColors.backgroundBody,
        // The status bar sits on a canvas band, so the grid scrolls under it rather than behind the clock.
        topBar = { StatusBarBand() },
        bottomBar = {
            // Arrives with the list (loading takes a moment), so the screen changes once rather than twice.
            if (!state.loading) {
                LibraryPillBar(
                    onOpenCamera = { onOpenCamera(null) },
                    onOpenGallery = onOpenGallery,
                    onCreate = { showCreateSheet = true },
                )
            }
        },
        snackbarHost = { PoseitSnackbarHost(snackbar) },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val startPadding = padding.calculateStartPadding(layoutDirection) + Space.edge
        val endPadding = padding.calculateEndPadding(layoutDirection) + Space.edge
        if (state.loading) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                LibraryTopBar(total = null, onOpenSettings = onOpenSettings)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PoseitSpinner() }
            }
            return@Scaffold
        }
        val showCreateCard = state.filter.tab == LibraryTab.MINE && state.tabTotal > 0
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val nameLines = rememberRowNameLines(
                names = state.poses.map { it.name },
                leadingName = if (showCreateCard) CreatePoseCardName else null,
                gridWidth = maxWidth - startPadding - endPadding,
            )
            LazyVerticalGrid(
                columns = PoseGridCells,
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = startPadding,
                    end = endPadding,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + Space.edge,
                ),
                horizontalArrangement = Arrangement.spacedBy(Space.edge),
                verticalArrangement = Arrangement.spacedBy(Space.edge),
            ) {
                fullWidth("top") {
                    // The top bar card, fused to Today's pose on the unfiltered home view.
                    val hero = state.poseOfTheDay?.takeIf { state.showHero }
                    LibraryTopBar(
                        total = state.libraryTotal,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.bleedHorizontally(Space.edge),
                        joined = hero?.let { pose ->
                            {
                                TodaysPoseCard(
                                    pose = pose,
                                    onOpen = { onOpenPose(pose.id) },
                                    onTry = { onOpenCamera(pose.id) },
                                )
                            }
                        },
                    )
                }
                fullWidth("search") {
                    LibrarySearchField(query = query, onQueryChange = onQueryChange)
                }
                fullWidth("tabs") {
                    LibraryTabs(
                        selected = state.filter.tab,
                        onSelect = onSelectTab,
                        modifier = Modifier.padding(top = Space.s),
                    )
                }
                if (state.tabTotal > 0) fullWidth("filters") {
                    LibraryFilters(
                        filter = state.filter,
                        categories = state.categories,
                        locationTags = state.locationTags,
                        moodTags = state.moodTags,
                        onPeople = onSelectPeople,
                        onCategory = onToggleCategory,
                        onLocation = onToggleLocation,
                        onMood = onToggleMood,
                    )
                }
                // On the unfiltered list the top bar's badge already gives the count.
                if (state.poses.isNotEmpty() && (state.filter.tab != LibraryTab.ALL || state.filter.hasRefinements)) {
                    fullWidth("results_header") {
                        ResultsHeader(
                            count = state.poses.size,
                            showClear = state.filter.hasRefinements,
                            onClear = onClearFilters,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                if (showCreateCard) {
                    item(key = "create_card", contentType = "create") {
                        CreatePoseCard(
                            onClick = { showCreateSheet = true },
                            modifier = Modifier.animateItem(),
                            nameLines = nameLines.leading,
                        )
                    }
                }
                itemsIndexed(state.poses, key = { _, pose -> pose.id }, contentType = { _, _ -> "pose" }) { index, pose ->
                    PoseCard(
                        nameLines = nameLines.forItem(index),
                        pose = pose,
                        isFavorite = pose.id in state.favorites,
                        onOpen = { onOpenPose(pose.id) },
                        onToggleFavorite = { onToggleFavorite(pose.id) },
                        onTry = { onOpenCamera(pose.id) },
                        onCustomise = { onOpenEditor(pose.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
                if (state.poses.isEmpty()) {
                    fullWidth("empty") {
                        LibraryEmptyState(
                            modifier = Modifier
                                .animateItem()
                                .padding(top = Space.s),
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
    }

    if (showCreateSheet) {
        CreatePoseSheet(
            onDismiss = { showCreateSheet = false },
            onFromPhoto = onCreateFromPhoto,
            onDraw = { onOpenEditor(null) },
        )
    }
}

/** A canvas-coloured band as tall as the status bar. */
@Composable
private fun StatusBarBand() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .background(PoseitColors.backgroundBody)
            .windowInsetsTopHeight(WindowInsets.statusBars),
    )
}

/**
 * The home top bar card: the "Poseit" wordmark, the library's size as the lavender badge and Settings, with [joined]
 * (Today's pose) fused under it. The status bar band above already keeps it clear of the clock.
 */
@Composable
private fun LibraryTopBar(
    total: Int?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    joined: (@Composable () -> Unit)? = null,
) {
    TopBarCard(
        title = "Poseit",
        modifier = modifier,
        wordmark = true,
        badge = total?.let { n -> { Badge(if (n == 1) "1 pose" else "$n poses") } },
        windowInsets = WindowInsets(0),
        actions = { PlainIconButton(PoseitIcons.settings, "Settings", onClick = onOpenSettings) },
        joined = joined,
    )
}

/**
 * The app's floating bottom bar on the home screen (docs/DESIGN.md, "Floating pill bar"): the pose camera, the
 * Poses / Photos switch (Photos opens your photos; coming back shows Poses again) and Create a pose.
 */
@Composable
private fun LibraryPillBar(onOpenCamera: () -> Unit, onOpenGallery: () -> Unit, onCreate: () -> Unit) {
    FloatingPillBar {
        CircleIconButton(PoseitIcons.camera, "Pose camera", onClick = onOpenCamera, size = PillBarItemSize)
        PillBarSegments(
            options = listOf("Poses", "Photos"),
            selectedIndex = 0,
            onSelect = { if (it == 1) onOpenGallery() },
        )
        CircleIconButton(PoseitIcons.add, "Create a pose", onClick = onCreate, size = PillBarItemSize)
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

/** Lines each grid tile reserves for its name: per row, the most any name in that row wraps to (1 or 2). */
private class RowNameLines(private val perItem: List<Int>, private val offset: Int) {
    /** The create tile in front of the poses, if there is one. */
    val leading: Int get() = if (offset > 0) perItem.firstOrNull() ?: 1 else 1

    fun forItem(index: Int): Int = perItem.getOrElse(index + offset) { 1 }
}

/**
 * Measures the tiles' names the way [com.poseit.app.ui.components.PoseTile] sets them (Body strong, at most two
 * lines, in the caption's width) and gives every tile of a row the line count of the row's longest name, so the
 * dark cards of a row end on one line without reserving an empty line under every short name.
 *
 * @param leadingName the caption of a tile placed before the poses (the create tile), or null
 * @param gridWidth the grid's width inside its content padding
 */
@Composable
private fun rememberRowNameLines(names: List<String>, leadingName: String?, gridWidth: Dp): RowNameLines {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    return remember(names, leadingName, gridWidth, density) {
        val gridPx = with(density) { gridWidth.roundToPx() }.coerceAtLeast(0)
        val spacingPx = with(density) { Space.edge.roundToPx() }
        val cells = with(PoseGridCells) { density.calculateCrossAxisCellSizes(gridPx, spacingPx) }
        val columns = cells.size.coerceAtLeast(1)
        // The caption's padding inside a pose tile: 16dp at the start, 12dp at the end.
        val textPx = with(density) { ((cells.minOrNull() ?: 0) - (Space.l + Space.m).roundToPx()) }.coerceAtLeast(1)
        val all = listOfNotNull(leadingName) + names
        val lines = all.map { name ->
            measurer.measure(name, PoseitType.bodyStrong, maxLines = 2, constraints = Constraints(maxWidth = textPx)).lineCount
        }
        val rows = lines.chunked(columns).map { row -> row.max() }
        RowNameLines(List(all.size) { rows[it / columns] }, offset = if (leadingName != null) 1 else 0)
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable LazyGridItemScope.() -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = key) { content() }
}

/** "24 poses" on the text inset, and "Clear filters" while anything narrows the list. */
@Composable
private fun ResultsHeader(count: Int, showClear: Boolean, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = ButtonHeight)
            // Text on the canvas lines up with text inside the cards; the button's label lands on the card padding.
            .padding(start = Space.cardPadding, end = Space.cardPadding - Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (count == 1) "1 pose" else "$count poses",
            style = PoseitType.supporting,
            color = PoseitColors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        AnimatedVisibility(
            visible = showClear,
            enter = fadeIn(PoseitMotion.standard(PoseitMotion.MEDIUM)),
            exit = fadeOut(tween(PoseitMotion.FAST, easing = PoseitMotion.EaseExit)),
        ) {
            TertiaryButton("Clear filters", onClick = onClear)
        }
    }
}

/**
 * The empty list as a dark card, like the mockups' challenge card: the title, one sentence and the one button that
 * fills the list (the screen's accent, since the Today's pose card is not shown with it).
 */
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
    val tab = state.filter.tab
    val content = when {
        state.tabTotal == 0 && tab == LibraryTab.FAVORITES -> EmptyCopy(
            title = "No favourites yet",
            message = "Tap the heart on any pose to keep it here for your next shoot.",
            action = "Browse poses",
            onAction = onShowAll,
        )

        state.tabTotal == 0 && tab == LibraryTab.RECENT -> EmptyCopy(
            title = "Nothing tried yet",
            message = "Poses you use in the camera show up here, so you can find them again quickly.",
            action = "Open the pose camera",
            onAction = onOpenCamera,
        )

        state.tabTotal == 0 && tab == LibraryTab.MINE -> EmptyCopy(
            title = "Make your first pose",
            message = "Turn any photo you love into a pose guide, or drag the mannequin into a pose of your own.",
            action = "Create a pose",
            onAction = onCreate,
        )

        state.tabTotal == 0 -> EmptyCopy(
            title = "No poses here",
            message = "Create your own pose from a photo or draw one with the mannequin.",
            action = "Draw a pose",
            onAction = onDraw,
        )

        else -> EmptyCopy(
            title = "No poses match",
            message = if (state.filter.query.isNotBlank()) {
                "Nothing found for “${state.filter.query.trim()}”. Try another word or clear the filters."
            } else {
                "Try a different combination of filters."
            },
            action = "Clear filters",
            onAction = onClearFilters,
        )
    }
    PoseitCard(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Space.cardPadding, vertical = Space.xs),
    ) {
        EmptyState(
            title = content.title,
            message = content.message,
            horizontalPadding = 0.dp,
            action = { AccentButton(content.action, onClick = content.onAction, form = ButtonForm.Block) },
        )
    }
}

private class EmptyCopy(val title: String, val message: String, val action: String, val onAction: () -> Unit)

/** Create a pose: a cream sheet with the two ways in as outlined rows, each saying what it does. */
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
    PoseitBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Full-height sheets would otherwise slide under the status bar (M3 1.3 only insets the bottom).
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = Space.edge, end = Space.edge, top = Space.s, bottom = Space.l),
        ) {
            Text(
                "Create a pose",
                style = PoseitType.title,
                color = PoseitColors.textPrimary,
                modifier = Modifier
                    .padding(horizontal = Space.cardPadding)
                    .semantics { heading() },
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                "Your poses work in the camera just like the built-in ones.",
                style = PoseitType.body,
                color = PoseitColors.textSecondary,
                modifier = Modifier.padding(horizontal = Space.cardPadding),
            )
            Spacer(Modifier.height(Space.l))
            OutlinedRow(
                title = "Pose from a photo",
                subtitle = "Pick a photo you like. Poseit traces the pose and turns it into a guide.",
                onClick = { closeThen(onFromPhoto) },
            )
            Spacer(Modifier.height(Space.edge))
            OutlinedRow(
                title = "Draw a pose",
                subtitle = "Drag the mannequin's joints into exactly the pose you have in mind.",
                onClick = { closeThen(onDraw) },
            )
        }
    }
}
