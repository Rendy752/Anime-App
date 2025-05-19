package com.luminoverse.animevibe.ui.animeHome

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.luminoverse.animevibe.models.episodeDetailComplementPlaceholder
import com.luminoverse.animevibe.models.listAnimeDetailResponsePlaceholder
import com.luminoverse.animevibe.ui.animeHome.components.AnimeSchedulesGrid
import com.luminoverse.animevibe.ui.animeHome.components.AnimeSchedulesGridSkeleton
import com.luminoverse.animevibe.ui.animeHome.components.FilterChipBar
import com.luminoverse.animevibe.ui.animeHome.components.TopAnimeCarousel
import com.luminoverse.animevibe.ui.animeHome.components.TopAnimeCarouselSkeleton
import com.luminoverse.animevibe.ui.common.ContinueWatchingAnime
import com.luminoverse.animevibe.ui.common.LimitAndPaginationQueryState
import com.luminoverse.animevibe.ui.common.LimitAndPaginationSection
import com.luminoverse.animevibe.ui.common.MessageDisplay
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import com.luminoverse.animevibe.utils.Resource
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AnimeHomeScreen(
    homeState: HomeState = HomeState(
        animeSchedules = Resource.Success(listAnimeDetailResponsePlaceholder),
        continueWatchingEpisode = episodeDetailComplementPlaceholder,
        isShowPopup = true
    ),
    carouselState: CarouselState = CarouselState(),
    remainingTimes: Map<String, String> = emptyMap(),
    onAction: (HomeAction) -> Unit = {},
    mainState: MainState = MainState(),
    currentRoute: String? = NavRoute.Home.route,
    navController: NavHostController = rememberNavController()
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val density = LocalDensity.current
    val maxScrollHeightPx = with(density) { 50.dp.toPx() }
    val gridState = rememberLazyGridState()
    val scrollOffsetPx by remember {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex > 0) {
                maxScrollHeightPx
            } else {
                gridState.firstVisibleItemScrollOffset.toFloat()
            }
        }
    }

    val scrollProgress = remember(scrollOffsetPx, maxScrollHeightPx) {
        (scrollOffsetPx / maxScrollHeightPx).coerceIn(0f, 1f)
    }

    val carouselHeight by animateDpAsState(
        targetValue = 200.dp - (maxScrollHeightPx.dp * scrollProgress),
        animationSpec = tween(durationMillis = 300, easing = EaseInOut),
        label = "carousel_height"
    )

    LaunchedEffect(currentRoute) {
        if (currentRoute == NavRoute.Home.route) {
            onAction(HomeAction.FetchContinueWatchingEpisode)
        }
    }

    LaunchedEffect(homeState.isMinimized) {
        if (!homeState.isMinimized) {
            delay(10000)
            onAction(HomeAction.SetMinimized(true))
        }
    }

    LaunchedEffect(mainState.isConnected) {
        if (!mainState.isConnected) return@LaunchedEffect

        if (homeState.animeSchedules is Resource.Error) onAction(HomeAction.GetAnimeSchedules)
        if (homeState.top10Anime is Resource.Error) onAction(HomeAction.GetTop10Anime)
    }

    Scaffold { paddingValues ->
        PullToRefreshBox(
            isRefreshing = homeState.isRefreshing,
            onRefresh = { onAction(HomeAction.GetAnimeSchedules) },
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(paddingValues),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = homeState.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullToRefreshState
                )
            }
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy((-16).dp)) {
                when (homeState.top10Anime) {
                    is Resource.Success -> {
                        TopAnimeCarousel(
                            modifier = Modifier.height(carouselHeight),
                            topAnimeList = homeState.top10Anime.data.data,
                            currentCarouselPage = carouselState.currentCarouselPage,
                            autoScrollEnabled = carouselState.autoScrollEnabled,
                            carouselLastInteractionTime = carouselState.carouselLastInteractionTime,
                            onPageChanged = { onAction(HomeAction.SetCurrentCarouselPage(it)) },
                            onAutoScrollEnabledChanged = {
                                onAction(HomeAction.SetAutoScrollEnabled(it))
                            },
                            onCarouselInteraction = { onAction(HomeAction.UpdateCarouselLastInteractionTime) },
                            navController = navController,
                            scrollProgress = scrollProgress
                        )
                    }

                    is Resource.Loading -> {
                        TopAnimeCarouselSkeleton()
                    }

                    is Resource.Error -> {
                        TopAnimeCarouselSkeleton(isError = true)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    FilterChipBar(
                        queryState = homeState.queryState,
                        onApplyFilters = { onAction(HomeAction.ApplyFilters(it)) }
                    )
                    when (homeState.animeSchedules) {
                        is Resource.Loading -> {
                            AnimeSchedulesGridSkeleton(mainState.isLandscape)
                        }

                        is Resource.Success -> {
                            homeState.animeSchedules.data.let { animeSchedules ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    AnimeSchedulesGrid(
                                        animeSchedules = animeSchedules.data,
                                        remainingTimes = remainingTimes,
                                        isLandscape = mainState.isLandscape,
                                        onItemClick = { anime ->
                                            navController.navigateTo(
                                                NavRoute.AnimeDetail.fromId(anime.mal_id)
                                            )
                                        },
                                        gridState = gridState
                                    )
                                }
                            }
                        }

                        is Resource.Error -> {
                            if (mainState.isConnected) Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { MessageDisplay("Error Loading Data") }
                            else Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { MessageDisplay("No internet connection") }
                        }
                    }
                    LimitAndPaginationSection(
                        isVisible = homeState.animeSchedules is Resource.Success && !mainState.isLandscape,
                        pagination = homeState.animeSchedules.data?.pagination,
                        query = LimitAndPaginationQueryState(
                            homeState.queryState.page,
                            homeState.queryState.limit
                        ),
                        onQueryChanged = {
                            onAction(
                                HomeAction.ApplyFilters(
                                    homeState.queryState.copy(page = it.page, limit = it.limit)
                                )
                            )
                        }
                    )
                }

                homeState.continueWatchingEpisode?.let { continueWatchingEpisode ->
                    if (homeState.isShowPopup && currentRoute == NavRoute.Home.route && !mainState.isLandscape) Popup(
                        alignment = Alignment.BottomEnd,
                        offset = IntOffset(0, (-200).dp.value.toInt()),
                    ) {
                        ContinueWatchingAnime(
                            episodeDetailComplement = continueWatchingEpisode,
                            isMinimized = homeState.isMinimized,
                            onSetMinimize = { onAction(HomeAction.SetMinimized(it)) },
                            onTitleClick = { navController.navigateTo(NavRoute.AnimeDetail.fromId(it)) },
                            onEpisodeClick = { malId, episodeId ->
                                navController.navigateTo(
                                    NavRoute.AnimeWatch.fromParams(
                                        malId = malId, episodeId = episodeId
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}