package com.example.animeapp.ui.animeWatch.ui

import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R as RMedia3
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.IntroOutroHandler
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.PictureInPictureAlt
import com.example.animeapp.ui.animeWatch.AnimeWatchViewModel
import androidx.compose.material.icons.filled.Speed

@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSection(
    episodeDetailComplement: EpisodeDetailComplement,
    viewModel: AnimeWatchViewModel,
    isPipMode: Boolean,
    onEnterPipMode: () -> Unit,
    onPlayerError: (String) -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val introOutroHandler =
        remember { IntroOutroHandler(exoPlayer, episodeDetailComplement.sources) }
    val playerView = remember { PlayerView(context) }
    val showIntro = introOutroHandler.showIntroButton.value
    val showOutro = introOutroHandler.showOutroButton.value
    var isLoading by remember { mutableStateOf(false) }
    var showNextEpisode by remember { mutableStateOf(false) }
    var nextEpisodeName by remember { mutableStateOf("") }
    var mediaSession: MediaSession? by remember { mutableStateOf(null) }
    var isHolding by remember { mutableStateOf(false) }
    var isFromHolding by remember { mutableStateOf(false) }
    var speedUpText by remember { mutableStateOf("1x speed") }
    var showSpeedUp by remember { mutableStateOf(false) }
    var showPip by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    DisposableEffect(exoPlayer) {
        isLoading = true
        HlsPlayerUtil.initializePlayer(exoPlayer, episodeDetailComplement.sources)
        introOutroHandler.start()

        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                if (error is ExoPlaybackException && error.type == ExoPlaybackException.TYPE_SOURCE) {
                    onPlayerError("Playback error, try a different server.")
                }
                isLoading = false
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    (context as? FragmentActivity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    HlsPlayerUtil.requestAudioFocus(audioManager)
                } else {
                    HlsPlayerUtil.abandonAudioFocus(audioManager)
                }
                updateMediaSessionPlaybackState(exoPlayer, mediaSession)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                isLoading = false
                viewModel.episodeDetailComplement.value.data?.servers.let { servers ->
                    viewModel.episodes.value?.let { episodes ->
                        if (playbackState == Player.STATE_ENDED) {
                            playerView.hideController()
                            if (episodes.isNotEmpty()) {
                                val currentEpisode = servers?.episodeNo
                                val nextEpisode =
                                    episodes.find { it.episodeNo == (currentEpisode?.plus(1) ?: 0) }
                                if (nextEpisode == null) {
                                    showNextEpisode = false
                                } else {
                                    showNextEpisode = true
                                    nextEpisodeName = nextEpisode.name
                                }
                            } else {
                                showNextEpisode = false
                            }
                        } else {
                            showNextEpisode = false
                        }
                    }
                }
            }
        }

        exoPlayer.addListener(listener)

        val sessionId = "episode_${System.currentTimeMillis()}"
        mediaSession = MediaSession.Builder(context, exoPlayer).setId(sessionId).build()

        onDispose {
            exoPlayer.removeListener(listener)
            introOutroHandler.stop()
            HlsPlayerUtil.releasePlayer(playerView)
            mediaSession?.release()
            mediaSession = null
        }
    }

    DisposableEffect(Unit) {
        HlsPlayerUtil.requestAudioFocus(audioManager)
        onDispose {
            HlsPlayerUtil.abandonAudioFocus(audioManager)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { playerView },
            modifier = Modifier.fillMaxSize()
        ) { view ->
            view.player = exoPlayer
            view.setShowPreviousButton(false)
            view.setShowNextButton(false)
            view.setFullscreenButtonState(true)
            view.setShowSubtitleButton(true)
            view.useController = !isPipMode
            view.setFullscreenButtonClickListener {
                isFullscreen = !isFullscreen
                val activity = context as? FragmentActivity
                activity?.window?.let { window ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val controller = window.insetsController
                        if (isFullscreen) {
                            controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                            controller?.systemBarsBehavior =
                                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        } else {
                            controller?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        }
                    } else {
                        if (isFullscreen) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                        }
                    }
                }
            }
            view.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isHolding = true
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (isHolding && exoPlayer.playbackParameters.speed != 2f) {
                                exoPlayer.playbackParameters =
                                    exoPlayer.playbackParameters.withSpeed(2f)
                                view.useController = false
                                showPip = false
                                showSpeedUp = true
                                speedUpText = "2x speed"
                                isFromHolding = true
                            }
                        }, 1000)
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        android.os.Handler(android.os.Looper.getMainLooper())
                            .removeCallbacksAndMessages(null)
                        if (isFromHolding) {
                            exoPlayer.playbackParameters =
                                exoPlayer.playbackParameters.withSpeed(1f)
                            view.useController = true
                            showSpeedUp = false
                            speedUpText = "1x speed"
                        }
                        isHolding = false
                        isFromHolding = false
                    }
                }
                if (!isHolding) {
                    view.performClick()
                }
                true
            }
            view.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                showPip = visibility == View.VISIBLE
                val subtitleView = view.subtitleView
                val bottomBar = view.findViewById<ViewGroup>(RMedia3.id.exo_bottom_bar)
                val orientation = view.resources.configuration.orientation
                subtitleView?.setPadding(
                    0,
                    0,
                    0,
                    if (visibility == View.VISIBLE && orientation == Configuration.ORIENTATION_LANDSCAPE || (visibility == View.VISIBLE && (context as? FragmentActivity)?.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT)) bottomBar.height else 0
                )
            })
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (showNextEpisode) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Text(text = nextEpisodeName, color = Color.White)
                Row {
                    IconButton(onClick = {
                        exoPlayer.seekTo(0)
                        exoPlayer.play()
                        showNextEpisode = false
                    }) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = "Restart",
                        )
                    }
                    IconButton(onClick = {
                        viewModel.episodeSourcesQuery.value?.let { query ->
                            viewModel.handleSelectedEpisodeServer(
                                query.copy(
                                    id = viewModel.episodes.value?.find { it.name == nextEpisodeName }?.episodeId
                                        ?: ""
                                )
                            )
                        }
                        showNextEpisode = false
                    }) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Skip Next",
                        )
                    }
                }
            }
        }

        if (showIntro) {
            Button(
                onClick = {
                    introOutroHandler.skipIntro(
                        episodeDetailComplement.sources.intro?.end ?: 0
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 76.dp, bottom = 60.dp)
            ) {
                Text("Skip Intro")
            }
        }

        if (showOutro) {
            Button(
                onClick = {
                    introOutroHandler.skipOutro(
                        episodeDetailComplement.sources.outro?.end ?: 0
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 76.dp, bottom = 60.dp)
            ) {
                Text("Skip Outro")
            }
        }

        if (showPip) {
            IconButton(
                onClick = { onEnterPipMode() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PictureInPictureAlt,
                    contentDescription = "PIP"
                )
            }
        }

        if (showSpeedUp) {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                onClick = { },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                enabled = false
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = "Speed Up",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = speedUpText,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}


private fun updateMediaSessionPlaybackState(player: Player, mediaSession: MediaSession?) {
    val playbackState = when (player.playbackState) {
        Player.STATE_IDLE -> PlaybackStateCompat.STATE_NONE
        Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
        Player.STATE_READY -> if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
        else -> PlaybackStateCompat.STATE_NONE
    }

    PlaybackStateCompat.Builder()
        .setState(playbackState, player.currentPosition, 1f, System.currentTimeMillis())
        .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)

    mediaSession?.setPlayer(player)
}