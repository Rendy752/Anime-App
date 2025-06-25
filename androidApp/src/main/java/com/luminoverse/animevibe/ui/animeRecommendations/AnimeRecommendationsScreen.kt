package com.luminoverse.animevibe.ui.animeRecommendations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.ui.animeRecommendations.components.RecommendationItem
import com.luminoverse.animevibe.ui.animeRecommendations.components.RecommendationItemSkeleton
import com.luminoverse.animevibe.ui.common.SomethingWentWrongDisplay
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import com.luminoverse.animevibe.utils.resource.Resource
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

    val density = LocalDensity.current
    val statusBarPadding = with(density) {
        WindowInsets.systemBars.getTop(density).toDp()
    }

    LaunchedEffect(mainState.isConnected, recommendationsState.animeRecommendations) {
        if (mainState.isConnected && recommendationsState.animeRecommendations is Resource.Error) {
            onAction(RecommendationsAction.LoadRecommendations)
        }
    }

    PullToRefreshBox(
        isRefreshing = recommendationsState.isRefreshing,
        onRefresh = { onAction(RecommendationsAction.LoadRecommendations) },
        modifier = Modifier.fillMaxSize(),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            when (recommendationsState.animeRecommendations) {
                is Resource.Loading -> {
                    if (!mainState.isLandscape) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            state = portraitScrollState
                        ) {
                            itemsIndexed((0 until 3).toList()) { index, _ ->
                                RecommendationItemSkeleton(
                                    modifier = if (index == 0) Modifier.padding(top = statusBarPadding) else Modifier
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(2) { columnIndex ->
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    state = if (columnIndex == 0) portraitScrollState else landscapeScrollState
                                ) {
                                    itemsIndexed((0 until 2).toList()) { index, _ ->
                                        RecommendationItemSkeleton(
                                            modifier = if (index == 0) Modifier.padding(top = statusBarPadding) else Modifier
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is Resource.Success -> {
                    recommendationsState.animeRecommendations.data.data.let { animeRecommendations ->
                        if (!mainState.isLandscape) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                state = portraitScrollState
                            ) {
                                itemsIndexed(animeRecommendations) { index, recommendation ->
                                    RecommendationItem(
                                        recommendation = recommendation,
                                        onItemClick = { malId ->
                                            navController.navigateTo(
                                                NavRoute.AnimeDetail.fromId(
                                                    malId
                                                )
                                            )
                                        },
                                        modifier = if (index == 0) Modifier.padding(top = statusBarPadding) else Modifier
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
                        } else {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            state = if (columnIndex == 0) portraitScrollState else landscapeScrollState
                                        ) {
                                            itemsIndexed(columnItems) { index, recommendation ->
                                                RecommendationItem(
                                                    recommendation = recommendation,
                                                    onItemClick = { malId ->
                                                        navController.navigateTo(
                                                            NavRoute.AnimeDetail.fromId(malId)
                                                        )
                                                    },
                                                    modifier = if (index == 0) Modifier.padding(
                                                        top = statusBarPadding
                                                    ) else Modifier
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
                        SomethingWentWrongDisplay(
                            message = if (mainState.isConnected) recommendationsState.animeRecommendations.message else "No internet connection",
                            suggestion = if (mainState.isConnected) null else "Please check your internet connection and try again"
                        )
                    }
                }
            }
        }
    }
}