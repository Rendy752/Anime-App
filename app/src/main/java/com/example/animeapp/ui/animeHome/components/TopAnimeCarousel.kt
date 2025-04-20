package com.example.animeapp.ui.animeHome.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.utils.Navigation.navigateToAnimeDetail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun TopAnimeCarousel(topAnimeList: List<AnimeDetail>, navController: NavHostController) {
    if (topAnimeList.isNotEmpty()) {
        val topAnimeCount = topAnimeList.size
        val pagerState = rememberPagerState(
            pageCount = { Int.MAX_VALUE },
            initialPage = 0
        )
        val coroutineScope = rememberCoroutineScope()
        var isAutoScrollingEnabled by remember { mutableStateOf(true) }
        val lastInteractionTime = remember { mutableLongStateOf(Date().time) }

        LaunchedEffect(isAutoScrollingEnabled) {
            if (isAutoScrollingEnabled && topAnimeCount > 1) {
                while (true) {
                    delay(3000)
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % topAnimeCount)
                }
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            coroutineScope.launch {
                delay(5000)
                val currentTime = Date().time
                if (currentTime - lastInteractionTime.longValue >= 5000 && !isAutoScrollingEnabled) {
                    isAutoScrollingEnabled = true
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isAutoScrollingEnabled = false
                            lastInteractionTime.longValue = Date().time
                        },
                        onDrag = { _, _ -> lastInteractionTime.longValue = Date().time },
                        onDragEnd = { lastInteractionTime.longValue = Date().time }
                    )
                },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                key = { index -> topAnimeList[index % topAnimeCount].mal_id }
            ) { page ->
                val index = page % topAnimeCount
                val animeDetail = topAnimeList[index]
                Box(modifier = Modifier.fillMaxWidth()) {
                    TopAnimeItem(animeDetail = animeDetail, onItemClick = { title, malId ->
                        navController.navigateToAnimeDetail(title, malId)
                    })
                }
            }
        }

        if (topAnimeCount > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(topAnimeCount) { index ->
                    val color =
                        if (pagerState.currentPage % topAnimeCount == index) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    Spacer(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(topAnimeCount) { index ->
                val color = if (index == 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                Spacer(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}