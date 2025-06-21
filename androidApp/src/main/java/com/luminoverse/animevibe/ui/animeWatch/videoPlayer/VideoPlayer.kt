package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.animeWatch.PlayerUiState
import com.luminoverse.animevibe.ui.common.BottomSheetConfig
import com.luminoverse.animevibe.ui.common.CustomModalBottomSheet
import com.luminoverse.animevibe.ui.common.ScreenshotDisplay
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.media.PositionState
import com.luminoverse.animevibe.utils.resource.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private const val FAST_FORWARD_REWIND_DEBOUNCE_MILLIS = 1000L
private const val DEFAULT_SEEK_INCREMENT = 10000L
private const val LONG_PRESS_THRESHOLD_MILLIS = 500L
private const val DOUBLE_TAP_THRESHOLD_MILLIS = 300L
private const val MAX_ZOOM_RATIO = 8f

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    playerView: PlayerView,
    player: ExoPlayer,
    coreState: PlayerCoreState,
    playerUiState: PlayerUiState,
    controlsState: StateFlow<ControlsState>,
    positionState: StateFlow<PositionState>,
    playerAction: (HlsPlayerAction) -> Unit,
    mediaController: MediaControllerCompat?,
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
    val controlsState by controlsState.collectAsStateWithLifecycle()
    val positionState by positionState.collectAsStateWithLifecycle()
    val currentEpisode = episodeDetailComplement.number
    val prevEpisode = episodes.find { it.episode_no == currentEpisode - 1 }
    val nextEpisode = episodes.find { it.episode_no == currentEpisode + 1 }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFirstLoad by remember { mutableStateOf(true) }

    // Gesture and UI states
    var isShowSeekIndicator by remember { mutableIntStateOf(0) }
    var seekAmount by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var fastForwardRewindCounter by remember { mutableIntStateOf(0) }
    var previousPlaybackSpeed by remember { mutableFloatStateOf(controlsState.playbackSpeed) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var zoomScaleProgress by remember { mutableFloatStateOf(controlsState.zoom) }
    var isZooming by remember { mutableStateOf(false) }
    var speedUpText by remember { mutableStateOf("") }
    var isHolding by remember { mutableStateOf(false) }
    var showLockReminder by remember { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(false) }
    var isDraggingSeekBar by remember { mutableStateOf(false) }
    var dragSeekPosition by remember { mutableLongStateOf(0L) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapX by remember { mutableStateOf<Float?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showPlaybackSpeedSheet by remember { mutableStateOf(false) }

    var seekDisplayHandler by remember { mutableStateOf<Handler?>(null) }
    var seekDisplayRunnable by remember { mutableStateOf<Runnable?>(null) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }

    val thumbnailTrackUrl = remember(episodeDetailComplement) {
        episodeDetailComplement.sources.tracks.find { it.kind == "thumbnails" }?.file
    }
    var isBufferingFromSeeking by remember { mutableStateOf(false) }
    var bottomBarHeight by remember { mutableFloatStateOf(0f) }
    var playerSize by remember { mutableStateOf(IntSize.Zero) }
    var videoSize by remember { mutableStateOf(player.videoSize) }

    val animatedZoom by animateFloatAsState(
        targetValue = zoomScaleProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PlayerZoomAnimation"
    )

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(newVideoSize: VideoSize) {
                videoSize = newVideoSize
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    val zoomToFillRatio by remember(playerSize, videoSize) {
        mutableFloatStateOf(
            if (playerSize.width > 0 && playerSize.height > 0 && videoSize.width > 0 && videoSize.height > 0) {
                val containerWidth = playerSize.width.toFloat()
                val containerHeight = playerSize.height.toFloat()
                val vWidth = videoSize.width.toFloat()
                val vHeight = videoSize.height.toFloat()

                val containerAspect = containerWidth / containerHeight
                val videoAspect = vWidth / vHeight

                val fittedWidth: Float
                val fittedHeight: Float
                if (videoAspect > containerAspect) {
                    fittedWidth = containerWidth
                    fittedHeight = containerWidth / videoAspect
                } else {
                    fittedHeight = containerHeight
                    fittedWidth = containerHeight * videoAspect
                }
                max(
                    containerWidth / fittedWidth,
                    containerHeight / fittedHeight
                ).coerceAtLeast(1f)
            } else {
                1f
            }
        )
    }

    LaunchedEffect(Unit) {
        seekDisplayHandler = Handler(Looper.getMainLooper())
        seekDisplayRunnable = Runnable {
            Log.d("PlayerView", "Debounce period ended. Performing accumulated seeks.")
            val totalSeekSeconds = fastForwardRewindCounter
            val fixedSeekPerCall = (DEFAULT_SEEK_INCREMENT / 1000L).toInt()

            if (totalSeekSeconds != 0) {
                val numCalls = abs(totalSeekSeconds) / fixedSeekPerCall
                repeat(numCalls) {
                    if (totalSeekSeconds > 0) {
                        playerAction(HlsPlayerAction.FastForward)
                    } else {
                        playerAction(HlsPlayerAction.Rewind)
                    }
                }
            }

            isShowSeekIndicator = 0
            seekAmount = 0L
            isSeeking = false
            fastForwardRewindCounter = 0
            playerAction(HlsPlayerAction.Play)
            Log.d("PlayerView", "Seek actions completed and states reset.")
        }
    }

    LaunchedEffect(isLandscape, playerUiState.isFullscreen) {
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        playerView.postDelayed({
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }, 1)
    }

    LaunchedEffect(coreState.playbackState, isDraggingSeekBar) {
        if (isDraggingSeekBar && (coreState.playbackState == Player.STATE_READY || coreState.playbackState == Player.STATE_BUFFERING)
        ) {
            isBufferingFromSeeking = true
        } else if (coreState.playbackState != Player.STATE_BUFFERING) {
            isBufferingFromSeeking = false
        }
    }

    LaunchedEffect(coreState, player.isPlaying) {
        if (coreState.playbackState == Player.STATE_READY && player.isPlaying) {
            isFirstLoad = false
        }
    }

    LaunchedEffect(showLockReminder) {
        if (showLockReminder && controlsState.isLocked) {
            delay(3000)
            showLockReminder = false
        }
    }

    val calculatedShouldShowResumeOverlay = !isAutoPlayVideo && playerUiState.isShowResume &&
            episodeDetailComplement.lastTimestamp != null &&
            coreState.playbackState == Player.STATE_READY && !player.isPlaying

    DisposableEffect(
        mediaController,
        playerUiState.isPipMode,
        controlsState.isLocked,
        calculatedShouldShowResumeOverlay,
        playerUiState.isShowNextEpisode
    ) {
        onDispose {
            seekDisplayHandler?.removeCallbacksAndMessages(null)
            longPressJob?.cancel()
            if (isHolding) {
                isHolding = false
                playerAction(HlsPlayerAction.SetPlaybackSpeed(previousPlaybackSpeed))
            }
            Log.d("PlayerView", "PlayerView disposed, MediaControllerCallback unregistered")
        }
    }

    fun getZoomRatioText(): String {
        return when {
            abs(zoomScaleProgress - zoomToFillRatio) < 0.01f && zoomToFillRatio > 1f -> "Full View"
            zoomScaleProgress > 1f -> String.format(Locale.US, "%.1fx", zoomScaleProgress)
            else -> "Original"
        }
    }

    fun resetZoom() {
        zoomScaleProgress = 1f
        playerAction(HlsPlayerAction.SetZoom(1f))
        offsetX = 0f
        offsetY = 0f
    }

    fun handleSingleTap() {
        if (!controlsState.isLocked && !isHolding) {
            playerAction(HlsPlayerAction.RequestToggleControlsVisibility())
            Log.d("PlayerView", "Single tap: Toggled controls visibility")
        } else if (controlsState.isLocked) {
            showLockReminder = true
            Log.d("PlayerView", "Single tap: Player is locked, showing lock reminder.")
        }
    }

    fun handleDoubleTap(x: Float, screenWidth: Float) {
        if (!controlsState.isLocked && coreState.playbackState != Player.STATE_IDLE && !isFirstLoad) {
            Log.d("PlayerView", "Double tap at x=$x")
            val newSeekDirection = when {
                x < screenWidth * 0.4 -> -1
                x > screenWidth * 0.6 -> 1
                else -> 0
            }

            if (newSeekDirection != 0) {
                seekDisplayHandler?.removeCallbacks(seekDisplayRunnable!!)
                playerAction(HlsPlayerAction.Pause)
                val seekIncrementSeconds = (DEFAULT_SEEK_INCREMENT / 1000L)

                if (isShowSeekIndicator != newSeekDirection && isShowSeekIndicator != 0) {
                    seekAmount = seekIncrementSeconds
                    fastForwardRewindCounter = newSeekDirection * seekIncrementSeconds.toInt()
                } else {
                    seekAmount += seekIncrementSeconds
                    fastForwardRewindCounter += newSeekDirection * seekIncrementSeconds.toInt()
                }

                isShowSeekIndicator = newSeekDirection
                isSeeking = true

                seekDisplayHandler?.postDelayed(
                    seekDisplayRunnable!!,
                    FAST_FORWARD_REWIND_DEBOUNCE_MILLIS
                )
            } else {
                playerAction(HlsPlayerAction.Play)
                seekDisplayHandler?.removeCallbacks(seekDisplayRunnable!!)
                if (fastForwardRewindCounter == 0) {
                    isShowSeekIndicator = 0
                    seekAmount = 0L
                    isSeeking = false
                }
                fastForwardRewindCounter = 0
            }
        }
    }

    fun handleLongPressStart() {
        if (controlsState.isLocked || !player.isPlaying || coreState.playbackState != Player.STATE_IDLE && isFirstLoad) return
        isHolding = true
        speedUpText = "2x speed"
        previousPlaybackSpeed = controlsState.playbackSpeed
        playerAction(HlsPlayerAction.SetPlaybackSpeed(2f))
        Log.d("PlayerView", "Long press started: Speed set to 2x")
    }

    fun handleLongPressEnd() {
        if (isHolding) {
            isHolding = false
            speedUpText = ""
            playerAction(HlsPlayerAction.SetPlaybackSpeed(previousPlaybackSpeed))
            Log.d("PlayerView", "Long press ended")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { playerSize = it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()

                        if (controlsState.isControlsVisible && down.position.y > (playerSize.height - bottomBarHeight)) {
                            return@awaitEachGesture
                        }

                        longPressJob?.cancel()
                        longPressJob = scope.launch {
                            delay(LONG_PRESS_THRESHOLD_MILLIS)
                            handleLongPressStart()
                            down.consume()
                        }

                        var isMultiTouch = false
                        var isDragging = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val allFingersUp = event.changes.none { it.pressed }

                            if (allFingersUp) {
                                longPressJob?.cancel()
                                break
                            }

                            if (event.changes.size > 1 && !controlsState.isLocked) {
                                if (!isMultiTouch) {
                                    isMultiTouch = true
                                    isZooming = true
                                    playerAction(
                                        HlsPlayerAction.RequestToggleControlsVisibility(false)
                                    )
                                    longPressJob?.cancel()
                                    handleLongPressEnd()
                                }
                                val zoomChange = event.calculateZoom()
                                zoomScaleProgress =
                                    (zoomScaleProgress * zoomChange).coerceIn(1f, MAX_ZOOM_RATIO)

                                val (maxOffsetX, maxOffsetY) = calculateOffsetBounds(
                                    size,
                                    videoSize,
                                    zoomScaleProgress
                                )
                                offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)

                                event.changes.forEach { it.consume() }

                            } else if (
                                event.changes.size == 1 &&
                                !isMultiTouch &&
                                controlsState.zoom > 1f &&
                                !controlsState.isLocked
                            ) {
                                val pan = event.calculatePan()
                                if (pan.x != 0f || pan.y != 0f) {
                                    if (!isDragging) {
                                        isDragging = true
                                        playerAction(
                                            HlsPlayerAction.RequestToggleControlsVisibility(false)
                                        )
                                        longPressJob?.cancel()
                                        handleLongPressEnd()
                                    }

                                    val (maxOffsetX, maxOffsetY) = calculateOffsetBounds(
                                        size,
                                        videoSize,
                                        zoomScaleProgress
                                    )
                                    offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }

                        if (isMultiTouch) {
                            val halfWayRatio = (1f + zoomToFillRatio) / 2
                            val finalZoom = if (zoomScaleProgress < halfWayRatio) 1f
                            else if (zoomScaleProgress in halfWayRatio..zoomToFillRatio) zoomToFillRatio
                            else zoomScaleProgress

                            playerAction(HlsPlayerAction.SetZoom(finalZoom))
                            zoomScaleProgress = finalZoom

                            if (zoomScaleProgress <= 1.01f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                            isZooming = false
                        } else if (!isDragging) {
                            longPressJob?.cancel()
                            val currentTime = System.currentTimeMillis()
                            val tapX = down.position.x

                            if (isHolding) {
                                handleLongPressEnd()
                            } else {
                                if (lastTapTime > 0 && (currentTime - lastTapTime) < DOUBLE_TAP_THRESHOLD_MILLIS &&
                                    lastTapX != null && abs(tapX - lastTapX!!) < 100f
                                ) {
                                    handleDoubleTap(tapX, size.width.toFloat())
                                } else {
                                    handleSingleTap()
                                }
                                lastTapTime = currentTime
                                lastTapX = tapX
                            }
                        }
                    }
                }
        ) {
            val borderModifier = if (isZooming) {
                Modifier.border(16.dp, Color.White.copy(alpha = 0.5f))
            } else {
                Modifier
            }

            if (isFirstLoad) {
                ScreenshotDisplay(
                    imageUrl = episodeDetailComplement.imageUrl,
                    screenshot = episodeDetailComplement.screenshot,
                    modifier = Modifier.fillMaxSize(),
                    onClick = { handleSingleTap() }
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
                            translationX = offsetX,
                            translationY = offsetY
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
                        }
                        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        view.subtitleView?.setPadding(
                            0, 0, 0,
                            if (!playerUiState.isPipMode && controlsState.isControlsVisible && (isLandscape || !playerUiState.isFullscreen)) 100 else 0
                        )
                    }
                )
            }
        }


        val isPlayerControlsVisible = (controlsState.isControlsVisible || isDraggingSeekBar) &&
                !calculatedShouldShowResumeOverlay && !playerUiState.isPipMode && !controlsState.isLocked

        thumbnailTrackUrl?.let {
            val showThumbnail =
                isBufferingFromSeeking || (!player.isPlaying && isDraggingSeekBar && !isFirstLoad)
            val thumbnailSeekPositionKey = remember(dragSeekPosition) {
                (dragSeekPosition / 10000L) * 10000L
            }

            AnimatedVisibility(
                visible = showThumbnail,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ThumbnailPreview(
                    modifier = Modifier.fillMaxSize(),
                    seekPosition = thumbnailSeekPositionKey,
                    thumbnailTrackUrl = it
                )
            }
        }

        AnimatedVisibility(
            visible = isPlayerControlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControls(
                isPlaying = player.isPlaying,
                playbackState = coreState.playbackState,
                positionState = positionState,
                onHandleBackPress = onHandleBackPress,
                episodeDetailComplement = episodeDetailComplement,
                hasPreviousEpisode = prevEpisode != null,
                nextEpisode = nextEpisode,
                nextEpisodeDetailComplement = episodeDetailComplements[nextEpisode?.id]?.data,
                isSideSheetVisible = isSideSheetVisible,
                setSideSheetVisibility = setSideSheetVisibility,
                isLandscape = isLandscape,
                isShowSpeedUp = (isHolding && !playerUiState.isPipMode && !controlsState.isLocked && !calculatedShouldShowResumeOverlay && !playerUiState.isShowNextEpisode),
                zoomText = getZoomRatioText(),
                onZoomReset = {
                    resetZoom()
                },
                handlePlay = { playerAction(HlsPlayerAction.Play) },
                handlePause = { playerAction(HlsPlayerAction.Pause);isFirstLoad = false },
                onPreviousEpisode = {
                    if (prevEpisode != null) {
                        handleSelectedEpisodeServer(
                            episodeSourcesQuery.copy(id = prevEpisode.id),
                            false
                        )
                    }
                },
                onNextEpisode = {
                    if (nextEpisode != null) {
                        handleSelectedEpisodeServer(
                            episodeSourcesQuery.copy(id = nextEpisode.id), false
                        )
                        setShowNextEpisode(false)
                    }
                },
                onSeekTo = { position ->
                    playerAction(HlsPlayerAction.SeekTo(position))
                },
                seekAmount = seekAmount * 1000L,
                isShowSeekIndicator = isShowSeekIndicator,
                dragSeekPosition = dragSeekPosition,
                onDraggingSeekBarChange = { isDragging, position ->
                    isDraggingSeekBar = isDragging
                    dragSeekPosition = position
                },
                isDraggingSeekBar = isDraggingSeekBar,
                showRemainingTime = showRemainingTime,
                setShowRemainingTime = { showRemainingTime = it },
                onSettingsClick = { showSettingsSheet = true },
                onFullscreenToggle = {
                    if (!controlsState.isLocked) {
                        setFullscreenChange(!playerUiState.isFullscreen)
                    }
                },
                onBottomBarMeasured = { height -> bottomBarHeight = height }
            )
        }

        LoadingIndicator(
            modifier = Modifier.align(Alignment.Center),
            isVisible = (coreState.playbackState == Player.STATE_BUFFERING || coreState.playbackState == Player.STATE_IDLE) && !isPlayerControlsVisible && errorMessage == null
        )

        SeekIndicator(
            seekDirection = isShowSeekIndicator,
            seekAmount = seekAmount,
            isLandscape = isLandscape,
            isFullscreen = playerUiState.isFullscreen,
            modifier = Modifier.align(Alignment.Center)
        )

        episodeDetailComplement.lastTimestamp?.let { timestamp ->
            ResumePlaybackOverlay(
                modifier = Modifier.align(Alignment.Center),
                isVisible = calculatedShouldShowResumeOverlay,
                isPipMode = playerUiState.isPipMode,
                lastTimestamp = timestamp,
                onDismiss = {
                    setShowResume(false)
                    playerAction(
                        HlsPlayerAction.RequestToggleControlsVisibility(true)
                    )
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

        nextEpisode?.let { nextEpisode ->
            NextEpisodeOverlay(
                modifier = Modifier.align(Alignment.Center),
                isVisible = playerUiState.isShowNextEpisode,
                isLandscape = isLandscape,
                isPipMode = playerUiState.isPipMode,
                animeImage = episodeDetailComplement.imageUrl,
                nextEpisode = nextEpisode,
                nextEpisodeDetailComplement = episodeDetailComplements[nextEpisode.id]?.data,
                onDismiss = {
                    setShowNextEpisode(false)
                    playerAction(
                        HlsPlayerAction.RequestToggleControlsVisibility(true)
                    )
                },
                onRestart = {
                    playerAction(HlsPlayerAction.SeekTo(0))
                    playerAction(HlsPlayerAction.Play)
                    setShowNextEpisode(false)
                },
                onPlayNext = {
                    handleSelectedEpisodeServer(
                        episodeSourcesQuery.copy(id = nextEpisode.id), false
                    )
                    setShowNextEpisode(false)
                }
            )
        }

        val isSkipVisible =
            !playerUiState.isPipMode && !controlsState.isLocked && !isHolding && !isDraggingSeekBar && coreState.playbackState != Player.STATE_ENDED && !calculatedShouldShowResumeOverlay && !playerUiState.isShowNextEpisode

        episodeDetailComplement.sources.let { sources ->
            SkipButton(
                label = "Skip Intro",
                isVisible = controlsState.showIntroButton && isSkipVisible,
                onSkip = {
                    playerAction(HlsPlayerAction.SkipIntro(sources.intro.end))
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
            SkipButton(
                label = "Skip Outro",
                isVisible = controlsState.showOutroButton && isSkipVisible,
                onSkip = {
                    playerAction(HlsPlayerAction.SkipOutro(sources.outro.end))
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        SpeedUpIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isVisible = (isHolding && !playerUiState.isPipMode && !controlsState.isLocked && !calculatedShouldShowResumeOverlay && !playerUiState.isShowNextEpisode),
            speedText = speedUpText
        )

        ZoomIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            visible = isZooming,
            isShowSpeedUp = (isHolding && !playerUiState.isPipMode && !controlsState.isLocked && !calculatedShouldShowResumeOverlay && !playerUiState.isShowNextEpisode),
            zoomText = getZoomRatioText(),
            isClickable = zoomScaleProgress > 1f,
            onClick = { resetZoom() }
        )

        val isLockButtonVisible =
            controlsState.isLocked && !playerUiState.isPipMode && showLockReminder
        LockButton(
            visible = isLockButtonVisible,
            onClick = {
                (context as? FragmentActivity)?.let { activity ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }
                playerAction(HlsPlayerAction.ToggleLock(false))
            },
            modifier = Modifier.align(Alignment.TopEnd)
        )

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = showSettingsSheet && !playerUiState.isPipMode && !controlsState.isLocked,
            isFullscreen = playerUiState.isFullscreen,
            isLandscape = isLandscape,
            config = BottomSheetConfig(
                landscapeWidthFraction = 0.4f,
                landscapeHeightFraction = 0.7f
            ),
            onDismiss = { showSettingsSheet = false }
        ) {
            SettingsContent(
                onDismiss = {
                    showSettingsSheet = false
                },
                onLockClick = {
                    showLockReminder = true
                    setFullscreenChange(true)
                    if (coreState.playbackState == Player.STATE_ENDED) {
                        playerAction(HlsPlayerAction.SeekTo(0))
                    }
                    playerAction(HlsPlayerAction.Play)
                    playerAction(HlsPlayerAction.ToggleLock(true))
                },
                onPipClick = { onEnterPipMode() },
                selectedPlaybackSpeed = controlsState.playbackSpeed,
                onPlaybackSpeedClick = { showPlaybackSpeedSheet = true },
                isSubtitleAvailable = episodeDetailComplement.sources.tracks.any { it.kind == "captions" } == true,
                selectedSubtitle = controlsState.selectedSubtitle,
                onSubtitleClick = { showSubtitleSheet = true }
            )
        }

        episodeDetailComplement.sources.tracks.let { tracks ->
            CustomModalBottomSheet(
                modifier = Modifier.align(Alignment.BottomCenter),
                isVisible = showSubtitleSheet && !playerUiState.isPipMode && !controlsState.isLocked,
                isFullscreen = playerUiState.isFullscreen,
                isLandscape = isLandscape,
                config = BottomSheetConfig(
                    landscapeWidthFraction = 0.4f,
                    landscapeHeightFraction = 0.7f
                ),
                onDismiss = { showSubtitleSheet = false }
            ) {
                SubtitleContent(
                    tracks = tracks,
                    selectedSubtitle = controlsState.selectedSubtitle,
                    onSubtitleSelected = { track ->
                        playerAction(HlsPlayerAction.SetSubtitle(track))
                        showSubtitleSheet = false
                    }
                )
            }
        }

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = showPlaybackSpeedSheet && !playerUiState.isPipMode && !controlsState.isLocked,
            isFullscreen = playerUiState.isFullscreen,
            isLandscape = isLandscape,
            config = BottomSheetConfig(
                landscapeWidthFraction = 0.4f,
                landscapeHeightFraction = 0.7f
            ),
            onDismiss = { showPlaybackSpeedSheet = false }
        ) {
            PlaybackSpeedContent(
                selectedPlaybackSpeed = controlsState.playbackSpeed,
                onSpeedChange = { speed ->
                    playerAction(HlsPlayerAction.SetPlaybackSpeed(speed))
                    showPlaybackSpeedSheet = false
                }
            )
        }
    }
}

/**
 * A helper function to calculate the maximum allowed pan offsets based on the container size,
 * the video's intrinsic size, and the current zoom scale. This prevents panning into blank areas.
 */
private fun calculateOffsetBounds(
    containerSize: IntSize,
    videoSize: VideoSize,
    scale: Float
): Pair<Float, Float> {
    if (containerSize.width == 0 || containerSize.height == 0 || videoSize.width == 0 || videoSize.height == 0) {
        return 0f to 0f
    }

    val containerWidth = containerSize.width.toFloat()
    val containerHeight = containerSize.height.toFloat()
    val vWidth = videoSize.width.toFloat()
    val vHeight = videoSize.height.toFloat()

    val containerAspect = containerWidth / containerHeight
    val videoAspect = vWidth / vHeight

    val fittedWidth: Float
    val fittedHeight: Float
    if (videoAspect > containerAspect) {
        fittedWidth = containerWidth
        fittedHeight = containerWidth / videoAspect
    } else {
        fittedHeight = containerHeight
        fittedWidth = containerHeight * videoAspect
    }

    val maxOffsetX = (fittedWidth * scale - containerWidth).coerceAtLeast(0f) / 2f
    val maxOffsetY = (fittedHeight * scale - containerHeight).coerceAtLeast(0f) / 2f

    return maxOffsetX to maxOffsetY
}