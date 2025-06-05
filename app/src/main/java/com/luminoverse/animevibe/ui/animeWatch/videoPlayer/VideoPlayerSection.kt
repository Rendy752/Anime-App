package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.luminoverse.animevibe.AnimeApplication
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.animeWatch.WatchState
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.MediaPlaybackAction
import com.luminoverse.animevibe.utils.media.MediaPlaybackService
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.media.PositionState
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SuppressLint("ImplicitSamInstance")
@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSection(
    watchState: WatchState,
    coreState: PlayerCoreState,
    controlsState: StateFlow<ControlsState>,
    positionState: StateFlow<PositionState>,
    playerAction: (HlsPlayerAction) -> Unit,
    getPlayer: () -> ExoPlayer?,
    updateStoredWatchState: (EpisodeDetailComplement) -> Unit,
    onHandleBackPress: () -> Unit,
    isScreenOn: Boolean,
    isAutoPlayVideo: Boolean,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery, Boolean) -> Unit,
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
    var retryCount by remember { mutableIntStateOf(0) }
    val maxRetries = 2

    val playerView = remember { PlayerView(context).apply { useController = false } }
    val player by remember { mutableStateOf(getPlayer()) }
    var mediaBrowser by remember { mutableStateOf<MediaBrowserCompat?>(null) }
    var mediaController by remember { mutableStateOf<MediaControllerCompat?>(null) }
    var mediaPlaybackService by remember { mutableStateOf<MediaPlaybackService?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isShowResumeOverlay by remember { mutableStateOf(watchState.episodeDetailComplement.data?.lastTimestamp != null) }
    var isShowNextEpisode by remember { mutableStateOf(false) }
    var nextEpisodeName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val application = context.applicationContext as AnimeApplication

    fun setupPlayer(
        service: MediaPlaybackService?,
        playerView: PlayerView,
        complement: EpisodeDetailComplement,
        episodes: List<Episode>,
        query: EpisodeSourcesQuery
    ) {
        if (player != null) {
            playerView.player = player
            playerAction(HlsPlayerAction.UpdateWatchState { position, duration, screenshot ->
                watchState.episodeDetailComplement.data?.let {
                    updateStoredWatchState(
                        it.copy(
                            isFavorite = watchState.isFavorite,
                            lastTimestamp = position,
                            duration = duration,
                            screenshot = screenshot,
                            lastWatched = LocalDateTime.now()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    )
                }
            })
            val videoSurface = playerView.videoSurfaceView
            playerAction(HlsPlayerAction.SetVideoSurface(videoSurface))
            Log.d(
                "VideoPlayerSection",
                "Player bound to PlayerView, video surface set: ${videoSurface?.javaClass?.simpleName}"
            )
        } else {
            Log.w("VideoPlayerSection", "Player is null")
            onPlayerError("Player not initialized")
            isLoading = false
            return
        }

        service?.dispatch(
            MediaPlaybackAction.SetEpisodeData(
                complement = complement,
                episodes = episodes,
                query = query,
                handleSelectedEpisodeServer = { episodeQuery ->
                    handleSelectedEpisodeServer(episodeQuery, false)
                }
            )
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
                playerAction(HlsPlayerAction.SetVideoSurface(null))
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

    LaunchedEffect(episodeSourcesQuery, retryCount) {
        delay(500L)
        if (retryCount < maxRetries) {
            Log.d(
                "VideoPlayerSection",
                "Attempting playback, retry #$retryCount for episode: ${episodeSourcesQuery.id}"
            )
            initializePlayer()
        } else {
            onPlayerError("Failed to load video after $maxRetries attempts")
            isLoading = false
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
                    Log.d(
                        "VideoPlayerSection",
                        "Playback state changed: ${state.state}"
                    )
                }
            }
        }
    }

    val mediaBrowserConnectionCallback = remember {
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                Log.d("VideoPlayerSection", "MediaBrowser connected")
                mediaBrowser?.sessionToken?.let { token ->
                    try {
                        mediaController = MediaControllerCompat(context, token)
                        mediaController?.registerCallback(mediaControllerCallback)
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
                mediaController?.unregisterCallback(mediaControllerCallback)
                mediaController = null
            }

            override fun onConnectionFailed() {
                Log.e("VideoPlayerSection", "MediaBrowser connection failed")
                onPlayerError("Media browser connection failed")
                isLoading = false
            }
        }
    }

    DisposableEffect(Unit) {
        initializePlayer()

        val playerListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (retryCount < maxRetries) {
                    retryCount++
                    Log.d(
                        "VideoPlayerSection",
                        "Playback error, retrying ($retryCount/$maxRetries)"
                    )
                    handleSelectedEpisodeServer(episodeSourcesQuery, true)
                } else {
                    onPlayerError("Playback failed: ${error.message}")
                    isLoading = false
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        Log.d("VideoPlayerSection", "Episode ended, showing next episode overlay")
                        watchState.episodeDetailComplement.data?.let { episodeDetailComplement ->
                            val nextEpisode =
                                episodes.find { it.episodeNo == episodeDetailComplement.servers.episodeNo + 1 }
                            if (nextEpisode != null) {
                                nextEpisodeName = nextEpisode.name
                                isShowNextEpisode = true
                            } else {
                                isShowNextEpisode = false
                            }
                            isShowResumeOverlay = false
                            (context as? FragmentActivity)?.window?.clearFlags(
                                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            )
                        }
                    }

                    Player.STATE_READY -> {
                        isLoading = false
                        Log.d("VideoPlayerSection", "Player ready")
                    }

                    Player.STATE_BUFFERING -> {
                        isLoading = true
                        Log.d("VideoPlayerSection", "Player buffering")
                    }

                    Player.STATE_IDLE -> {
                        isLoading = true
                        Log.d("VideoPlayerSection", "Player idle")
                    }
                }
            }
        }
        player?.addListener(playerListener)

        Log.d("VideoPlayerSection", "Initializing MediaBrowser")
        mediaBrowser = MediaBrowserCompat(
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
            Log.d("VideoPlayerSection", "Disposing VideoPlayerSection")
            try {
                playerAction(HlsPlayerAction.Pause)
                Log.d("VideoPlayerSection", "Paused playback before disposal")
                player?.removeListener(playerListener)
                coroutineScope.launch {
                    mediaPlaybackService?.state?.collectLatest { state ->
                        val isNotificationActive = state.isForeground
                        Log.d("VideoPlayerSection", "isForeground: $isNotificationActive")
                        if (!isNotificationActive) {
                            Log.d(
                                "VideoPlayerSection",
                                "Stopping MediaPlaybackService (no notification active)"
                            )
                            mediaPlaybackService?.dispatch(MediaPlaybackAction.StopService)
                            if (!application.isMediaServiceBound()) {
                                context.unbindService(serviceConnection)
                                Log.d("VideoPlayerSection", "Unbound service")
                            } else {
                                Log.d(
                                    "VideoPlayerSection",
                                    "Service kept bound by AnimeApplication"
                                )
                            }
                        } else {
                            Log.d(
                                "VideoPlayerSection",
                                "Keeping service alive due to foreground notification"
                            )
                        }
                        this@launch.cancel()
                    }
                }

                Log.d("VideoPlayerSection", "Disconnecting MediaBrowser")
                mediaController?.unregisterCallback(mediaControllerCallback)
                mediaBrowser?.disconnect()

                playerView.player = null
                playerAction(HlsPlayerAction.SetVideoSurface(null))
            } catch (e: IllegalArgumentException) {
                Log.w("VideoPlayerSection", "Service already unbound", e)
            }
        }
    }

    LaunchedEffect(episodeSourcesQuery) {
        Log.d("VideoPlayerSection", "episodeSourcesQuery changed: ${episodeSourcesQuery.id}")
        watchState.episodeDetailComplement.data?.let {
            playerAction(
                HlsPlayerAction.SetMedia(
                    videoData = it.sources,
                    isAutoPlayVideo = isAutoPlayVideo,
                    positionState = PositionState(
                        currentPosition = it.lastTimestamp ?: 0,
                        duration = it.duration ?: 0
                    ),
                    onReady = { isLoading = false },
                    onError = { error ->
                        onPlayerError(error)
                        isLoading = false
                    }
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

    LaunchedEffect(isScreenOn) {
        if (!isScreenOn) {
            playerAction(HlsPlayerAction.Pause)
            Log.d("VideoPlayerSection", "Paused due to screen off")
        }
    }

    LaunchedEffect(watchState.errorMessage) {
        if (watchState.errorMessage != null) {
            onPlayerError(watchState.errorMessage)
            isLoading = false
            Log.d("VideoPlayerSection", "Error from watchState: ${watchState.errorMessage}")
        }
    }

    watchState.episodeDetailComplement.data?.let { episodeDetailComplement ->
        player?.let {
            VideoPlayer(
                playerView = playerView,
                player = it,
                coreState = coreState,
                controlsState = controlsState,
                positionState = positionState,
                playerAction = playerAction,
                mediaController = mediaController,
                onHandleBackPress = onHandleBackPress,
                episodeDetailComplement = episodeDetailComplement,
                episodes = episodes,
                episodeSourcesQuery = episodeSourcesQuery,
                handleSelectedEpisodeServer = handleSelectedEpisodeServer,
                isPipMode = isPipMode,
                onEnterPipMode = onEnterPipMode,
                isFullscreen = isFullscreen,
                onFullscreenChange = onFullscreenChange,
                isAutoPlayVideo = isAutoPlayVideo,
                isShowResumeOverlay = isShowResumeOverlay,
                setShowResumeOverlay = { isShowResumeOverlay = it },
                isShowNextEpisode = isShowNextEpisode,
                setShowNextEpisode = { isShowNextEpisode = it },
                nextEpisodeName = nextEpisodeName,
                isLandscape = isLandscape,
                errorMessage = watchState.errorMessage,
                modifier = modifier,
                videoSize = videoSize
            )
        }
    }
}