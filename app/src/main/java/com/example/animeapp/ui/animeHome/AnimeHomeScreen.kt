package com.example.animeapp.ui.animeHome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.animeapp.R
import com.example.animeapp.models.animeSeasonNowResponsePlaceholder
import com.example.animeapp.models.episodeDetailComplementPlaceholder
import com.example.animeapp.ui.animeHome.components.ContinueWatchingPopup
import com.example.animeapp.ui.animeHome.components.AnimeSeasonNowGrid
import com.example.animeapp.ui.animeHome.components.AnimeSeasonNowGridSkeleton
import com.example.animeapp.ui.common_ui.LimitAndPaginationQueryState
import com.example.animeapp.ui.common_ui.LimitAndPaginationSection
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.ui.main.BottomScreen
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.utils.Resource
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AnimeHomeScreen(
    state: HomeState = HomeState(
        animeSeasonNows = Resource.Success(animeSeasonNowResponsePlaceholder),
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
        if (mainState.isConnected && state.animeSeasonNows is Resource.Error) action(HomeAction.GetAnimeSeasonNow)
    }

    Scaffold { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { action(HomeAction.GetAnimeSeasonNow) },
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
                when (state.animeSeasonNows) {
                    is Resource.Loading -> {
                        AnimeSeasonNowGridSkeleton(mainState.isLandscape)
                    }

                    is Resource.Success -> {
                        state.animeSeasonNows.data.let { animeSeasonNow ->
                            Column(modifier = Modifier.weight(1f)) {
                                AnimeSeasonNowGrid(
                                    animeSeasonNow = animeSeasonNow.data,
                                    isLandscape = mainState.isLandscape,
                                    onItemClick = { anime ->
                                        navController.navigate("animeDetail/${anime.title}/${anime.mal_id}")
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
                    isVisible = state.animeSeasonNows is Resource.Success,
                    pagination = state.animeSeasonNows.data?.pagination,
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