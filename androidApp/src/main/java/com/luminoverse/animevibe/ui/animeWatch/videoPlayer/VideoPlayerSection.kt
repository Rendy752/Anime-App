package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.main.PlayerDisplayMode
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.MediaPlaybackAction
import com.luminoverse.animevibe.utils.media.MediaPlaybackService
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.resource.Resource
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("ImplicitSamInstance")
@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSection(
    episodeDetailComplement: EpisodeDetailComplement,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    networkDataSource: NetworkDataSource,
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
    displayMode: PlayerDisplayMode,
    setPlayerDisplayMode: (PlayerDisplayMode) -> Unit,
    onEnterSystemPipMode: () -> Unit,
    isSideSheetVisible: Boolean,
    setSideSheetVisibility: (Boolean) -> Unit,
    setPlayerError: (String) -> Unit,
    rememberedTopPadding: Dp,
    verticalDragOffset: Float,
    onVerticalDrag: (Float) -> Unit,
    onDragEnd: (flingVelocity: Float) -> Unit,
    pipEndDestinationPx: Offset,
    pipEndSizePx: IntSize,
    onMaxDragAmountCalculated: (Float) -> Unit
) {
    val context = LocalContext.current

    val playerView = remember { PlayerView(context).apply { useController = false } }
    val player by remember { mutableStateOf(getPlayer()) }
    var mediaBrowser by remember { mutableStateOf<MediaBrowserCompat?>(null) }
    var mediaPlaybackService by remember { mutableStateOf<MediaPlaybackService?>(null) }

    fun setupPlayer() {
        if (player != null) {
            playerView.player = player
            val videoSurface = playerView.videoSurfaceView
            playerAction(HlsPlayerAction.SetVideoSurface(videoSurface))
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
            }
        }
    }

    LaunchedEffect(player) {
        if (player == null || player?.playbackState == Player.STATE_IDLE) {
            setupPlayer()
        }
    }

    LaunchedEffect(coreState.error) {
        coreState.error?.let { errorMessage ->
            setPlayerError(errorMessage)
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, MediaPlaybackService::class.java)
        try {
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d("VideoPlayerSection", "Binding to MediaPlaybackService.")
        } catch (e: Exception) {
            Log.e("VideoPlayerSection", "Failed to bind service", e)
            setPlayerError("Failed to bind service")
        }

        onDispose {
            mediaBrowser?.disconnect()


            Log.d("VideoPlayerSection", "Disposing completely. Resetting player state and service.")
            playerAction(HlsPlayerAction.Reset)
            mediaPlaybackService?.dispatch(MediaPlaybackAction.ClearMediaData)
            mediaPlaybackService?.dispatch(MediaPlaybackAction.StopService)

            try {
                context.unbindService(serviceConnection)
                Log.d("VideoPlayerSection", "Unbound from MediaPlaybackService.")
            } catch (e: IllegalArgumentException) {
                Log.w("VideoPlayerSection", "Service was not registered or already unbound.", e)
            }
            mediaPlaybackService = null
        }
    }

    LaunchedEffect(episodeDetailComplement.sources.link.file) {
        Log.d("VideoPlayerSection", "episodeSourcesQuery changed: ${episodeSourcesQuery.id}")
        playerAction(
            HlsPlayerAction.SetMedia(
                videoData = episodeDetailComplement.sources,
                isAutoPlayVideo = isAutoPlayVideo,
                currentPosition = episodeDetailComplement.lastTimestamp ?: 0,
                duration = episodeDetailComplement.duration ?: 0,
                onError = { error -> setPlayerError(error) }
            )
        )
        setupPlayer()
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
            networkDataSource = networkDataSource,
            updateStoredWatchState = updateStoredWatchState,
            captureScreenshot = captureScreenshot,
            coreState = coreState,
            controlsStateFlow = controlsStateFlow,
            playerAction = playerAction,
            onHandleBackPress = onHandleBackPress,
            episodeDetailComplement = episodeDetailComplement,
            episodeDetailComplements = episodeDetailComplements,
            episodes = episodes,
            episodeSourcesQuery = episodeSourcesQuery,
            handleSelectedEpisodeServer = handleSelectedEpisodeServer,
            displayMode = displayMode,
            setPlayerDisplayMode = setPlayerDisplayMode,
            onEnterSystemPipMode = onEnterSystemPipMode,
            isSideSheetVisible = isSideSheetVisible,
            setSideSheetVisibility = setSideSheetVisibility,
            isAutoplayEnabled = isAutoPlayVideo,
            isLandscape = isLandscape,
            rememberedTopPadding = rememberedTopPadding,
            verticalDragOffset = verticalDragOffset,
            onVerticalDrag = onVerticalDrag,
            onDragEnd = onDragEnd,
            pipEndDestinationPx = pipEndDestinationPx,
            pipEndSizePx = pipEndSizePx,
            onMaxDragAmountCalculated = onMaxDragAmountCalculated
        )
    }
}