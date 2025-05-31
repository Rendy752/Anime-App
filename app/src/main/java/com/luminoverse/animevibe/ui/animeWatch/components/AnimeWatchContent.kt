package com.luminoverse.animevibe.ui.animeWatch.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.animeWatch.WatchState
import com.luminoverse.animevibe.ui.animeWatch.PlayerUiState
import com.luminoverse.animevibe.ui.animeWatch.infoContent.InfoContentSection
import com.luminoverse.animevibe.ui.animeWatch.videoPlayer.VideoPlayerSection
import com.luminoverse.animevibe.ui.animeWatch.watchContent.WatchContentSection
import com.luminoverse.animevibe.ui.common.SkeletonBox
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.utils.media.HlsPlayerState
import com.luminoverse.animevibe.utils.resource.Resource

@Composable
fun AnimeWatchContent(
    navController: NavController,
    watchState: WatchState,
    isScreenOn: Boolean,
    isAutoPlayVideo: Boolean,
    playerUiState: PlayerUiState,
    hlsPlayerState: HlsPlayerState,
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
            val serverScrollState = rememberScrollState()
            Row(modifier = Modifier.fillMaxWidth()) {
                if (watchState.episodeDetailComplement is Resource.Success && mainState.isConnected) {
                    VideoPlayerSection(
                        updateStoredWatchState = updateStoredWatchState,
                        watchState = watchState,
                        isScreenOn = isScreenOn,
                        isAutoPlayVideo = isAutoPlayVideo,
                        episodes = episodeList,
                        episodeSourcesQuery = query,
                        handleSelectedEpisodeServer = { episodeSourcesQuery, isRefresh ->
                            handleSelectedEpisodeServer(
                                episodeSourcesQuery,
                                isRefresh
                            )
                        },
                        hlsPlayerState = hlsPlayerState,
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
                                    newEpisodeCount = watchState.newEpisodeCount,
                                    episodeSourcesQuery = query,
                                    serverScrollState = serverScrollState,
                                    handleSelectedEpisodeServer = {
                                        handleSelectedEpisodeServer(it, false)
                                    }
                                )
                            } else {
                                InfoContentSection(
                                    animeDetail = watchState.animeDetail,
                                    navController = navController
                                )
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
                            newEpisodeCount = watchState.newEpisodeCount,
                            episodeSourcesQuery = query,
                            serverScrollState = serverScrollState,
                            handleSelectedEpisodeServer = { handleSelectedEpisodeServer(it, false) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoContentSection(
                            animeDetail = watchState.animeDetail,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}