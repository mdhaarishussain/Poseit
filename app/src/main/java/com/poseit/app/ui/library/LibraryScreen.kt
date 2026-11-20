package com.poseit.app.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.poseit.app.appContainer
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTag
import com.poseit.app.ui.theme.PoseitColors
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
        factory = viewModelFactory { initializer { LibraryViewModel(appContext.appContainer.poseRepository) } },
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val gridState = rememberLazyGridState()
    val fabExpanded by remember { derivedStateOf { gridState.firstVisibleItemIndex < 2 } }
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Poseit",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            brush = Brush.linearGradient(listOf(Color.White, Color(0xFFD9CCFF), PoseitColors.Coral)),
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                },
                actions = {
                    IconButton(onClick = onOpenGallery) { Icon(Icons.Filled.PhotoLibrary, contentDescription = "Your photos") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SmallFloatingActionButton(
                    onClick = { showCreateSheet = true },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create a pose")
                }
                ExtendedFloatingActionButton(
                    onClick = { onOpenCamera(null) },
                    expanded = fabExpanded,
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                    text = { Text("Pose camera") },
                    containerColor = PoseitColors.Violet,
                    contentColor = Color.White,
                )
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = PoseGridCells,
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 152.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            fullWidth("search") {
                LibrarySearchField(query = query, onQueryChange = onQueryChange)
            }
            if (state.showHero) {
                val hero = state.poseOfTheDay
                if (hero != null) {
                    fullWidth("hero") {
                        PoseOfTheDayCard(
                            pose = hero,
                            onOpen = { onOpenPose(hero.id) },
                            onTry = { onOpenCamera(hero.id) },
                            modifier = Modifier.animateItem(),
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
                )
            }
            if (state.poses.isNotEmpty()) {
                fullWidth("results_header") {
                    ResultsHeader(
                        count = state.poses.size,
                        showClear = state.filter.hasRefinements,
                        onClear = onClearFilters,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            if (state.filter.tab == LibraryTab.MINE && state.tabTotal > 0) {
                item(key = "create_card", contentType = "create") {
                    CreatePoseCard(
                        onClick = { showCreateSheet = true },
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .aspectRatio(0.68f),
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
                    modifier = Modifier.animateItem(),
                )
            }
            if (state.poses.isEmpty()) {
                fullWidth("empty") {
                    LibraryEmptyState(
                        modifier = Modifier.animateItem(),
                        state = state,
                        onClearFilters = onClearFilters,
                        onShowAll = { onSelectTab(LibraryTab.ALL) },
                        onOpenCamera = { onOpenCamera(null) },
                        onCreateFromPhoto = onCreateFromPhoto,
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

@Composable
private fun ResultsHeader(count: Int, showClear: Boolean, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (count == 1) "1 pose" else "$count poses",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        AnimatedVisibility(visible = showClear, enter = fadeIn(), exit = fadeOut()) {
            TextButton(onClick = onClear) { Text("Clear filters") }
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
    onCreateFromPhoto: () -> Unit,
    onDraw: () -> Unit,
) {
    val tab = state.filter.tab
    when {
        state.tabTotal == 0 && tab == LibraryTab.FAVORITES -> EmptyState(
            modifier = modifier,
            emoji = "💜",
            title = "No favourites yet",
            message = "Tap the heart on any pose to keep it here for your next shoot.",
        ) { Button(onClick = onShowAll) { Text("Browse poses") } }

        state.tabTotal == 0 && tab == LibraryTab.RECENT -> EmptyState(
            modifier = modifier,
            emoji = "🕒",
            title = "Nothing tried yet",
            message = "Poses you use in the camera show up here, so you can find them again quickly.",
        ) {
            Button(onClick = onOpenCamera) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open the pose camera")
            }
        }

        state.tabTotal == 0 && tab == LibraryTab.MINE -> EmptyState(
            modifier = modifier,
            emoji = "✨",
            title = "Make your first pose",
            message = "Turn any photo you love into a pose guide, or drag the mannequin into a pose of your own.",
        ) {
            Button(onClick = onCreateFromPhoto) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("From a photo")
            }
            OutlinedButton(onClick = onDraw) {
                Icon(Icons.Filled.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Draw your own")
            }
        }

        state.tabTotal == 0 -> EmptyState(
            modifier = modifier,
            emoji = "🧍",
            title = "No poses here",
            message = "Create your own pose from a photo or draw one with the mannequin.",
        ) { FilledTonalButton(onClick = onDraw) { Text("Draw a pose") } }

        else -> EmptyState(
            modifier = modifier,
            emoji = "🔍",
            title = "No poses match",
            message = if (state.filter.query.isNotBlank()) {
                "Nothing found for “${state.filter.query.trim()}”. Try another word or clear the filters."
            } else {
                "Try a different combination of filters."
            },
        ) { Button(onClick = onClearFilters) { Text("Clear filters") } }
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Create a pose", style = MaterialTheme.typography.titleLarge)
            Text(
                "Your poses work just like the built-in ones: match, coach and auto-capture.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            CreateOption(
                icon = Icons.Filled.AddAPhoto,
                accent = PoseitColors.Coral,
                title = "From a photo",
                subtitle = "Pick a photo you like. Poseit traces the pose and turns it into a guide.",
                onClick = { closeThen(onFromPhoto) },
            )
            CreateOption(
                icon = Icons.Filled.Draw,
                accent = PoseitColors.Mint,
                title = "Draw your own",
                subtitle = "Drag the mannequin's joints into exactly the pose you have in mind.",
                onClick = { closeThen(onDraw) },
            )
        }
    }
}

@Composable
private fun CreateOption(icon: ImageVector, accent: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
