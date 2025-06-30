package com.luminoverse.animevibe.utils.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.Player
import androidx.media3.session.R as RMedia3
import com.luminoverse.animevibe.android.R
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.main.MainActivity
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import javax.inject.Inject
import androidx.core.graphics.scale
import kotlinx.coroutines.isActive

private const val NOTIFICATION_ID = 123
private const val CHANNEL_ID = "anime_playback_channel"
private const val NOTIFICATION_UPDATE_INTERVAL_MS = 1_000L

data class MediaPlaybackState(
    val playbackState: Int = PlaybackStateCompat.STATE_STOPPED,
    val errorMessage: String? = null,
    val isPlayerReady: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val episodeComplement: EpisodeDetailComplement? = null,
    val episodes: List<Episode> = emptyList(),
    val episodeQuery: EpisodeSourcesQuery? = null
)

sealed class MediaPlaybackAction {
    data class SetEpisodeData(
        val complement: EpisodeDetailComplement,
        val episodes: List<Episode>,
        val query: EpisodeSourcesQuery,
        val handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit
    ) : MediaPlaybackAction()

    data object ClearMediaData : MediaPlaybackAction()
    data object StopService : MediaPlaybackAction()
}

@AndroidEntryPoint
class MediaPlaybackService : MediaBrowserServiceCompat() {
    @Inject
    lateinit var hlsPlayerUtils: HlsPlayerUtils

    private var mediaSession: MediaSessionCompat? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var notificationUpdateJob: Job? = null
    private var handleSelectedEpisodeServer: ((EpisodeSourcesQuery) -> Unit)? = null
    private var playerListener: Player.Listener? = null

    private val _state = MutableStateFlow(MediaPlaybackState())
    val state: StateFlow<MediaPlaybackState> = _state.asStateFlow()

    private val binder = MediaPlaybackBinder()

    inner class MediaPlaybackBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("MediaPlaybackService", "onBind called with action: ${intent?.action}")
        return if (intent?.action == "android.media.browse.MediaBrowserService") {
            super.onBind(intent)
        } else {
            binder
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MediaPlaybackService", "Service created")
        initializeService()
    }

    fun dispatch(action: MediaPlaybackAction) {
        when (action) {
            is MediaPlaybackAction.SetEpisodeData -> setEpisodeData(
                action.complement,
                action.episodes,
                action.query,
                action.handleSelectedEpisodeServer
            )

            is MediaPlaybackAction.ClearMediaData -> clearMediaData()
            is MediaPlaybackAction.StopService -> stopService()
        }
    }

    private fun initializeService() {
        initializeMediaSession()
        createNotificationChannel()
        observePlayerUtilsState()
        startPeriodicNotificationUpdates()
        _state.update { it.copy(playbackState = PlaybackStateCompat.STATE_STOPPED) }
    }

    private fun initializeMediaSession() {
        if (mediaSession != null) {
            Log.w("MediaPlaybackService", "MediaSession already initialized, skipping")
            return
        }
        mediaSession = MediaSessionCompat(this, "MediaPlaybackService").apply {
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackStateCompat.ACTION_STOP or
                                PlaybackStateCompat.ACTION_SEEK_TO or
                                PlaybackStateCompat.ACTION_REWIND or
                                PlaybackStateCompat.ACTION_FAST_FORWARD
                    )
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f)
                    .build()
            )
            setCallback(MediaSessionCallback())
            isActive = true
        }
        sessionToken = mediaSession?.sessionToken
        Log.d("MediaPlaybackService", "MediaSession initialized")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Video Playback",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel for video playback controls"
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )
    }

    private suspend fun loadImageBitmap(url: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrEmpty()) return@withContext null
        try {
            val imageLoader = ImageLoader.Builder(this@MediaPlaybackService)
                .memoryCache {
                    MemoryCache.Builder(this@MediaPlaybackService).maxSizePercent(0.25).build()
                }
                .build()
            val request = ImageRequest.Builder(this@MediaPlaybackService)
                .data(url)
                .size(1024)
                .build()
            val result = imageLoader.execute(request)
            val originalBitmap =
                (result as? SuccessResult)?.drawable?.let { it as? BitmapDrawable }?.bitmap

            originalBitmap?.let { bitmap ->
                val width = bitmap.width
                val height = bitmap.height

                val squareSize = width.coerceAtMost(height)

                val left = (width - squareSize) / 2
                val top = (height - squareSize) / 2

                val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, squareSize, squareSize)

                val finalNotificationSize = 512
                val scaledBitmap = croppedBitmap.scale(finalNotificationSize, finalNotificationSize)

                if (croppedBitmap != bitmap) croppedBitmap.recycle()
                if (scaledBitmap != croppedBitmap) croppedBitmap.recycle()

                scaledBitmap
            }
        } catch (e: Exception) {
            Log.e(
                "MediaPlaybackService",
                "Failed to load and process image for notification: $url",
                e
            )
            null
        }
    }

    private fun updateNotification() {
        coroutineScope.launch {
            val player = hlsPlayerUtils.getPlayer() ?: return@launch
            if (player.playbackState != Player.STATE_READY) {
                Log.d("MediaPlaybackService", "Skipping notification update: player not ready")
                return@launch
            }

            val mediaMetadata = mediaSession?.controller?.metadata
            val duration = player.duration.takeIf { it > 0 }?.toInt() ?: 0
            val position = player.currentPosition.takeIf { it >= 0 }?.toInt() ?: 0

            val currentEpisodeNo = _state.value.episodeComplement?.number ?: -1
            val hasPreviousEpisode =
                currentEpisodeNo > 1 && _state.value.episodes.any { it.episode_no == currentEpisodeNo - 1 }
            val hasNextEpisode = _state.value.episodes.any { it.episode_no == currentEpisodeNo + 1 }

            val imageBitmap = loadImageBitmap(_state.value.episodeComplement?.imageUrl)
            val builder = NotificationCompat.Builder(this@MediaPlaybackService, CHANNEL_ID).apply {
                val actionIndices = mutableListOf<Int>()
                if (hasPreviousEpisode) actionIndices.add(0)
                actionIndices.add(if (hasPreviousEpisode) 1 else 0)
                if (hasNextEpisode) actionIndices.add(if (hasPreviousEpisode) 2 else 1)
                setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(sessionToken)
                        .setShowActionsInCompactView(*actionIndices.toIntArray())
                )
                setSmallIcon(R.drawable.ic_video_black_24dp)
                setContentTitle(
                    mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                        ?: "Anime Episode"
                )
                setContentText(
                    mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
                        ?: "Anime Series"
                )
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setOnlyAlertOnce(true)
                setProgress(duration, position, duration <= 0)
                imageBitmap?.let { setLargeIcon(it) }

                if (hasPreviousEpisode) {
                    addAction(
                        NotificationCompat.Action(
                            RMedia3.drawable.media3_icon_previous,
                            "Previous",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this@MediaPlaybackService,
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            )
                        )
                    )
                }

                addAction(
                    NotificationCompat.Action(
                        if (player.isPlaying && player.playbackState == Player.STATE_READY) RMedia3.drawable.media3_icon_pause else RMedia3.drawable.media3_icon_play,
                        if (player.isPlaying && player.playbackState == Player.STATE_READY) "Pause" else "Play",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this@MediaPlaybackService,
                            PlaybackStateCompat.ACTION_PLAY_PAUSE
                        )
                    )
                )

                if (hasNextEpisode) {
                    addAction(
                        NotificationCompat.Action(
                            RMedia3.drawable.media3_icon_next,
                            "Next",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this@MediaPlaybackService,
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            )
                        )
                    )
                }

                val malId = _state.value.episodeComplement?.malId ?: return@apply
                val episodeId = _state.value.episodeComplement?.id ?: return@apply
                val encodedMalId = URLEncoder.encode(malId.toString(), "UTF-8")
                val encodedEpisodeId = URLEncoder.encode(episodeId, "UTF-8")
                val deepLinkUri = "animevibe://anime/watch/$encodedMalId/$encodedEpisodeId"
                val openAppIntent = Intent(Intent.ACTION_VIEW, deepLinkUri.toUri()).apply {
                    setClass(this@MediaPlaybackService, MainActivity::class.java)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                setContentIntent(
                    PendingIntent.getActivity(
                        this@MediaPlaybackService,
                        0,
                        openAppIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.areNotificationsEnabled()) {
                Log.d(
                    "MediaPlaybackService",
                    "Notifications disabled, cannot start foreground service"
                )
                stopSelf()
                return@launch
            }
            try {
                startForeground(NOTIFICATION_ID, builder.build())
            } catch (e: Exception) {
                Log.e("MediaPlaybackService", "Failed to start foreground service", e)
            }
        }
    }

    private fun updateMediaMetadata(duration: Long) {
        _state.value.episodeComplement?.let { complement ->
            mediaSession?.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_TITLE,
                        complement.episodeTitle
                    )
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, complement.animeTitle)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                    .build()
            )
        }
    }

    private fun startPeriodicNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = coroutineScope.launch {
            while (true) {
                if (_state.value.isPlayerReady && _state.value.playbackState == PlaybackStateCompat.STATE_PLAYING) {
                    updateNotification()
                    Log.d("MediaPlaybackService", "Periodic notification update triggered")
                }
                delay(NOTIFICATION_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopPeriodicNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = null
        Log.d("MediaPlaybackService", "Stopped periodic notification updates")
    }

    private fun updatePlaybackState(playbackState: Int, errorMessage: String? = null) {
        val position = hlsPlayerUtils.getPlayer()?.currentPosition?.takeIf { it >= 0 } ?: 0
        val builder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_REWIND or
                        PlaybackStateCompat.ACTION_FAST_FORWARD or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
        if (playbackState == PlaybackStateCompat.STATE_ERROR && errorMessage != null) {
            builder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR, errorMessage)
        }
        builder.setState(playbackState, position, 1.0f)
        mediaSession?.setPlaybackState(builder.build())
        _state.update { state ->
            state.copy(
                playbackState = playbackState,
                errorMessage = errorMessage,
                currentPosition = position,
                duration = hlsPlayerUtils.getPlayer()?.duration?.takeIf { it > 0 } ?: 0
            )
        }
        updateNotification()
    }

    private fun observePlayerUtilsState() {
        val player = hlsPlayerUtils.getPlayer() ?: return
        var positionUpdateJob: Job? = null

        coroutineScope.launch {
            hlsPlayerUtils.playerCoreState.collectLatest { coreState ->
                val playbackStateCompat = when (coreState.playbackState) {
                    Player.STATE_IDLE -> PlaybackStateCompat.STATE_STOPPED
                    Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
                    Player.STATE_READY -> if (coreState.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                    Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
                    else -> PlaybackStateCompat.STATE_NONE
                }
                updatePlaybackState(playbackStateCompat, coreState.error?.message)

                if (coreState.isPlaying && coreState.playbackState == Player.STATE_READY) {
                    if (positionUpdateJob?.isActive != true) {
                        positionUpdateJob = launch {
                            while (isActive) {
                                val currentPosition = player.currentPosition
                                val duration = if (player.duration > 0) player.duration else 0L

                                _state.update {
                                    it.copy(
                                        currentPosition = currentPosition,
                                        duration = duration
                                    )
                                }

                                if (duration > 0) {
                                    updateMediaMetadata(duration)
                                }

                                delay(500L)
                            }
                        }
                    }
                } else {
                    positionUpdateJob?.cancel()

                    _state.update {
                        it.copy(
                            currentPosition = player.currentPosition,
                            duration = if (player.duration > 0) player.duration else 0L
                        )
                    }
                }
            }
        }
    }

    private fun setEpisodeData(
        complement: EpisodeDetailComplement,
        episodes: List<Episode>,
        query: EpisodeSourcesQuery,
        handler: (EpisodeSourcesQuery) -> Unit,
    ) {
        handleSelectedEpisodeServer = handler
        _state.update {
            it.copy(
                episodeComplement = complement,
                episodes = episodes,
                episodeQuery = query
            )
        }

        coroutineScope.launch {
            updateMediaMetadata(hlsPlayerUtils.getPlayer()?.duration?.takeIf { it > 0 } ?: 0)
            updateNotification()
        }
    }

    private fun clearMediaData() {
        _state.update {
            it.copy(
                episodeComplement = null,
                episodes = emptyList(),
                episodeQuery = null
            )
        }
        mediaSession?.setMetadata(null)
        Log.d("MediaPlaybackService", "Media data cleared.")
    }

    private fun stopService() {
        Log.d("MediaPlaybackService", "stopService() called. Stopping foreground and self.")
        hlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopPeriodicNotificationUpdates()
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("MediaPlaybackService", "Task removed, stopping service")
        dispatch(MediaPlaybackAction.StopService)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MediaPlaybackService", "Service is being destroyed. Releasing all resources.")
        playerListener?.let { hlsPlayerUtils.getPlayer()?.removeListener(it) }
        playerListener = null
        mediaSession?.release()
        mediaSession = null
        handler.removeCallbacksAndMessages(null)
        stopPeriodicNotificationUpdates()
        coroutineScope.cancel()
        Log.d("MediaPlaybackService", "All resources released.")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("media_root_id", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem?>?>
    ) {
        result.sendResult(mutableListOf())
    }

    inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            hlsPlayerUtils.dispatch(HlsPlayerAction.Play)
        }

        override fun onPause() {
            hlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
        }

        override fun onRewind() {
            hlsPlayerUtils.dispatch(HlsPlayerAction.Rewind)
        }

        override fun onFastForward() {
            hlsPlayerUtils.dispatch(HlsPlayerAction.FastForward)
        }

        override fun onSkipToNext() {
            val currentEpisodeNo = _state.value.episodeComplement?.number ?: return
            val nextEpisode = _state.value.episodes.find { it.episode_no == currentEpisodeNo + 1 }
            if (nextEpisode != null) {
                val newQuery = _state.value.episodeQuery?.copy(id = nextEpisode.id)
                val newComplement = _state.value.episodeComplement?.copy(id = nextEpisode.id)
                if (newQuery != null && newComplement != null) {
                    _state.update {
                        it.copy(
                            episodeQuery = newQuery,
                            episodeComplement = newComplement
                        )
                    }
                    handleSelectedEpisodeServer?.invoke(newQuery)
                    Log.d(
                        "MediaPlaybackService",
                        "onSkipToNext: Handler invoked for episode ${nextEpisode.id}"
                    )
                    updateNotification()
                }
            }
        }

        override fun onSkipToPrevious() {
            val currentEpisodeNo = _state.value.episodeComplement?.number ?: return
            val previousEpisode =
                _state.value.episodes.find { it.episode_no == currentEpisodeNo - 1 }
            if (previousEpisode != null) {
                val newQuery = _state.value.episodeQuery?.copy(id = previousEpisode.id)
                val newComplement =
                    _state.value.episodeComplement?.copy(id = previousEpisode.id)
                if (newQuery != null && newComplement != null) {
                    _state.update {
                        it.copy(
                            episodeQuery = newQuery,
                            episodeComplement = newComplement
                        )
                    }
                    handleSelectedEpisodeServer?.invoke(newQuery)
                    Log.d(
                        "MediaPlaybackService",
                        "onSkipToPrevious: Handler invoked for episode ${previousEpisode.id}"
                    )
                    updateNotification()
                }
            }
        }

        override fun onStop() {
            dispatch(MediaPlaybackAction.StopService)
        }

        override fun onSeekTo(pos: Long) {
            hlsPlayerUtils.dispatch(HlsPlayerAction.SeekTo(pos))
        }

        override fun onSetPlaybackSpeed(speed: Float) {
            hlsPlayerUtils.dispatch(HlsPlayerAction.SetPlaybackSpeed(speed))
        }
    }
}