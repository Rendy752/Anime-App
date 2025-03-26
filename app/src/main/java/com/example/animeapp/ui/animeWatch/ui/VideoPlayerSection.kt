package com.example.animeapp.ui.animeWatch.ui

import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.IntroOutroHandler
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.runtime.LaunchedEffect
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.ui.animeWatch.components.VideoPlayer

@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSection(
    episodeDetailComplement: EpisodeDetailComplement,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
    isPipMode: Boolean,
    onEnterPipMode: () -> Unit,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    isScreenOn: Boolean,
    isLandscape: Boolean,
    onPlayerError: (String) -> Unit,
    modifier: Modifier = Modifier,
    videoSize: Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val introOutroHandler =
        remember { IntroOutroHandler(exoPlayer, episodeDetailComplement.sources) }
    val playerView = remember { PlayerView(context) }
    var isLoading by remember { mutableStateOf(false) }
    var isShowNextEpisode by remember { mutableStateOf(false) }
    var nextEpisodeName by remember { mutableStateOf("") }
    var mediaSession: MediaSession? by remember { mutableStateOf(null) }

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
                episodeDetailComplement.servers.let { servers ->
                    if (playbackState == Player.STATE_ENDED) {
                        playerView.hideController()
                        isShowNextEpisode = episodes.isNotEmpty() && updateNextEpisodeName(
                            episodes,
                            servers.episodeNo,
                            setNextEpisodeName = { nextEpisodeName = it }
                        )
                    } else {
                        isShowNextEpisode = false
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
            HlsPlayerUtil.abandonAudioFocus(audioManager)
            HlsPlayerUtil.releasePlayer(playerView)
            mediaSession?.release()
            mediaSession = null
        }
    }

    LaunchedEffect(isScreenOn) {
        if (!isScreenOn) exoPlayer.pause()
    }

    VideoPlayer(
        playerView = playerView,
        introOutroHandler = introOutroHandler,
        exoPlayer = exoPlayer,
        episodeDetailComplement = episodeDetailComplement,
        episodes = episodes,
        episodeSourcesQuery = episodeSourcesQuery,
        handleSelectedEpisodeServer = handleSelectedEpisodeServer,
        isPipMode = isPipMode,
        onEnterPipMode = onEnterPipMode,
        isFullscreen = isFullscreen,
        onFullscreenChange = onFullscreenChange,
        isShowNextEpisode = isShowNextEpisode,
        setShowNextEpisode = { isShowNextEpisode = it },
        nextEpisodeName = nextEpisodeName,
        isLandscape = isLandscape,
        modifier = modifier,
        videoSize = videoSize
    )
}

private fun updateNextEpisodeName(
    episodes: List<Episode>,
    currentEpisode: Int,
    setNextEpisodeName: (String) -> Unit
): Boolean {
    val nextEpisode = episodes.find { it.episodeNo == currentEpisode + 1 }
    return if (nextEpisode != null) {
        setNextEpisodeName(nextEpisode.name)
        true
    } else false
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