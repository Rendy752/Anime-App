package com.example.animeapp.ui.animeWatch.videoPlayer

import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.IntroOutroHandler

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    playerView: PlayerView,
    introOutroHandler: IntroOutroHandler?,
    mediaController: MediaControllerCompat?,
    episodeDetailComplement: EpisodeDetailComplement,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
    isPipMode: Boolean,
    onEnterPipMode: () -> Unit,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    isShowResumeOverlay: Boolean,
    setShowResumeOverlay: (Boolean) -> Unit,
    isShowNextEpisode: Boolean,
    setShowNextEpisode: (Boolean) -> Unit,
    nextEpisodeName: String,
    isLandscape: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    videoSize: Modifier,
    onPlay: () -> Unit,
    onFastForward: () -> Unit,
    onRewind: () -> Unit
) {
    val playerState by HlsPlayerUtil.state.collectAsStateWithLifecycle()
    val showIntro = introOutroHandler?.showIntroButton?.value == true
    val showOutro = introOutroHandler?.showOutroButton?.value == true
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
                    val isPlaying = playerState.isPlaying
                    val isPlayerReady = playerState.isReady
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

    val shouldShowResumeOverlay = isShowResumeOverlay &&
            episodeDetailComplement.lastTimestamp != null &&
            playerState.isReady &&
            !playerState.isPlaying &&
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

    LaunchedEffect(playerState.isReady, isShowResumeOverlay, isShowNextEpisode, errorMessage) {
        if (playerState.isReady && !playerState.isPlaying && !isShowResumeOverlay && !isShowNextEpisode && errorMessage == null) {
            Log.d("VideoPlayer", "Auto-playing video")
            onPlay()
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
        errorMessage
    ) {
        Log.d(
            "VideoPlayer",
            "UI State: shouldShowResumeOverlay=$shouldShowResumeOverlay, isShowNextEpisode=$isShowNextEpisode, " +
                    "isShowPip=$isShowPip, isShowSpeedUp=$isShowSpeedUp, isShowSeekIndicator=$isShowSeekIndicator, isLocked=$isLocked, errorMessage=$errorMessage"
        )
    }

    Box(modifier = modifier.then(videoSize)) {
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
                        )
                    )
                    setShowNextEpisode(false)
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (errorMessage != null) {
            RetryButton(
                onRetry = {
                    if (errorMessage.contains("Failed to initialize player: Source error")) handleSelectedEpisodeServer(
                        episodeSourcesQuery
                    )
                    else onRetry
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (!isPipMode && !isLocked && !shouldShowResumeOverlay && !isShowNextEpisode && (showIntro || showOutro) && errorMessage == null) {
            SkipIntroOutroButtons(
                showIntro = showIntro,
                showOutro = showOutro,
                introEnd = episodeDetailComplement.sources.intro?.end ?: 0,
                outroEnd = episodeDetailComplement.sources.outro?.end ?: 0,
                onSkipIntro = { introOutroHandler.skipIntro(it) },
                onSkipOutro = { introOutroHandler.skipOutro(it) },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

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