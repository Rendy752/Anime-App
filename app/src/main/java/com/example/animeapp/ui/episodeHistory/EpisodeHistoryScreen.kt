package com.example.animeapp.ui.episodeHistory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.animeapp.ui.episodeHistory.components.FilterContent
import com.example.animeapp.ui.episodeHistory.components.HistoryContent
import com.example.animeapp.ui.main.MainState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeHistoryScreen(
    navController: NavHostController,
    mainState: MainState,
    historyState: EpisodeHistoryState,
    onAction: (EpisodeHistoryAction) -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(mainState.isConnected, historyState.episodeHistoryResults) {
        if (mainState.isConnected && historyState.episodeHistoryResults is com.example.animeapp.utils.Resource.Error) {
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
            Column(modifier = Modifier.fillMaxSize()) {
                FilterContent(
                    queryState = historyState.queryState,
                    onAction = onAction
                )
                HistoryContent(
                    navController = navController,
                    state = historyState,
                    onAction = onAction
                )
            }
        }
    }
}