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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
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
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.MediaPlaybackAction
import com.luminoverse.animevibe.utils.media.MediaPlaybackService
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.resource.Resource
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("ImplicitSamInstance")
@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSection(
    episodeDetailComplement: EpisodeDetailComplement,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    errorMessage: String?,
    playerUiState: PlayerUiState,
    coreState: PlayerCoreState,
    controlsStateFlow: StateFlow<ControlsState>,
    playerAction: (HlsPlayerAction) -> Unit,
    isLandscape: Boolean,
    getPlayer: () -> ExoPlayer?,
    captureScreenshot: suspend () -> String?,
    updateStoredWatchState: (Long?, Long?, String?) -> Unit,
    onHandleBackPress: () -> Unit,
    isScreenOn: Boolean,
    isAutoPlayVideo: Boolean,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery, Boolean) -> Unit,
    onEnterPipMode: () -> Unit,
    isSideSheetVisible: Boolean,
    setSideSheetVisibility: (Boolean) -> Unit,
    setFullscreenChange: (Boolean) -> Unit,
    setShowResume: (Boolean) -> Unit,
    setShowNextEpisode: (Boolean) -> Unit,
    setPlayerError: (String?) -> Unit,
) {
    val context = LocalContext.current

    val playerView = remember { PlayerView(context).apply { useController = false } }
    val player by remember { mutableStateOf(getPlayer()) }
    var mediaBrowser by remember { mutableStateOf<MediaBrowserCompat?>(null) }
    var mediaController by remember { mutableStateOf<MediaControllerCompat?>(null) }
    var mediaPlaybackService by remember { mutableStateOf<MediaPlaybackService?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val application = context.applicationContext as AnimeApplication

    fun setupPlayer() {
        if (player != null) {
            playerView.player = player
            playerAction(HlsPlayerAction.UpdateWatchState { position, duration, screenshot ->
                updateStoredWatchState(position, duration, screenshot)
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
            return
        }

        mediaPlaybackService?.dispatch(
            MediaPlaybackAction.SetEpisodeData(
                complement = episodeDetailComplement,
                episodes = episodes,
                query = episodeSourcesQuery,
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
                val binder = service as MediaPlaybackService.MediaPlaybackBinder
                mediaPlaybackService = binder.getService()
                setupPlayer()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w("VideoPlayerSection", "Service disconnected")
                mediaPlaybackService = null
                playerView.player = null
                playerAction(HlsPlayerAction.SetVideoSurface(null))
                setPlayerError("Service disconnected")
            }
        }
    }

    fun initializePlayer() {
        Log.d("VideoPlayerSection", "Initializing player for episode: ${episodeSourcesQuery.id}")
        setPlayerError(null)

        if (application.isMediaServiceBound()) {
            mediaPlaybackService = application.getMediaPlaybackService()
            setupPlayer()
        } else {
            val intent = Intent(context, MediaPlaybackService::class.java)
            try {
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                Log.d("VideoPlayerSection", "Binding to MediaPlaybackService")
            } catch (e: Exception) {
                Log.e("VideoPlayerSection", "Failed to bind service", e)
                setPlayerError("Failed to bind service")
            }
        }
    }

    LaunchedEffect(player, episodeSourcesQuery) {
        if (player == null || player?.playbackState == Player.STATE_IDLE) {
            initializePlayer()
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
            }
        }
    }

    DisposableEffect(Unit) {
        initializePlayer()

        val playerListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                setPlayerError("Playback failed: ${error.message}, try a different server.")
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
                        cancel()
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

    LaunchedEffect(episodeDetailComplement.sources.link.file) {
        Log.d("VideoPlayerSection", "episodeSourcesQuery changed: ${episodeSourcesQuery.id}")
        playerAction(
            HlsPlayerAction.SetMedia(
                videoData = episodeDetailComplement.sources,
                isAutoPlayVideo = isAutoPlayVideo,
                currentPosition =
                    if (isAutoPlayVideo && ((episodeDetailComplement.lastTimestamp ?: 0) <
                                (episodeDetailComplement.duration ?: 0))
                    ) episodeDetailComplement.lastTimestamp ?: 0
                    else 0,
                duration = episodeDetailComplement.duration ?: 0,
                onError = { error -> setPlayerError(error) }
            )
        )
        setupPlayer()
        setShowResume(episodeDetailComplement.lastTimestamp != null)
        setShowNextEpisode(false)
    }

    LaunchedEffect(isScreenOn) {
        if (!isScreenOn) {
            playerAction(HlsPlayerAction.Pause)
            Log.d("VideoPlayerSection", "Paused due to screen off")
        }
    }

    player?.let {
        VideoPlayer(
            playerView = playerView,
            player = it,
            captureScreenshot = captureScreenshot,
            coreState = coreState,
            playerUiState = playerUiState,
            controlsStateFlow = controlsStateFlow,
            playerAction = playerAction,
            onHandleBackPress = onHandleBackPress,
            episodeDetailComplement = episodeDetailComplement,
            episodeDetailComplements = episodeDetailComplements,
            episodes = episodes,
            episodeSourcesQuery = episodeSourcesQuery,
            handleSelectedEpisodeServer = handleSelectedEpisodeServer,
            onEnterPipMode = onEnterPipMode,
            isSideSheetVisible = isSideSheetVisible,
            setSideSheetVisibility = setSideSheetVisibility,
            isAutoplayEnabled = isAutoPlayVideo,
            onFullscreenChange = setFullscreenChange,
            onShowResumeChange = setShowResume,
            onShowNextEpisodeChange = setShowNextEpisode,
            isLandscape = isLandscape,
            errorMessage = errorMessage
        )
    }
}