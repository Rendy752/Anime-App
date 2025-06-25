package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.app.Activity
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.view.OrientationEventListener
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.media3.ui.PlayerView
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.animeWatch.PlayerUiState
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.CustomSubtitleView
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.LoadingIndicator
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.LockButton
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.NextEpisodeOverlay
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.PlaybackSpeedContent
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.PlayerControls
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.ResumePlaybackOverlay
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.SeekCancelPreview
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
import com.luminoverse.animevibe.utils.media.findActiveCaptionCues
import com.luminoverse.animevibe.utils.resource.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

private enum class PhysicalOrientation {
    PORTRAIT, LANDSCAPE, REVERSE_LANDSCAPE, UNKNOWN;

    fun isLandscape(): Boolean {
        return this == LANDSCAPE || this == REVERSE_LANDSCAPE
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    playerView: PlayerView,
    player: ExoPlayer,
    networkDataSource: NetworkDataSource,
    updateStoredWatchState: (Long?, Long?, String?) -> Unit,
    captureScreenshot: suspend () -> String?,
    coreState: PlayerCoreState,
    playerUiState: PlayerUiState,
    controlsStateFlow: StateFlow<ControlsState>,
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
    isAutoplayEnabled: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    onShowResumeChange: (Boolean) -> Unit,
    onShowNextEpisodeChange: (Boolean) -> Unit,
    isLandscape: Boolean,
    errorMessage: String?
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val controlsState by controlsStateFlow.collectAsStateWithLifecycle()
    val videoPlayerState =
        rememberVideoPlayerState(
            key = episodeDetailComplement.sources.link.file,
            player = player,
            playerAction = playerAction,
            networkDataSource = networkDataSource
        )
    val currentPosition by videoPlayerState.currentPosition.collectAsStateWithLifecycle()

    val thumbnailTrackUrl =
        episodeDetailComplement.sources.tracks.find { it.kind == "thumbnails" }?.file
    LaunchedEffect(thumbnailTrackUrl) {
        if (thumbnailTrackUrl != null) {
            videoPlayerState.loadAndCacheThumbnails(context, thumbnailTrackUrl)
        }
    }

    val selectedSubtitle = controlsState.selectedSubtitle
    LaunchedEffect(selectedSubtitle) {
        if (selectedSubtitle != null) {
            videoPlayerState.loadAndCacheCaptions(selectedSubtitle.file)
        }
    }
    val activeCaptionCue = selectedSubtitle?.file?.let { url ->
        videoPlayerState.captionCues[url]?.let { cues ->
            findActiveCaptionCues(currentPosition, cues)
        }
    }

    val updatedControlsState = rememberUpdatedState(controlsState)
    val updatedCoreState = rememberUpdatedState(coreState)


    val animatedZoom by animateFloatAsState(
        targetValue = videoPlayerState.zoomScaleProgress,
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

    val shouldShowResumeOverlay = !isAutoplayEnabled && playerUiState.isShowResume &&
            episodeDetailComplement.lastTimestamp != null &&
            updatedCoreState.value.playbackState == Player.STATE_READY && !player.isPlaying

    LaunchedEffect(updatedCoreState.value.isPlaying) {
        if (updatedCoreState.value.isPlaying) {
            if (videoPlayerState.isFirstLoad && updatedCoreState.value.playbackState == Player.STATE_READY) {
                videoPlayerState.isFirstLoad = false
            }
            while (true) {
                val currentPosition = player.currentPosition
                videoPlayerState.updatePosition(currentPosition)
                if (player.duration > 0 && currentPosition >= 10_000) {
                    val position = currentPosition
                    val duration = player.duration
                    val screenshot = captureScreenshot()
                    updateStoredWatchState(position, duration, screenshot)
                }
                delay(500)
            }
        }
    }

    LaunchedEffect(videoPlayerState.showLockReminder, updatedControlsState.value.isLocked) {
        if (videoPlayerState.showLockReminder && updatedControlsState.value.isLocked) {
            delay(3000)
            videoPlayerState.showLockReminder = false
        }
    }

    LaunchedEffect(isLandscape, playerUiState.isFullscreen) {
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        playerView.postDelayed({
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }, 1)
    }

    var physicalOrientation by remember { mutableStateOf(PhysicalOrientation.UNKNOWN) }
    var isOrientationLocked by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    physicalOrientation = PhysicalOrientation.UNKNOWN
                    return
                }

                val newOrientation = when (orientation) {
                    in 345..359, in 0..15 -> PhysicalOrientation.PORTRAIT
                    in 75..105 -> PhysicalOrientation.REVERSE_LANDSCAPE
                    in 255..285 -> PhysicalOrientation.LANDSCAPE
                    else -> PhysicalOrientation.UNKNOWN
                }

                if (newOrientation != physicalOrientation) {
                    physicalOrientation = newOrientation
                }
            }
        }
        orientationEventListener.enable()
        onDispose {
            orientationEventListener.disable()
        }
    }

    LaunchedEffect(playerUiState.isFullscreen) {
        isOrientationLocked = true
    }

    LaunchedEffect(
        controlsState.isLocked,
        playerUiState.isFullscreen,
        physicalOrientation,
        isOrientationLocked
    ) {
        if (activity == null) return@LaunchedEffect

        val isSystemAutoRotateEnabled = Settings.System.getInt(
            context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0
        ) == 1

        if (controlsState.isLocked) {
            val lockedOrientation = if (isSystemAutoRotateEnabled) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            if (activity.requestedOrientation != lockedOrientation) {
                activity.requestedOrientation = lockedOrientation
            }
            return@LaunchedEffect
        }

        val isTargetingLandscape = playerUiState.isFullscreen

        if (isOrientationLocked) {
            val targetActivityInfo = if (isTargetingLandscape) {
                when (physicalOrientation) {
                    PhysicalOrientation.REVERSE_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            if (activity.requestedOrientation != targetActivityInfo) {
                activity.requestedOrientation = targetActivityInfo
            }

            val transitionComplete = if (isTargetingLandscape) {
                physicalOrientation.isLandscape()
            } else {
                physicalOrientation == PhysicalOrientation.PORTRAIT
            }

            if (transitionComplete) {
                isOrientationLocked = false
            }
        } else {
            val defaultOrientation = if (isSystemAutoRotateEnabled) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LOCKED
            }

            if (activity.requestedOrientation != defaultOrientation) {
                activity.requestedOrientation = defaultOrientation
            }
        }
    }

    LaunchedEffect(updatedCoreState.value.playbackState, videoPlayerState.isDraggingSeekBar) {
        if (updatedCoreState.value.playbackState == Player.STATE_ENDED) {
            val screenshot = captureScreenshot()
            updateStoredWatchState(player.duration, player.duration, screenshot)
        }

        isBufferingFromSeeking =
            if (videoPlayerState.isDraggingSeekBar && (updatedCoreState.value.playbackState == Player.STATE_READY || updatedCoreState.value.playbackState == Player.STATE_BUFFERING)) {
                true
            } else if (updatedCoreState.value.playbackState != Player.STATE_BUFFERING) {
                false
            } else {
                isBufferingFromSeeking
            }
    }

    val isCommonPartVisible = !playerUiState.isPipMode && !updatedControlsState.value.isLocked
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { videoPlayerState.playerSize = it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(episodeDetailComplement.sources.link.file) {
                    awaitEachGesture {
                        handleGestures(
                            state = videoPlayerState,
                            updatedControlsState = updatedControlsState,
                            updatedCoreState = updatedCoreState
                        )
                    }
                }
        ) {
            val borderModifier =
                if (videoPlayerState.isZooming && isCommonPartVisible) Modifier.border(
                    16.dp,
                    Color.White.copy(alpha = 0.5f)
                )
                else Modifier

            if (videoPlayerState.isFirstLoad) {
                ScreenshotDisplay(
                    imageUrl = episodeDetailComplement.imageUrl,
                    screenshot = episodeDetailComplement.screenshot,
                    modifier = Modifier.fillMaxSize(),
                    onClick = { videoPlayerState.handleSingleTap(updatedControlsState.value.isLocked) }
                )
            } else {
                AndroidView(
                    factory = { playerView },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF14161A))
                        .then(borderModifier)
                        .then(
                            if (!playerUiState.isPipMode) {
                                Modifier.graphicsLayer(
                                    scaleX = animatedZoom,
                                    scaleY = animatedZoom,
                                    translationX = videoPlayerState.offsetX,
                                    translationY = videoPlayerState.offsetY
                                )
                            } else Modifier
                        ),
                    update = { view ->
                        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                )
            }
        }

        val isPlayerControlsVisible =
            (updatedControlsState.value.isControlsVisible || videoPlayerState.isDraggingSeekBar) && !shouldShowResumeOverlay && isCommonPartVisible

        thumbnailTrackUrl?.let { url ->
            val showThumbnail =
                isBufferingFromSeeking || (!player.isPlaying && videoPlayerState.isDraggingSeekBar && !videoPlayerState.isFirstLoad)
            val thumbnailSeekPositionKey =
                remember(videoPlayerState.dragSeekPosition) { (videoPlayerState.dragSeekPosition / 10000L) * 10000L }

            AnimatedVisibility(visible = showThumbnail, enter = fadeIn(), exit = fadeOut()) {
                ThumbnailPreview(
                    modifier = Modifier.fillMaxSize(),
                    seekPosition = thumbnailSeekPositionKey,
                    cues = videoPlayerState.thumbnailCues[url]
                )
            }
        }

        val animatedTopPadding by animateDpAsState(
            targetValue = if (isCommonPartVisible && isPlayerControlsVisible && isLandscape && !isSideSheetVisible) {
                videoPlayerState.bottomBarHeight.dp - 16.dp
            } else {
                if (isSideSheetVisible) 16.dp else 8.dp
            },
            label = "SubtitleTopPadding"
        )

        val animatedBottomPadding by animateDpAsState(
            targetValue = if (isCommonPartVisible && isPlayerControlsVisible && isLandscape) {
                videoPlayerState.bottomBarHeight.dp
            } else {
                if (isSideSheetVisible) 16.dp else 8.dp
            },
            label = "SubtitleBottomPadding"
        )
        CustomSubtitleView(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = animatedTopPadding, bottom = animatedBottomPadding,
                    start = 8.dp, end = 8.dp
                ),
            cues = activeCaptionCue,
            isLandscape = isLandscape,
            isPipMode = playerUiState.isPipMode
        )

        AnimatedVisibility(visible = isPlayerControlsVisible, enter = fadeIn(), exit = fadeOut()) {
            PlayerControls(
                isPlaying = player.isPlaying,
                currentPosition = currentPosition,
                duration = player.duration.takeIf { it > 0 }
                    ?: episodeDetailComplement.duration ?: 0,
                bufferedPosition = player.bufferedPosition,
                playbackState = updatedCoreState.value.playbackState,
                errorMessage = errorMessage,
                onHandleBackPress = onHandleBackPress,
                episodeDetailComplement = episodeDetailComplement,
                hasPreviousEpisode = prevEpisode != null,
                nextEpisode = nextEpisode,
                nextEpisodeDetailComplement = episodeDetailComplements[nextEpisode?.id]?.data,
                isSideSheetVisible = isSideSheetVisible,
                setSideSheetVisibility = setSideSheetVisibility,
                isLandscape = isLandscape,
                isShowSpeedUp = videoPlayerState.isHolding,
                zoomText = videoPlayerState.getZoomRatioText(),
                onZoomReset = { videoPlayerState.resetZoom() },
                handlePlay = { playerAction(HlsPlayerAction.Play) },
                handlePause = { playerAction(HlsPlayerAction.Pause) },
                onPreviousEpisode = {
                    prevEpisode?.let {
                        handleSelectedEpisodeServer(episodeSourcesQuery.copy(id = it.id), false)
                    }
                },
                onNextEpisode = {
                    nextEpisode?.let {
                        handleSelectedEpisodeServer(episodeSourcesQuery.copy(id = it.id), false)
                        onShowNextEpisodeChange(false)
                    }
                },
                onSeekTo = { position -> playerAction(HlsPlayerAction.SeekTo(position)) },
                seekAmount = videoPlayerState.seekAmount * 1000L,
                isShowSeekIndicator = videoPlayerState.isShowSeekIndicator,
                dragSeekPosition = videoPlayerState.dragSeekPosition,
                dragCancelTrigger = videoPlayerState.dragCancelTrigger,
                onDraggingSeekBarChange = { isDragging, position ->
                    videoPlayerState.isDraggingSeekBar = isDragging
                    videoPlayerState.dragSeekPosition = position
                },
                isDraggingSeekBar = videoPlayerState.isDraggingSeekBar,
                showRemainingTime = videoPlayerState.showRemainingTime,
                setShowRemainingTime = { videoPlayerState.showRemainingTime = it },
                onSettingsClick = { videoPlayerState.showSettingsSheet = true },
                onFullscreenToggle = {
                    if (!updatedControlsState.value.isLocked) onFullscreenChange(!playerUiState.isFullscreen)
                },
                onBottomBarMeasured = { height -> videoPlayerState.bottomBarHeight = height }
            )
        }

        LoadingIndicator(
            modifier = Modifier.align(Alignment.Center),
            isVisible = (updatedCoreState.value.playbackState == Player.STATE_BUFFERING || updatedCoreState.value.playbackState == Player.STATE_IDLE) && !isPlayerControlsVisible && errorMessage == null
        )

        SeekIndicator(
            modifier = Modifier.align(Alignment.Center),
            seekDirection = videoPlayerState.isShowSeekIndicator,
            seekAmount = videoPlayerState.seekAmount,
            isLandscape = isLandscape,
            isFullscreen = playerUiState.isFullscreen
        )

        episodeDetailComplement.lastTimestamp?.let { lastTimestamp ->
            ResumePlaybackOverlay(
                modifier = Modifier.align(Alignment.Center),
                isVisible = shouldShowResumeOverlay,
                isPipMode = playerUiState.isPipMode,
                lastTimestamp = lastTimestamp,
                onDismiss = {
                    onShowResumeChange(false)
                    playerAction(HlsPlayerAction.RequestToggleControlsVisibility(true))
                },
                onRestart = {
                    playerAction(HlsPlayerAction.SeekTo(0))
                    playerAction(HlsPlayerAction.Play)
                    onShowResumeChange(false)
                },
                onResume = {
                    playerAction(HlsPlayerAction.SeekTo(it))
                    playerAction(HlsPlayerAction.Play)
                    onShowResumeChange(false)
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
                    onShowNextEpisodeChange(false)
                    playerAction(HlsPlayerAction.RequestToggleControlsVisibility(true))
                },
                onRestart = {
                    playerAction(HlsPlayerAction.SeekTo(0))
                    playerAction(HlsPlayerAction.Play)
                    onShowNextEpisodeChange(false)
                },
                onPlayNext = {
                    handleSelectedEpisodeServer(episodeSourcesQuery.copy(id = it.id), false)
                    onShowNextEpisodeChange(false)
                }
            )
        }

        val isSkipVisible =
            isCommonPartVisible && !videoPlayerState.isHolding && !videoPlayerState.isDraggingSeekBar && updatedCoreState.value.playbackState != Player.STATE_ENDED && updatedCoreState.value.playbackState != Player.STATE_IDLE && !shouldShowResumeOverlay && !playerUiState.isShowNextEpisode && !videoPlayerState.isFirstLoad

        SkipButtonsContainer(
            modifier = Modifier.align(Alignment.BottomEnd),
            currentPosition = player.currentPosition,
            intro = episodeDetailComplement.sources.intro,
            outro = episodeDetailComplement.sources.outro,
            isSkipVisible = isSkipVisible,
            onSkip = { seekPosition -> playerAction(HlsPlayerAction.SeekTo(seekPosition)) }
        )

        SpeedUpIndicator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            isVisible = videoPlayerState.isHolding && isCommonPartVisible,
            speedText = videoPlayerState.speedUpText
        )

        ZoomIndicator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            visible = videoPlayerState.isZooming && isCommonPartVisible,
            isShowSpeedUp = videoPlayerState.isHolding,
            zoomText = videoPlayerState.getZoomRatioText(),
            isClickable = videoPlayerState.zoomScaleProgress > 1f,
            onClick = { videoPlayerState.resetZoom() }
        )

        SeekCancelPreview(
            modifier = Modifier.align(Alignment.TopEnd),
            visible = videoPlayerState.isDraggingSeekBar && isCommonPartVisible && !videoPlayerState.isHolding,
            captureScreenshot = captureScreenshot,
            imageUrl = episodeDetailComplement.imageUrl,
            onCancelSeekBarDrag = {
                videoPlayerState.cancelSeekBarDrag()
                playerAction(HlsPlayerAction.Play)
                playerAction(HlsPlayerAction.RequestToggleControlsVisibility(false))
            },
        )

        LockButton(
            visible = updatedControlsState.value.isLocked && !playerUiState.isPipMode && videoPlayerState.showLockReminder,
            onClick = {
                val isSystemAutoRotateEnabled = Settings.System.getInt(
                    context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0
                ) == 1
                (context as? FragmentActivity)?.requestedOrientation =
                    if (isSystemAutoRotateEnabled) ActivityInfo.SCREEN_ORIENTATION_SENSOR
                    else ActivityInfo.SCREEN_ORIENTATION_LOCKED
                playerAction(HlsPlayerAction.ToggleLock(false))
            },
            modifier = Modifier.align(Alignment.TopEnd)
        )

        val settingsSheetConfig = BottomSheetConfig(
            landscapeWidthFraction = 0.4f,
            landscapeHeightFraction = 0.7f
        )
        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = videoPlayerState.showSettingsSheet && isCommonPartVisible,
            isFullscreen = playerUiState.isFullscreen,
            isLandscape = isLandscape,
            config = settingsSheetConfig,
            onDismiss = { videoPlayerState.showSettingsSheet = false }
        ) {
            SettingsContent(
                onDismiss = { videoPlayerState.showSettingsSheet = false },
                onLockClick = {
                    videoPlayerState.showLockReminder = true
                    onFullscreenChange(true)
                    if (updatedCoreState.value.playbackState == Player.STATE_ENDED) playerAction(
                        HlsPlayerAction.SeekTo(0)
                    )
                    playerAction(HlsPlayerAction.Play)
                    playerAction(HlsPlayerAction.ToggleLock(true))
                },
                onPipClick = { onEnterPipMode() },
                selectedPlaybackSpeed = updatedControlsState.value.playbackSpeed,
                onPlaybackSpeedClick = { videoPlayerState.showPlaybackSpeedSheet = true },
                isSubtitleAvailable = episodeDetailComplement.sources.tracks.any { it.kind == "captions" },
                selectedSubtitle = updatedControlsState.value.selectedSubtitle,
                onSubtitleClick = { videoPlayerState.showSubtitleSheet = true }
            )
        }

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = videoPlayerState.showSubtitleSheet && isCommonPartVisible,
            isFullscreen = playerUiState.isFullscreen,
            isLandscape = isLandscape,
            config = settingsSheetConfig,
            onDismiss = { videoPlayerState.showSubtitleSheet = false }
        ) {
            SubtitleContent(
                tracks = episodeDetailComplement.sources.tracks,
                selectedSubtitle = updatedControlsState.value.selectedSubtitle,
                onSubtitleSelected = { subtitleTrack ->
                    playerAction(HlsPlayerAction.SetSubtitle(subtitleTrack))
                    videoPlayerState.showSubtitleSheet = false
                }
            )
        }

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = videoPlayerState.showPlaybackSpeedSheet && isCommonPartVisible,
            isFullscreen = playerUiState.isFullscreen,
            isLandscape = isLandscape,
            config = settingsSheetConfig,
            onDismiss = { videoPlayerState.showPlaybackSpeedSheet = false }
        ) {
            PlaybackSpeedContent(
                selectedPlaybackSpeed = updatedControlsState.value.playbackSpeed,
                onSpeedChange = { speed ->
                    playerAction(HlsPlayerAction.SetPlaybackSpeed(speed))
                    videoPlayerState.showPlaybackSpeedSheet = false
                }
            )
        }
    }
}