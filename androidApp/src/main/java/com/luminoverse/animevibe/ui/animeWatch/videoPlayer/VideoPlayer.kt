package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.*
import com.luminoverse.animevibe.ui.common.BottomSheetConfig
import com.luminoverse.animevibe.ui.common.CustomModalBottomSheet
import com.luminoverse.animevibe.ui.common.ImageAspectRatio
import com.luminoverse.animevibe.ui.common.ImageDisplay
import com.luminoverse.animevibe.ui.common.ImageRoundedCorner
import com.luminoverse.animevibe.ui.main.PlayerDisplayMode
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.MediaPlaybackAction
import com.luminoverse.animevibe.utils.media.MediaPlaybackService
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.resource.Resource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * The main VideoPlayer composable, responsible for displaying the player view, controls, overlays,
 * and handling service connections and side effects related to playback.
 */
@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayer(
    episodeDetailComplement: EpisodeDetailComplement,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    imagePlaceholder: String?,
    isRefreshing: Boolean,
    networkDataSource: NetworkDataSource,
    coreState: PlayerCoreState,
    controlsStateFlow: StateFlow<ControlsState>,
    playerAction: (HlsPlayerAction) -> Unit,
    isLandscape: Boolean,
    player: ExoPlayer,
    captureScreenshot: suspend () -> String?,
    updateStoredWatchState: (Long?, Long?, String?) -> Unit,
    isScreenOn: Boolean,
    screenHeightPx: Float,
    isAutoPlayVideo: Boolean,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery, Boolean) -> Unit,
    displayMode: PlayerDisplayMode,
    setPlayerDisplayMode: (PlayerDisplayMode) -> Unit,
    onEnterSystemPipMode: () -> Unit,
    isSideSheetVisible: Boolean,
    setSideSheetVisibility: (Boolean) -> Unit,
    isAutoplayNextEpisodeEnabled: Boolean,
    setAutoplayNextEpisodeEnabled: (Boolean) -> Unit,
    rememberedTopPadding: Dp,
    verticalDragOffset: Animatable<Float, *>,
    maxVerticalDrag: Float,
    pipDragProgress: Float,
    pipWidth: Dp,
    pipEndDestinationPx: Offset,
    pipEndSizePx: IntSize,
    onMaxDragAmountCalculated: (Float) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val landscapeEpisodeListState = rememberLazyListState()

    // region Service Connection and Player Setup
    val playerView = remember { PlayerView(context).apply { useController = false } }
    var mediaBrowser by remember { mutableStateOf<MediaBrowserCompat?>(null) }
    var mediaPlaybackService by remember { mutableStateOf<MediaPlaybackService?>(null) }

    fun setupPlayer() {
        playerView.player = player
        val videoSurface = playerView.videoSurfaceView
        playerAction(HlsPlayerAction.SetVideoSurface(videoSurface))

        mediaPlaybackService?.dispatch(
            MediaPlaybackAction.SetEpisodeData(
                complement = episodeDetailComplement,
                episodes = episodes,
                query = episodeSourcesQuery,
                handleSelectedEpisodeServer = { episodeQuery ->
                    handleSelectedEpisodeServer(episodeQuery, false)
                }
            )
        )
    }

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d("VideoPlayer", "Service connected")
                val binder = service as MediaPlaybackService.MediaPlaybackBinder
                mediaPlaybackService = binder.getService()
                setupPlayer()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w("VideoPlayer", "Service disconnected")
                mediaPlaybackService = null
            }
        }
    }

    LaunchedEffect(player) {
        if (player.playbackState == Player.STATE_IDLE) {
            setupPlayer()
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, MediaPlaybackService::class.java)
        try {
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e("VideoPlayer", "Failed to bind service", e)
        }
        onDispose {
            mediaBrowser?.disconnect()
            playerAction(HlsPlayerAction.Reset)
            mediaPlaybackService?.dispatch(MediaPlaybackAction.ClearMediaData)
            mediaPlaybackService?.dispatch(MediaPlaybackAction.StopService)
            try {
                context.unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                Log.w("VideoPlayer", "Service was not registered or already unbound.", e)
            }
            mediaPlaybackService = null
        }
    }
    // endregion

    // region Side Effects
    LaunchedEffect(episodeDetailComplement.sources.link.file) {
        playerAction(
            HlsPlayerAction.SetMedia(
                videoData = episodeDetailComplement.sources,
                isAutoPlayVideo = isAutoPlayVideo,
                currentPosition = episodeDetailComplement.lastTimestamp ?: 0,
                duration = episodeDetailComplement.duration ?: 0
            )
        )
        setupPlayer()
    }

    LaunchedEffect(isScreenOn) {
        if (!isScreenOn) {
            playerAction(HlsPlayerAction.Pause)
        }
    }
    // endregion

    // region State Holders
    val topPaddingPx = with(LocalDensity.current) { rememberedTopPadding.toPx() }
    val maxUpwardDrag = screenHeightPx * -0.5f
    val maxDownwardDragLandscape = screenHeightPx * -0.4f
    val videoPlayerState = rememberVideoPlayerState(
        key = episodeDetailComplement.sources.link.file,
        player = player,
        onPlayerAction = playerAction,
        networkDataSource = networkDataSource,
        episodeDetails = episodeDetailComplement,
        controlsStateFlow = controlsStateFlow,
        coreState = coreState,
        displayMode = displayMode,
        setPlayerDisplayMode = setPlayerDisplayMode,
        setSideSheetVisibility = setSideSheetVisibility,
        isLandscape = isLandscape,
        pipEndDestinationPx = pipEndDestinationPx,
        pipEndSizePx = pipEndSizePx,
        onMaxDragAmountCalculated = onMaxDragAmountCalculated,
        updateStoredWatchState = updateStoredWatchState,
        captureScreenshot = captureScreenshot
    )
    val controlsState by controlsStateFlow.collectAsStateWithLifecycle()
    val currentPositionMs by videoPlayerState.currentPositionMs.collectAsStateWithLifecycle()
    // endregion

    // region Local UI State & Derived State
    val animatedZoom by animateFloatAsState(
        targetValue = videoPlayerState.zoomScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PlayerZoomAnimation"
    )

    val currentEpisode = episodes.find { it.id == episodeDetailComplement.id } ?: episodes.first()
    val prevEpisode = episodes.find { it.episode_no == currentEpisode.episode_no - 1 }
    val nextEpisode = episodes.find { it.episode_no == currentEpisode.episode_no + 1 }
    val episodesToShow = episodes.filter { episode -> episode.id != currentEpisode.id }
    LaunchedEffect(episodes, currentEpisode) {
        nextEpisode?.let {
            val nextIndex = episodesToShow.indexOfFirst { episode -> episode.id == it.id }
            if (nextIndex != -1) {
                landscapeEpisodeListState.scrollToItem(nextIndex)
            }
        }
    }

    val shouldShowResumeOverlay = !isAutoPlayVideo && videoPlayerState.shouldShowResumeOverlay &&
            episodeDetailComplement.lastTimestamp != null &&
            coreState.playbackState == Player.STATE_READY && !coreState.isPlaying

    val isPlayerDisplayFullscreen = displayMode in listOf(
        PlayerDisplayMode.FULLSCREEN_PORTRAIT,
        PlayerDisplayMode.FULLSCREEN_LANDSCAPE
    )
    val isPlayerDisplayPip =
        displayMode in listOf(PlayerDisplayMode.PIP, PlayerDisplayMode.SYSTEM_PIP)

    val isOverlayVisible =
        displayMode != PlayerDisplayMode.SYSTEM_PIP && !controlsState.isLocked && verticalDragOffset.value == 0f

    val animatedCornerRadius by animateDpAsState(
        targetValue = if (displayMode == PlayerDisplayMode.PIP || verticalDragOffset.value != 0f) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "cornerRadiusAnimation"
    )

    val currentLandscapeDragProgress = if (videoPlayerState.playerContainerSize.height > 0f) {
        (verticalDragOffset.value / videoPlayerState.playerContainerSize.height).coerceIn(
            0f, 1f
        )
    } else 0f
    val landscapeBackgroundAlpha = lerp(0f, 0.6f, currentLandscapeDragProgress * 2)
    val landscapeEpisodeListProgress = if (maxDownwardDragLandscape != 0f) {
        (verticalDragOffset.value / maxDownwardDragLandscape).coerceIn(0f, 1f)
    } else 0f
    // endregion

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { videoPlayerState.playerContainerSize = it }
            .background(Color.Black.copy(alpha = if (isLandscape) landscapeBackgroundAlpha else 0f))
            .graphicsLayer {
                if (isLandscape) {
                    if (verticalDragOffset.value < 0) return@graphicsLayer
                    val scale = lerp(1f, 0.8f, currentLandscapeDragProgress * 2)
                    scaleX = scale
                    scaleY = scale
                    translationY = verticalDragOffset.value
                } else {
                    if (verticalDragOffset.value < 0) {
                        val scale = lerp(
                            1f,
                            1.1f,
                            (verticalDragOffset.value / maxUpwardDrag).coerceIn(0f, 1f) * 6
                        )
                        val playerHeight = videoPlayerState.playerContainerSize.height.toFloat()
                        scaleX = scale
                        scaleY = scale
                        translationY = -(playerHeight * (scale - 1)) / 2
                    } else if (verticalDragOffset.value > 0) {
                        val playerWidth = videoPlayerState.playerContainerSize.width.toFloat()
                        val playerHeight = videoPlayerState.playerContainerSize.height.toFloat()

                        if (playerWidth > 0f && playerHeight > 0f) {
                            val targetScale = min(
                                pipEndSizePx.width / playerWidth,
                                pipEndSizePx.height / playerHeight
                            ).coerceAtLeast(0.1f)

                            val finalTranslationX =
                                (pipEndDestinationPx.x + pipEndSizePx.width / 2f) - (playerWidth / 2f)
                            val finalTranslationY =
                                (pipEndDestinationPx.y + pipEndSizePx.height / 2f) - (playerHeight / 2f)

                            translationX = lerp(0f, finalTranslationX, pipDragProgress)
                            translationY = lerp(0f, finalTranslationY, pipDragProgress)
                            scaleX = lerp(1f, targetScale, pipDragProgress)
                            scaleY = lerp(1f, targetScale, pipDragProgress)
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(animatedCornerRadius))
                .clipToBounds()
                .pointerInput(
                    episodeDetailComplement.sources.link.file,
                    displayMode,
                    controlsState.zoom,
                    isLandscape
                ) {
                    coroutineScope {
                        awaitEachGesture {
                            try {
                                handleGestures(
                                    state = videoPlayerState,
                                    isPlayerDisplayFullscreen = isPlayerDisplayFullscreen,
                                    isLandscape = isLandscape,
                                    topPaddingPx = topPaddingPx,
                                    onVerticalDrag = { delta ->
                                        scope.launch {
                                            if (isLandscape) {
                                                val currentOffset = verticalDragOffset.value
                                                val newOffset = currentOffset + delta
                                                val dismissMaxDrag =
                                                    videoPlayerState.playerContainerSize.height.toFloat() * 0.5f

                                                if (currentOffset < 0 || (currentOffset == 0f && delta < 0)) {
                                                    verticalDragOffset.snapTo(
                                                        newOffset.coerceIn(
                                                            maxDownwardDragLandscape, 0f
                                                        )
                                                    )
                                                } else {
                                                    verticalDragOffset.snapTo(
                                                        newOffset.coerceIn(
                                                            0f, dismissMaxDrag
                                                        )
                                                    )
                                                }
                                            } else {
                                                val newOffset = (verticalDragOffset.value + delta)
                                                    .coerceIn(maxUpwardDrag, maxVerticalDrag)
                                                verticalDragOffset.snapTo(newOffset)
                                            }
                                        }
                                    },
                                    onDragEnd = { flingVelocity ->
                                        val flingThreshold = 1.8f
                                        scope.launch {
                                            if (isLandscape) {
                                                val currentOffset = verticalDragOffset.value
                                                if (currentOffset < 0) {
                                                    val threshold = maxDownwardDragLandscape * 0.5f
                                                    if (currentOffset < threshold || flingVelocity < -flingThreshold) {
                                                        verticalDragOffset.animateTo(
                                                            maxDownwardDragLandscape
                                                        )
                                                        videoPlayerState.showLandscapeEpisodeList =
                                                            true
                                                        verticalDragOffset.snapTo(0f)
                                                    } else {
                                                        verticalDragOffset.animateTo(0f)
                                                        videoPlayerState.showLandscapeEpisodeList =
                                                            false
                                                    }
                                                } else {
                                                    val threshold =
                                                        videoPlayerState.playerContainerSize.height.toFloat() * 0.5f
                                                    if (currentOffset >= threshold) {
                                                        setPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN_PORTRAIT)
                                                    }
                                                    verticalDragOffset.animateTo(
                                                        0f,
                                                        animationSpec = spring()
                                                    )
                                                }
                                            } else {
                                                val pipPositionThreshold = maxVerticalDrag * 0.5f

                                                val shouldGoToPip =
                                                    (flingVelocity > flingThreshold) || (verticalDragOffset.value > pipPositionThreshold)
                                                val shouldGoToLandscape =
                                                    (flingVelocity < -flingThreshold && verticalDragOffset.value < 0)
                                                            || (verticalDragOffset.value < maxUpwardDrag * 0.28f)

                                                when {
                                                    shouldGoToPip -> {
                                                        verticalDragOffset.animateTo(
                                                            maxVerticalDrag,
                                                            spring(stiffness = 400f)
                                                        )
                                                        setPlayerDisplayMode(PlayerDisplayMode.PIP)
                                                        verticalDragOffset.snapTo(0f)
                                                    }

                                                    shouldGoToLandscape -> {
                                                        setPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN_LANDSCAPE)
                                                    }

                                                    else -> {
                                                        verticalDragOffset.animateTo(0f, spring())
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            } finally {
                                if (videoPlayerState.isSpeedingUpWithLongPress) videoPlayerState.handleLongPressEnd()
                                if (videoPlayerState.isZooming) videoPlayerState.isZooming = false
                            }
                        }
                    }
                }
        ) {
            val borderModifier =
                if (videoPlayerState.isZooming && isOverlayVisible) Modifier.border(
                    16.dp,
                    Color.White.copy(alpha = 0.5f)
                ) else Modifier

            if (videoPlayerState.isInitialLoading) {
                ImageDisplay(
                    modifier = Modifier.fillMaxSize(),
                    image = episodeDetailComplement.screenshot,
                    imagePlaceholder = imagePlaceholder,
                    ratio = ImageAspectRatio.WIDESCREEN.ratio,
                    contentDescription = "Thumbnail Image",
                    roundedCorners = ImageRoundedCorner.NONE
                )
            } else {
                AndroidView(
                    factory = { playerView },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .then(borderModifier)
                        .then(
                            if (isPlayerDisplayFullscreen) {
                                Modifier.graphicsLayer(
                                    scaleX = animatedZoom,
                                    scaleY = animatedZoom,
                                    translationX = videoPlayerState.panOffsetX,
                                    translationY = videoPlayerState.panOffsetY
                                )
                            } else Modifier
                        ),
                    update = { view -> view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT }
                )
            }
        }

        // PIP Mode Seek Bar
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = displayMode == PlayerDisplayMode.PIP,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CustomSeekBar(
                currentPosition = currentPositionMs,
                bufferedPosition = player.bufferedPosition,
                duration = player.duration.takeIf { it > 0 } ?: episodeDetailComplement.duration
                ?: 0,
                intro = episodeDetailComplement.sources.intro,
                outro = episodeDetailComplement.sources.outro,
                seekAmount = videoPlayerState.seekAmountSeconds * 1000L,
                dragCancelTrigger = videoPlayerState.seekBarDragCancellationId,
                isShowSeekIndicator = videoPlayerState.seekIndicatorDirection,
                touchTargetHeight = 3.dp,
                trackHeight = 3.dp
            )
        }

        // Thumbnail Preview on Seek
        val thumbnailTrackUrl =
            episodeDetailComplement.sources.tracks.find { it.kind == "thumbnails" }?.file
        thumbnailTrackUrl?.let { url ->
            val showThumbnail =
                videoPlayerState.isBufferingFromSeeking || (!coreState.isPlaying && videoPlayerState.isDraggingSeekBar && !videoPlayerState.isInitialLoading)
            val thumbnailSeekPositionKey =
                remember(videoPlayerState.dragSeekPositionMs) { (videoPlayerState.dragSeekPositionMs / 10000L) * 10000L }

            AnimatedVisibility(visible = showThumbnail, enter = fadeIn(), exit = fadeOut()) {
                ThumbnailPreview(
                    modifier = Modifier.fillMaxSize(),
                    seekPosition = thumbnailSeekPositionKey,
                    cues = videoPlayerState.thumbnailCues[url]
                )
            }
        }

        // Subtitle View
        val animatedSubtitleBottomPadding by animateDpAsState(
            targetValue = if (isOverlayVisible && controlsState.isControlsVisible && isLandscape && displayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE) videoPlayerState.bottomBarHeight.dp else if (isSideSheetVisible) 16.dp else 8.dp,
            label = "SubtitleBottomPadding"
        )
        CustomSubtitleView(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = animatedSubtitleBottomPadding, start = 8.dp, end = 8.dp),
            cues = videoPlayerState.activeCaptionCue,
            isLandscape = isLandscape,
            isPipMode = isPlayerDisplayPip,
            pipWidth = pipWidth
        )

        // Player Controls Overlay
        val isPlayerControlsVisible =
            (controlsState.isControlsVisible || videoPlayerState.isDraggingSeekBar) && !shouldShowResumeOverlay && isOverlayVisible && isPlayerDisplayFullscreen && !videoPlayerState.showLandscapeEpisodeList
        AnimatedVisibility(visible = isPlayerControlsVisible, enter = fadeIn(), exit = fadeOut()) {
            PlayerControls(
                isPlaying = coreState.isPlaying,
                currentPosition = currentPositionMs,
                bufferedPosition = player.bufferedPosition,
                duration = player.duration.takeIf { it > 0 } ?: episodeDetailComplement.duration
                ?: 0,
                playbackState = coreState.playbackState,
                isRefreshing = isRefreshing,
                setDisplayModePip = {
                    scope.launch {
                        verticalDragOffset.animateTo(maxVerticalDrag, spring(stiffness = 400f))
                        setPlayerDisplayMode(PlayerDisplayMode.PIP)
                        verticalDragOffset.snapTo(0f)
                    }
                },
                episodeDetailComplement = episodeDetailComplement,
                hasPreviousEpisode = prevEpisode != null,
                nextEpisode = nextEpisode,
                nextEpisodeDetailComplement = episodeDetailComplements[nextEpisode?.id]?.data,
                isSideSheetVisible = isSideSheetVisible,
                setSideSheetVisibility = setSideSheetVisibility,
                isLandscape = isLandscape,
                isShowSpeedUp = videoPlayerState.isSpeedingUpWithLongPress,
                zoomText = videoPlayerState.getZoomRatioText(),
                onZoomReset = { videoPlayerState.resetZoom() },
                handlePlay = { playerAction(HlsPlayerAction.Play) },
                handlePause = { playerAction(HlsPlayerAction.Pause) },
                onPreviousEpisode = {
                    prevEpisode?.let {
                        handleSelectedEpisodeServer(
                            episodeSourcesQuery.copy(id = it.id),
                            false
                        )
                    }
                },
                onNextEpisode = {
                    nextEpisode?.let {
                        handleSelectedEpisodeServer(
                            episodeSourcesQuery.copy(id = it.id), false
                        )
                    }
                },
                onSeekTo = { position -> playerAction(HlsPlayerAction.SeekTo(position)) },
                seekAmount = videoPlayerState.seekAmountSeconds * 1000L,
                isShowSeekIndicator = videoPlayerState.seekIndicatorDirection,
                dragSeekPosition = videoPlayerState.dragSeekPositionMs,
                dragCancelTrigger = videoPlayerState.seekBarDragCancellationId,
                onDraggingSeekBarChange = { isDragging, positionMs ->
                    videoPlayerState.isDraggingSeekBar = isDragging
                    videoPlayerState.dragSeekPositionMs = positionMs
                },
                isDraggingSeekBar = videoPlayerState.isDraggingSeekBar,
                showRemainingTime = videoPlayerState.showRemainingTime,
                setShowRemainingTime = { videoPlayerState.showRemainingTime = it },
                onSettingsClick = { videoPlayerState.showSettingsSheet = true },
                isAutoplayPlayNextEpisode = isAutoplayNextEpisodeEnabled,
                onAutoplayNextEpisodeToggle = { enabled ->
                    setAutoplayNextEpisodeEnabled(enabled)
                    videoPlayerState.onAutoplayNextEpisodeToggle(enabled)
                },
                onFullscreenToggle = { setPlayerDisplayMode(if (displayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE) PlayerDisplayMode.FULLSCREEN_PORTRAIT else PlayerDisplayMode.FULLSCREEN_LANDSCAPE) },
                onBottomBarMeasured = { height -> videoPlayerState.bottomBarHeight = height }
            )
        }

        // Center Indicators (Loading, Seek, etc.)
        LoadingIndicator(
            modifier = Modifier.align(Alignment.Center),
            isVisible = (coreState.playbackState == Player.STATE_BUFFERING || isRefreshing) && displayMode != PlayerDisplayMode.PIP && !isPlayerControlsVisible
        )
        SeekIndicator(
            modifier = Modifier.align(Alignment.Center),
            seekDirection = videoPlayerState.seekIndicatorDirection,
            seekAmount = videoPlayerState.seekAmountSeconds,
            isLandscape = isLandscape,
            isFullscreen = isPlayerDisplayFullscreen
        )

        // Skip Intro/Outro Buttons
        val isSkipVisible =
            displayMode != PlayerDisplayMode.SYSTEM_PIP && !controlsState.isLocked && isOverlayVisible && !videoPlayerState.isSpeedingUpWithLongPress && !videoPlayerState.isDraggingSeekBar && coreState.playbackState !in listOf(
                Player.STATE_ENDED,
                Player.STATE_IDLE
            ) && !shouldShowResumeOverlay && !videoPlayerState.isInitialLoading
        val skipButtonPadding =
            if (displayMode == PlayerDisplayMode.PIP) 8.dp else if (!isLandscape) 60.dp else 120.dp
        SkipButtonsContainer(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = skipButtonPadding, bottom = skipButtonPadding),
            currentPosition = currentPositionMs,
            duration = player.duration,
            intro = episodeDetailComplement.sources.intro,
            outro = episodeDetailComplement.sources.outro,
            isSkipVisible = isSkipVisible,
            onSkip = { seekPosition -> playerAction(HlsPlayerAction.SeekTo(seekPosition)) }
        )

        // Top-Aligned Indicators
        SpeedUpIndicator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isLandscape) 32.dp else 0.dp),
            isVisible = videoPlayerState.isSpeedingUpWithLongPress && isOverlayVisible,
            speedText = videoPlayerState.speedUpIndicatorText
        )
        ZoomIndicator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isLandscape) 32.dp else 0.dp),
            visible = videoPlayerState.isZooming && isOverlayVisible,
            isShowSpeedUp = videoPlayerState.isSpeedingUpWithLongPress,
            zoomText = videoPlayerState.getZoomRatioText(),
            isClickable = videoPlayerState.zoomScale > 1f,
            onClick = { videoPlayerState.resetZoom() }
        )
        AutoplayNextEpisodeStatusIndicator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isLandscape) 100.dp else 68.dp),
            statusText = videoPlayerState.autoplayStatusText
        )

        // Top-Right Aligned Components
        SeekCancelPreview(
            modifier = Modifier.align(Alignment.TopEnd),
            visible = videoPlayerState.isDraggingSeekBar && isOverlayVisible && !videoPlayerState.isSpeedingUpWithLongPress,
            captureScreenshot = captureScreenshot,
            imageUrl = episodeDetailComplement.imageUrl,
            onCancelSeekBarDrag = {
                videoPlayerState.cancelSeekBarDrag()
                playerAction(HlsPlayerAction.Play)
                playerAction(HlsPlayerAction.RequestToggleControlsVisibility(false))
            },
        )
        LockButton(
            visible = controlsState.isLocked && !isPlayerDisplayPip && videoPlayerState.showLockReminder,
            onClick = { playerAction(HlsPlayerAction.ToggleLock(false)) },
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Fullscreen Overlays
        episodeDetailComplement.lastTimestamp?.let { lastTimestamp ->
            ResumePlaybackOverlay(
                modifier = Modifier.align(Alignment.Center),
                isVisible = shouldShowResumeOverlay,
                isPipMode = isPlayerDisplayPip,
                lastTimestamp = lastTimestamp,
                onDismiss = {
                    videoPlayerState.shouldShowResumeOverlay = false
                    playerAction(HlsPlayerAction.RequestToggleControlsVisibility(true))
                },
                onRestart = {
                    playerAction(HlsPlayerAction.SeekTo(0))
                    playerAction(HlsPlayerAction.Play)
                    videoPlayerState.shouldShowResumeOverlay = false
                },
                onResume = {
                    playerAction(HlsPlayerAction.SeekTo(it))
                    playerAction(HlsPlayerAction.Play)
                    videoPlayerState.shouldShowResumeOverlay = false
                }
            )
        }

        // Landscape Episode List
        AnimatedVisibility(
            visible = isPlayerDisplayFullscreen && isLandscape && (verticalDragOffset.value < 0 || videoPlayerState.showLandscapeEpisodeList),
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            LandscapeEpisodeList(
                modifier = Modifier.graphicsLayer {
                    translationY = if (videoPlayerState.showLandscapeEpisodeList) {
                        0f
                    } else {
                        lerp(
                            start = videoPlayerState.playerContainerSize.height.toFloat(),
                            stop = 0f,
                            fraction = landscapeEpisodeListProgress
                        )
                    }
                },
                listState = landscapeEpisodeListState,
                episodesToShow = episodesToShow,
                imagePlaceholder = imagePlaceholder,
                episodeDetailComplements = episodeDetailComplements,
                onEpisodeSelected = { episode ->
                    handleSelectedEpisodeServer(
                        episodeSourcesQuery.copy(id = episode.id),
                        false
                    )
                },
                onClose = {
                    scope.launch {
                        verticalDragOffset.snapTo(maxDownwardDragLandscape)
                        videoPlayerState.showLandscapeEpisodeList = false
                        verticalDragOffset.animateTo(0f)
                    }
                }
            )
        }

        nextEpisode?.let {
            NextEpisodeOverlay(
                modifier = Modifier.align(Alignment.Center),
                isVisible = videoPlayerState.shouldShowNextEpisodeOverlay,
                isOnlyShowEpisodeDetail = isPlayerDisplayPip,
                isLandscape = isLandscape,
                animeImage = episodeDetailComplement.imageUrl,
                nextEpisode = it,
                nextEpisodeDetailComplement = episodeDetailComplements[it.id]?.data,
                isAutoplayNextEpisode = isAutoplayNextEpisodeEnabled,
                onDismiss = {
                    videoPlayerState.shouldShowNextEpisodeOverlay = false
                    playerAction(HlsPlayerAction.RequestToggleControlsVisibility(true))
                },
                onRestart = {
                    playerAction(HlsPlayerAction.SeekTo(0))
                    playerAction(HlsPlayerAction.Play)
                    videoPlayerState.shouldShowNextEpisodeOverlay = false
                },
                onPlayNext = {
                    handleSelectedEpisodeServer(episodeSourcesQuery.copy(id = it.id), false)
                    videoPlayerState.shouldShowNextEpisodeOverlay = false
                }
            )
        }

        // Bottom Sheets
        val settingsSheetConfig =
            BottomSheetConfig(landscapeWidthFraction = 0.4f, landscapeHeightFraction = 0.7f)
        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = videoPlayerState.showSettingsSheet && isOverlayVisible,
            isLandscape = isLandscape,
            config = settingsSheetConfig,
            onDismiss = { videoPlayerState.showSettingsSheet = false }) {
            SettingsContent(
                onDismiss = { videoPlayerState.showSettingsSheet = false },
                onLockClick = {
                    videoPlayerState.showLockReminder = true
                    setPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN_LANDSCAPE)
                    if (coreState.playbackState == Player.STATE_ENDED) playerAction(
                        HlsPlayerAction.SeekTo(
                            0
                        )
                    )
                    playerAction(HlsPlayerAction.Play)
                    playerAction(HlsPlayerAction.ToggleLock(true))
                },
                onPipClick = { onEnterSystemPipMode() },
                selectedPlaybackSpeed = controlsState.playbackSpeed,
                onPlaybackSpeedClick = { videoPlayerState.showPlaybackSpeedSheet = true },
                isSubtitleAvailable = episodeDetailComplement.sources.tracks.any { it.kind == "captions" },
                selectedSubtitle = controlsState.selectedSubtitle,
                onSubtitleClick = { videoPlayerState.showSubtitleSheet = true }
            )
        }
        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = videoPlayerState.showSubtitleSheet && isOverlayVisible,
            isLandscape = isLandscape,
            config = settingsSheetConfig,
            onDismiss = { videoPlayerState.showSubtitleSheet = false }) {
            SubtitleContent(
                tracks = episodeDetailComplement.sources.tracks,
                selectedSubtitle = controlsState.selectedSubtitle,
                onSubtitleSelected = { subtitleTrack ->
                    playerAction(HlsPlayerAction.SetSubtitle(subtitleTrack))
                    videoPlayerState.showSubtitleSheet = false
                }
            )
        }
        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = videoPlayerState.showPlaybackSpeedSheet && isOverlayVisible,
            isLandscape = isLandscape,
            config = settingsSheetConfig,
            onDismiss = { videoPlayerState.showPlaybackSpeedSheet = false }) {
            PlaybackSpeedContent(
                selectedPlaybackSpeed = controlsState.playbackSpeed,
                onSpeedChange = { speed ->
                    playerAction(HlsPlayerAction.SetPlaybackSpeed(speed))
                    videoPlayerState.showPlaybackSpeedSheet = false
                }
            )
        }
    }
}