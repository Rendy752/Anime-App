package com.example.animeapp.ui.animeWatch.videoPlayer

import android.annotation.SuppressLint
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
import android.util.Log
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
import com.example.animeapp.AnimeApplication
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.IntroOutroHandler
import com.example.animeapp.utils.MediaPlaybackService

@SuppressLint("ImplicitSamInstance")
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
    val playerView = remember { PlayerView(context).apply { useController = true } }
    var mediaBrowserCompat by remember { mutableStateOf<MediaBrowserCompat?>(null) }
    var mediaControllerCompat by remember { mutableStateOf<MediaControllerCompat?>(null) }
    var mediaPlaybackService by remember { mutableStateOf<MediaPlaybackService?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isShowResumeOverlay by remember { mutableStateOf(episodeDetailComplement.lastTimestamp != null) }
    var isShowNextEpisode by remember { mutableStateOf(false) }
    var nextEpisodeName by remember { mutableStateOf("") }
    var introOutroHandler by remember { mutableStateOf<IntroOutroHandler?>(null) }

    val application = context.applicationContext as AnimeApplication

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d("VideoPlayerSection", "Service connected")
                val binder = service as MediaPlaybackService.MediaPlaybackBinder
                mediaPlaybackService = binder.getService()
                val exoPlayer = mediaPlaybackService?.getExoPlayer()
                if (exoPlayer != null) {
                    playerView.player = exoPlayer
                    Log.d("VideoPlayerSection", "ExoPlayer bound to PlayerView")
                    introOutroHandler = IntroOutroHandler(
                        player = exoPlayer,
                        videoData = episodeDetailComplement.sources
                    ).apply { start() }
                } else {
                    Log.w("VideoPlayerSection", "ExoPlayer is null in service")
                    onPlayerError("ExoPlayer not initialized")
                }
                mediaPlaybackService?.setEpisodeData(
                    complement = episodeDetailComplement,
                    episodes = episodes,
                    query = episodeSourcesQuery,
                    handler = { handleSelectedEpisodeServer(it) },
                    updateStoredWatchState = { updateStoredWatchState(it) },
                    onPlayerError = { error ->
                        Log.e("VideoPlayerSection", "Player error: $error")
                        onPlayerError(error)
                        isLoading = false
                    },
                    onPlayerReady = {
                        isShowNextEpisode = false
                        isLoading = false
                        Log.d("VideoPlayerSection", "Player ready")
                        playerView.player = mediaPlaybackService?.getExoPlayer()
                        introOutroHandler?.start()
                    }
                )
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w("VideoPlayerSection", "Service disconnected")
                mediaPlaybackService = null
                playerView.player = null
                introOutroHandler?.stop()
                introOutroHandler = null
            }
        }
    }

    DisposableEffect(Unit) {
        Log.d(
            "VideoPlayerSection",
            "Attempting to bind to MediaPlaybackService for episode: ${episodeSourcesQuery.id}"
        )
        if (application.isMediaServiceBound()) {
            mediaPlaybackService = application.getMediaPlaybackService()
            val exoPlayer = mediaPlaybackService?.getExoPlayer()
            if (exoPlayer != null) {
                playerView.player = exoPlayer
                Log.d("VideoPlayerSection", "ExoPlayer bound to PlayerView (already bound)")
            } else {
                Log.w("VideoPlayerSection", "ExoPlayer is null in bound service")
                onPlayerError("ExoPlayer not initialized")
            }
            mediaPlaybackService?.setEpisodeData(
                complement = episodeDetailComplement,
                episodes = episodes,
                query = episodeSourcesQuery,
                handler = { handleSelectedEpisodeServer(it) },
                updateStoredWatchState = { updateStoredWatchState(it) },
                onPlayerError = { error ->
                    Log.e("VideoPlayerSection", "Player error: $error")
                    onPlayerError(error)
                    isLoading = false
                },
                onPlayerReady = {
                    isShowNextEpisode = false
                    isLoading = false
                    Log.d("VideoPlayerSection", "Player ready (already bound)")
                    mediaPlaybackService?.getExoPlayer()?.let { exoPlayer ->
                        playerView.player = exoPlayer
                        introOutroHandler = IntroOutroHandler(
                            player = exoPlayer,
                            videoData = episodeDetailComplement.sources
                        ).apply { start() }
                    }
                }
            )
        } else {
            val intent = Intent(context, MediaPlaybackService::class.java)
            try {
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                Log.d("VideoPlayerSection", "Binding to MediaPlaybackService")
            } catch (e: Exception) {
                Log.e("VideoPlayerSection", "Failed to bind service", e)
                onPlayerError("Failed to bind service")
                isLoading = false
            }
        }
        onDispose {
            Log.d("VideoPlayerSection", "Disposing VideoPlayerSection")
            try {
                // Pause playback to remove foreground state
                mediaControllerCompat?.transportControls?.pause()
                mediaPlaybackService?.getExoPlayer()?.pause()
                Log.d("VideoPlayerSection", "Paused playback before disposal")

                // Stop IntroOutroHandler
                introOutroHandler?.stop()
                introOutroHandler = null
                Log.d("VideoPlayerSection", "Stopped IntroOutroHandler")

                // Check if service should persist
                val isNotificationActive = mediaPlaybackService?.isForegroundService() == true
                Log.d("VideoPlayerSection", "isForegroundService: $isNotificationActive")
                if (!isNotificationActive) {
                    Log.d("VideoPlayerSection", "Stopping MediaPlaybackService (no notification active)")
                    mediaPlaybackService?.stopService()
                    if (!application.isMediaServiceBound()) {
                        context.unbindService(serviceConnection)
                        Log.d("VideoPlayerSection", "Unbound service")
                    } else {
                        Log.d("VideoPlayerSection", "Service kept bound by AnimeApplication")
                        application.cleanupService()
                    }
                } else {
                    Log.d("VideoPlayerSection", "Keeping service alive due to foreground notification")
                }
                playerView.player = null
                HlsPlayerUtil.abandonAudioFocus(audioManager)
            } catch (e: IllegalArgumentException) {
                Log.w("VideoPlayerSection", "Service already unbound", e)
            }
        }
    }

    LaunchedEffect(episodeSourcesQuery) {
        Log.d("VideoPlayerSection", "episodeSourcesQuery changed: ${episodeSourcesQuery.id}")
        introOutroHandler?.stop()
        introOutroHandler = null
        mediaPlaybackService?.getExoPlayer()?.let { player ->
            player.stop()
            player.clearMediaItems()
            player.pause()
        }
        mediaPlaybackService?.setEpisodeData(
            complement = episodeDetailComplement,
            episodes = episodes,
            query = episodeSourcesQuery,
            handler = { handleSelectedEpisodeServer(it) },
            updateStoredWatchState = { updateStoredWatchState(it) },
            onPlayerError = { error ->
                Log.e("VideoPlayerSection", "Player error: $error")
                onPlayerError(error)
                isLoading = false
            },
            onPlayerReady = {
                isShowNextEpisode = false
                isLoading = false
                Log.d("VideoPlayerSection", "Player ready for new episode")
                mediaPlaybackService?.getExoPlayer()?.let { exoPlayer ->
                    playerView.player = exoPlayer
                    introOutroHandler = IntroOutroHandler(
                        player = exoPlayer,
                        videoData = episodeDetailComplement.sources
                    ).apply { start() }
                }
            }
        )
        isShowResumeOverlay = episodeDetailComplement.lastTimestamp != null
        isShowNextEpisode = false
        nextEpisodeName = ""
    }

    val mediaControllerCallback = remember {
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                state?.let {
                    Log.d(
                        "VideoPlayerSection",
                        "Playback state changed: state=${it.state}, position=${it.position}"
                    )
                    isLoading = it.state == PlaybackStateCompat.STATE_BUFFERING ||
                            it.state == PlaybackStateCompat.STATE_CONNECTING ||
                            it.state == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT ||
                            it.state == PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS ||
                            it.state == PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM

                    when (it.state) {
                        PlaybackStateCompat.STATE_PLAYING -> {
                            isShowNextEpisode = false
                            isShowResumeOverlay = false
                            (context as? FragmentActivity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            HlsPlayerUtil.requestAudioFocus(audioManager)
                            Log.d(
                                "VideoPlayerSection",
                                "Playing: Audio focus requested, screen kept on"
                            )
                        }

                        PlaybackStateCompat.STATE_PAUSED -> {
                            HlsPlayerUtil.abandonAudioFocus(audioManager)
                            (context as? FragmentActivity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            Log.d(
                                "VideoPlayerSection",
                                "Paused: Audio focus abandoned, screen-on cleared"
                            )
                        }

                        PlaybackStateCompat.STATE_STOPPED -> {
                            isShowNextEpisode = updateNextEpisodeName(
                                episodes = episodes,
                                currentEpisode = episodeDetailComplement.servers.episodeNo,
                                setNextEpisodeName = { nextEpisodeName = it }
                            )
                            HlsPlayerUtil.abandonAudioFocus(audioManager)
                            (context as? FragmentActivity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            Log.d(
                                "VideoPlayerSection",
                                "Stopped: Showing next episode if available"
                            )
                        }

                        PlaybackStateCompat.STATE_ERROR -> {
                            onPlayerError("Playback error: ${state.errorMessage}")
                            Log.e("VideoPlayerSection", "Playback error: ${state.errorMessage}")
                            HlsPlayerUtil.abandonAudioFocus(audioManager)
                            (context as? FragmentActivity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }

                        PlaybackStateCompat.STATE_BUFFERING -> {
                            isLoading = true
                            Log.d("VideoPlayerSection", "Buffering: Showing loading indicator")
                        }

                        PlaybackStateCompat.STATE_NONE -> {
                            isLoading = false
                            HlsPlayerUtil.abandonAudioFocus(audioManager)
                            (context as? FragmentActivity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            Log.d("VideoPlayerSection", "None: Player idle, no media loaded")
                        }

                        PlaybackStateCompat.STATE_CONNECTING -> {
                            isLoading = true
                            Log.d("VideoPlayerSection", "Connecting: Preparing media source")
                        }

                        PlaybackStateCompat.STATE_FAST_FORWARDING -> {
                            isLoading = false
                            isShowNextEpisode = false
                            isShowResumeOverlay = false
                            HlsPlayerUtil.requestAudioFocus(audioManager)
                            Log.d("VideoPlayerSection", "Fast-forwarding: Maintaining audio focus")
                        }

                        PlaybackStateCompat.STATE_REWINDING -> {
                            isLoading = false
                            isShowNextEpisode = false
                            isShowResumeOverlay = false
                            HlsPlayerUtil.requestAudioFocus(audioManager)
                            Log.d("VideoPlayerSection", "Rewinding: Maintaining audio focus")
                        }

                        PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> {
                            isLoading = true
                            isShowNextEpisode = false
                            isShowResumeOverlay = false
                            Log.d("VideoPlayerSection", "Skipping to next: Loading new episode")
                        }

                        PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS -> {
                            isLoading = true
                            isShowNextEpisode = false
                            isShowResumeOverlay = false
                            Log.d("VideoPlayerSection", "Skipping to previous: Loading new episode")
                        }

                        PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM -> {
                            isLoading = true
                            isShowNextEpisode = false
                            isShowResumeOverlay = false
                            Log.d(
                                "VideoPlayerSection",
                                "Skipping to queue item: Loading specific episode"
                            )
                        }
                    }
                }
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
        introOutroHandler = introOutroHandler,
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