package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.common.ScreenshotDisplay
import com.luminoverse.animevibe.utils.media.HlsPlayerState

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    playerView: PlayerView,
    hlsPlayerState: HlsPlayerState,
    mediaController: MediaControllerCompat?,
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
    onPlay: () -> Unit,
    onFastForward: () -> Unit,
    onRewind: () -> Unit,
    onSkipIntro: () -> Unit,
    onSkipOutro: () -> Unit
) {
    var isFirstLoad by remember { mutableStateOf(true) }
    LaunchedEffect(hlsPlayerState.isPlaying) {
        if (hlsPlayerState.isPlaying) isFirstLoad = false
    }

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

    val showPlaceholder =
        isFirstLoad && !hlsPlayerState.isPlaying && (hlsPlayerState.playbackState == Player.STATE_IDLE || hlsPlayerState.playbackState == Player.STATE_BUFFERING)

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
            Log.d(
                "VideoPlayer",
                "Hiding controller due to overlay: shouldShowResumeOverlay=$shouldShowResumeOverlay, isShowNextEpisode=$isShowNextEpisode, errorMessage=$errorMessage"
            )
        }
    }

    LaunchedEffect(
        shouldShowResumeOverlay,
        isShowNextEpisode,
        isShowPip,
        isShowSpeedUp,
        isShowSeekIndicator,
        isLocked,
        errorMessage,
        showPlaceholder
    ) {
        Log.d(
            "VideoPlayer",
            "UI State: shouldShowResumeOverlay=$shouldShowResumeOverlay, isShowNextEpisode=$isShowNextEpisode, " +
                    "isShowPip=$isShowPip, isShowSpeedUp=$isShowSpeedUp, isShowSeekIndicator=$isShowSeekIndicator, " +
                    "isLocked=$isLocked, showPlaceholder=$showPlaceholder, errorMessage=$errorMessage"
        )
    }

    Box(modifier = modifier.then(videoSize)) {
        if (showPlaceholder) {
            ScreenshotDisplay(
                imageUrl = episodeDetailComplement.imageUrl,
                screenshot = episodeDetailComplement.screenshot,
                modifier = Modifier.fillMaxSize(),
                clickable = false
            )
            CircularProgressIndicator(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = Color.White.copy(0.5f))
                    .padding(8.dp)
                    .align(Alignment.Center)
                    .size(48.dp),
                color = Color.Black,
                strokeWidth = 4.dp
            )
        } else {
            PlayerViewWrapper(
                playerView = playerView,
                mediaController = mediaController,
                tracks = episodeDetailComplement.sources.tracks,
                isPipMode = isPipMode,
                onFullscreenChange = onFullscreenChange,
                isFullscreen = isFullscreen,
                isLandscape = isLandscape,
                isLocked = isLocked || shouldShowResumeOverlay || isShowNextEpisode || errorMessage != null,
                onPipVisibilityChange = { isShowPip = it },
                onSpeedChange = { speed, isHolding ->
                    speedUpText = "${speed.toInt()}x speed"
                    isShowSpeedUp = isHolding
                },
                onHoldingChange = { holding, fromHolding ->
                    isHolding = holding
                    isFromHolding = fromHolding
                },
                onSeek = { direction, amount ->
                    isShowSeekIndicator = true
                    seekDirection = direction
                    seekAmount = amount
                    isSeeking = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        isShowSeekIndicator = false
                        isSeeking = false
                    }, 1000)
                },
                onFastForward = onFastForward,
                onRewind = onRewind
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
                onClose = { setShowResumeOverlay(false) },
                onRestart = {
                    mediaController?.transportControls?.seekTo(0)
                    onPlay()
                    setShowResumeOverlay(false)
                },
                onResume = {
                    mediaController?.transportControls?.seekTo(it)
                    onPlay()
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
                    onPlay()
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
            isVisible = !isPipMode && !isLocked && !shouldShowResumeOverlay && !isShowNextEpisode && (hlsPlayerState.showIntroButton || hlsPlayerState.showOutroButton) && errorMessage == null && hlsPlayerState.showIntroButton,
            onSkip = onSkipIntro,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
        SkipButton(
            label = "Skip Outro",
            isVisible = !isPipMode && !isLocked && !shouldShowResumeOverlay && !isShowNextEpisode && (hlsPlayerState.showIntroButton || hlsPlayerState.showOutroButton) && errorMessage == null && hlsPlayerState.showOutroButton,
            onSkip = onSkipOutro,
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        if (isShowPip && !isPipMode && !isLocked && !shouldShowResumeOverlay && !isShowNextEpisode && errorMessage == null) {
            PipButton(
                onEnterPipMode = onEnterPipMode,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (isShowSpeedUp && !isPipMode && !isLocked && !shouldShowResumeOverlay && !isShowNextEpisode && errorMessage == null) {
            SpeedUpIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                speedText = speedUpText
            )
        }

        if (!isPipMode && errorMessage == null) {
            val isControllerVisible = isShowPip && !shouldShowResumeOverlay && !isShowNextEpisode
            LockButton(
                icon = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = if (isLocked) "Unlock player" else "Lock player",
                onLockToggle = { isLocked = !isLocked },
                containerColor = Color.White.copy(alpha = if (isControllerVisible || isLocked) 1f else 0.5f),
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}