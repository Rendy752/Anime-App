package com.example.animeapp.ui.animeHome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.animeapp.models.episodeDetailComplementPlaceholder
import com.example.animeapp.models.listAnimeDetailResponsePlaceholder
import com.example.animeapp.ui.animeHome.components.AnimeSchedulesGrid
import com.example.animeapp.ui.animeHome.components.AnimeSchedulesGridSkeleton
import com.example.animeapp.ui.animeHome.components.FilterChipBar
import com.example.animeapp.ui.animeHome.components.TopAnimeCarousel
import com.example.animeapp.ui.animeHome.components.TopAnimeCarouselSkeleton
import com.example.animeapp.ui.common_ui.ContinueWatchingAnime
import com.example.animeapp.ui.common_ui.LimitAndPaginationQueryState
import com.example.animeapp.ui.common_ui.LimitAndPaginationSection
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.ui.main.navigation.NavRoute
import com.example.animeapp.ui.main.navigation.navigateTo
import com.example.animeapp.utils.Resource
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
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = !mainState.isLandscape,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 1000, easing = EaseInOut)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 1000, easing = EaseInOut)
                    )
                ) {
                    Column {
                        when (homeState.top10Anime) {
                            is Resource.Success -> {
                                TopAnimeCarousel(
                                    topAnimeList = homeState.top10Anime.data.data,
                                    currentCarouselPage = carouselState.currentCarouselPage,
                                    autoScrollEnabled = carouselState.autoScrollEnabled,
                                    carouselLastInteractionTime = carouselState.carouselLastInteractionTime,
                                    onPageChanged = { onAction(HomeAction.SetCurrentCarouselPage(it)) },
                                    onAutoScrollEnabledChanged = {
                                        onAction(HomeAction.SetAutoScrollEnabled(it))
                                    },
                                    onCarouselInteraction = { onAction(HomeAction.UpdateCarouselLastInteractionTime) },
                                    navController = navController
                                )
                            }

                            is Resource.Loading -> {
                                TopAnimeCarouselSkeleton()
                            }

                            is Resource.Error -> {
                                TopAnimeCarouselSkeleton(isError = true)
                            }
                        }
                        FilterChipBar(
                            queryState = homeState.queryState,
                            onApplyFilters = { onAction(HomeAction.ApplyFilters(it)) }
                        )
                    }
                }
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
                                        navController.navigateTo(NavRoute.AnimeDetail.fromId(anime.mal_id))
                                    }
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

                homeState.continueWatchingEpisode?.let { continueWatchingEpisode ->
                    if (homeState.isShowPopup) Popup(
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