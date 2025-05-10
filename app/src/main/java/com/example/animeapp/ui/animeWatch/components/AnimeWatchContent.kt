package com.example.animeapp.ui.animeWatch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.ui.animeWatch.WatchState
import com.example.animeapp.ui.animeWatch.PlayerUiState
import com.example.animeapp.ui.animeWatch.infoContent.InfoContentSection
import com.example.animeapp.ui.animeWatch.videoPlayer.VideoPlayerSection
import com.example.animeapp.ui.animeWatch.watchContent.WatchContentSection
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.utils.Resource

@Composable
fun AnimeWatchContent(
    navController: NavController,
    watchState: WatchState,
    isScreenOn: Boolean,
    playerUiState: PlayerUiState,
    mainState: MainState,
    updateStoredWatchState: (Long?, Long?, String?) -> Unit,
    onLoadEpisodeDetailComplement: (String) -> Unit,
    scrollState: LazyListState,
    onEnterPipMode: () -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    onPlayerError: (String?) -> Unit,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery, Boolean) -> Unit,
    modifier: Modifier,
    videoSize: Modifier
) {
    watchState.animeDetailComplement?.episodes?.let { episodeList ->
        watchState.episodeSourcesQuery.let { query ->
            Row(modifier = Modifier.fillMaxWidth()) {
                if (watchState.episodeDetailComplement is Resource.Success && mainState.isConnected) {
                    VideoPlayerSection(
                        updateStoredWatchState = updateStoredWatchState,
                        watchState = watchState,
                        isScreenOn = isScreenOn,
                        episodes = episodeList,
                        episodeSourcesQuery = query,
                        handleSelectedEpisodeServer = { handleSelectedEpisodeServer(it, true) },
                        isPipMode = playerUiState.isPipMode,
                        onEnterPipMode = onEnterPipMode,
                        isFullscreen = playerUiState.isFullscreen,
                        onFullscreenChange = onFullscreenChange,
                        isLandscape = mainState.isLandscape,
                        onPlayerError = onPlayerError,
                        modifier = modifier,
                        videoSize = videoSize
                    )
                } else {
                    Box(modifier = modifier.then(videoSize)) {
                        SkeletonBox(modifier = Modifier.fillMaxSize())
                    }
                }

                if (mainState.isLandscape && !playerUiState.isPipMode && !playerUiState.isFullscreen) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxSize()
                            .weight(0.5f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        state = scrollState
                    ) {
                        item {
                            if (watchState.selectedContentIndex == 0) {
                                WatchContentSection(
                                    animeDetail = watchState.animeDetail,
                                    isFavorite = watchState.isFavorite,
                                    episodeDetailComplements = watchState.episodeDetailComplements,
                                    onLoadEpisodeDetailComplement = onLoadEpisodeDetailComplement,
                                    episodeDetailComplement = watchState.episodeDetailComplement,
                                    episodes = episodeList,
                                    episodeSourcesQuery = query,
                                    handleSelectedEpisodeServer = {
                                        handleSelectedEpisodeServer(it, false)
                                    }
                                )
                            } else {
                                InfoContentSection(animeDetail = watchState.animeDetail, navController = navController)
                            }
                        }
                    }
                }
            }
            if (!mainState.isLandscape && !playerUiState.isPipMode && !playerUiState.isFullscreen) {
                LazyColumn(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    state = scrollState
                ) {
                    item {
                        WatchContentSection(
                            animeDetail = watchState.animeDetail,
                            isFavorite = watchState.isFavorite,
                            episodeDetailComplements = watchState.episodeDetailComplements,
                            onLoadEpisodeDetailComplement = onLoadEpisodeDetailComplement,
                            episodeDetailComplement = watchState.episodeDetailComplement,
                            episodes = episodeList,
                            episodeSourcesQuery = query,
                            handleSelectedEpisodeServer = { handleSelectedEpisodeServer(it, false) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoContentSection(animeDetail = watchState.animeDetail, navController = navController)
                    }
                }
            }
        }
    }
}