package com.pdfapp.reader.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vtsoft.pdfapp.reader.R
import com.pdfapp.reader.ui.theme.Accent

private data class OnboardingPage(val imageRes: Int, val titleRes: Int, val descRes: Int)

private val pages = listOf(
    OnboardingPage(R.drawable.onboarding_1, R.string.onboarding_title_1, R.string.onboarding_desc_1),
    OnboardingPage(R.drawable.onboarding_2, R.string.onboarding_title_2, R.string.onboarding_desc_2),
    OnboardingPage(R.drawable.onboarding_3, R.string.onboarding_title_3, R.string.onboarding_desc_3)
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val vm: OnboardingViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isDark = isSystemInDarkTheme()

    // Sync pager -> VM (user swipe updates VM)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            vm.setPage(page)
        }
    }
    // Sync VM -> pager (programmatic navigation)
    LaunchedEffect(state.currentPage) {
        if (pagerState.settledPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }
    LaunchedEffect(state.finished) {
        if (state.finished) onFinish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top half: pager illustration + skip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                
                // Floating animation logic
                val infiniteTransition = rememberInfiniteTransition()
                val offsetY by infiniteTransition.animateFloat(
                    initialValue = -16f,
                    targetValue = 16f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Image(
                    painter = painterResource(pages[pageIndex].imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .offset(y = offsetY.dp)
                )
            }
            TextButton(
                onClick = { vm.skip() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(stringResource(R.string.onboarding_skip), color = Accent)
            }
        }

        // Bottom half: text + indicators + button
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(pages[state.currentPage].titleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(pages[state.currentPage].descRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            // Dot indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (i == state.currentPage) 24.dp else 8.dp,
                                height = 8.dp
                            )
                            .background(
                                color = if (i == state.currentPage) Accent else Accent.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { vm.nextPage() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Text(
                    text = if (state.currentPage == pages.size - 1)
                        stringResource(R.string.onboarding_get_started)
                    else
                        stringResource(R.string.onboarding_next),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
