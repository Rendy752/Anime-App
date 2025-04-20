package com.example.animeapp.ui.animeHome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.animeapp.R
import com.example.animeapp.models.animeSchedulesResponsePlaceholder
import com.example.animeapp.models.episodeDetailComplementPlaceholder
import com.example.animeapp.ui.animeHome.components.ContinueWatchingPopup
import com.example.animeapp.ui.animeHome.components.AnimeSchedulesGrid
import com.example.animeapp.ui.animeHome.components.AnimeSchedulesGridSkeleton
import com.example.animeapp.ui.common_ui.FilterChipView
import com.example.animeapp.ui.common_ui.LimitAndPaginationQueryState
import com.example.animeapp.ui.common_ui.LimitAndPaginationSection
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.utils.TimeUtils.getDayOfWeekList
import com.example.animeapp.ui.main.BottomScreen
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.utils.Navigation.navigateToAnimeDetail
import com.example.animeapp.utils.Resource
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AnimeHomeScreen(
    state: HomeState = HomeState(
        animeSchedules = Resource.Success(animeSchedulesResponsePlaceholder),
        continueWatchingEpisode = episodeDetailComplementPlaceholder,
        isShowPopup = true
    ),
    mainState: MainState = MainState(),
    action: (HomeAction) -> Unit = {},
    currentRoute: String? = BottomScreen.Home.route,
    navController: NavHostController = rememberNavController()
) {
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(currentRoute) {
        if (currentRoute == BottomScreen.Home.route) action(HomeAction.FetchContinueWatchingEpisode)
    }

    LaunchedEffect(state.isMinimized) {
        if (!state.isMinimized) delay(10000)
        action(HomeAction.SetMinimized(true))
    }

    LaunchedEffect(mainState.isConnected) {
        if (mainState.isConnected && state.animeSchedules is Resource.Error) action(HomeAction.GetAnimeSchedules)
    }

    Scaffold { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { action(HomeAction.GetAnimeSchedules) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = state.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullToRefreshState
                )
            },
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp, Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChipView(
                            text = "All",
                            checked = state.queryState.filter == null,
                            useDisabled = true,
                            onCheckedChange = {
                                action(
                                    HomeAction.ApplyFilters(
                                        state.queryState.copy(filter = null, page = 1)
                                    )
                                )
                            }
                        )
                        getDayOfWeekList().forEach { dayName ->
                            FilterChipView(
                                text = dayName,
                                checked = dayName == state.queryState.filter,
                                useDisabled = true,
                                onCheckedChange = {
                                    action(
                                        HomeAction.ApplyFilters(
                                            state.queryState.copy(filter = dayName, page = 1)
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
                when (state.animeSchedules) {
                    is Resource.Loading -> {
                        AnimeSchedulesGridSkeleton(mainState.isLandscape)
                    }

                    is Resource.Success -> {
                        state.animeSchedules.data.let { animeSchedules ->
                            Column(modifier = Modifier.weight(1f)) {
                                AnimeSchedulesGrid(
                                    animeSchedules = animeSchedules.data,
                                    isLandscape = mainState.isLandscape,
                                    onItemClick = { anime ->
                                        navController.navigateToAnimeDetail(
                                            anime.title, anime.mal_id
                                        )
                                    }
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                        if (mainState.isConnected) Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { MessageDisplay(stringResource(R.string.error_loading_data)) }
                        else Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { MessageDisplay(stringResource(R.string.no_internet_connection)) }
                    }
                }
                LimitAndPaginationSection(
                    isVisible = state.animeSchedules is Resource.Success && !mainState.isLandscape,
                    pagination = state.animeSchedules.data?.pagination,
                    query = LimitAndPaginationQueryState(
                        state.queryState.page,
                        state.queryState.limit
                    ),
                    onQueryChanged = {
                        action(
                            HomeAction.ApplyFilters(
                                state.queryState.copy(page = it.page, limit = it.limit)
                            )
                        )
                    }
                )
                if (state.isShowPopup) ContinueWatchingPopup(
                    episodeDetailComplement = state.continueWatchingEpisode,
                    isMinimized = state.isMinimized,
                    onSetMinimize = { action(HomeAction.SetMinimized(it)) }
                )
            }
        }
    }
}