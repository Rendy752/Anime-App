package com.luminoverse.animevibe.ui.animeWatch.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.ui.animeWatch.WatchState
import com.luminoverse.animevibe.ui.animeWatch.PlayerUiState
import com.luminoverse.animevibe.ui.animeWatch.WatchAction
import com.luminoverse.animevibe.ui.animeWatch.videoPlayer.VideoPlayerSection
import com.luminoverse.animevibe.ui.animeWatch.watchContent.WatchContentSection
import com.luminoverse.animevibe.ui.common.ImageAspectRatio
import com.luminoverse.animevibe.ui.common.ImageDisplay
import com.luminoverse.animevibe.ui.common.ImageRoundedCorner
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.ui.main.PlayerDisplayMode
import com.luminoverse.animevibe.ui.main.SnackbarMessage
import com.luminoverse.animevibe.ui.main.SnackbarMessageType
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AnimeWatchContent(
    malId: Int,
    navController: NavController,
    networkDataSource: NetworkDataSource,
    watchState: WatchState,
    showSnackbar: (SnackbarMessage) -> Unit,
    isScreenOn: Boolean,
    playerUiState: PlayerUiState,
    mainState: MainState,
    playerCoreState: PlayerCoreState,
    controlsStateFlow: StateFlow<ControlsState>,
    dispatchPlayerAction: (HlsPlayerAction) -> Unit,
    getPlayer: () -> ExoPlayer?,
    captureScreenshot: suspend () -> String?,
    onHandleBackPress: () -> Unit,
    onAction: (WatchAction) -> Unit,
    scrollState: LazyListState,
    displayMode: PlayerDisplayMode,
    onEnterSystemPipMode: () -> Unit,
    modifier: Modifier
) {
    val serverScrollState = rememberScrollState()
    val isSideSheetVisible = mainState.isLandscape && watchState.isSideSheetVisible

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val containerWidth = this.maxWidth
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                if (watchState.episodeDetailComplement == null || watchState.episodeDetailComplement.sources.link.file.isEmpty() || watchState.animeDetailComplement?.episodes == null || watchState.episodeSourcesQuery == null) {
                    if (playerCoreState.error != null && !watchState.isRefreshing && !playerCoreState.isPlaying) ImageDisplay(
                        modifier = modifier,
                        image = watchState.animeDetail?.images?.webp?.large_image_url,
                        ratio = ImageAspectRatio.WIDESCREEN.ratio,
                        contentDescription = "Anime cover",
                        roundedCorners = ImageRoundedCorner.NONE
                    ) else Box(
                        modifier = modifier.background(Color(0xFF14161A))
                    )
                } else {
                    VideoPlayerSection(
                        episodeDetailComplement = watchState.episodeDetailComplement,
                        episodeDetailComplements = watchState.episodeDetailComplements,
                        networkDataSource = networkDataSource,
                        playerUiState = playerUiState,
                        coreState = playerCoreState,
                        controlsStateFlow = controlsStateFlow,
                        playerAction = dispatchPlayerAction,
                        isLandscape = mainState.isLandscape,
                        getPlayer = getPlayer,
                        captureScreenshot = captureScreenshot,
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
                        isAutoPlayVideo = mainState.isAutoPlayVideo,
                        episodes = watchState.animeDetailComplement.episodes,
                        episodeSourcesQuery = watchState.episodeSourcesQuery,
                        handleSelectedEpisodeServer = { episodeSourcesQuery, isRefresh ->
                            onAction(
                                WatchAction.HandleSelectedEpisodeServer(
                                    episodeSourcesQuery = episodeSourcesQuery, isRefresh = isRefresh
                                )
                            )
                        },
                        displayMode = displayMode,
                        onEnterSystemPipMode = onEnterSystemPipMode,
                        isSideSheetVisible = watchState.isSideSheetVisible,
                        setSideSheetVisibility = { onAction(WatchAction.SetSideSheetVisibility(it)) },
                        setFullscreenChange = { onAction(WatchAction.SetFullscreen(it)) },
                        setShowResume = { onAction(WatchAction.SetShowResume(it)) },
                        setPlayerError = {
                            showSnackbar(
                                SnackbarMessage(
                                    message = it,
                                    type = SnackbarMessageType.ERROR,
                                    actionLabel = "RETRY",
                                    onAction = {
                                        onAction(
                                            WatchAction.HandleSelectedEpisodeServer(
                                                episodeSourcesQuery = watchState.episodeSourcesQuery,
                                                isRefresh = true
                                            )
                                        )
                                    }
                                ))
                        },
                    )
                }
            }

            val sideSheetWidth by animateDpAsState(
                targetValue = if (isSideSheetVisible) containerWidth * 0.3f else 0.dp,
                label = "sideSheetWidthAnimation"
            )

            if (sideSheetWidth > 0.dp) {
                Column(modifier = Modifier.width(sideSheetWidth)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Info",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onAction(WatchAction.SetSideSheetVisibility(false)) }
                                .padding(8.dp),
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close currently watching anime info",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )

                    LazyColumn(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxSize()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        state = scrollState
                    ) {
                        item {
                            InfoContentSection(
                                animeDetail = watchState.animeDetail,
                                navController = navController,
                            )
                        }
                    }
                }
            }
        }
    }

    if (displayMode == PlayerDisplayMode.FULLSCREEN && !mainState.isLandscape && !playerUiState.isPipMode && watchState.animeDetailComplement?.episodes != null && watchState.animeDetail?.mal_id == malId) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 8.dp)
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
                    episodeDetailComplement = watchState.episodeDetailComplement,
                    onLoadEpisodeDetailComplement = {
                        onAction(WatchAction.LoadEpisodeDetailComplement(it))
                    },
                    episodeDetailComplements = watchState.episodeDetailComplements,
                    episodes = watchState.animeDetailComplement.episodes,
                    newEpisodeIdList = watchState.newEpisodeIdList,
                    episodeSourcesQuery = watchState.episodeSourcesQuery,
                    episodeJumpNumber = watchState.episodeJumpNumber,
                    setEpisodeJumpNumber = { onAction(WatchAction.SetEpisodeJumpNumber(it)) },
                    serverScrollState = serverScrollState,
                    handleSelectedEpisodeServer = {
                        onAction(
                            WatchAction.HandleSelectedEpisodeServer(
                                episodeSourcesQuery = it, isRefresh = false
                            )
                        )
                    },
                )

                InfoContentSection(
                    animeDetail = watchState.animeDetail,
                    navController = navController,
                )
            }
        }
    }
}