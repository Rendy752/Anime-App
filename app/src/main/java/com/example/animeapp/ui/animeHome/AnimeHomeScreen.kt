package com.example.animeapp.ui.animeHome

import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.animeapp.R
import com.example.animeapp.ui.animeHome.components.ContinueWatchingPopup
import com.example.animeapp.ui.animeHome.components.WatchRecentEpisodeGrid
import com.example.animeapp.ui.animeHome.components.WatchRecentEpisodeGridSkeleton
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeHomeScreen(
    currentRoute: String?,
    navController: NavHostController,
    isConnected: Boolean,
    isLandscape: Boolean
) {
    val viewModel: HomeViewModel = hiltViewModel()

    val watchRecentEpisode by viewModel.watchRecentEpisode.collectAsStateWithLifecycle()
    val continueWatchingEpisode by viewModel.continueWatchingEpisode.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val state = rememberPullToRefreshState()

    var isShowPopup by remember { mutableStateOf(false) }
    var isMinimized by remember { mutableStateOf(false) }
    var episodeData by remember {
        mutableStateOf<com.example.animeapp.models.EpisodeDetailComplement?>(
            null
        )
    }

    LaunchedEffect(currentRoute) {
        viewModel.fetchContinueWatchingEpisode()
    }

    LaunchedEffect(continueWatchingEpisode) {
        if (continueWatchingEpisode is Resource.Success) {
            episodeData = continueWatchingEpisode.data
            isShowPopup = episodeData != null
            if (isShowPopup) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isMinimized) {
                        isShowPopup = false
                    }
                }, 10000)
            }
        } else {
            isShowPopup = false
        }
    }

    LaunchedEffect(isConnected) {
        if (isConnected && watchRecentEpisode is Resource.Error) viewModel.getWatchRecentEpisode()
    }

    Scaffold { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.getWatchRecentEpisode() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = state,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = state
                )
            },
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                when (watchRecentEpisode) {
                    is Resource.Loading -> {
                        WatchRecentEpisodeGridSkeleton(isLandscape)
                    }

                    is Resource.Success -> {
                        watchRecentEpisode.data?.data?.let { recentEpisodes ->
                            WatchRecentEpisodeGrid(
                                watchRecentEpisodes = recentEpisodes,
                                isLandscape = isLandscape,
                                onItemClick = { episode ->
                                    navController.navigate("animeDetail/${episode.entry.title}/${episode.entry.mal_id}")
                                }
                            )
                        }
                    }

                    is Resource.Error -> {
                        if (isConnected) Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { MessageDisplay(stringResource(R.string.error_loading_data)) }
                        else Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { MessageDisplay(stringResource(R.string.no_internet_connection)) }
                    }
                }
                ContinueWatchingPopup(
                    isShowPopup = isShowPopup,
                    episode = episodeData,
                    onMinimize = {
                        isShowPopup = false
                        isMinimized = true
                    },
                    onRestore = {
                        isShowPopup = true
                        isMinimized = false
                    },
                    isMinimized = isMinimized
                )
            }
        }
    }
}