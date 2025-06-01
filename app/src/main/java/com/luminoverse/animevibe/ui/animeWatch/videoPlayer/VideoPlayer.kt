package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.common.BottomSheetConfig
import com.luminoverse.animevibe.ui.common.CustomModalBottomSheet
import com.luminoverse.animevibe.ui.common.ScreenshotDisplay
import com.luminoverse.animevibe.utils.FullscreenUtils
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.HlsPlayerState
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.awaitCancellation

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(FlowPreview::class)
@Composable
fun VideoPlayer(
    playerView: PlayerView,
    hlsPlayerState: HlsPlayerState,
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
    videoSize: Modifier,
    onFastForward: () -> Unit,
    onRewind: () -> Unit,
    onSkipIntro: () -> Unit,
    onSkipOutro: () -> Unit
) {
    val context = LocalContext.current
    var isFirstLoad by remember { mutableStateOf(true) }
    var isHolding by remember { mutableStateOf(false) }
    var isFromHolding by remember { mutableStateOf(false) }
    var speedUpText by remember { mutableStateOf("1x speed") }
    var isShowSpeedUp by remember { mutableStateOf(false) }
    var isShowSeekIndicator by remember { mutableIntStateOf(0) }
    var seekDirection by remember { mutableIntStateOf(0) }
    var seekAmount by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var isDraggingSeekBar by remember { mutableStateOf(false) }
    var dragSeekPosition by remember { mutableLongStateOf(0L) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showPlaybackSpeedSheet by remember { mutableStateOf(false) }

    val isLocked = hlsPlayerState.isLocked

    LaunchedEffect(hlsPlayerState.isPlaying) {
        if (hlsPlayerState.isPlaying) isFirstLoad = false
    }

    val calculatedShouldShowResumeOverlay = !isAutoPlayVideo && isShowResumeOverlay &&
            episodeDetailComplement.lastTimestamp != null &&
            hlsPlayerState.isReady &&
            !hlsPlayerState.isPlaying &&
            errorMessage == null

    val mediaControllerCallback = remember {
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                state?.let {
                    val isPlaying = it.state == PlaybackStateCompat.STATE_PLAYING
                    if (isPlaying) {
                        setShowResumeOverlay(false)
                    }
                    Log.d(
                        "VideoPlayer",
                        "MediaControllerCompat Playback state: ${it.state}, isPlaying=$isPlaying"
                    )
                }
            }
        }
    }

    DisposableEffect(
        mediaController,
        isPipMode,
        isLocked,
        calculatedShouldShowResumeOverlay,
        isShowNextEpisode
    ) {
        mediaController?.registerCallback(mediaControllerCallback)
        onDispose {
            mediaController?.unregisterCallback(mediaControllerCallback)
            Log.d("VideoPlayer", "PlayerView disposed, MediaControllerCallback unregistered")
        }
    }

    fun handleSingleTap() {
        if (!isLocked) {
            HlsPlayerUtils.dispatch(HlsPlayerAction.RequestToggleControlsVisibility(!hlsPlayerState.isControlsVisible))
            Log.d(
                "VideoPlayer",
                "Single tap: Requested toggle visibility to ${!hlsPlayerState.isControlsVisible}"
            )
        }
    }

    fun handleDoubleTap(x: Float, screenWidth: Float) {
        if (!isSeeking && !isLocked && hlsPlayerState.isReady) {
            Log.d("VideoPlayer", "Double tap at x=$x")
            if (isFromHolding) {
                HlsPlayerUtils.dispatch(HlsPlayerAction.SetPlaybackSpeed(1f))
                isHolding = false
                isFromHolding = false
            }

            when {
                x < screenWidth * 0.4 -> {
                    Log.d("VideoPlayer", "Rewind triggered")
                    onRewind()
                    isShowSeekIndicator = 1
                    seekDirection = -1
                    seekAmount = 10L
                    isSeeking = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        isShowSeekIndicator = 0
                        isSeeking = false
                        Log.d("VideoPlayer", "Seek reset: isSeeking=false")
                    }, 1000)
                }

                x > screenWidth * 0.6 -> {
                    Log.d("VideoPlayer", "Fast forward triggered")
                    onFastForward()
                    isShowSeekIndicator = 1
                    seekDirection = 1
                    seekAmount = 10L
                    Handler(Looper.getMainLooper()).postDelayed({
                        isShowSeekIndicator = 0
                        isSeeking = false
                        Log.d("VideoPlayer", "Seek reset: isSeeking=false")
                    }, 1000)
                    isSeeking = true
                }

                else -> {
                    Log.d("VideoPlayer", "Play/Pause triggered")
                    if (hlsPlayerState.isPlaying) {
                        HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
                    } else {
                        HlsPlayerUtils.dispatch(HlsPlayerAction.Play)
                    }
                }
            }
        }
    }

    fun handleLongPressStartActual() {
        if (!isSeeking && hlsPlayerState.isPlaying) {
            if (hlsPlayerState.playbackSpeed != 2f) {
                Log.d("VideoPlayer", "Long press: Setting speed to 2f")
                HlsPlayerUtils.dispatch(HlsPlayerAction.SetPlaybackSpeed(2f))
                isFromHolding = true
            }
        }
    }

    fun handleLongPressEnd() {
        if (isFromHolding) {
            Log.d("VideoPlayer", "Resetting speed to 1f because long press ended.")
            HlsPlayerUtils.dispatch(HlsPlayerAction.SetPlaybackSpeed(1f))
        }
        isHolding = false
        isFromHolding = false
    }

    Box(
        modifier = modifier
            .then(videoSize)
            .pointerInput(hlsPlayerState.isControlsVisible) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isLocked && hlsPlayerState.isReady) {
                            handleDoubleTap(offset.x, size.width.toFloat())
                        }
                    },
                    onTap = {
                        if (!isLocked && hlsPlayerState.isReady) {
                            handleSingleTap()
                        }
                    },
                    onLongPress = {
                        if (!isLocked && hlsPlayerState.isReady) {
                            isHolding = true
                            handleLongPressStartActual()
                        }
                    },
                    onPress = {
                        try {
                            awaitCancellation()
                        } finally {
                            if (isHolding) {
                                handleLongPressEnd()
                            }
                        }
                    }
                )
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
            PlayerViewWrapper(
                playerView = playerView,
                controlsAreVisible = hlsPlayerState.isControlsVisible,
                isFullscreen = isFullscreen,
                isLandscape = isLandscape,
            )
        }

        AnimatedVisibility(
            visible = hlsPlayerState.isControlsVisible && !calculatedShouldShowResumeOverlay && !isShowNextEpisode && !isPipMode && !isLocked && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControls(
                hlsPlayerState = hlsPlayerState,
                onHandleBackPress = onHandleBackPress,
                episodeDetailComplement = episodeDetailComplement,
                episodes = episodes,
                isFullscreen = isFullscreen,
                handlePlay = {
                    HlsPlayerUtils.dispatch(HlsPlayerAction.Play)
                },
                handlePause = {
                    HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
                },
                onPlayPauseRestart = {
                    when (hlsPlayerState.playbackState) {
                        Player.STATE_ENDED -> HlsPlayerUtils.dispatch(HlsPlayerAction.SeekTo(0))
                        else -> if (hlsPlayerState.isPlaying) {
                            HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
                        } else {
                            HlsPlayerUtils.dispatch(HlsPlayerAction.Play)
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
                    HlsPlayerUtils.dispatch(HlsPlayerAction.SeekTo(position))
                },
                isDraggingSeekBar = isDraggingSeekBar,
                dragSeekPosition = dragSeekPosition,
                onDraggingSeekBarChange = { isDragging, position ->
                    isDraggingSeekBar = isDragging
                    dragSeekPosition = position
                },
                onPipClick = {
                    onEnterPipMode()
                },
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
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleLock(!isLocked))
                },
                onSubtitleClick = {
                    showSubtitleSheet = true
                },
                onPlaybackSpeedClick = {
                    showPlaybackSpeedSheet = true
                },
                onFullscreenToggle = {
                    if (!isLocked) {
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
            isVisible = (hlsPlayerState.playbackState == Player.STATE_BUFFERING || hlsPlayerState.playbackState == Player.STATE_IDLE) && !hlsPlayerState.isControlsVisible && errorMessage == null
        )

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = showSubtitleSheet && !isPipMode && !isLocked,
            isLandscape = isLandscape,
            config = BottomSheetConfig(
                landscapeWidthFraction = 0.4f,
                landscapeHeightFraction = 0.7f
            ),
            onDismiss = {
                showSubtitleSheet = false
            }
        ) {
            SubtitleContent(
                tracks = episodeDetailComplement.sources.tracks,
                selectedSubtitle = hlsPlayerState.selectedSubtitle,
                onSubtitleSelected = { track ->
                    HlsPlayerUtils.dispatch(HlsPlayerAction.SetSubtitle(track))
                    showSubtitleSheet = false
                }
            )
        }

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = showPlaybackSpeedSheet && !isPipMode && !isLocked,
            isLandscape = isLandscape,
            config = BottomSheetConfig(
                landscapeWidthFraction = 0.4f,
                landscapeHeightFraction = 0.7f
            ),
            onDismiss = {
                showPlaybackSpeedSheet = false
            }
        ) {
            PlaybackSpeedContent(
                selectedPlaybackSpeed = hlsPlayerState.playbackSpeed,
                onSpeedChange = { speed ->
                    HlsPlayerUtils.dispatch(HlsPlayerAction.SetPlaybackSpeed(speed))
                    showPlaybackSpeedSheet = false
                }
            )
        }

        SeekIndicator(
            seekDirection = seekDirection,
            seekAmount = seekAmount,
            isVisible = isShowSeekIndicator != 0 && errorMessage == null,
            modifier = Modifier.align(Alignment.Center)
        )

        episodeDetailComplement.lastTimestamp?.let { timestamp ->
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.Center),
                visible = calculatedShouldShowResumeOverlay,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ResumePlaybackOverlay(
                    isPipMode = isPipMode,
                    lastTimestamp = timestamp,
                    onClose = {
                        setShowResumeOverlay(false)
                        HlsPlayerUtils.dispatch(HlsPlayerAction.RequestToggleControlsVisibility(true))
                    },
                    onRestart = {
                        HlsPlayerUtils.dispatch(HlsPlayerAction.SeekTo(0))
                        HlsPlayerUtils.dispatch(HlsPlayerAction.Play)
                        setShowResumeOverlay(false)
                    },
                    onResume = {
                        HlsPlayerUtils.dispatch(HlsPlayerAction.SeekTo(it))
                        HlsPlayerUtils.dispatch(HlsPlayerAction.Play)
                        setShowResumeOverlay(false)
                    }
                )
            }
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = isShowNextEpisode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            NextEpisodeOverlay(
                nextEpisodeName = nextEpisodeName,
                onRestart = {
                    HlsPlayerUtils.dispatch(HlsPlayerAction.SeekTo(0))
                    HlsPlayerUtils.dispatch(HlsPlayerAction.Play)
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
        }

        RetryButton(
            modifier = Modifier.align(Alignment.Center),
            isVisible = errorMessage != null,
            onRetry = {
                handleSelectedEpisodeServer(episodeSourcesQuery, true)
            }
        )

        SkipButton(
            label = "Skip Intro",
            isVisible = !isPipMode && !isLocked && hlsPlayerState.playbackState != Player.STATE_ENDED && !calculatedShouldShowResumeOverlay && !isShowNextEpisode && hlsPlayerState.showIntroButton && errorMessage == null,
            onSkip = {
                onSkipIntro()
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
        SkipButton(
            label = "Skip Outro",
            isVisible = !isPipMode && !isLocked && hlsPlayerState.playbackState != Player.STATE_ENDED && !calculatedShouldShowResumeOverlay && !isShowNextEpisode && hlsPlayerState.showOutroButton && errorMessage == null,
            onSkip = {
                onSkipOutro()
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        if (isShowSpeedUp && !isPipMode && !isLocked && !calculatedShouldShowResumeOverlay && !isShowNextEpisode && errorMessage == null) {
            SpeedUpIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                speedText = speedUpText
            )
        }

        if (!isPipMode && errorMessage == null) {
            LockButton(
                isLocked = isLocked,
                onClick = {
                    (context as? FragmentActivity)?.let { activity ->
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                    }
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleLock(!isLocked))
                    HlsPlayerUtils.dispatch(HlsPlayerAction.RequestToggleControlsVisibility(true))
                },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}