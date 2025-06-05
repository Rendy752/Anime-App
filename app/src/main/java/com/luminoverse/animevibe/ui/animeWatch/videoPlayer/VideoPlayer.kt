package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.luminoverse.animevibe.ui.common.BottomSheetConfig
import com.luminoverse.animevibe.ui.common.CustomModalBottomSheet
import com.luminoverse.animevibe.ui.common.ScreenshotDisplay
import com.luminoverse.animevibe.utils.FullscreenUtils
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.media.PositionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val FAST_FORWARD_REWIND_DEBOUNCE_MILLIS = 1000L
private const val DEFAULT_SEEK_INCREMENT = 10000L
private const val LONG_PRESS_THRESHOLD_MILLIS = 500L
private const val DOUBLE_TAP_THRESHOLD_MILLIS = 300L

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    playerView: PlayerView,
    player: ExoPlayer,
    coreState: PlayerCoreState,
    controlsState:  StateFlow<ControlsState>,
    positionState:  StateFlow<PositionState>,
    playerAction: (HlsPlayerAction) -> Unit,
    mediaController: MediaControllerCompat?,
    onHandleBackPress: () -> Unit,
    episodeDetailComplement: EpisodeDetailComplement,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery, Boolean) -> Unit,
    isPipMode: Boolean,
    onEnterPipMode: () -> Unit,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    isAutoPlayVideo: Boolean,
    isShowResumeOverlay: Boolean,
    setShowResumeOverlay: (Boolean) -> Unit,
    isShowNextEpisode: Boolean,
    setShowNextEpisode: (Boolean) -> Unit,
    nextEpisodeName: String,
    isLandscape: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    videoSize: Modifier
) {
    val controlsState by controlsState.collectAsStateWithLifecycle()
    val positionState by positionState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFirstLoad by remember { mutableStateOf(true) }

    var isShowSeekIndicator by remember { mutableIntStateOf(0) }
    var seekAmount by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var fastForwardRewindCounter by remember { mutableIntStateOf(0) }
    var previousPlaybackSpeed by remember { mutableFloatStateOf(controlsState.playbackSpeed) }
    var speedUpText by remember { mutableStateOf("") }
    var isHolding by remember { mutableStateOf(false) }
    var isDraggingSeekBar by remember { mutableStateOf(false) }
    var dragSeekPosition by remember { mutableLongStateOf(0L) }
    var seekDisplayHandler by remember { mutableStateOf<Handler?>(null) }
    var seekDisplayRunnable by remember { mutableStateOf<Runnable?>(null) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapX by remember { mutableStateOf<Float?>(null) }

    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showPlaybackSpeedSheet by remember { mutableStateOf(false) }

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

    LaunchedEffect(coreState, player.isPlaying) {
        if (coreState.playbackState == Player.STATE_READY && player.isPlaying) {
            isFirstLoad = false
        }
    }

    val calculatedShouldShowResumeOverlay = !isAutoPlayVideo && isShowResumeOverlay &&
            episodeDetailComplement.lastTimestamp != null &&
            coreState.playbackState == Player.STATE_READY && !player.isPlaying &&
            errorMessage == null

    DisposableEffect(
        mediaController,
        isPipMode,
        controlsState.isLocked,
        calculatedShouldShowResumeOverlay,
        isShowNextEpisode
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

    fun handleSingleTap() {
        if (!controlsState.isLocked && !isHolding) {
            playerAction(HlsPlayerAction.RequestToggleControlsVisibility(!controlsState.isControlsVisible))
            Log.d("PlayerView", "Single tap: Toggled controls visibility")
        }
    }

    fun handleDoubleTap(x: Float, screenWidth: Float) {
        if (!controlsState.isLocked) {
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
        if (controlsState.isLocked || !player.isPlaying || coreState.playbackState != Player.STATE_READY) return
        isHolding = true
        speedUpText = "2x speed"
        previousPlaybackSpeed = controlsState.playbackSpeed
        playerAction(HlsPlayerAction.SetPlaybackSpeed(2f, fromLongPress = true))
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
        modifier = modifier
            .then(videoSize)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    Log.d(
                        "VideoPlayer",
                        "Touch down detected at ${down.position.x}, ${down.position.y}"
                    )
                    longPressJob?.cancel()
                    longPressJob = scope.launch {
                        delay(LONG_PRESS_THRESHOLD_MILLIS)
                        handleLongPressStart()
                    }

                    val up = waitForUpOrCancellation()
                    longPressJob?.cancel()
                    if (up != null) {
                        Log.d(
                            "VideoPlayer",
                            "Touch up detected at ${up.position.x}, ${up.position.y}"
                        )
                        val currentTime = System.currentTimeMillis()
                        val tapX = up.position.x

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
                    } else {
                        Log.d("VideoPlayer", "Touch cancelled")
                        handleLongPressEnd()
                    }
                }
            }
    ) {
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
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.useController = false
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
                        if (!isPipMode && controlsState.isControlsVisible && (isLandscape || !isFullscreen)) 100 else 0
                    )
                }
            )
        }

        val isPlayerControlsVisible = (controlsState.isControlsVisible || isDraggingSeekBar) &&
                !calculatedShouldShowResumeOverlay && !isShowNextEpisode && !isPipMode && !controlsState.isLocked && errorMessage == null
        val isShowSpeedUp =
            isHolding && !isPipMode && !controlsState.isLocked && !calculatedShouldShowResumeOverlay && !isShowNextEpisode && errorMessage == null
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
                episodes = episodes,
                isLocked = controlsState.isLocked,
                isHolding = isHolding,
                isFullscreen = isFullscreen,
                isShowSpeedUp = isShowSpeedUp,
                handlePlay = { playerAction(HlsPlayerAction.Play) },
                handlePause = { playerAction(HlsPlayerAction.Pause) },
                onPlayPauseRestart = {
                    when (coreState.playbackState) {
                        Player.STATE_ENDED -> playerAction(HlsPlayerAction.SeekTo(0))
                        else -> if (player.isPlaying) {
                            playerAction(HlsPlayerAction.Pause)
                        } else {
                            playerAction(HlsPlayerAction.Play)
                        }
                    }
                },
                onPreviousEpisode = {
                    val currentEpisode = episodeDetailComplement.servers.episodeNo
                    val prevEpisode = episodes.find { it.episodeNo == currentEpisode - 1 }
                    if (prevEpisode != null) {
                        handleSelectedEpisodeServer(
                            episodeSourcesQuery.copy(id = prevEpisode.episodeId),
                            false
                        )
                    }
                },
                onNextEpisode = {
                    val currentEpisode = episodeDetailComplement.servers.episodeNo
                    val nextEpisode = episodes.find { it.episodeNo == currentEpisode + 1 }
                    if (nextEpisode != null) {
                        handleSelectedEpisodeServer(
                            episodeSourcesQuery.copy(id = nextEpisode.episodeId),
                            false
                        )
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
                onPipClick = { onEnterPipMode() },
                onLockClick = {
                    (context as? FragmentActivity)?.let { activity ->
                        activity.window?.let { window ->
                            FullscreenUtils.handleFullscreenToggle(
                                window = window,
                                isFullscreen = false,
                                isLandscape = false,
                                activity = activity,
                                onFullscreenChange = onFullscreenChange,
                                isLockLandscapeOrientation = true
                            )
                        }
                    }
                    playerAction(HlsPlayerAction.Play)
                    playerAction(HlsPlayerAction.ToggleLock(true))
                },
                onSubtitleClick = { showSubtitleSheet = true },
                onPlaybackSpeedClick = { showPlaybackSpeedSheet = true },
                onFullscreenToggle = {
                    if (!controlsState.isLocked) {
                        (context as? FragmentActivity)?.let { activity ->
                            activity.window?.let { window ->
                                FullscreenUtils.handleFullscreenToggle(
                                    window = window,
                                    isFullscreen = isFullscreen,
                                    isLandscape = isLandscape,
                                    activity = activity,
                                    onFullscreenChange = onFullscreenChange
                                )
                            }
                        }
                    }
                }
            )
        }

        LoadingIndicator(
            modifier = Modifier.align(Alignment.Center),
            isVisible = (coreState.playbackState == Player.STATE_BUFFERING || coreState.playbackState == Player.STATE_IDLE) && !isPlayerControlsVisible
        )

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = showSubtitleSheet && !isPipMode && !controlsState.isLocked,
            isFullscreen = isFullscreen,
            isLandscape = isLandscape,
            config = BottomSheetConfig(
                landscapeWidthFraction = 0.4f,
                landscapeHeightFraction = 0.7f
            ),
            onDismiss = { showSubtitleSheet = false }
        ) {
            SubtitleContent(
                tracks = episodeDetailComplement.sources.tracks,
                selectedSubtitle = controlsState.selectedSubtitle,
                onSubtitleSelected = { track ->
                    playerAction(HlsPlayerAction.SetSubtitle(track))
                    showSubtitleSheet = false
                }
            )
        }

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = showPlaybackSpeedSheet && !isPipMode && !controlsState.isLocked,
            isFullscreen = isFullscreen,
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

        SeekIndicator(
            seekDirection = isShowSeekIndicator,
            seekAmount = seekAmount,
            isLandscape = isLandscape,
            isFullscreen = isFullscreen,
            errorMessage = errorMessage,
            modifier = Modifier.align(Alignment.Center)
        )

        episodeDetailComplement.lastTimestamp?.let { timestamp ->
            ResumePlaybackOverlay(
                modifier = Modifier.align(Alignment.Center),
                isVisible = calculatedShouldShowResumeOverlay,
                isPipMode = isPipMode,
                lastTimestamp = timestamp,
                onDismiss = {
                    setShowResumeOverlay(false)
                    playerAction(HlsPlayerAction.RequestToggleControlsVisibility(true))
                },
                onRestart = {
                    playerAction(HlsPlayerAction.SeekTo(0))
                    playerAction(HlsPlayerAction.Play)
                    setShowResumeOverlay(false)
                },
                onResume = {
                    playerAction(HlsPlayerAction.SeekTo(it))
                    playerAction(HlsPlayerAction.Play)
                    setShowResumeOverlay(false)
                }
            )
        }

        NextEpisodeOverlay(
            modifier = Modifier.align(Alignment.Center),
            isVisible = isShowNextEpisode,
            nextEpisodeName = nextEpisodeName,
            onDismiss = {
                setShowNextEpisode(false)
                playerAction(HlsPlayerAction.RequestToggleControlsVisibility(true))
            },
            onRestart = {
                playerAction(HlsPlayerAction.SeekTo(0))
                playerAction(HlsPlayerAction.Play)
                setShowNextEpisode(false)
            },
            onSkipNext = {
                handleSelectedEpisodeServer(
                    episodeSourcesQuery.copy(
                        id = episodes.find { it.name == nextEpisodeName }?.episodeId ?: ""
                    ),
                    false
                )
                setShowNextEpisode(false)
            }
        )

        RetryButton(
            modifier = Modifier.align(Alignment.Center),
            isVisible = errorMessage != null,
            onRetry = { handleSelectedEpisodeServer(episodeSourcesQuery, true) }
        )

        val isSkipVisible =
            !isPipMode && !controlsState.isLocked && !isHolding && !isDraggingSeekBar && coreState.playbackState != Player.STATE_ENDED && !calculatedShouldShowResumeOverlay && !isShowNextEpisode && errorMessage == null
        SkipButton(
            label = "Skip Intro",
            isVisible = controlsState.showIntroButton && isSkipVisible,
            onSkip = {
                episodeDetailComplement.sources.intro?.end?.let { endTime ->
                    playerAction(HlsPlayerAction.SkipIntro(endTime))
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
        SkipButton(
            label = "Skip Outro",
            isVisible = controlsState.showOutroButton && isSkipVisible,
            onSkip = {
                episodeDetailComplement.sources.outro?.end?.let { endTime ->
                    playerAction(HlsPlayerAction.SkipOutro(endTime))
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        SpeedUpIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isVisible = isShowSpeedUp,
            speedText = speedUpText
        )

        LockButton(
            isLocked = controlsState.isLocked && !isPipMode && errorMessage == null,
            onClick = {
                (context as? FragmentActivity)?.let { activity ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }
                playerAction(HlsPlayerAction.ToggleLock(false))
            },
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}