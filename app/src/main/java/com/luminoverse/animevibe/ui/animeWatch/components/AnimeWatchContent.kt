package com.luminoverse.animevibe.ui.animeWatch.components

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.luminoverse.animevibe.ui.animeWatch.WatchState
import com.luminoverse.animevibe.ui.animeWatch.PlayerUiState
import com.luminoverse.animevibe.ui.animeWatch.WatchAction
import com.luminoverse.animevibe.ui.animeWatch.videoPlayer.VideoPlayerSection
import com.luminoverse.animevibe.ui.animeWatch.watchContent.WatchContentSection
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.media.PositionState
import com.luminoverse.animevibe.utils.resource.Resource
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AnimeWatchContent(
    navController: NavController,
    watchState: WatchState,
    isConnected: Boolean,
    isScreenOn: Boolean,
    isAutoPlayVideo: Boolean,
    playerUiState: PlayerUiState,
    mainState: MainState,
    playerCoreState: PlayerCoreState,
    controlsState: StateFlow<ControlsState>,
    positionState: StateFlow<PositionState>,
    dispatchPlayerAction: (HlsPlayerAction) -> Unit,
    getPlayer: () -> ExoPlayer?,
    onHandleBackPress: () -> Unit,
    onAction: (WatchAction) -> Unit,
    scrollState: LazyListState,
    onEnterPipMode: () -> Unit,
    modifier: Modifier,
    videoSize: Modifier
) {
    val serverScrollState = rememberScrollState()
    LaunchedEffect(watchState.errorMessage) {
        if (watchState.errorMessage != null) {
            onAction(WatchAction.SetErrorMessage(watchState.errorMessage))
            onAction(WatchAction.SetIsLoading(false))
            Log.d("VideoPlayerSection", "Error from watchState: ${watchState.errorMessage}")
        }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = modifier
                .then(videoSize)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            if (watchState.animeDetailComplement == null || watchState.animeDetailComplement.episodes == null || watchState.episodeSourcesQuery == null || watchState.episodeDetailComplement !is Resource.Success) Box(
                modifier = modifier
                    .then(videoSize)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) else {
                VideoPlayerSection(
                    episodeDetailComplement = watchState.episodeDetailComplement.data,
                    episodeDetailComplements = watchState.episodeDetailComplements,
                    errorMessage = watchState.errorMessage,
                    isFavorite = watchState.isFavorite,
                    isConnected = isConnected,
                    playerUiState = playerUiState,
                    coreState = playerCoreState,
                    controlsState = controlsState,
                    positionState = positionState,
                    playerAction = dispatchPlayerAction,
                    isLandscape = mainState.isLandscape,
                    getPlayer = getPlayer,
                    updateStoredWatchState = {
                        onAction(WatchAction.UpdateEpisodeDetailComplement(it))
                        onAction(WatchAction.UpdateLastEpisodeWatchedId(it.id))
                    },
                    onHandleBackPress = onHandleBackPress,
                    isScreenOn = isScreenOn,
                    isAutoPlayVideo = isAutoPlayVideo,
                    episodes = watchState.animeDetailComplement.episodes,
                    episodeSourcesQuery = watchState.episodeSourcesQuery,
                    handleSelectedEpisodeServer = { episodeSourcesQuery, isRefresh ->
                        onAction(
                            WatchAction.HandleSelectedEpisodeServer(
                                episodeSourcesQuery = episodeSourcesQuery, isRefresh = isRefresh
                            )
                        )
                    },
                    onEnterPipMode = onEnterPipMode,
                    setIsLoading = { onAction(WatchAction.SetIsLoading(it)) },
                    setFullscreenChange = { onAction(WatchAction.SetFullscreen(it)) },
                    setShowResume = { onAction(WatchAction.SetShowResume(it)) },
                    setShowNextEpisode = { onAction(WatchAction.SetShowNextEpisode(it)) },
                    setPlayerError = { onAction(WatchAction.SetErrorMessage(it)) },
                )
            }
            watchState.episodeSourcesQuery?.let { episodeSourcesQuery ->
                RetryButton(
                    modifier = Modifier.align(Alignment.Center),
                    isVisible = watchState.episodeDetailComplement is Resource.Error,
                    onRetry = {
                        onAction(
                            WatchAction.HandleSelectedEpisodeServer(
                                episodeSourcesQuery = episodeSourcesQuery, isRefresh = true
                            )
                        )
                    }
                )
            }
        }

        if (mainState.isLandscape && !playerUiState.isPipMode && !playerUiState.isFullscreen && watchState.animeDetailComplement?.episodes != null) {
            LazyColumn(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize()
                    .weight(0.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                state = scrollState
            ) {
                item {
                    WatchContentSection(
                        animeDetail = watchState.animeDetail,
                        networkStatus = mainState.networkStatus,
                        isFavorite = watchState.isFavorite,
                        onFavoriteToggle = { updatedComplement ->
                            onAction(WatchAction.SetFavorite(updatedComplement.isFavorite))
                        },
                        episodeDetailComplement = watchState.episodeDetailComplement,
                        onLoadEpisodeDetailComplement = {
                            onAction(WatchAction.LoadEpisodeDetailComplement(it))
                        },
                        episodeDetailComplements = watchState.episodeDetailComplements,
                        episodes = watchState.animeDetailComplement.episodes,
                        newEpisodeCount = watchState.newEpisodeCount,
                        episodeSourcesQuery = watchState.episodeSourcesQuery,
                        serverScrollState = serverScrollState,
                        handleSelectedEpisodeServer = {
                            onAction(
                                WatchAction.HandleSelectedEpisodeServer(
                                    episodeSourcesQuery = it, isRefresh = false
                                )
                            )
                        },
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

    if (!mainState.isLandscape && !playerUiState.isPipMode && !playerUiState.isFullscreen && watchState.animeDetailComplement?.episodes != null) {
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
                    networkStatus = mainState.networkStatus,
                    isFavorite = watchState.isFavorite,
                    onFavoriteToggle = { updatedComplement ->
                        onAction(WatchAction.SetFavorite(updatedComplement.isFavorite))
                    },
                    episodeDetailComplement = watchState.episodeDetailComplement,
                    onLoadEpisodeDetailComplement = {
                        onAction(WatchAction.LoadEpisodeDetailComplement(it))
                    },
                    episodeDetailComplements = watchState.episodeDetailComplements,
                    episodes = watchState.animeDetailComplement.episodes,
                    newEpisodeCount = watchState.newEpisodeCount,
                    episodeSourcesQuery = watchState.episodeSourcesQuery,
                    serverScrollState = serverScrollState,
                    handleSelectedEpisodeServer = {
                        onAction(
                            WatchAction.HandleSelectedEpisodeServer(
                                episodeSourcesQuery = it, isRefresh = false
                            )
                        )
                    },
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