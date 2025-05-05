package com.example.animeapp.ui.animeWatch.videoPlayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.animeapp.AnimeApplication
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.IntroOutroHandler
import com.example.animeapp.utils.MediaPlaybackService
import com.example.animeapp.utils.PlayerAction

@SuppressLint("ImplicitSamInstance")
@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSection(
    updateStoredWatchState: (EpisodeDetailComplement, Long?, Long?, String?) -> Unit,
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
    errorMessage: String?,
    onPlayerError: (String?) -> Unit,
    modifier: Modifier = Modifier,
    videoSize: Modifier
) {
    val context = LocalContext.current
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

    fun setupPlayer(
        service: MediaPlaybackService?,
        playerView: PlayerView,
        complement: EpisodeDetailComplement,
        episodes: List<Episode>,
        query: EpisodeSourcesQuery
    ) {
        val player = HlsPlayerUtil.getPlayer()
        if (player != null) {
            playerView.player = player
            val videoSurface = playerView.videoSurfaceView
            HlsPlayerUtil.setVideoSurface(videoSurface)
            Log.d(
                "VideoPlayerSection",
                "Player bound to PlayerView, video surface set: ${videoSurface?.javaClass?.simpleName}"
            )
            introOutroHandler = IntroOutroHandler(
                player = player,
                videoData = complement.sources
            ).apply { start() }
        } else {
            Log.w("VideoPlayerSection", "Player is null in service")
            onPlayerError("Player not initialized")
            isLoading = false
            return
        }

        service?.setEpisodeData(
            complement = complement,
            episodes = episodes,
            query = query,
            handler = { handleSelectedEpisodeServer(it) },
            updateStoredWatchState = { position, duration, screenshot ->
                updateStoredWatchState(complement, position, duration, screenshot)
            },
            onPlayerError = { error ->
                Log.e("VideoPlayerSection", "Player error: $error")
                onPlayerError(error)
                isLoading = false
            },
            onPlayerReady = {
                isShowNextEpisode = false
                isLoading = false
                onPlayerError(null)
                Log.d("VideoPlayerSection", "Player ready")
                playerView.player = HlsPlayerUtil.getPlayer()
                HlsPlayerUtil.setVideoSurface(playerView.videoSurfaceView)
                introOutroHandler?.start()
            }
        )
    }

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d("VideoPlayerSection", "Service connected")
                val binder = service as MediaPlaybackService.MediaPlaybackBinder
                mediaPlaybackService = binder.getService()
                setupPlayer(
                    mediaPlaybackService,
                    playerView,
                    episodeDetailComplement,
                    episodes,
                    episodeSourcesQuery
                )
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w("VideoPlayerSection", "Service disconnected")
                mediaPlaybackService = null
                playerView.player = null
                HlsPlayerUtil.setVideoSurface(null)
                introOutroHandler?.stop()
                introOutroHandler = null
                onPlayerError("Service disconnected")
                isLoading = false
            }
        }
    }

    fun initializePlayer() {
        Log.d("VideoPlayerSection", "Initializing player for episode: ${episodeSourcesQuery.id}")
        isLoading = true
        onPlayerError(null)

        if (application.isMediaServiceBound()) {
            mediaPlaybackService = application.getMediaPlaybackService()
            setupPlayer(
                mediaPlaybackService,
                playerView,
                episodeDetailComplement,
                episodes,
                episodeSourcesQuery
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
    }

    DisposableEffect(Unit) {
        initializePlayer()

        val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    Log.d("VideoPlayerSection", "Episode ended, showing next episode overlay")
                    isShowNextEpisode = updateNextEpisodeName(
                        episodes = episodes,
                        currentEpisode = episodeDetailComplement.servers.episodeNo,
                        setNextEpisodeName = { nextEpisodeName = it }
                    )
                    isShowResumeOverlay = false
                    (context as? FragmentActivity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
        HlsPlayerUtil.getPlayer()?.addListener(playerListener)

        onDispose {
            Log.d("VideoPlayerSection", "Disposing VideoPlayerSection")
            try {
                mediaControllerCompat?.transportControls?.pause()
                HlsPlayerUtil.dispatch(PlayerAction.Pause)
                Log.d("VideoPlayerSection", "Paused playback before disposal")
                HlsPlayerUtil.getPlayer()?.removeListener(playerListener)

                val isNotificationActive = mediaPlaybackService?.isForegroundService() == true
                Log.d("VideoPlayerSection", "isForegroundService: $isNotificationActive")
                if (!isNotificationActive) {
                    Log.d(
                        "VideoPlayerSection",
                        "Stopping MediaPlaybackService (no notification active)"
                    )
                    mediaPlaybackService?.stopService()
                    if (!application.isMediaServiceBound()) {
                        context.unbindService(serviceConnection)
                        Log.d("VideoPlayerSection", "Unbound service")
                    } else {
                        Log.d("VideoPlayerSection", "Service kept bound by AnimeApplication")
                    }
                } else {
                    Log.d(
                        "VideoPlayerSection",
                        "Keeping service alive due to foreground notification"
                    )
                }
                introOutroHandler?.stop()
                introOutroHandler = null
                playerView.player = null
                HlsPlayerUtil.setVideoSurface(null)
            } catch (e: IllegalArgumentException) {
                Log.w("VideoPlayerSection", "Service already unbound", e)
            }
        }
    }

    LaunchedEffect(episodeSourcesQuery) {
        Log.d("VideoPlayerSection", "episodeSourcesQuery changed: ${episodeSourcesQuery.id}")
        introOutroHandler?.stop()
        introOutroHandler = null
        HlsPlayerUtil.dispatch(
            PlayerAction.SetMedia(
                videoData = episodeDetailComplement.sources,
                lastTimestamp = null,
                onReady = {},
                onError = {}
            )
        )
        setupPlayer(
            mediaPlaybackService,
            playerView,
            episodeDetailComplement,
            episodes,
            episodeSourcesQuery
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
                            Log.d("VideoPlayerSection", "Playing: Screen kept on")
                        }

                        PlaybackStateCompat.STATE_PAUSED -> {
                            val player = HlsPlayerUtil.getPlayer()
                            if (player?.playbackState == Player.STATE_ENDED) {
                                Log.d(
                                    "VideoPlayerSection",
                                    "Paused after episode end, showing next episode overlay"
                                )
                                isShowNextEpisode = updateNextEpisodeName(
                                    episodes = episodes,
                                    currentEpisode = episodeDetailComplement.servers.episodeNo,
                                    setNextEpisodeName = { nextEpisodeName = it }
                                )
                                isShowResumeOverlay = false
                            }
                            (context as? FragmentActivity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            Log.d("VideoPlayerSection", "Paused: Screen-on cleared")
                        }

                        PlaybackStateCompat.STATE_STOPPED -> {
                            isShowNextEpisode = updateNextEpisodeName(
                                episodes = episodes,
                                currentEpisode = episodeDetailComplement.servers.episodeNo,
                                setNextEpisodeName = { nextEpisodeName = it }
                            )
                            isShowResumeOverlay = false
                            (context as? FragmentActivity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            Log.d(
                                "VideoPlayerSection",
                                "Stopped: Showing next episode if available"
                            )
                        }

                        PlaybackStateCompat.STATE_ERROR -> {
                            onPlayerError("Playback error: ${state.errorMessage}")
                            Log.e("VideoPlayerSection", "Playback error: ${state.errorMessage}")
                            (context as? FragmentActivity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }

                        PlaybackStateCompat.STATE_BUFFERING -> {
                            isLoading = true
                            Log.d("VideoPlayerSection", "Buffering: Showing loading indicator")
                        }

                        PlaybackStateCompat.STATE_NONE -> {
                            isLoading = false
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
                            Log.d("VideoPlayerSection", "Fast-forwarding")
                        }

                        PlaybackStateCompat.STATE_REWINDING -> {
                            isLoading = false
                            isShowNextEpisode = false
                            isShowResumeOverlay = false
                            Log.d("VideoPlayerSection", "Rewinding")
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
                        isLoading = false
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
                isLoading = false
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
                isLoading = false
            }
        }
        onDispose {
            Log.d("VideoPlayerSection", "Disconnecting MediaBrowser")
            mediaControllerCompat?.unregisterCallback(mediaControllerCallback)
            mediaBrowserCompat?.disconnect()
            HlsPlayerUtil.setVideoSurface(null) // Clear video surface to prevent leaks
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
        errorMessage = errorMessage,
        modifier = modifier,
        videoSize = videoSize,
        onPlay = { mediaControllerCompat?.transportControls?.play() },
        onFastForward = { HlsPlayerUtil.dispatch(PlayerAction.FastForward) },
        onRewind = { HlsPlayerUtil.dispatch(PlayerAction.Rewind) }
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