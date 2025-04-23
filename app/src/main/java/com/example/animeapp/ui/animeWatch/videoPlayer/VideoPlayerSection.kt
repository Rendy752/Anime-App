package com.example.animeapp.ui.animeWatch.videoPlayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.MediaPlaybackService
import android.util.Log

@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSection(
    updateStoredWatchState: (Long?) -> Unit,
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
    onPlayerError: (String?) -> Unit,
    modifier: Modifier = Modifier,
    videoSize: Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val playerView = remember { PlayerView(context) }
    var mediaBrowserCompat by remember { mutableStateOf<MediaBrowserCompat?>(null) }
    var mediaControllerCompat by remember { mutableStateOf<MediaControllerCompat?>(null) }
    var mediaPlaybackService by remember { mutableStateOf<MediaPlaybackService?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isShowResumeOverlay by remember { mutableStateOf(episodeDetailComplement.lastTimestamp != null) }
    var isShowNextEpisode by remember { mutableStateOf(false) }
    var nextEpisodeName by remember { mutableStateOf("") }

    // Service binding
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d("VideoPlayerSection", "Service connected")
                val binder = service as MediaPlaybackService.MediaPlaybackBinder
                mediaPlaybackService = binder.getService()
                playerView.player = mediaPlaybackService?.getExoPlayer()
                mediaPlaybackService?.setEpisodeData(
                    complement = episodeDetailComplement,
                    episodes = episodes,
                    query = episodeSourcesQuery,
                    handler = { handleSelectedEpisodeServer(it) },
                    updateStoredWatchState = { updateStoredWatchState(it) },
                    onPlayerError = onPlayerError,
                    onPlayerReady = {
                        isShowNextEpisode = false
                        isLoading = false
                        Log.d("VideoPlayerSection", "Player ready")
                    }
                )
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w("VideoPlayerSection", "Service disconnected")
                mediaPlaybackService = null
                playerView.player = null
            }
        }
    }

    // Bind to service
    DisposableEffect(episodeDetailComplement, episodeSourcesQuery) {
        Log.d("VideoPlayerSection", "Binding to service")
        val intent = Intent(context, MediaPlaybackService::class.java)
        try {
            context.startForegroundService(intent)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e("VideoPlayerSection", "Failed to bind service", e)
            onPlayerError("Failed to bind service")
        }
        onDispose {
            Log.d("VideoPlayerSection", "Unbinding service")
            try {
                context.unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                Log.w("VideoPlayerSection", "Service already unbound", e)
            }
            playerView.player = null
            HlsPlayerUtil.abandonAudioFocus(audioManager)
        }
    }

    val mediaControllerCallback = remember {
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                state?.let {
                    Log.d(
                        "VideoPlayerSection",
                        "Playback state changed: state=${it.state}, position=${it.position}"
                    )
                    isLoading = false
                    when (it.state) {
                        PlaybackStateCompat.STATE_PLAYING -> {
                            isShowNextEpisode = false
                            isShowResumeOverlay = false
                            (context as? FragmentActivity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            HlsPlayerUtil.requestAudioFocus(audioManager)
                        }

                        PlaybackStateCompat.STATE_PAUSED -> {
                            HlsPlayerUtil.abandonAudioFocus(audioManager)
                        }

                        PlaybackStateCompat.STATE_STOPPED -> {
                            isShowNextEpisode = updateNextEpisodeName(
                                episodes = episodes,
                                currentEpisode = episodeDetailComplement.servers.episodeNo,
                                setNextEpisodeName = { nextEpisodeName = it }
                            )
                        }

                        PlaybackStateCompat.STATE_ERROR -> {
                            onPlayerError("Playback error: ${state.errorMessage}")
                            Log.e("VideoPlayerSection", "Playback error: ${state.errorMessage}")
                        }

                        PlaybackStateCompat.STATE_BUFFERING -> {
                            TODO()
                        }

                        PlaybackStateCompat.STATE_CONNECTING -> {
                            TODO()
                        }

                        PlaybackStateCompat.STATE_FAST_FORWARDING -> {
                            TODO()
                        }

                        PlaybackStateCompat.STATE_NONE -> {
                            TODO()
                        }

                        PlaybackStateCompat.STATE_REWINDING -> {
                            TODO()
                        }

                        PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> {
                            TODO()
                        }

                        PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS -> {
                            TODO()
                        }

                        PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM -> {
                            TODO()
                        }
                    }
                }
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                Log.d(
                    "VideoPlayerSection",
                    "Metadata changed: title=${metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)}, duration=${
                        metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                    }"
                )
            }
        }
    }

    val mediaBrowserConnectionCallback = remember {
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                Log.d("VideoPlayerSection", "MediaBrowser connected")
                mediaBrowserCompat?.sessionToken?.let { token ->
                    try {
                        mediaControllerCompat = MediaControllerCompat(context, token)
                        mediaControllerCompat?.registerCallback(mediaControllerCallback)
                        Log.d("VideoPlayerSection", "MediaController initialized")
                    } catch (e: Exception) {
                        Log.e("VideoPlayerSection", "MediaController initialization failed", e)
                        onPlayerError("Media controller initialization failed")
                    }
                }
            }

            override fun onConnectionSuspended() {
                Log.w("VideoPlayerSection", "MediaBrowser connection suspended")
                mediaControllerCompat?.unregisterCallback(mediaControllerCallback)
                mediaControllerCompat = null
            }

            override fun onConnectionFailed() {
                Log.e("VideoPlayerSection", "MediaBrowser connection failed")
                onPlayerError("Media browser connection failed")
            }
        }
    }

    // Initialize MediaBrowserCompat
    DisposableEffect(Unit) {
        Log.d("VideoPlayerSection", "Initializing MediaBrowser")
        mediaBrowserCompat = MediaBrowserCompat(
            context,
            ComponentName(context, MediaPlaybackService::class.java),
            mediaBrowserConnectionCallback,
            null
        ).apply {
            try {
                connect()
            } catch (e: Exception) {
                Log.e("VideoPlayerSection", "MediaBrowser connection failed", e)
                onPlayerError("Media browser connection failed")
            }
        }
        onDispose {
            Log.d("VideoPlayerSection", "Disconnecting MediaBrowser")
            mediaControllerCompat?.unregisterCallback(mediaControllerCallback)
            mediaBrowserCompat?.disconnect()
        }
    }

    LaunchedEffect(isScreenOn) {
        if (!isScreenOn) mediaControllerCompat?.transportControls?.pause()
    }

    VideoPlayer(
        playerView = playerView,
        introOutroHandler = null,
        mediaController = mediaControllerCompat,
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
        videoSize = videoSize,
        onPlay = { mediaControllerCompat?.transportControls?.play() },
        onPause = { mediaControllerCompat?.transportControls?.pause() },
        onSkipNext = { mediaControllerCompat?.transportControls?.skipToNext() },
        onSkipPrevious = { mediaControllerCompat?.transportControls?.skipToPrevious() },
        onFastForward = {
            val currentPosition = mediaPlaybackService?.getExoPlayer()?.currentPosition
                ?: mediaControllerCompat?.playbackState?.position
                ?: 0
            val duration = mediaPlaybackService?.getExoPlayer()?.duration
                ?: mediaControllerCompat?.metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                ?: Long.MAX_VALUE
            val newPosition = (currentPosition + 10000).coerceAtMost(duration)
            Log.d(
                "VideoPlayerSection",
                "Fast-forward: current=$currentPosition, new=$newPosition, duration=$duration"
            )
            mediaControllerCompat?.transportControls?.seekTo(newPosition)
        },
        onRewind = {
            val currentPosition = mediaPlaybackService?.getExoPlayer()?.currentPosition
                ?: mediaControllerCompat?.playbackState?.position
                ?: 0
            val duration = mediaPlaybackService?.getExoPlayer()?.duration
                ?: mediaControllerCompat?.metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                ?: Long.MAX_VALUE
            val newPosition = (currentPosition - 10000).coerceAtLeast(0)
            Log.d(
                "VideoPlayerSection",
                "Rewind: current=$currentPosition, new=$newPosition, duration=$duration"
            )
            mediaControllerCompat?.transportControls?.seekTo(newPosition)
        }
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
    } else {
        false
    }
}