package com.example.animeapp.ui.animeRecommendations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.animeapp.ui.animeRecommendations.recommendations.RecommendationItem
import com.example.animeapp.ui.animeRecommendations.recommendations.RecommendationItemSkeleton
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.utils.Navigation.navigateToAnimeDetail
import com.example.animeapp.utils.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeRecommendationsScreen(
    navController: NavHostController,
    mainState: MainState,
    recommendationsState: RecommendationsState,
    onAction: (RecommendationsAction) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val portraitScrollState = rememberLazyListState()
    val landscapeScrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTopPortrait by remember {
        derivedStateOf { portraitScrollState.firstVisibleItemIndex > 10 }
    }
    val showScrollToTopLandscape by remember {
        derivedStateOf { landscapeScrollState.firstVisibleItemIndex > 10 }
    }

    LaunchedEffect(mainState.isConnected, recommendationsState.animeRecommendations) {
        if (mainState.isConnected && recommendationsState.animeRecommendations is Resource.Error) {
            onAction(RecommendationsAction.LoadRecommendations)
        }
    }

    Scaffold { paddingValues ->
        PullToRefreshBox(
            isRefreshing = recommendationsState.isRefreshing,
            onRefresh = { onAction(RecommendationsAction.LoadRecommendations) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = recommendationsState.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullToRefreshState
                )
            }
        ) {
            when (recommendationsState.animeRecommendations) {
                is Resource.Loading -> {
                    if (!mainState.isLandscape) {
                        LazyColumn(state = portraitScrollState) {
                            items(3) { RecommendationItemSkeleton() }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxSize()) {
                            repeat(2) {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    state = if (it == 0) portraitScrollState else landscapeScrollState
                                ) {
                                    items(2) { RecommendationItemSkeleton() }
                                }
                            }
                        }
                    }
                }

                is Resource.Success -> {
                    recommendationsState.animeRecommendations.data.data.let { animeRecommendations ->
                        if (!mainState.isLandscape) {
                            Box {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    state = portraitScrollState
                                ) {
                                    items(animeRecommendations) {
                                        RecommendationItem(
                                            recommendation = it,
                                            onItemClick = { malId ->
                                                navController.navigateToAnimeDetail(malId)
                                            }
                                        )
                                    }
                                }
                                AnimatedVisibility(
                                    visible = showScrollToTopPortrait,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp),
                                    enter = scaleIn(initialScale = 0.0f),
                                    exit = scaleOut(targetScale = 0.0f)
                                ) {
                                    FloatingActionButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                portraitScrollState.animateScrollToItem(0)
                                            }
                                        },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Icon(
                                            Icons.Filled.ArrowUpward,
                                            contentDescription = "Scroll to top"
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                repeat(2) { columnIndex ->
                                    val listSize = animeRecommendations.size
                                    val itemsPerColumn = (listSize + 1) / 2
                                    val startIndex = columnIndex * itemsPerColumn
                                    val endIndex = minOf(startIndex + itemsPerColumn, listSize)
                                    val columnItems =
                                        animeRecommendations.subList(startIndex, endIndex)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            state = if (columnIndex == 0) portraitScrollState else landscapeScrollState
                                        ) {
                                            items(columnItems) {
                                                RecommendationItem(
                                                    recommendation = it,
                                                    onItemClick = { malId ->
                                                        navController.navigateToAnimeDetail(
                                                            malId
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = if (columnIndex == 0) showScrollToTopPortrait else showScrollToTopLandscape,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(16.dp),
                                            enter = scaleIn(initialScale = 0.0f),
                                            exit = scaleOut(targetScale = 0.0f)
                                        ) {
                                            FloatingActionButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val state =
                                                            if (columnIndex == 0) portraitScrollState else landscapeScrollState
                                                        state.animateScrollToItem(0)
                                                    }
                                                },
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ) {
                                                Icon(
                                                    Icons.Filled.ArrowUpward,
                                                    contentDescription = "Scroll to top"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is Resource.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        MessageDisplay(
                            recommendationsState.animeRecommendations.message
                                ?: "Error Loading Data"
                        )
                    }
                }
            }
        }
    }
}