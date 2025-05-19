package com.luminoverse.animevibe.ui.animeHome.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import kotlinx.coroutines.delay
import java.util.Date

@Composable
fun TopAnimeCarousel(
    modifier: Modifier = Modifier,
    topAnimeList: List<AnimeDetail>,
    currentCarouselPage: Int,
    autoScrollEnabled: Boolean,
    carouselLastInteractionTime: Long,
    onPageChanged: (Int) -> Unit,
    onAutoScrollEnabledChanged: (Boolean) -> Unit,
    onCarouselInteraction: () -> Unit,
    navController: NavHostController,
    scrollProgress: Float
) {
    if (topAnimeList.isNotEmpty()) {
        val topAnimeCount = topAnimeList.size
        val pagerState = rememberPagerState(
            pageCount = { Int.MAX_VALUE },
            initialPage = currentCarouselPage
        )

        val colorOverlay by animateColorAsState(
            targetValue = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f * scrollProgress),
            animationSpec = tween(durationMillis = 300, easing = EaseInOut),
            label = "carousel_blue_tint"
        )

        val blurRadius by animateFloatAsState(
            targetValue = 10f * scrollProgress,
            animationSpec = tween(durationMillis = 300, easing = EaseInOut),
            label = "carousel_blur"
        )

        LaunchedEffect(autoScrollEnabled) {
            if (autoScrollEnabled && topAnimeCount > 1) {
                while (true) {
                    delay(3000)
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % topAnimeCount)
                }
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            onPageChanged(pagerState.currentPage)
        }

        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                val currentTime = Date().time
                if (!autoScrollEnabled && currentTime - carouselLastInteractionTime >= 5000) {
                    onAutoScrollEnabledChanged(true)
                }
            }
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    renderEffect = BlurEffect(
                        radiusX = blurRadius,
                        radiusY = blurRadius,
                    )
                }
                .background(colorOverlay)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            onAutoScrollEnabledChanged(false)
                            onCarouselInteraction()
                        },
                        onDrag = { _, _ -> onCarouselInteraction() },
                        onDragEnd = { onCarouselInteraction() }
                    )
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                key = { index -> topAnimeList[index % topAnimeCount].mal_id }
            ) { page ->
                val index = page % topAnimeCount
                val animeDetail = topAnimeList[index]
                Box(modifier = Modifier.fillMaxWidth()) {
                    TopAnimeItem(animeDetail = animeDetail, onItemClick = {
                        navController.navigateTo(NavRoute.AnimeDetail.fromId(animeDetail.mal_id))
                    })
                }
            }
        }
    }
}

@Preview
@Composable
fun TopAnimeCarouselSkeleton(isError: Boolean = false) {
    val topAnimeCount = if (isError) 1 else 10
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(topAnimeCount) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (isError) TopAnimeItemError()
                    else TopAnimeItemSkeleton()
                }
            }
        }
    }
}