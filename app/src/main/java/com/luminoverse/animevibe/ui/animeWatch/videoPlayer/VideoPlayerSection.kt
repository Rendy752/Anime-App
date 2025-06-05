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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.luminoverse.animevibe.AnimeApplication
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.animeWatch.PlayerUiState
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
    playerUiState: PlayerUiState,
    coreState: PlayerCoreState,
    controlsState: StateFlow<ControlsState>,
    positionState: StateFlow<PositionState>,
    playerAction: (HlsPlayerAction) -> Unit,
    isLandscape: Boolean,
    getPlayer: () -> ExoPlayer?,
    updateStoredWatchState: (EpisodeDetailComplement) -> Unit,
    onHandleBackPress: () -> Unit,
    isScreenOn: Boolean,
    isAutoPlayVideo: Boolean,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery, Boolean) -> Unit,
    onEnterPipMode: () -> Unit,
    setIsLoading: (Boolean) -> Unit,
    setFullscreenChange: (Boolean) -> Unit,
    setShowResume: (Boolean) -> Unit,
    setShowNextEpisode: (Boolean) -> Unit,
    setPlayerError: (String?) -> Unit,
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
            setPlayerError("Player not initialized")
            setIsLoading(false)
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
                setPlayerError("Service disconnected")
                setIsLoading(false)
            }
        }
    }

    fun initializePlayer() {
        Log.d("VideoPlayerSection", "Initializing player for episode: ${episodeSourcesQuery.id}")
        setIsLoading(true)
        setPlayerError(null)

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
                setPlayerError("Failed to bind service")
                setIsLoading(false)
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
            setPlayerError("Failed to load video after $maxRetries attempts")
            setIsLoading(false)
        }
    }

    val mediaControllerCallback = remember {
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                state?.let {
                    val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
                    if (isPlaying) {
                        setShowResume(false)
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
                        setPlayerError("Media controller initialization failed")
                        setIsLoading(false)
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
                setPlayerError("Media browser connection failed")
                setIsLoading(false)
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
                    setPlayerError("Playback failed: ${error.message}")
                    setIsLoading(false)
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
                setPlayerError("Media browser connection failed")
                setIsLoading(false)
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
                    onReady = { setIsLoading(false) },
                    onError = { error ->
                        setPlayerError(error)
                        setIsLoading(false)
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
            setShowResume(it.lastTimestamp != null)
            setShowNextEpisode(false)
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
            setPlayerError(watchState.errorMessage)
            setIsLoading(false)
            Log.d("VideoPlayerSection", "Error from watchState: ${watchState.errorMessage}")
        }
    }

    watchState.episodeDetailComplement.data?.let { episodeDetailComplement ->
        player?.let {
            VideoPlayer(
                playerView = playerView,
                player = it,
                coreState = coreState,
                playerUiState = playerUiState,
                controlsState = controlsState,
                positionState = positionState,
                playerAction = playerAction,
                mediaController = mediaController,
                onHandleBackPress = onHandleBackPress,
                episodeDetailComplement = episodeDetailComplement,
                episodeDetailComplements = watchState.episodeDetailComplements,
                episodes = episodes,
                episodeSourcesQuery = episodeSourcesQuery,
                handleSelectedEpisodeServer = handleSelectedEpisodeServer,
                onEnterPipMode = onEnterPipMode,
                isAutoPlayVideo = isAutoPlayVideo,
                setFullscreenChange = setFullscreenChange,
                setShowResume = setShowResume,
                setShowNextEpisode = setShowNextEpisode,
                isLandscape = isLandscape,
                errorMessage = watchState.errorMessage,
                modifier = modifier,
                videoSize = videoSize
            )
        }
    }
}