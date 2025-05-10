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
import com.example.animeapp.ui.animeWatch.WatchState
import com.example.animeapp.utils.HlsPlayerUtils
import com.example.animeapp.utils.IntroOutroHandler
import com.example.animeapp.utils.MediaPlaybackService
import com.example.animeapp.utils.HlsPlayerAction
import com.example.animeapp.utils.HlsPlayerState

@SuppressLint("ImplicitSamInstance")
@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSection(
    updateStoredWatchState: (Long?, Long?, String?) -> Unit,
    watchState: WatchState,
    isScreenOn: Boolean,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
    hlsPlayerState: HlsPlayerState,
    isPipMode: Boolean,
    onEnterPipMode: () -> Unit,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    isLandscape: Boolean,
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
    var isShowResumeOverlay by remember { mutableStateOf(watchState.episodeDetailComplement.data?.lastTimestamp != null) }
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
        val player = HlsPlayerUtils.getPlayer()
        if (player != null) {
            playerView.player = player
            val videoSurface = playerView.videoSurfaceView
            HlsPlayerUtils.setVideoSurface(videoSurface)
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
                updateStoredWatchState(position, duration, screenshot)
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
                playerView.player = HlsPlayerUtils.getPlayer()
                HlsPlayerUtils.setVideoSurface(playerView.videoSurfaceView)
                introOutroHandler?.start()
            }
        )
    }

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d("VideoPlayerSection", "Service connected")
                watchState.episodeDetailComplement.data?.let {
                    val binder = service as MediaPlaybackService.MediaPlaybackBinder
                    mediaPlaybackService = binder.getService()
                    setupPlayer(
                        mediaPlaybackService,
                        playerView,
                        it,
                        episodes,
                        episodeSourcesQuery
                    )
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w("VideoPlayerSection", "Service disconnected")
                mediaPlaybackService = null
                playerView.player = null
                HlsPlayerUtils.setVideoSurface(null)
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
            watchState.episodeDetailComplement.data?.let {
                mediaPlaybackService = application.getMediaPlaybackService()
                setupPlayer(
                    mediaPlaybackService,
                    playerView,
                    it,
                    episodes,
                    episodeSourcesQuery
                )
            }
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
                    watchState.episodeDetailComplement.data?.let {
                        isShowNextEpisode = updateNextEpisodeName(
                            episodes = episodes,
                            currentEpisode = it.servers.episodeNo,
                            setNextEpisodeName = { nextEpisodeName = it }
                        )
                        isShowResumeOverlay = false
                        (context as? FragmentActivity)?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }
        HlsPlayerUtils.getPlayer()?.addListener(playerListener)

        onDispose {
            Log.d("VideoPlayerSection", "Disposing VideoPlayerSection")
            try {
                mediaControllerCompat?.transportControls?.pause()
                HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
                Log.d("VideoPlayerSection", "Paused playback before disposal")
                HlsPlayerUtils.getPlayer()?.removeListener(playerListener)

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
                HlsPlayerUtils.setVideoSurface(null)
            } catch (e: IllegalArgumentException) {
                Log.w("VideoPlayerSection", "Service already unbound", e)
            }
        }
    }

    LaunchedEffect(episodeSourcesQuery) {
        Log.d("VideoPlayerSection", "episodeSourcesQuery changed: ${episodeSourcesQuery.id}")
        introOutroHandler?.stop()
        introOutroHandler = null

        watchState.episodeDetailComplement.data?.let {
            HlsPlayerUtils.dispatch(
                HlsPlayerAction.SetMedia(
                    videoData = it.sources,
                    lastTimestamp = null,
                    onReady = {},
                    onError = {}
                )
            )
            setupPlayer(
                mediaPlaybackService,
                playerView,
                it,
                episodes,
                episodeSourcesQuery
            )
            isShowResumeOverlay = it.lastTimestamp != null
            isShowNextEpisode = false
            nextEpisodeName = ""
        }
    }

    val mediaControllerCallback = remember {
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                state?.let {
                    val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
                    if (isPlaying) {
                        isShowResumeOverlay = false
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
            HlsPlayerUtils.setVideoSurface(null)
        }
    }

    LaunchedEffect(isScreenOn) {
        if (!isScreenOn) mediaControllerCompat?.transportControls?.pause()
    }

    watchState.episodeDetailComplement.data?.let {
        VideoPlayer(
            playerView = playerView,
            hlsPlayerState = hlsPlayerState,
            introOutroHandler = introOutroHandler,
            mediaController = mediaControllerCompat,
            episodeDetailComplement = it,
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
            errorMessage = watchState.errorMessage,
            modifier = modifier,
            videoSize = videoSize,
            onPlay = { mediaControllerCompat?.transportControls?.play() },
            onFastForward = { HlsPlayerUtils.dispatch(HlsPlayerAction.FastForward) },
            onRewind = { HlsPlayerUtils.dispatch(HlsPlayerAction.Rewind) }
        )
    }
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