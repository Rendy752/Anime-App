package com.example.animeapp.ui.animeWatch.components

import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R as RMedia3
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.utils.IntroOutroHandler
import com.example.animeapp.utils.basicContainer

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
    var showSpeedUp by remember { mutableStateOf(false) }
    var showPip by remember { mutableStateOf(false) }

    Box(modifier = modifier.then(videoSize)) {
        PlayerViewWrapper(
            playerView = playerView,
            exoPlayer = exoPlayer,
            isPipMode = isPipMode,
            onFullscreenChange = onFullscreenChange,
            isFullscreen = isFullscreen,
            isLandscape = isLandscape,
            onPipVisibilityChange = { showPip = it },
            onSpeedChange = { speed, isHolding ->
                speedUpText = "${speed.toInt()}x speed"
                showSpeedUp = isHolding
            },
            onHoldingChange = { holding, fromHolding ->
                isHolding = holding
                isFromHolding = fromHolding
            }
        )

        NextEpisodeOverlay(
            isShowNextEpisode = isShowNextEpisode,
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

        if (!isPipMode) SkipIntroOutroButtons(
            showIntro = showIntro,
            showOutro = showOutro,
            introEnd = episodeDetailComplement.sources.intro?.end ?: 0,
            outroEnd = episodeDetailComplement.sources.outro?.end ?: 0,
            onSkipIntro = { introOutroHandler.skipIntro(it) },
            onSkipOutro = { introOutroHandler.skipOutro(it) },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        if (showPip && !isPipMode) PipButton(
            onEnterPipMode = onEnterPipMode,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (showSpeedUp && !isPipMode) SpeedUpIndicator(
            speedText = speedUpText,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerViewWrapper(
    playerView: PlayerView,
    exoPlayer: ExoPlayer,
    isPipMode: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    isFullscreen: Boolean,
    isLandscape: Boolean,
    onPipVisibilityChange: (Boolean) -> Unit,
    onSpeedChange: (Float, Boolean) -> Unit,
    onHoldingChange: (Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    AndroidView(
        factory = { playerView },
        modifier = Modifier.fillMaxSize()
    ) { view ->
        view.player = exoPlayer
        view.setShowPreviousButton(false)
        view.setShowNextButton(false)
        view.setFullscreenButtonState(true)
        view.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        view.setShowSubtitleButton(true)
        view.useController = !isPipMode

        view.setFullscreenButtonClickListener {
            onFullscreenChange(!isFullscreen)
            (context as? FragmentActivity)?.window?.let { window ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val controller = window.insetsController
                    if (!isFullscreen) {
                        controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        controller?.systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        controller?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    }
                } else {
                    if (!isFullscreen) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    }
                }
            }
        }

        var isHolding = false
        var isFromHolding = false
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isHolding = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isHolding && exoPlayer.playbackParameters.speed != 2f) {
                            exoPlayer.playbackParameters =
                                exoPlayer.playbackParameters.withSpeed(2f)
                            view.useController = false
                            onSpeedChange(2f, true)
                            isFromHolding = true
                        }
                    }, 1000)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
                    if (isFromHolding) {
                        exoPlayer.playbackParameters = exoPlayer.playbackParameters.withSpeed(1f)
                        view.useController = true
                        onSpeedChange(1f, false)
                    }
                    isHolding = false
                    isFromHolding = false
                }
            }
            onHoldingChange(isHolding, isFromHolding)
            if (!isHolding) view.performClick()
            true
        }

        view.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            onPipVisibilityChange(visibility == View.VISIBLE)
            view.subtitleView?.let { subtitleView ->
                val bottomBar = view.findViewById<ViewGroup>(RMedia3.id.exo_bottom_bar)
                subtitleView.setPadding(
                    0, 0, 0,
                    if (visibility == View.VISIBLE && (isLandscape || (context as? FragmentActivity)?.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT)) bottomBar.height else 0
                )
            }
        })
    }
}

@Composable
private fun NextEpisodeOverlay(
    isShowNextEpisode: Boolean,
    nextEpisodeName: String,
    onRestart: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier
) {
    if (isShowNextEpisode) {
        Column(
            modifier = modifier.basicContainer(isPrimary = true),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = nextEpisodeName, color = MaterialTheme.colorScheme.onPrimary)
            Row {
                IconButton(onClick = onRestart) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = "Restart",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = onSkipNext) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Skip Next",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SkipIntroOutroButtons(
    showIntro: Boolean,
    showOutro: Boolean,
    introEnd: Long,
    outroEnd: Long,
    onSkipIntro: (Long) -> Unit,
    onSkipOutro: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (showIntro) SkipButton(
        label = "Skip Intro",
        skipTime = introEnd,
        onSkip = onSkipIntro,
        modifier = modifier
    )

    if (showOutro) SkipButton(
        label = "Skip Outro",
        skipTime = outroEnd,
        onSkip = onSkipOutro,
        modifier = modifier
    )
}

@Composable
private fun PipButton(
    onEnterPipMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onEnterPipMode,
        modifier = modifier.padding(16.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
        )
    ) {
        Icon(
            Icons.Filled.PictureInPictureAlt,
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            contentDescription = "PIP"
        )
    }
}

@Composable
private fun SpeedUpIndicator(
    speedText: String,
    modifier: Modifier = Modifier
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        onClick = { },
        modifier = modifier.padding(16.dp),
        enabled = false
    ) {
        Icon(
            Icons.Filled.Speed,
            contentDescription = "Speed Up",
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = speedText,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            fontSize = 16.sp
        )
    }
}