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
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.luminoverse.animevibe.AnimeApplication
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.animeWatch.WatchState
import com.luminoverse.animevibe.utils.media.HlsPlayerUtils
import com.luminoverse.animevibe.utils.media.MediaPlaybackAction
import com.luminoverse.animevibe.utils.media.MediaPlaybackService
import com.luminoverse.animevibe.utils.media.HlsPlayerState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.media3.common.PlaybackException

@SuppressLint("ImplicitSamInstance")
@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSection(
    updateStoredWatchState: (Long?, Long?, String?) -> Unit,
    watchState: WatchState,
    isScreenOn: Boolean,
    isAutoPlayVideo: Boolean,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery, Boolean) -> Unit,
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
    var retryCount by remember { mutableIntStateOf(0) }
    val maxRetries = 3

    val playerView = remember { PlayerView(context).apply { useController = false } }
    var mediaBrowserCompat by remember { mutableStateOf<MediaBrowserCompat?>(null) }
    var mediaControllerCompat by remember { mutableStateOf<MediaControllerCompat?>(null) }
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
        val player = HlsPlayerUtils.getPlayer()
        if (player != null) {
            playerView.player = player
            val videoSurface = playerView.videoSurfaceView
            HlsPlayerUtils.dispatch(HlsPlayerAction.SetVideoSurface(videoSurface))
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
                },
                isAutoPlayVideo = isAutoPlayVideo,
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
                    HlsPlayerUtils.dispatch(HlsPlayerAction.SetVideoSurface(playerView.videoSurfaceView))
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
                HlsPlayerUtils.dispatch(HlsPlayerAction.SetVideoSurface(null))
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
                HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
                Log.d("VideoPlayerSection", "Paused playback before disposal")
                HlsPlayerUtils.getPlayer()?.removeListener(playerListener)

                mediaPlaybackService?.dispatch(MediaPlaybackAction.QueryForegroundStatus)
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

                playerView.player = null
                HlsPlayerUtils.dispatch(HlsPlayerAction.SetVideoSurface(null))
            } catch (e: IllegalArgumentException) {
                Log.w("VideoPlayerSection", "Service already unbound", e)
            }
        }
    }

    LaunchedEffect(episodeSourcesQuery) {
        Log.d("VideoPlayerSection", "episodeSourcesQuery changed: ${episodeSourcesQuery.id}")
        watchState.episodeDetailComplement.data?.let {
            HlsPlayerUtils.dispatch(
                HlsPlayerAction.SetMedia(
                    videoData = it.sources,
                    lastTimestamp = it.lastTimestamp,
                    isAutoPlayVideo = isAutoPlayVideo,
                    onReady = {
                        isLoading = false
                    },
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

    val mediaControllerCallback = remember {
        object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                state?.let {
                    val isPlaying = hlsPlayerState.isPlaying
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
            HlsPlayerUtils.dispatch(HlsPlayerAction.SetVideoSurface(null))
        }
    }

    LaunchedEffect(isScreenOn) {
        if (!isScreenOn) HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
    }

    watchState.episodeDetailComplement.data?.let {
        VideoPlayer(
            playerView = playerView,
            hlsPlayerState = hlsPlayerState,
            mediaController = mediaControllerCompat,
            episodeDetailComplement = it,
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
            videoSize = videoSize,
            onPlay = { mediaControllerCompat?.transportControls?.play() },
            onFastForward = { HlsPlayerUtils.dispatch(HlsPlayerAction.FastForward) },
            onRewind = { HlsPlayerUtils.dispatch(HlsPlayerAction.Rewind) },
            onSkipIntro = {
                it.sources.intro?.end?.let { endTime ->
                    HlsPlayerUtils.dispatch(HlsPlayerAction.SkipIntro(endTime))
                }
            },
            onSkipOutro = {
                it.sources.outro?.end?.let { endTime ->
                    HlsPlayerUtils.dispatch(HlsPlayerAction.SkipOutro(endTime))
                }
            }
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