package com.example.animeapp.ui.animeWatch.videoPlayer

import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.utils.IntroOutroHandler

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    playerView: PlayerView,
    introOutroHandler: IntroOutroHandler,
    exoPlayer: ExoPlayer,
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
    modifier: Modifier = Modifier,
    videoSize: Modifier
) {
    val showIntro = introOutroHandler.showIntroButton.value
    val showOutro = introOutroHandler.showOutroButton.value
    var isHolding by remember { mutableStateOf(false) }
    var isFromHolding by remember { mutableStateOf(false) }
    var speedUpText by remember { mutableStateOf("1x speed") }
    var isShowSpeedUp by remember { mutableStateOf(false) }
    var isShowPip by remember { mutableStateOf(false) }
    var isShowSeekIndicator by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableIntStateOf(0) }
    var seekAmount by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }

    Box(modifier = modifier.then(videoSize)) {
        PlayerViewWrapper(
            playerView = playerView,
            exoPlayer = exoPlayer,
            isPipMode = isPipMode,
            onFullscreenChange = onFullscreenChange,
            isFullscreen = isFullscreen,
            isLandscape = isLandscape,
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
            }
        )
        if (isShowSeekIndicator) SeekIndicator(
            seekDirection = seekDirection,
            seekAmount = seekAmount,
            modifier = Modifier.align(Alignment.Center)
        )

        if (!isPipMode && isShowResumeOverlay && episodeDetailComplement.lastTimestamp != null) {
            ResumePlaybackOverlay(
                lastTimestamp = episodeDetailComplement.lastTimestamp,
                onClose = { setShowResumeOverlay(false) },
                onRestart = { exoPlayer.seekTo(0); exoPlayer.play(); setShowResumeOverlay(false) },
                onResume = { exoPlayer.seekTo(it); exoPlayer.play(); setShowResumeOverlay(false) },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (isShowNextEpisode) NextEpisodeOverlay(
            nextEpisodeName = nextEpisodeName,
            onRestart = { exoPlayer.seekTo(0); exoPlayer.play(); setShowNextEpisode(false) },
            onSkipNext = {
                handleSelectedEpisodeServer(
                    episodeSourcesQuery.copy(
                        id = episodes.find { it.name == nextEpisodeName }?.episodeId ?: ""
                    )
                ); setShowNextEpisode(false)
            },
            modifier = Modifier.align(Alignment.Center)
        )

        if (!isPipMode && !isShowResumeOverlay) SkipIntroOutroButtons(
            showIntro = showIntro,
            showOutro = showOutro,
            introEnd = episodeDetailComplement.sources.intro?.end ?: 0,
            outroEnd = episodeDetailComplement.sources.outro?.end ?: 0,
            onSkipIntro = { introOutroHandler.skipIntro(it) },
            onSkipOutro = { introOutroHandler.skipOutro(it) },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        if (isShowPip && !isPipMode) PipButton(
            onEnterPipMode = onEnterPipMode,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (isShowSpeedUp && !isPipMode) SpeedUpIndicator(
            speedText = speedUpText,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}