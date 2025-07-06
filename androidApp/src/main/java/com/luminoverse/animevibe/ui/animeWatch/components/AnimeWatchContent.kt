package com.luminoverse.animevibe.ui.animeWatch.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.ui.animeWatch.WatchAction
import com.luminoverse.animevibe.ui.animeWatch.WatchState
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
import kotlinx.coroutines.launch

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun AnimeWatchContent(
    malId: Int,
    navController: NavController,
    networkDataSource: NetworkDataSource,
    watchState: WatchState,
    showSnackbar: (SnackbarMessage) -> Unit,
    isScreenOn: Boolean,
    mainState: MainState,
    playerCoreState: PlayerCoreState,
    controlsStateFlow: StateFlow<ControlsState>,
    dispatchPlayerAction: (HlsPlayerAction) -> Unit,
    getPlayer: () -> ExoPlayer?,
    captureScreenshot: suspend () -> String?,
    onAction: (WatchAction) -> Unit,
    playerDisplayMode: PlayerDisplayMode,
    setPlayerDisplayMode: (PlayerDisplayMode) -> Unit,
    onEnterSystemPipMode: () -> Unit,
    dragProgress: Float,
    maxVerticalDrag: Float,
    setMaxVerticalDrag: (Float) -> Unit,
    verticalDragOffset: Animatable<Float, *>,
    rememberedTopPadding: Dp,
    rememberedBottomPadding: Dp,
    pipWidth: Dp,
    pipEndDestinationPx: Offset,
    pipEndSizePx: IntSize
) {
    val player by remember { mutableStateOf(getPlayer()) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val serverScrollState = rememberScrollState()

    val isPortrait = !mainState.isLandscape && playerDisplayMode in listOf(
        PlayerDisplayMode.FULLSCREEN_LANDSCAPE, PlayerDisplayMode.FULLSCREEN_PORTRAIT
    )
    val isSideSheetVisible = !isPortrait && watchState.isSideSheetVisible && dragProgress == 0f

    val scope = rememberCoroutineScope()
    val animatedBackgroundColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background.copy(alpha = 1f - dragProgress),
        animationSpec = tween(durationMillis = 150),
        label = "backgroundAlpha"
    )

    val contentModifier = Modifier
        .graphicsLayer {
            translationY = verticalDragOffset.value
            alpha = 1f - (dragProgress * 1.5f).coerceIn(0f, 1f)
        }
        .background(animatedBackgroundColor)
        .padding(bottom = if (isPortrait) rememberedBottomPadding else 0.dp)
        .padding(horizontal = 8.dp)
        .fillMaxSize()
        .blur(radius = (dragProgress * 10.dp).coerceAtMost(10.dp))

    Column(modifier = Modifier.fillMaxSize()) {
        val columnScrollState = rememberScrollState()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(animatedBackgroundColor)
                .padding(top = if (isPortrait) rememberedTopPadding else 0.dp)
        ) {
            val containerWidth = this.maxWidth
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isPortrait) {
                                Modifier.height(screenWidth * 9 / 16)
                            } else {
                                Modifier.fillMaxSize()
                            }
                        )
                        .weight(1f)
                ) {
                    if (player == null || watchState.episodeDetailComplement == null || watchState.episodeDetailComplement.sources.link.file.isEmpty() || watchState.animeDetailComplement?.episodes == null || watchState.episodeSourcesQuery == null) {
                        ImageDisplay(
                            image = watchState.episodeDetailComplement?.screenshot,
                            imagePlaceholder = watchState.episodeDetailComplement?.imageUrl ?: watchState.animeDetail?.images?.webp?.large_image_url,
                            ratio = ImageAspectRatio.WIDESCREEN.ratio,
                            contentDescription = "Anime cover",
                            roundedCorners = ImageRoundedCorner.NONE
                        )
                    } else {
                        player?.let { player ->
                            VideoPlayerSection(
                                episodeDetailComplement = watchState.episodeDetailComplement,
                                episodeDetailComplements = watchState.episodeDetailComplements,
                                networkDataSource = networkDataSource,
                                coreState = playerCoreState,
                                controlsStateFlow = controlsStateFlow,
                                playerAction = dispatchPlayerAction,
                                isLandscape = mainState.isLandscape,
                                player = player,
                                captureScreenshot = captureScreenshot,
                                updateStoredWatchState = { currentPosition, duration, screenShot ->
                                    onAction(WatchAction.UpdateLastEpisodeWatchedId(watchState.episodeDetailComplement.id))
                                    onAction(
                                        WatchAction.UpdateStoredWatchState(
                                            currentPosition, duration, screenShot
                                        )
                                    )
                                },
                                isScreenOn = isScreenOn,
                                isAutoPlayVideo = mainState.isAutoPlayVideo,
                                episodes = watchState.animeDetailComplement.episodes,
                                episodeSourcesQuery = watchState.episodeSourcesQuery,
                                handleSelectedEpisodeServer = { episodeSourcesQuery, isRefresh ->
                                    onAction(
                                        WatchAction.HandleSelectedEpisodeServer(
                                            episodeSourcesQuery = episodeSourcesQuery,
                                            isRefresh = isRefresh
                                        )
                                    )
                                },
                                displayMode = playerDisplayMode,
                                setPlayerDisplayMode = setPlayerDisplayMode,
                                onEnterSystemPipMode = onEnterSystemPipMode,
                                isSideSheetVisible = watchState.isSideSheetVisible,
                                setSideSheetVisibility = {
                                    onAction(
                                        WatchAction.SetSideSheetVisibility(
                                            it
                                        )
                                    )
                                },
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
                                        )
                                    )
                                },
                                rememberedTopPadding = rememberedTopPadding,
                                verticalDragOffset = verticalDragOffset.value,
                                onVerticalDrag = { delta ->
                                    scope.launch {
                                        val newOffset =
                                            (verticalDragOffset.value + delta).coerceIn(
                                                0f, maxVerticalDrag
                                            )
                                        verticalDragOffset.snapTo(newOffset)
                                    }
                                },
                                onDragEnd = { flingVelocity ->
                                    scope.launch {
                                        val flingVelocityThreshold = 1.8f
                                        val positionThreshold = maxVerticalDrag * 0.5f

                                        if (flingVelocity > flingVelocityThreshold) {
                                            setPlayerDisplayMode(PlayerDisplayMode.PIP)
                                            verticalDragOffset.snapTo(0f)
                                        } else if (flingVelocity < -flingVelocityThreshold) {
                                            verticalDragOffset.animateTo(
                                                0f,
                                                animationSpec = tween(durationMillis = 300)
                                            )
                                        } else {
                                            if (maxVerticalDrag.isFinite() && verticalDragOffset.value > positionThreshold) {
                                                setPlayerDisplayMode(PlayerDisplayMode.PIP)
                                                verticalDragOffset.snapTo(0f)
                                            } else {
                                                verticalDragOffset.animateTo(
                                                    0f,
                                                    animationSpec = tween(durationMillis = 300)
                                                )
                                            }
                                        }
                                    }
                                },
                                pipWidth = pipWidth,
                                pipEndDestinationPx = pipEndDestinationPx,
                                pipEndSizePx = pipEndSizePx,
                                onMaxDragAmountCalculated = { setMaxVerticalDrag(it) }
                            )
                        }
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
                                    .clickable {
                                        onAction(
                                            WatchAction.SetSideSheetVisibility(
                                                false
                                            )
                                        )
                                    }
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

                        InfoContentSection(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .fillMaxSize()
                                .weight(1f),
                            animeDetail = watchState.animeDetail,
                            navController = navController,
                            setPlayerDisplayMode = setPlayerDisplayMode
                        )
                    }
                }
            }
        }

        if (isPortrait && watchState.animeDetailComplement?.episodes != null && watchState.animeDetail?.mal_id == malId) Column(
            modifier = contentModifier.verticalScroll(columnScrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
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
                setPlayerDisplayMode = setPlayerDisplayMode
            )
        }
    }
}