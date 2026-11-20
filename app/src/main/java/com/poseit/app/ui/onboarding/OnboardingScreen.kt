package com.poseit.app.ui.onboarding

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.poseit.app.appContainer
import com.poseit.app.ui.theme.PoseitColors
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
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(TopGlow)
                .systemBarsPadding(),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        title = "Never wonder how to pose again",
                        body = "Pick a pose from a library of original poses for solo, couple and group photos, then line yourself up with the guide.",
                    ) { MannequinCarousel(Modifier.fillMaxSize()) }

                    1 -> OnboardingPage(
                        title = "Match the outline",
                        body = "Poseit sees your body through the camera, colours each limb from red to green and tells you exactly what to fix.",
                    ) { MatchDemo(Modifier.fillMaxSize()) }

                    2 -> OnboardingPage(
                        title = "Hands-free shots",
                        body = "No more running back and forth to the timer. Hold the pose and Poseit takes the picture for you.",
                    ) { HandsFreeDemo(Modifier.fillMaxSize()) }

                    else -> OnboardingPage(
                        title = "Make it yours",
                        body = "Turn photos you love into pose guides, or drag the mannequin into any pose you can imagine.",
                        footer = { CameraPermissionNote() },
                    ) { MakeItYoursDemo(Modifier.fillMaxSize()) }
                }
            }

            PageIndicator(
                count = PAGE_COUNT,
                current = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp),
            )

            AnimatedContent(
                targetState = isLast,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboardingButtons",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
            ) { last ->
                if (last) {
                    Button(
                        onClick = getStarted,
                        enabled = !finishing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PoseitColors.Violet, contentColor = Color.White),
                    ) {
                        if (finishing) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                        } else {
                            Text("Get started", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(PAGE_COUNT - 1) } }) {
                            Text("Skip")
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                            modifier = Modifier.height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PoseitColors.Violet, contentColor = Color.White),
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private val TopGlow = Brush.radialGradient(
    colors = listOf(PoseitColors.Violet.copy(alpha = 0.22f), Color.Transparent),
    center = androidx.compose.ui.geometry.Offset(0f, 0f),
    radius = 1400f,
)

@Composable
private fun OnboardingPage(
    title: String,
    body: String,
    footer: (@Composable () -> Unit)? = null,
    visual: @Composable BoxScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(top = 16.dp, bottom = 20.dp),
            contentAlignment = Alignment.Center,
            content = visual,
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (footer != null) {
            Spacer(Modifier.height(16.dp))
            footer()
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics { contentDescription = "Page ${current + 1} of $count" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val selected = i == current
            val width by animateDpAsState(if (selected) 26.dp else 8.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (selected) PoseitColors.Violet else MaterialTheme.colorScheme.outline,
                label = "dotColor",
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun CameraPermissionNote() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PoseitColors.Mint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = PoseitColors.Mint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            "Next, Poseit asks for your camera to show the live guide. Everything is processed on your phone: " +
                "no uploads, no account, no ads.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
