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
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AnimeWatchContent(
    malId: Int,
    navController: NavController,
    watchState: WatchState,
    isScreenOn: Boolean,
    isAutoPlayVideo: Boolean,
    playerUiState: PlayerUiState,
    mainState: MainState,
    playerCoreState: PlayerCoreState,
    controlsState: StateFlow<ControlsState>,
    positionState: StateFlow<PositionState>,
    dispatchPlayerAction: (HlsPlayerAction) -> Unit,
    getPlayer: () -> ExoPlayer?,
    onHandleBackPress: () -> Any?,
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
            Log.d("VideoPlayerSection", "Error from watchState: ${watchState.errorMessage}")
        }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = modifier
                .then(videoSize)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            if (watchState.episodeDetailComplement?.sources?.sources[0]?.url == null || watchState.animeDetailComplement?.episodes == null || watchState.episodeSourcesQuery == null) Box(
                modifier = modifier
                    .then(videoSize)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) else {
                VideoPlayerSection(
                    episodeDetailComplement = watchState.episodeDetailComplement,
                    episodeDetailComplements = watchState.episodeDetailComplements,
                    errorMessage = watchState.errorMessage,
                    playerUiState = playerUiState,
                    coreState = playerCoreState,
                    controlsState = controlsState,
                    positionState = positionState,
                    playerAction = dispatchPlayerAction,
                    isLandscape = mainState.isLandscape,
                    getPlayer = getPlayer,
                    updateStoredWatchState = { currentPosition, duration, screenShot ->
                        onAction(WatchAction.UpdateLastEpisodeWatchedId(watchState.episodeDetailComplement.id))
                        onAction(
                            WatchAction.UpdateStoredWatchState(
                                currentPosition, duration, screenShot
                            )
                        )
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
                    setFullscreenChange = { onAction(WatchAction.SetFullscreen(it)) },
                    setShowResume = { onAction(WatchAction.SetShowResume(it)) },
                    setShowNextEpisode = { onAction(WatchAction.SetShowNextEpisode(it)) },
                    setPlayerError = { onAction(WatchAction.SetErrorMessage(it)) },
                )
            }
            watchState.episodeSourcesQuery?.let { episodeSourcesQuery ->
                RetryButton(
                    modifier = Modifier.align(Alignment.Center),
                    isVisible = watchState.episodeDetailComplement == null && watchState.errorMessage != null && !watchState.isRefreshing,
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

        if (mainState.isLandscape && !playerUiState.isPipMode && !playerUiState.isFullscreen && watchState.animeDetailComplement?.episodes != null && watchState.animeDetail?.mal_id == malId && watchState.animeDetailComplement.malId == malId) {
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
                        onFavoriteToggle = { isFavorite ->
                            onAction(WatchAction.SetFavorite(isFavorite))
                        },
                        isRefreshing = watchState.isRefreshing,
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

    if (!mainState.isLandscape && !playerUiState.isPipMode && !playerUiState.isFullscreen && watchState.animeDetailComplement?.episodes != null && watchState.animeDetail?.mal_id == malId && watchState.animeDetailComplement.malId == malId) {
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
                    onFavoriteToggle = { isFavorite ->
                        onAction(WatchAction.SetFavorite(isFavorite))
                    },
                    isRefreshing = watchState.isRefreshing,
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