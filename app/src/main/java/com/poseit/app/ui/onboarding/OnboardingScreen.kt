package com.poseit.app.ui.onboarding

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.poseit.app.appContainer
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 4

/** First-run intro + camera permission explanation. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val vm: OnboardingViewModel = viewModel(
        factory = viewModelFactory { initializer { OnboardingViewModel(appContext.appContainer.settingsRepository) } },
    )
    val finishing by vm.finishing.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    LaunchedEffect(done) { if (done) onDone() }

    // The result doesn't matter: the camera screen asks again (or explains) if it was denied.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.finish() }
    val getStarted: () -> Unit = {
        if (!finishing) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                vm.finish()
            } else {
                try {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                } catch (e: ActivityNotFoundException) {
                    vm.finish()
                }
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == PAGE_COUNT - 1
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    // A Surface (not a plain Box) so text gets the theme's on-background colour.
    Surface(color = Studio.Paper, contentColor = Studio.Ink, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            StepMarks(
                count = PAGE_COUNT,
                current = pagerState.currentPage,
                modifier = Modifier.padding(start = Space.gutter, top = Space.l, bottom = Space.l),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        title = "Pick a pose to copy",
                        body = "Choose from original poses for one person, a couple or a group. Each pose is a " +
                            "figure you line up with in the camera.",
                    ) { FigurePicture() }

                    1 -> OnboardingPage(
                        title = "Match the guide",
                        body = "The camera tracks your body and colours each limb of the guide, from red when it's " +
                            "off to green when it matches. A short hint says what to change.",
                    ) { MatchPicture() }

                    2 -> OnboardingPage(
                        title = "Hold the pose",
                        body = "Poseit takes the photo for you after a short countdown. To start the timer " +
                            "yourself, raise a hand above your head.",
                    ) { CountdownPicture() }

                    else -> OnboardingPage(
                        title = "Make your own poses",
                        body = "Turn a photo into a pose guide, or drag the figure's joints into a new pose.",
                        note = "Next, Poseit asks for the camera to show the guide over the live view. The camera " +
                            "feed and your photos stay on this phone.",
                    ) { PosingPicture() }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    // Skip's text lines up with the 20dp gutter (a text button pads its label by 12dp).
                    .padding(start = Space.gutter - 12.dp, end = Space.gutter, top = Space.l, bottom = Space.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isLast) {
                    TertiaryButton("Skip", onClick = { scope.launch { pagerState.animateScrollToPage(PAGE_COUNT - 1) } })
                }
                Spacer(Modifier.weight(1f))
                PrimaryButton(
                    onClick = {
                        if (isLast) getStarted() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    enabled = !(isLast && finishing),
                    modifier = Modifier.widthIn(min = 128.dp),
                ) {
                    if (isLast && finishing) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Studio.Ink, strokeWidth = 2.dp)
                    } else {
                        Text(if (isLast) "Get started" else "Next", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/**
 * One page: the picture on a Set tile that takes the free height, then the headline (Display) and the body,
 * left-aligned on the gutters. [note] is a smaller line under the body.
 */
@Composable
private fun OnboardingPage(
    title: String,
    body: String,
    note: String? = null,
    picture: @Composable BoxScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Space.gutter),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .clip(StudioShapes.Medium)
                .background(Studio.Set),
            contentAlignment = Alignment.Center,
            content = picture,
        )
        Spacer(Modifier.height(Space.xl))
        Text(
            title,
            style = StudioType.display,
            color = Studio.Ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Space.m))
        // A page with a note reserves fewer body lines, so the tile keeps about the same height on every page.
        Text(body, style = StudioType.body, color = Studio.Graphite, minLines = if (note == null) 4 else 2)
        if (note != null) {
            Spacer(Modifier.height(Space.m))
            Text(note, style = StudioType.label, color = Studio.Graphite)
        }
    }
}

/** Where the intro is: one short mark per page, Ink up to the current page and Hairline after it. */
@Composable
private fun StepMarks(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics { contentDescription = "Page ${current + 1} of $count" },
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        repeat(count) { i ->
            val color by animateColorAsState(
                if (i <= current) Studio.Ink else Studio.Hairline,
                animationSpec = tween(200),
                label = "stepMark",
            )
            Box(
                Modifier
                    .size(width = 24.dp, height = 2.dp)
                    .background(color),
            )
        }
    }
}
