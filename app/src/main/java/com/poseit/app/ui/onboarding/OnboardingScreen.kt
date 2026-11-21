package com.poseit.app.ui.onboarding

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.poseit.app.appContainer
import com.poseit.app.ui.components.AccentButton
import com.poseit.app.ui.components.Badge
import com.poseit.app.ui.components.CapsLabel
import com.poseit.app.ui.components.CardSegment
import com.poseit.app.ui.components.FillingCardStack
import com.poseit.app.ui.components.PoseitSpinner
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.components.TopBarContent
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 4

/**
 * First-run intro + camera permission explanation (docs/DESIGN.md, "Onboarding"): on the cream canvas, each page
 * is one connected stack of dark cards filling the screen, like the mockups' "Train your coordination." screen: the
 * top bar card (the wordmark and the step badge), fused to the headline card, fused to a tall picture card where
 * the lay figure shows the idea. Below the stack: "Next" / "Get started" in lavender and "Skip".
 */
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
    Surface(color = PoseitColors.backgroundBody, contentColor = PoseitColors.textPrimary, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            HorizontalPager(
                state = pagerState,
                // Each page's stack keeps the 8dp cream frame; the next one slides in beside it.
                contentPadding = PaddingValues(horizontal = Space.edge),
                pageSpacing = Space.edge * 2,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        page = page,
                        title = "Pick a pose to copy",
                        body = "Choose from original poses for one person, a couple or a group. Each pose is a " +
                            "figure you line up with in the camera.",
                    ) { FigurePicture() }

                    1 -> OnboardingPage(
                        page = page,
                        title = "Match the guide",
                        body = "The camera tracks your body and colours each limb of the guide, from red when it's " +
                            "off to green when it matches. A short hint says what to change.",
                    ) { MatchPicture() }

                    2 -> OnboardingPage(
                        page = page,
                        title = "Hold the pose",
                        body = "Poseit takes the photo for you after a short countdown. To start the timer " +
                            "yourself, raise a hand above your head.",
                    ) { CountdownPicture() }

                    else -> OnboardingPage(
                        page = page,
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
                    .padding(horizontal = Space.edge, vertical = Space.edge),
                horizontalArrangement = Arrangement.spacedBy(Space.edge),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isLast) {
                    TertiaryButton(
                        "Skip",
                        onClick = { scope.launch { pagerState.animateScrollToPage(PAGE_COUNT - 1) } },
                        // The label lines up with the text inside the cards above.
                        modifier = Modifier.padding(start = Space.cardPadding - Space.m),
                    )
                }
                AccentButton(
                    onClick = {
                        if (isLast) getStarted() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    enabled = !(isLast && finishing),
                    modifier = Modifier.weight(1f),
                ) {
                    if (isLast && finishing) {
                        PoseitSpinner(size = 20.dp, color = PoseitColors.onAccent)
                    } else {
                        CapsLabel(if (isLast) "Get started" else "Next")
                    }
                }
            }
        }
    }
}

/**
 * One page: the connected stack filling the page. The top bar card says where you are ("2 / 4"), the headline card
 * holds the headline in the display cut and the body in mist, and the picture card takes the rest of the height;
 * [note] is a smaller line at the foot of the picture.
 */
@Composable
private fun OnboardingPage(
    page: Int,
    title: String,
    body: String,
    note: String? = null,
    picture: @Composable BoxScope.() -> Unit,
) {
    FillingCardStack(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = Space.edge),
        // A large font pushes the stack past the bottom rather than crushing the picture.
        minFillHeight = 120.dp,
        cards = listOf<@Composable () -> Unit>(
            {
                TopBarContent(
                    "Poseit",
                    wordmark = true,
                    badge = {
                        Badge(
                            "${page + 1} / $PAGE_COUNT",
                            Modifier.clearAndSetSemantics { contentDescription = "Page ${page + 1} of $PAGE_COUNT" },
                        )
                    },
                )
            },
            {
                CardSegment(contentPadding = PaddingValues(start = Space.cardPadding, end = Space.cardPadding, top = Space.l, bottom = Space.cardPadding)) {
                    Text(
                        title,
                        style = PoseitType.display,
                        color = PoseitColors.textPrimaryInverted,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(Space.m))
                    Text(body, style = PoseitType.body, color = PoseitColors.textSecondaryInverted)
                }
            },
            {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                        content = picture,
                    )
                    if (note != null) {
                        Text(
                            note,
                            style = PoseitType.supporting,
                            color = PoseitColors.textSecondaryInverted,
                            modifier = Modifier.padding(start = Space.cardPadding, end = Space.cardPadding, bottom = Space.cardPadding - Space.xs),
                        )
                    }
                }
            },
        ),
    )
}
