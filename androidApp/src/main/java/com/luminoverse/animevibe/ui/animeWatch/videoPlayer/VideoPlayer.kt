package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.content.pm.ActivityInfo
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.animeWatch.PlayerUiState
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.LoadingIndicator
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.LockButton
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.NextEpisodeOverlay
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.PlaybackSpeedContent
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.PlayerControls
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.ResumePlaybackOverlay
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.SeekIndicator
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.SettingsContent
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.SkipButtonsContainer
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.SpeedUpIndicator
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.SubtitleContent
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.ThumbnailPreview
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.ZoomIndicator
import com.luminoverse.animevibe.ui.common.BottomSheetConfig
import com.luminoverse.animevibe.ui.common.CustomModalBottomSheet
import com.luminoverse.animevibe.ui.common.ScreenshotDisplay
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.media.PositionState
import com.luminoverse.animevibe.utils.resource.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    playerView: PlayerView,
    player: ExoPlayer,
    coreState: PlayerCoreState,
    playerUiState: PlayerUiState,
    controlsStateFlow: StateFlow<ControlsState>,
    positionStateFlow: StateFlow<PositionState>,
    playerAction: (HlsPlayerAction) -> Unit,
    onHandleBackPress: () -> Unit,
    episodeDetailComplement: EpisodeDetailComplement,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery, Boolean) -> Unit,
    onEnterPipMode: () -> Unit,
    isSideSheetVisible: Boolean,
    setSideSheetVisibility: (Boolean) -> Unit,
    isAutoPlayVideo: Boolean,
    setFullscreenChange: (Boolean) -> Unit,
    setShowResume: (Boolean) -> Unit,
    setShowNextEpisode: (Boolean) -> Unit,
    isLandscape: Boolean,
    errorMessage: String?
) {
    val controlsState by controlsStateFlow.collectAsStateWithLifecycle()
    val currentPositionState = positionStateFlow.collectAsStateWithLifecycle()
    val state = rememberVideoPlayerState(player, playerAction)

    val updatedControlsState = rememberUpdatedState(controlsState)
    val updatedCoreState = rememberUpdatedState(coreState)
    var isFirstLoad by remember { mutableStateOf(true) }
    val updatedIsFirstLoadState = rememberUpdatedState(isFirstLoad)

    val animatedZoom by animateFloatAsState(
        targetValue = state.zoomScaleProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PlayerZoomAnimation"
    )

    val currentEpisode = episodeDetailComplement.number
    val prevEpisode = episodes.find { it.episode_no == currentEpisode - 1 }
    val nextEpisode = episodes.find { it.episode_no == currentEpisode + 1 }

    var isBufferingFromSeeking by remember { mutableStateOf(false) }

    val calculatedShouldShowResumeOverlay = !isAutoPlayVideo && playerUiState.isShowResume &&
            episodeDetailComplement.lastTimestamp != null &&
            updatedCoreState.value.playbackState == Player.STATE_READY && !player.isPlaying

    LaunchedEffect(updatedCoreState, player.isPlaying) {
        if (updatedIsFirstLoadState.value && updatedCoreState.value.playbackState == Player.STATE_READY && player.isPlaying) {
            isFirstLoad = false
        }
    }

    LaunchedEffect(state.showLockReminder, updatedControlsState.value.isLocked) {
        if (state.showLockReminder && updatedControlsState.value.isLocked) {
            delay(3000)
            state.showLockReminder = false
        }
    }

    LaunchedEffect(isLandscape, playerUiState.isFullscreen) {
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        playerView.postDelayed({
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }, 1)
    }

    LaunchedEffect(updatedCoreState.value.playbackState, state.isDraggingSeekBar) {
        isBufferingFromSeeking =
            if (state.isDraggingSeekBar && (updatedCoreState.value.playbackState == Player.STATE_READY || updatedCoreState.value.playbackState == Player.STATE_BUFFERING)) {
                true
            } else if (updatedCoreState.value.playbackState != Player.STATE_BUFFERING) {
                false
            } else {
                isBufferingFromSeeking
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { state.playerSize = it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        handleGestures(
                            state = state,
                            updatedControlsState = updatedControlsState,
                            updatedCoreState = updatedCoreState,
                            updatedIsFirstLoadState = updatedIsFirstLoadState
                        )
                    }
                }
        ) {
            val borderModifier =
                if (state.isZooming) Modifier.border(16.dp, Color.White.copy(alpha = 0.5f))
                else Modifier

            if (updatedIsFirstLoadState.value) {
                ScreenshotDisplay(
                    imageUrl = episodeDetailComplement.imageUrl,
                    screenshot = episodeDetailComplement.screenshot,
                    modifier = Modifier.fillMaxSize(),
                    onClick = { state.handleSingleTap(updatedControlsState.value.isLocked) }
                )
            } else {
                AndroidView(
                    factory = { playerView },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF14161A))
                        .then(borderModifier)
                        .graphicsLayer(
                            scaleX = animatedZoom,
                            scaleY = animatedZoom,
                            translationX = state.offsetX,
                            translationY = state.offsetY
                        ),
                    update = { view ->
                        view.subtitleView?.apply {
                            setStyle(
                                CaptionStyleCompat(
                                    Color.White.toArgb(),
                                    Color.Transparent.toArgb(),
                                    Color.Transparent.toArgb(),
                                    CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                    Color.Black.toArgb(),
                                    null
                                )
                            )
                            setPadding(
                                0, 0, 0,
                                if (!playerUiState.isPipMode && updatedControlsState.value.isControlsVisible && (isLandscape || !playerUiState.isFullscreen)) 100 else 0
                            )
                        }
                        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                )
            }
        }

        val isPlayerControlsVisible =
            (updatedControlsState.value.isControlsVisible || state.isDraggingSeekBar) &&
                    !calculatedShouldShowResumeOverlay && !playerUiState.isPipMode && !updatedControlsState.value.isLocked

        episodeDetailComplement.sources.tracks.find { it.kind == "thumbnails" }?.file?.let {
            val showThumbnail =
                isBufferingFromSeeking || (!player.isPlaying && state.isDraggingSeekBar && !updatedIsFirstLoadState.value)
            val thumbnailSeekPositionKey =
                remember(state.dragSeekPosition) { (state.dragSeekPosition / 10000L) * 10000L }

            AnimatedVisibility(visible = showThumbnail, enter = fadeIn(), exit = fadeOut()) {
                ThumbnailPreview(
                    modifier = Modifier.fillMaxSize(),
                    seekPosition = thumbnailSeekPositionKey,
                    thumbnailTrackUrl = it
                )
            }
        }

        AnimatedVisibility(visible = isPlayerControlsVisible, enter = fadeIn(), exit = fadeOut()) {
            PlayerControls(
                isPlaying = player.isPlaying,
                playbackState = updatedCoreState.value.playbackState,
                positionState = currentPositionState,
                onHandleBackPress = onHandleBackPress,
                episodeDetailComplement = episodeDetailComplement,
                hasPreviousEpisode = prevEpisode != null,
                nextEpisode = nextEpisode,
                nextEpisodeDetailComplement = episodeDetailComplements[nextEpisode?.id]?.data,
                isSideSheetVisible = isSideSheetVisible,
                setSideSheetVisibility = setSideSheetVisibility,
                isLandscape = isLandscape,
                isShowSpeedUp = state.isHolding,
                zoomText = state.getZoomRatioText(),
                onZoomReset = { state.resetZoom() },
                handlePlay = { playerAction(HlsPlayerAction.Play) },
                handlePause = { playerAction(HlsPlayerAction.Pause); isFirstLoad = false },
                onPreviousEpisode = {
                    prevEpisode?.let {
                        handleSelectedEpisodeServer(episodeSourcesQuery.copy(id = it.id), false)
                    }
                },
                onNextEpisode = {
                    nextEpisode?.let {
                        handleSelectedEpisodeServer(episodeSourcesQuery.copy(id = it.id), false)
                        setShowNextEpisode(false)
                    }
                },
                onSeekTo = { position -> playerAction(HlsPlayerAction.SeekTo(position)) },
                seekAmount = state.seekAmount * 1000L,
                isShowSeekIndicator = state.isShowSeekIndicator,
                dragSeekPosition = state.dragSeekPosition,
                onDraggingSeekBarChange = { isDragging, position ->
                    state.isDraggingSeekBar = isDragging
                    state.dragSeekPosition = position
                },
                isDraggingSeekBar = state.isDraggingSeekBar,
                showRemainingTime = state.showRemainingTime,
                setShowRemainingTime = { state.showRemainingTime = it },
                onSettingsClick = { state.showSettingsSheet = true },
                onFullscreenToggle = {
                    if (!updatedControlsState.value.isLocked) setFullscreenChange(!playerUiState.isFullscreen)
                },
                onBottomBarMeasured = { height -> state.bottomBarHeight = height }
            )
        }

        LoadingIndicator(
            modifier = Modifier.align(Alignment.Center),
            isVisible = (updatedCoreState.value.playbackState == Player.STATE_BUFFERING || updatedCoreState.value.playbackState == Player.STATE_IDLE) && !isPlayerControlsVisible && errorMessage == null
        )

        SeekIndicator(
            modifier = Modifier.align(Alignment.Center),
            seekDirection = state.isShowSeekIndicator,
            seekAmount = state.seekAmount,
            isLandscape = isLandscape,
            isFullscreen = playerUiState.isFullscreen
        )

        episodeDetailComplement.lastTimestamp?.let { timestamp ->
            ResumePlaybackOverlay(
                modifier = Modifier.align(Alignment.Center),
                isVisible = calculatedShouldShowResumeOverlay,
                isPipMode = playerUiState.isPipMode,
                lastTimestamp = timestamp,
                onDismiss = {
                    setShowResume(false)
                    playerAction(HlsPlayerAction.RequestToggleControlsVisibility(true))
                },
                onRestart = {
                    playerAction(HlsPlayerAction.SeekTo(0))
                    playerAction(HlsPlayerAction.Play)
                    setShowResume(false)
                },
                onResume = {
                    playerAction(HlsPlayerAction.SeekTo(it))
                    playerAction(HlsPlayerAction.Play)
                    setShowResume(false)
                }
            )
        }

        nextEpisode?.let {
            NextEpisodeOverlay(
                modifier = Modifier.align(Alignment.Center),
                isVisible = playerUiState.isShowNextEpisode,
                isLandscape = isLandscape,
                isPipMode = playerUiState.isPipMode,
                animeImage = episodeDetailComplement.imageUrl,
                nextEpisode = it,
                nextEpisodeDetailComplement = episodeDetailComplements[it.id]?.data,
                onDismiss = {
                    setShowNextEpisode(false)
                    playerAction(HlsPlayerAction.RequestToggleControlsVisibility(true))
                },
                onRestart = {
                    playerAction(HlsPlayerAction.SeekTo(0))
                    playerAction(HlsPlayerAction.Play)
                    setShowNextEpisode(false)
                },
                onPlayNext = {
                    handleSelectedEpisodeServer(episodeSourcesQuery.copy(id = it.id), false)
                    setShowNextEpisode(false)
                }
            )
        }

        val isSkipVisible =
            !playerUiState.isPipMode && !updatedControlsState.value.isLocked && !state.isHolding &&
                    !state.isDraggingSeekBar && updatedCoreState.value.playbackState != Player.STATE_ENDED && updatedCoreState.value.playbackState != Player.STATE_IDLE &&
                    !calculatedShouldShowResumeOverlay && !playerUiState.isShowNextEpisode && !updatedIsFirstLoadState.value

        SkipButtonsContainer(
            modifier = Modifier.align(Alignment.BottomEnd),
            positionState = currentPositionState,
            intro = episodeDetailComplement.sources.intro,
            outro = episodeDetailComplement.sources.outro,
            isSkipVisible = isSkipVisible,
            onSkip = { seekPosition -> playerAction(HlsPlayerAction.SeekTo(seekPosition)) }
        )

        SpeedUpIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isVisible = state.isHolding && !playerUiState.isPipMode && !updatedControlsState.value.isLocked,
            speedText = state.speedUpText
        )

        ZoomIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            visible = state.isZooming,
            isShowSpeedUp = state.isHolding,
            zoomText = state.getZoomRatioText(),
            isClickable = state.zoomScaleProgress > 1f,
            onClick = { state.resetZoom() }
        )

        val context = LocalContext.current
        LockButton(
            visible = updatedControlsState.value.isLocked && !playerUiState.isPipMode && state.showLockReminder,
            onClick = {
                (context as? FragmentActivity)?.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR
                playerAction(HlsPlayerAction.ToggleLock(false))
            },
            modifier = Modifier.align(Alignment.TopEnd)
        )

        val bottomSheetConfig = BottomSheetConfig(
            landscapeWidthFraction = 0.4f,
            landscapeHeightFraction = 0.7f
        )
        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = state.showSettingsSheet && !playerUiState.isPipMode && !updatedControlsState.value.isLocked,
            isFullscreen = playerUiState.isFullscreen,
            isLandscape = isLandscape,
            config = bottomSheetConfig,
            onDismiss = { state.showSettingsSheet = false }
        ) {
            SettingsContent(
                onDismiss = { state.showSettingsSheet = false },
                onLockClick = {
                    state.showLockReminder = true
                    setFullscreenChange(true)
                    if (updatedCoreState.value.playbackState == Player.STATE_ENDED) playerAction(
                        HlsPlayerAction.SeekTo(0)
                    )
                    playerAction(HlsPlayerAction.Play)
                    playerAction(HlsPlayerAction.ToggleLock(true))
                },
                onPipClick = { onEnterPipMode() },
                selectedPlaybackSpeed = updatedControlsState.value.playbackSpeed,
                onPlaybackSpeedClick = { state.showPlaybackSpeedSheet = true },
                isSubtitleAvailable = episodeDetailComplement.sources.tracks.any { it.kind == "captions" },
                selectedSubtitle = updatedControlsState.value.selectedSubtitle,
                onSubtitleClick = { state.showSubtitleSheet = true }
            )
        }

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = state.showSubtitleSheet && !playerUiState.isPipMode && !updatedControlsState.value.isLocked,
            isFullscreen = playerUiState.isFullscreen,
            isLandscape = isLandscape,
            config = bottomSheetConfig,
            onDismiss = { state.showSubtitleSheet = false }
        ) {
            SubtitleContent(
                tracks = episodeDetailComplement.sources.tracks,
                selectedSubtitle = updatedControlsState.value.selectedSubtitle,
                onSubtitleSelected = { track ->
                    playerAction(HlsPlayerAction.SetSubtitle(track))
                    state.showSubtitleSheet = false
                }
            )
        }

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = state.showPlaybackSpeedSheet && !playerUiState.isPipMode && !updatedControlsState.value.isLocked,
            isFullscreen = playerUiState.isFullscreen,
            isLandscape = isLandscape,
            config = bottomSheetConfig,
            onDismiss = { state.showPlaybackSpeedSheet = false }
        ) {
            PlaybackSpeedContent(
                selectedPlaybackSpeed = updatedControlsState.value.playbackSpeed,
                onSpeedChange = { speed ->
                    playerAction(HlsPlayerAction.SetPlaybackSpeed(speed))
                    state.showPlaybackSpeedSheet = false
                }
            )
        }
    }
}