package com.example.animeapp.ui.animeWatch.videoPlayer

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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
import androidx.media3.ui.PlayerView
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.IntroOutroHandler
import androidx.compose.runtime.LaunchedEffect
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.models.EpisodeSourcesResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSection(
    episodeDetailComplement: EpisodeDetailComplement,
    updateEpisodeDetailComplement: (EpisodeDetailComplement) -> Unit,
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
    val currentEpisodeNo = episodeDetailComplement.servers.episodeNo
    val previousEpisode = remember(currentEpisodeNo) {
        episodes.find { it.episodeNo == currentEpisodeNo - 1 }
    }
    val nextEpisode = remember(currentEpisodeNo) {
        episodes.find { it.episodeNo == currentEpisodeNo + 1 }
    }
    var isLoading by remember { mutableStateOf(false) }
    var isShowResumeOverlay by remember { mutableStateOf(episodeDetailComplement.lastTimestamp != null) }
    var isShowNextEpisode by remember { mutableStateOf(false) }
    var nextEpisodeName by remember { mutableStateOf("") }
    var mediaSessionCompat: MediaSessionCompat? by remember { mutableStateOf(null) }
    var mediaControllerCompat: MediaControllerCompat? by remember { mutableStateOf(null) }
    val playbackStateBuilder = remember { PlaybackStateCompat.Builder() }

    fun updatePlaybackState(state: Int) {
        isShowResumeOverlay = false
        playbackStateBuilder.setState(state, exoPlayer.currentPosition, 1.0f)
        mediaSessionCompat?.setPlaybackState(playbackStateBuilder.build())
    }

    DisposableEffect(episodeSourcesQuery) {
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
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                } else {
                    HlsPlayerUtil.abandonAudioFocus(audioManager)
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                }
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
                if (exoPlayer.currentPosition > 0) isShowResumeOverlay = false
            }
        }
        exoPlayer.addListener(listener)

        val handler = Handler(Looper.getMainLooper())
        var nextClickCount = 0
        var nextClickRunnable: Runnable? = null
        var previousClickCount = 0
        var previousClickRunnable: Runnable? = null

        mediaSessionCompat = MediaSessionCompat(context, "VideoPlayerSession").apply {
            val metadataBuilder = MediaMetadataCompat.Builder()
            metadataBuilder.putString(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                episodeDetailComplement.servers.episodeId
            )
            mediaSessionCompat?.setMetadata(metadataBuilder.build())
            setPlaybackState(
                playbackStateBuilder.setActions(
                    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                ).build()
            )

            setCallback(object : MediaSessionCompat.Callback() {
                fun skipTimeRange(
                    sources: EpisodeSourcesResponse,
                    isToEnd: Boolean = true,
                    handleSelectedEpisodeServer: () -> Unit = {}
                ) {
                    val currentPosition = exoPlayer.currentPosition / 1000
                    val videoDuration = exoPlayer.duration / 1000

                    if (currentPosition == 0L || (videoDuration != -9223372036854775L && currentPosition >= videoDuration - 1)) {
                        handleSelectedEpisodeServer()
                    } else if (sources.outro?.start != null && currentPosition in sources.outro.start..sources.outro.end) {
                        exoPlayer.seekTo(if (isToEnd) sources.outro.end * 1000L else sources.outro.start * 1000L)
                    } else if (sources.intro?.start != null && currentPosition in sources.intro.start..sources.intro.end) {
                        exoPlayer.seekTo(if (isToEnd) sources.intro.end * 1000L else sources.intro.start * 1000L)
                    } else {
                        handleSelectedEpisodeServer()
                    }
                }

                override fun onPlay() {
                    exoPlayer.play()
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                }

                override fun onPause() {
                    exoPlayer.pause()
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                }

                override fun onSkipToNext() {
                    nextClickCount++
                    nextClickRunnable?.let { handler.removeCallbacks(it) }

                    if (nextClickCount >= 3) {
                        skipTimeRange(episodeDetailComplement.sources) {
                            nextEpisode?.let {
                                handleSelectedEpisodeServer(
                                    episodeSourcesQuery.copy(id = it.episodeId)
                                )
                            }
                        }
                        nextClickCount = 0
                    } else {
                        nextClickRunnable = Runnable {
                            if (nextClickCount == 1) {
                                exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                            }
                            nextClickCount = 0
                        }
                        handler.postDelayed(nextClickRunnable, 300)
                    }
                }

                override fun onSkipToPrevious() {
                    previousClickCount++
                    previousClickRunnable?.let { handler.removeCallbacks(it) }

                    if (previousClickCount >= 3) {
                        skipTimeRange(episodeDetailComplement.sources, false) {
                            previousEpisode?.let {
                                handleSelectedEpisodeServer(
                                    episodeSourcesQuery.copy(id = it.episodeId)
                                )
                            }
                        }
                        previousClickCount = 0
                    } else {
                        previousClickRunnable = Runnable {
                            if (previousClickCount == 1) {
                                exoPlayer.seekTo(exoPlayer.currentPosition - 10000)
                            }
                            previousClickCount = 0
                        }
                        handler.postDelayed(previousClickRunnable, 300)
                    }
                }

                override fun onStop() {
                    nextClickRunnable?.let { handler.removeCallbacks(it) }
                    previousClickRunnable?.let { handler.removeCallbacks(it) }
                    nextClickCount = 0
                    previousClickCount = 0
                }
            })
            isActive = true
        }

        mediaControllerCompat = MediaControllerCompat(context, mediaSessionCompat!!)

        onDispose {
            exoPlayer.removeListener(listener)
            introOutroHandler.stop()

            HlsPlayerUtil.abandonAudioFocus(audioManager)
            HlsPlayerUtil.releasePlayer(playerView)
            val seekPosition = exoPlayer.currentPosition
            if (seekPosition > 10000 && seekPosition != episodeDetailComplement.lastTimestamp) {
                val updatedEpisode = episodeDetailComplement.copy(
                    lastTimestamp = seekPosition,
                    lastWatched = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                )
                updateEpisodeDetailComplement(updatedEpisode)
            }

            mediaSessionCompat?.release()
            mediaSessionCompat = null
            mediaControllerCompat = null
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
        isShowResumeOverlay = isShowResumeOverlay,
        setShowResumeOverlay = { isShowResumeOverlay = it },
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