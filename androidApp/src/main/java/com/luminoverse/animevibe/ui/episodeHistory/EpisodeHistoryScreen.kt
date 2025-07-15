package com.luminoverse.animevibe.ui.episodeHistory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.ui.common.LimitAndPaginationQueryState
import com.luminoverse.animevibe.ui.common.LimitAndPaginationSection
import com.luminoverse.animevibe.ui.episodeHistory.components.FilterContent
import com.luminoverse.animevibe.ui.episodeHistory.components.HistoryContent
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.utils.resource.Resource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.unit.Dp
import com.luminoverse.animevibe.ui.common.SharedImageState
import com.luminoverse.animevibe.ui.main.SnackbarMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeHistoryScreen(
    currentRoute: String?,
    navController: NavHostController,
    playEpisode: (Int, String) -> Unit,
    rememberedTopPadding: Dp,
    showSnackbar: (SnackbarMessage) -> Unit,
    mainState: MainState,
    showImagePreview: (SharedImageState) -> Unit,
    historyState: EpisodeHistoryState,
    onAction: (EpisodeHistoryAction) -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val historyListState = rememberLazyListState()

    LaunchedEffect(currentRoute) {
        if (currentRoute == NavRoute.History.route) {
            onAction(EpisodeHistoryAction.CheckIfHistoryIsEmpty)
            onAction(EpisodeHistoryAction.FetchHistory)
        }
    }

    LaunchedEffect(mainState.networkStatus.isConnected, historyState.paginatedHistory) {
        if (mainState.networkStatus.isConnected && historyState.paginatedHistory is Resource.Error) {
            onAction(EpisodeHistoryAction.FetchHistory)
        }
    }

    PullToRefreshBox(
        modifier = Modifier.padding(top = rememberedTopPadding + 8.dp),
        isRefreshing = historyState.isRefreshing,
        onRefresh = { onAction(EpisodeHistoryAction.FetchHistory) },
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                isRefreshing = historyState.isRefreshing,
                containerColor = MaterialTheme.colorScheme.primary,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullToRefreshState
            )
        }
    ) {
        if (mainState.isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .padding(end = 8.dp)
                ) {
                    FilterContent(
                        modifier = Modifier.weight(1f),
                        isResultEmpty = (historyState.paginatedHistory as? Resource.Success)?.data.isNullOrEmpty(),
                        queryState = historyState.queryState,
                        onAction = onAction
                    )
                    LimitAndPaginationSection(
                        isVisible = historyState.paginatedHistory is Resource.Success,
                        pagination = historyState.pagination,
                        query = LimitAndPaginationQueryState(
                            page = historyState.queryState.page,
                            limit = historyState.queryState.limit
                        ),
                        onQueryChanged = { updatedQuery ->
                            if (updatedQuery.page != historyState.queryState.page) {
                                onAction(EpisodeHistoryAction.ChangePage(updatedQuery.page))
                            } else if (updatedQuery.limit != historyState.queryState.limit) {
                                onAction(
                                    EpisodeHistoryAction.ApplyFilters(
                                        historyState.queryState.copy(
                                            page = 1,
                                            limit = updatedQuery.limit
                                                ?: historyState.queryState.limit
                                        )
                                    )
                                )
                            }
                        },
                        useHorizontalPager = false
                    )
                }

                VerticalDivider(modifier = Modifier.fillMaxHeight())

                HistoryContent(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    navController = navController,
                    playEpisode = playEpisode,
                    showSnackbar = showSnackbar,
                    showImagePreview = showImagePreview,
                    listState = historyListState,
                    state = historyState,
                    onAction = onAction
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                FilterContent(
                    queryState = historyState.queryState,
                    isResultEmpty = (historyState.paginatedHistory as? Resource.Success)?.data.isNullOrEmpty(),
                    onAction = onAction
                )
                HistoryContent(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .weight(1f)
                        .fillMaxWidth(),
                    navController = navController,
                    playEpisode = playEpisode,
                    showSnackbar = showSnackbar,
                    showImagePreview = showImagePreview,
                    listState = historyListState,
                    state = historyState,
                    onAction = onAction
                )
                LimitAndPaginationSection(
                    isVisible = historyState.paginatedHistory is Resource.Success,
                    pagination = historyState.pagination,
                    query = LimitAndPaginationQueryState(
                        page = historyState.queryState.page,
                        limit = historyState.queryState.limit
                    ),
                    onQueryChanged = { updatedQuery ->
                        if (updatedQuery.page != historyState.queryState.page) {
                            onAction(EpisodeHistoryAction.ChangePage(updatedQuery.page))
                        } else if (updatedQuery.limit != historyState.queryState.limit) {
                            onAction(
                                EpisodeHistoryAction.ApplyFilters(
                                    historyState.queryState.copy(
                                        page = 1,
                                        limit = updatedQuery.limit
                                            ?: historyState.queryState.limit
                                    )
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}