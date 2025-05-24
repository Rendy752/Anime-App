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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeHistoryScreen(
    currentRoute: String?,
    navController: NavHostController,
    mainState: MainState,
    historyState: EpisodeHistoryState,
    onAction: (EpisodeHistoryAction) -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val historyListState = rememberLazyListState()

    LaunchedEffect(currentRoute) {
        if (currentRoute == NavRoute.History.route) {
            onAction(EpisodeHistoryAction.FetchHistory)
        }
    }

    LaunchedEffect(mainState.isConnected, historyState.episodeHistoryResults) {
        if (mainState.isConnected && historyState.episodeHistoryResults is Resource.Error) {
            onAction(EpisodeHistoryAction.FetchHistory)
        }
    }

    Scaffold { paddingValues ->
        PullToRefreshBox(
            isRefreshing = historyState.isRefreshing,
            onRefresh = { onAction(EpisodeHistoryAction.ApplyFilters(historyState.queryState)) },
            modifier = Modifier.padding(paddingValues),
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
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                            .padding(end = 8.dp)
                    ) {
                        FilterContent(
                            modifier = Modifier.weight(1f),
                            queryState = historyState.queryState,
                            onAction = onAction
                        )
                        LimitAndPaginationSection(
                            isVisible = historyState.episodeHistoryResults is Resource.Success,
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
                        listState = historyListState,
                        state = historyState,
                        onAction = onAction
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    FilterContent(
                        queryState = historyState.queryState,
                        onAction = onAction
                    )
                    HistoryContent(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .weight(1f)
                            .fillMaxWidth(),
                        navController = navController,
                        listState = historyListState,
                        state = historyState,
                        onAction = onAction
                    )
                    LimitAndPaginationSection(
                        isVisible = historyState.episodeHistoryResults is Resource.Success,
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
}