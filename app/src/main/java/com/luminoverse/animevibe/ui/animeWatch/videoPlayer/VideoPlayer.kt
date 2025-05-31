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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
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
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
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
    var isControlsVisible by remember { mutableStateOf(true) }
    var isHolding by remember { mutableStateOf(false) }
    var isFromHolding by remember { mutableStateOf(false) }
    var speedUpText by remember { mutableStateOf("1x speed") }
    var isShowSpeedUp by remember { mutableStateOf(false) }
    var isShowPip by remember { mutableStateOf(false) }
    var isShowSeekIndicator by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableIntStateOf(0) }
    var seekAmount by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showPlaybackSpeedSheet by remember { mutableStateOf(false) }

    LaunchedEffect(hlsPlayerState.isPlaying) {
        if (hlsPlayerState.isPlaying) isFirstLoad = false
    }

    LaunchedEffect(hlsPlayerState.isControlsVisible) {
        isControlsVisible = hlsPlayerState.isControlsVisible
    }

    LaunchedEffect(
        isControlsVisible,
        isSeeking,
        isLocked,
        isHolding,
        showSubtitleSheet,
        showPlaybackSpeedSheet
    ) {
        if (isControlsVisible && !isSeeking && !isLocked && !isHolding && !showSubtitleSheet && !showPlaybackSpeedSheet) {
            Log.d("VideoPlayer", "Auto-hide triggered: Hiding controls after 3s")
            delay(3000)
            isControlsVisible = false
            HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleControlsVisibility(false))
        } else {
            Log.d(
                "VideoPlayer",
                "Auto-hide blocked: isControlsVisible=$isControlsVisible, isSeeking=$isSeeking, isLocked=$isLocked, isHolding=$isHolding, showSubtitleSheet=$showSubtitleSheet, showPlaybackSpeedSheet=$showPlaybackSpeedSheet"
            )
        }
    }

    val mediaControllerCallback = remember {
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                state?.let {
                    val isPlaying = hlsPlayerState.isPlaying
                    val isPlayerReady = hlsPlayerState.isReady
                    if (isPlaying) {
                        setShowResumeOverlay(false)
                    }
                    Log.d(
                        "VideoPlayer",
                        "Playback state: ${it.state}, isPlaying=$isPlaying, isPlayerReady=$isPlayerReady"
                    )
                }
            }
        }
    }

    val shouldShowResumeOverlay = !isAutoPlayVideo && isShowResumeOverlay &&
            episodeDetailComplement.lastTimestamp != null &&
            hlsPlayerState.isReady &&
            !hlsPlayerState.isPlaying &&
            errorMessage == null

    DisposableEffect(
        mediaController,
        isPipMode,
        isLocked,
        shouldShowResumeOverlay,
        isShowNextEpisode
    ) {
        mediaController?.registerCallback(mediaControllerCallback)
        onDispose {
            mediaController?.unregisterCallback(mediaControllerCallback)
            Log.d("VideoPlayer", "PlayerView disposed")
        }
    }

    LaunchedEffect(shouldShowResumeOverlay, isShowNextEpisode, errorMessage) {
        if (shouldShowResumeOverlay || isShowNextEpisode || errorMessage != null) {
            playerView.hideController()
            isControlsVisible = false
            HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleControlsVisibility(false))
            Log.d(
                "VideoPlayer",
                "Hiding controller due to overlay: shouldShowResumeOverlay=$shouldShowResumeOverlay, isShowNextEpisode=$isShowNextEpisode, errorMessage=$errorMessage"
            )
        }
    }

    Box(modifier = modifier.then(videoSize)) {
        if (isFirstLoad) {
            ScreenshotDisplay(
                imageUrl = episodeDetailComplement.imageUrl,
                screenshot = episodeDetailComplement.screenshot,
                modifier = Modifier.fillMaxSize(),
                onClick = {
                    isControlsVisible = !isControlsVisible
                    HlsPlayerUtils.dispatch(
                        HlsPlayerAction.ToggleControlsVisibility(isControlsVisible)
                    )
                }
            )
        } else {
            PlayerViewWrapper(
                playerView = playerView,
                mediaController = mediaController,
                tracks = episodeDetailComplement.sources.tracks,
                isFullscreen = isFullscreen,
                isLandscape = isLandscape,
                isLocked = isLocked || shouldShowResumeOverlay || isShowNextEpisode || errorMessage != null,
                onPlayPause = {
                    if (hlsPlayerState.isPlaying) {
                        HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
                    } else {
                        HlsPlayerUtils.dispatch(HlsPlayerAction.Play)
                    }
                    isControlsVisible = true
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleControlsVisibility(true))
                },
                onPipVisibilityChange = { isShowPip = it },
                onSpeedChange = { speed, isHolding ->
                    speedUpText = "${speed.toInt()}x speed"
                    isShowSpeedUp = isHolding
                },
                onHoldingChange = { holding, fromHolding ->
                    isHolding = holding
                    isFromHolding = fromHolding
                    Log.d(
                        "VideoPlayer",
                        "onHoldingChange: isHolding=$holding, isFromHolding=$fromHolding"
                    )
                },
                onSeek = { direction, amount ->
                    isShowSeekIndicator = true
                    seekDirection = direction
                    seekAmount = amount
                    isSeeking = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        isShowSeekIndicator = false
                        isSeeking = false
                        Log.d("VideoPlayer", "Seek reset: isSeeking=false")
                    }, 1000)
                },
                onFastForward = onFastForward,
                onRewind = onRewind,
                onControlsToggle = { isVisible ->
                    isControlsVisible = isVisible
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleControlsVisibility(isVisible))
                    Log.d("VideoPlayer", "Controls toggled: isControlsVisible=$isVisible")
                }
            )
        }

        AnimatedVisibility(
            visible = isControlsVisible && !isPipMode && !isLocked && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControls(
                hlsPlayerState = hlsPlayerState,
                onHandleBackPress = onHandleBackPress,
                episodeDetailComplement = episodeDetailComplement,
                episodes = episodes,
                isFullscreen = isFullscreen,
                onPlayPauseRestart = {
                    when (hlsPlayerState.playbackState) {
                        Player.STATE_ENDED -> mediaController?.transportControls?.seekTo(0)
                        else -> if (hlsPlayerState.isPlaying) {
                            HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
                        } else {
                            HlsPlayerUtils.dispatch(HlsPlayerAction.Play)
                        }
                    }
                    isControlsVisible = true
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleControlsVisibility(true))
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
                    isControlsVisible = true
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleControlsVisibility(true))
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
                    isControlsVisible = true
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleControlsVisibility(true))
                },
                onSeekTo = { position ->
                    HlsPlayerUtils.dispatch(HlsPlayerAction.SeekTo(position))
                    isControlsVisible = true
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleControlsVisibility(true))
                },
                onPipClick = {
                    onEnterPipMode()
                    isControlsVisible = true
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleControlsVisibility(true))
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
                    isLocked = !isLocked
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleLock(isLocked))
                    isControlsVisible = true
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleControlsVisibility(true))
                },
                onSubtitleClick = { showSubtitleSheet = true },
                onPlaybackSpeedClick = { showPlaybackSpeedSheet = true },
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
                        isControlsVisible = true
                        HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleControlsVisibility(true))
                    }
                },
                onLayoutClick = {
                    isControlsVisible = false
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleControlsVisibility(false))
                    Log.d("VideoPlayer", "Box clicked: Hiding controls")
                }
            )
        }

        LoadingIndicator(
            modifier = Modifier.align(Alignment.Center),
            hlsPlayerState = hlsPlayerState,
            isControlsVisible = isControlsVisible,
            errorMessage = errorMessage
        )

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = showSubtitleSheet && !isPipMode && !isLocked,
            isLandscape = isLandscape,
            config = BottomSheetConfig(
                landscapeWidthFraction = 0.4f,
                landscapeHeightFraction = 0.7f
            ),
            onDismiss = { showSubtitleSheet = false }
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
            onDismiss = { showPlaybackSpeedSheet = false }
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
            isVisible = isShowSeekIndicator && errorMessage == null,
            modifier = Modifier.align(Alignment.Center)
        )

        if (shouldShowResumeOverlay) {
            ResumePlaybackOverlay(
                isPipMode = isPipMode,
                lastTimestamp = episodeDetailComplement.lastTimestamp,
                onClose = {
                    isFirstLoad = false
                    setShowResumeOverlay(false)
                },
                onRestart = {
                    mediaController?.transportControls?.seekTo(0)
                    HlsPlayerUtils.dispatch(HlsPlayerAction.Play)
                    setShowResumeOverlay(false)
                },
                onResume = {
                    mediaController?.transportControls?.seekTo(it)
                    HlsPlayerUtils.dispatch(HlsPlayerAction.Play)
                    setShowResumeOverlay(false)
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (isShowNextEpisode) {
            NextEpisodeOverlay(
                nextEpisodeName = nextEpisodeName,
                onRestart = {
                    mediaController?.transportControls?.seekTo(0)
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
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        RetryButton(
            modifier = Modifier.align(Alignment.Center),
            isVisible = errorMessage != null,
            onRetry = { handleSelectedEpisodeServer(episodeSourcesQuery, true) }
        )

        SkipButton(
            label = "Skip Intro",
            isVisible = !isPipMode && !isLocked && !shouldShowResumeOverlay && !isShowNextEpisode && hlsPlayerState.showIntroButton && errorMessage == null,
            onSkip = onSkipIntro,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
        SkipButton(
            label = "Skip Outro",
            isVisible = !isPipMode && !isLocked && !shouldShowResumeOverlay && !isShowNextEpisode && hlsPlayerState.showOutroButton && errorMessage == null,
            onSkip = onSkipOutro,
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        if (isShowSpeedUp && !isPipMode && !isLocked && !shouldShowResumeOverlay && !isShowNextEpisode && errorMessage == null) {
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
                    isLocked = !isLocked
                    HlsPlayerUtils.dispatch(HlsPlayerAction.ToggleLock(isLocked))
                },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}