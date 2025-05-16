package com.luminoverse.animevibe.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.luminoverse.animevibe.R
import androidx.media3.session.R as RMedia3
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicBoolean

private const val NOTIFICATION_ID = 123
private const val CHANNEL_ID = "anime_playback_channel"
private const val IMAGE_SIZE = 512
private const val WATCH_STATE_UPDATE_INTERVAL_MS = 5_000L
private const val NOTIFICATION_UPDATE_INTERVAL_MS = 1_000L

class MediaPlaybackService : MediaBrowserServiceCompat() {
    private var mediaSession: MediaSessionCompat? = null
    private var episodeDetailComplement: EpisodeDetailComplement? = null
    private var episodes: List<Episode> = emptyList()
    private var episodeSourcesQuery: EpisodeSourcesQuery? = null
    private var handleSelectedEpisodeServer: ((EpisodeSourcesQuery) -> Unit)? = null
    private var updateStoredWatchState: ((Long?, Long?, String?) -> Unit)? = null
    private var onPlayerError: ((String?) -> Unit)? = null
    private var onPlayerReady: (() -> Unit)? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private val isForeground = AtomicBoolean(false)
    private var watchStateUpdateJob: Job? = null
    private var notificationUpdateJob: Job? = null

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
        HlsPlayerUtils.dispatch(HlsPlayerAction.InitializeHlsPlayer(this))
        initializeMediaSession()
        createNotificationChannel()
        observePlayerState()
        setupPlayerListener()
        startPeriodicNotificationUpdates()
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
                .size(IMAGE_SIZE, IMAGE_SIZE)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request)
            (result as? SuccessResult)?.drawable?.let { it as? android.graphics.drawable.BitmapDrawable }?.bitmap
        } catch (e: Exception) {
            Log.e("MediaPlaybackService", "Failed to load image: $url", e)
            null
        }
    }

    private suspend fun captureScreenshot(): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = HlsPlayerUtils.captureFrame() ?: return@withContext null
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            bitmap.recycle()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("MediaPlaybackService", "Failed to capture screenshot", e)
            null
        }
    }

    private fun updateNotification() {
        coroutineScope.launch {
            val state = HlsPlayerUtils.state.value
            if (!state.isReady) {
                Log.d("MediaPlaybackService", "Skipping notification update: player not ready")
                return@launch
            }

            val player = HlsPlayerUtils.getPlayer() ?: return@launch
            val mediaMetadata = mediaSession?.controller?.metadata
            val duration = player.duration.takeIf { it > 0 }?.toInt() ?: 0
            val position = player.currentPosition.takeIf { it >= 0 }?.toInt() ?: 0

            val currentEpisodeNo = episodeDetailComplement?.servers?.episodeNo ?: -1
            val hasPreviousEpisode =
                currentEpisodeNo > 1 && episodes.any { it.episodeNo == currentEpisodeNo - 1 }
            val hasNextEpisode = episodes.any { it.episodeNo == currentEpisodeNo + 1 }

            val imageBitmap = loadImageBitmap(episodeDetailComplement?.imageUrl)
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
                        if (state.isPlaying)
                            RMedia3.drawable.media3_icon_pause
                        else
                            RMedia3.drawable.media3_icon_play,
                        if (state.isPlaying) "Pause" else "Play",
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

                val malId = episodeDetailComplement?.malId ?: return@apply
                val episodeId = episodeDetailComplement?.id ?: return@apply
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
            try {
                startForeground(NOTIFICATION_ID, builder.build())
                isForeground.set(true)
                Log.d(
                    "MediaPlaybackService",
                    "Foreground service started, isForeground=${isForeground.get()}"
                )
            } catch (e: Exception) {
                Log.e("MediaPlaybackService", "Failed to start foreground service", e)
            }
        }
    }

    private fun updateMediaMetadata(duration: Long) {
        episodeDetailComplement?.let { complement ->
            mediaSession?.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_TITLE,
                        "Eps. ${complement.number}, ${complement.episodeTitle}"
                    )
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, complement.animeTitle)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                    .build()
            )
        }
    }

    private fun setupPlayerListener() {
        HlsPlayerUtils.getPlayer()?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    onPlayerError?.invoke(null)
                    startPeriodicWatchStateUpdates()
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                } else {
                    stopPeriodicWatchStateUpdates()
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                updateMediaMetadata(HlsPlayerUtils.getPlayer()?.duration?.takeIf { it > 0 } ?: 0)
                updateNotification()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                HlsPlayerUtils.dispatch(HlsPlayerAction.SeekTo(0))
                updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING)
                updateNotification()
            }
        })
    }

    private fun startPeriodicNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = coroutineScope.launch {
            while (true) {
                val state = HlsPlayerUtils.state.value
                if (state.isReady) {
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

    private fun updatePlaybackState(state: Int, errorMessage: String? = null) {
        val position = HlsPlayerUtils.getPlayer()?.currentPosition?.takeIf { it >= 0 } ?: 0
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
        if (state == PlaybackStateCompat.STATE_ERROR && errorMessage != null) {
            builder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR, errorMessage)
        }
        builder.setState(state, position, 1.0f)
        mediaSession?.setPlaybackState(builder.build())
        updateNotification()
    }

    private fun startPeriodicWatchStateUpdates() {
        watchStateUpdateJob?.cancel()
        watchStateUpdateJob = coroutineScope.launch {
            while (true) {
                val state = HlsPlayerUtils.state.value
                if (state.isPlaying) {
                    HlsPlayerUtils.getPlayer()?.let { player ->
                        val position = player.currentPosition
                        val duration = player.duration
                        if (position > 10_000 && position < duration) {
                            try {
                                withTimeout(5_000) {
                                    val screenshot = captureScreenshot()
                                    updateStoredWatchState?.invoke(position, duration, screenshot)
                                    Log.d(
                                        "MediaPlaybackService",
                                        "Periodic watch state update: position=$position, " +
                                                "screenshot=${screenshot?.take(20)}..."
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "MediaPlaybackService",
                                    "Failed to save periodic watch state",
                                    e
                                )
                            }
                        }
                    }
                }
                delay(WATCH_STATE_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopPeriodicWatchStateUpdates() {
        watchStateUpdateJob?.cancel()
        watchStateUpdateJob = null
        Log.d("MediaPlaybackService", "Stopped periodic watch state updates")
    }

    private fun observePlayerState() {
        coroutineScope.launch {
            HlsPlayerUtils.state.collectLatest { state ->
                Log.d("MediaPlaybackService", "State changed: $state")
                if (state.error != null) {
                    updatePlaybackState(PlaybackStateCompat.STATE_ERROR, state.error)
                    onPlayerError?.invoke(state.error)
                }
                if (state.isReady) {
                    onPlayerReady?.invoke()
                }
            }
        }
    }

    fun setEpisodeData(
        complement: EpisodeDetailComplement,
        episodes: List<Episode>,
        query: EpisodeSourcesQuery,
        handler: (EpisodeSourcesQuery) -> Unit,
        updateStoredWatchState: (Long?, Long?, String?) -> Unit,
        onPlayerError: (String?) -> Unit,
        onPlayerReady: () -> Unit
    ) {
        this.episodeDetailComplement = complement
        this.episodes = episodes
        this.episodeSourcesQuery = query
        this.handleSelectedEpisodeServer = handler
        this.updateStoredWatchState = updateStoredWatchState
        this.onPlayerError = onPlayerError
        this.onPlayerReady = onPlayerReady

        coroutineScope.launch {
            if (query.id != complement.id) {
                Log.w(
                    "MediaPlaybackService",
                    "Mismatch in episodeSourcesQuery.id (${query.id}) and episodeDetailComplement.id (${complement.id})"
                )
                onPlayerError("Episode data mismatch")
                return@launch
            }

            HlsPlayerUtils.dispatch(
                HlsPlayerAction.SetMedia(
                    videoData = complement.sources,
                    lastTimestamp = complement.lastTimestamp,
                    onReady = { onPlayerReady() },
                    onError = { onPlayerError(it) }
                )
            )

            updateMediaMetadata(HlsPlayerUtils.getPlayer()?.duration?.takeIf { it > 0 } ?: 0)
            updateNotification()
        }
    }

    fun pausePlayer() {
        Log.d("MediaPlaybackService", "pausePlayer called")
        HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
        updateNotification()
        stopPeriodicWatchStateUpdates()
    }

    fun isForegroundService(): Boolean {
        val isForegroundValue = isForeground.get() && HlsPlayerUtils.state.value.isReady
        Log.d(
            "MediaPlaybackService",
            "isForegroundService called, returning $isForegroundValue (isForeground=${isForeground.get()}, isReady=${HlsPlayerUtils.state.value.isReady})"
        )
        return isForegroundValue
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("MediaPlaybackService", "Task removed, stopping service")
        HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground.set(false)
        Log.d(
            "MediaPlaybackService",
            "Foreground stopped on task removed, isForeground=${isForeground.get()}"
        )
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MediaPlaybackService", "Service destroyed")
        HlsPlayerUtils.getPlayer()?.let { player ->
            val position = player.currentPosition
            val duration = player.duration
            if (player.playbackState == Player.STATE_READY && position > 0 && position < duration) {
                coroutineScope.launch {
                    try {
                        withTimeout(5_000) {
                            val screenshot = captureScreenshot()
                            updateStoredWatchState?.invoke(position, duration, screenshot)
                        }
                    } catch (e: Exception) {
                        Log.e("MediaPlaybackService", "Failed to save watch state on destroy", e)
                    }
                }
            }
        }
        HlsPlayerUtils.dispatch(HlsPlayerAction.Release)
        mediaSession?.release()
        mediaSession = null
        handler.removeCallbacksAndMessages(null)
        stopPeriodicWatchStateUpdates()
        stopPeriodicNotificationUpdates()
        coroutineScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground.set(false)
        Log.d("MediaPlaybackService", "Resources released, isForeground=${isForeground.get()}")
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
            HlsPlayerUtils.dispatch(HlsPlayerAction.Play)
            updateNotification()
            startPeriodicWatchStateUpdates()
        }

        override fun onPause() {
            HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
            updateNotification()
            stopPeriodicWatchStateUpdates()
        }

        override fun onRewind() {
            HlsPlayerUtils.dispatch(HlsPlayerAction.Rewind)
            updateNotification()
        }

        override fun onFastForward() {
            HlsPlayerUtils.dispatch(HlsPlayerAction.FastForward)
            updateNotification()
        }

        override fun onSkipToNext() {
            val currentEpisodeNo = episodeDetailComplement?.servers?.episodeNo ?: return
            val nextEpisode = episodes.find { it.episodeNo == currentEpisodeNo + 1 }
            if (nextEpisode != null) {
                episodeSourcesQuery = episodeSourcesQuery?.copy(id = nextEpisode.episodeId)
                episodeDetailComplement = episodeDetailComplement?.copy(id = nextEpisode.episodeId)
                handleSelectedEpisodeServer?.invoke(episodeSourcesQuery!!)
            }
            updateNotification()
        }

        override fun onSkipToPrevious() {
            val currentEpisodeNo = episodeDetailComplement?.servers?.episodeNo ?: return
            val previousEpisode = episodes.find { it.episodeNo == currentEpisodeNo - 1 }
            if (previousEpisode != null) {
                episodeSourcesQuery = episodeSourcesQuery?.copy(id = previousEpisode.episodeId)
                episodeDetailComplement =
                    episodeDetailComplement?.copy(id = previousEpisode.episodeId)
                handleSelectedEpisodeServer?.invoke(episodeSourcesQuery!!)
            }
            updateNotification()
        }

        override fun onStop() {
            HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground.set(false)
            stopPeriodicWatchStateUpdates()
            Log.d(
                "MediaPlaybackService",
                "Foreground stopped on stop, isForeground=${isForeground.get()}"
            )
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            HlsPlayerUtils.dispatch(HlsPlayerAction.SeekTo(pos))
            val duration = HlsPlayerUtils.getPlayer()?.duration
            if (pos > 0 && pos < (duration ?: Long.MAX_VALUE)) {
                coroutineScope.launch {
                    val screenshot = captureScreenshot()
                    updateStoredWatchState?.invoke(pos, duration, screenshot)
                }
            }
            updateNotification()
        }

        override fun onSetPlaybackSpeed(speed: Float) {
            HlsPlayerUtils.dispatch(HlsPlayerAction.SetPlaybackSpeed(speed))
            updateNotification()
        }
    }

    fun stopService() {
        Log.d("MediaPlaybackService", "stopService called")
        HlsPlayerUtils.dispatch(HlsPlayerAction.Pause)
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground.set(false)
        stopPeriodicWatchStateUpdates()
        stopPeriodicNotificationUpdates()
        Log.d(
            "MediaPlaybackService",
            "Foreground stopped in stopService, isForeground=${isForeground.get()}"
        )
        stopSelf()
    }
}