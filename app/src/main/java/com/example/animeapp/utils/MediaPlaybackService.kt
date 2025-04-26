package com.example.animeapp.utils

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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.Player
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Size
import coil.transform.Transformation
import com.example.animeapp.R
import androidx.media3.session.R as RMedia3
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicBoolean

private const val NOTIFICATION_ID = 123
private const val CHANNEL_ID = "anime_playback_channel"
private const val IMAGE_SIZE = 512

class MediaPlaybackService : MediaBrowserServiceCompat() {
    private var mediaSession: MediaSessionCompat? = null
    private var episodeDetailComplement: EpisodeDetailComplement? = null
    private var episodes: List<Episode> = emptyList()
    private var episodeSourcesQuery: EpisodeSourcesQuery? = null
    private var handleSelectedEpisodeServer: ((EpisodeSourcesQuery) -> Unit)? = null
    private var updateStoredWatchState: ((Long?) -> Unit)? = null
    private var onPlayerError: ((String?) -> Unit)? = null
    private var onPlayerReady: (() -> Unit)? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private val isForeground = AtomicBoolean(false)

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
        HlsPlayerUtil.dispatch(PlayerAction.InitializePlayer(this))
        initializeMediaSession()
        createNotificationChannel()
        observePlayerState()
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
            "Anime Video Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Channel for anime video playback controls"
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )
    }

    private class TopCenterCropTransformation : Transformation {
        override val cacheKey: String = "TopCenterCropTransformation"
        override suspend fun transform(input: Bitmap, size: Size): Bitmap {
            val targetWidth = IMAGE_SIZE
            val targetHeight = IMAGE_SIZE
            val scale: Float
            val srcWidth: Int
            val srcHeight: Int
            if (input.width >= input.height) {
                scale = targetHeight.toFloat() / input.height
                srcWidth = (targetWidth / scale).toInt()
                srcHeight = input.height
            } else {
                scale = targetWidth.toFloat() / input.width
                srcWidth = input.width
                srcHeight = (targetHeight / scale).toInt()
            }
            val srcX = (input.width - srcWidth) / 2
            val srcY = 0
            return Bitmap.createBitmap(input, srcX, srcY, srcWidth, srcHeight).let { cropped ->
                val scaled = cropped.scale(targetWidth, targetHeight)
                if (cropped != scaled) cropped.recycle()
                scaled
            }
        }
    }

    private suspend fun loadImageBitmap(url: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrEmpty()) return@withContext null
        try {
            val imageLoader = ImageLoader.Builder(this@MediaPlaybackService)
                .memoryCache {
                    coil.memory.MemoryCache.Builder(this@MediaPlaybackService).maxSizePercent(0.25)
                        .build()
                }
                .build()
            val request = ImageRequest.Builder(this@MediaPlaybackService)
                .data(url)
                .size(IMAGE_SIZE, IMAGE_SIZE)
                .precision(Precision.EXACT)
                .allowHardware(false)
                .allowRgb565(false)
                .transformations(TopCenterCropTransformation())
                .build()
            val result = imageLoader.execute(request)
            (result as? SuccessResult)?.drawable?.let { it as? android.graphics.drawable.BitmapDrawable }?.bitmap
        } catch (e: Exception) {
            Log.e("MediaPlaybackService", "Failed to load image: $url", e)
            null
        }
    }

    private fun updateNotification() {
        coroutineScope.launch {
            val state = HlsPlayerUtil.state.value
            if (!state.isReady) {
                Log.d("MediaPlaybackService", "Skipping notification update: player not ready")
                return@launch
            }

            val player = HlsPlayerUtil.getPlayer() ?: return@launch
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
                setProgress(duration, position, false)
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
                val deepLinkUri = "animeapp://anime/watch/$encodedMalId/$encodedEpisodeId"
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

    private fun updatePlaybackState(state: Int, errorMessage: String? = null) {
        val position = HlsPlayerUtil.getPlayer()?.currentPosition?.takeIf { it >= 0 } ?: 0
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
        updateNotification() // Always update notification to reflect current state
    }

    private fun observePlayerState() {
        coroutineScope.launch {
            HlsPlayerUtil.state.collectLatest { state ->
                Log.d("MediaPlaybackService", "State changed: $state")
                updatePlaybackState(
                    when {
                        state.error != null -> PlaybackStateCompat.STATE_ERROR
                        state.isPlaying -> PlaybackStateCompat.STATE_PLAYING
                        else -> PlaybackStateCompat.STATE_PAUSED
                    },
                    state.error
                )
                when (state.playbackState) {
                    Player.STATE_ENDED -> {
                        coroutineScope.launch {
                            try {
                                val duration = HlsPlayerUtil.getPlayer()?.duration
                                if (duration != null && duration > 0) {
                                    withTimeout(5_000) { updateStoredWatchState?.invoke(duration) }
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "MediaPlaybackService",
                                    "Failed to save watch state at end",
                                    e
                                )
                            }
                        }
                        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                        val currentEpisodeNo =
                            episodeDetailComplement?.servers?.episodeNo ?: return@collectLatest
                        val nextEpisode = episodes.find { it.episodeNo == currentEpisodeNo + 1 }
                        if (nextEpisode != null) {
                            episodeSourcesQuery =
                                episodeSourcesQuery?.copy(id = nextEpisode.episodeId)
                            episodeDetailComplement =
                                episodeDetailComplement?.copy(id = nextEpisode.episodeId)
                            handleSelectedEpisodeServer?.invoke(episodeSourcesQuery!!)
                        }
                    }

                    Player.STATE_READY -> {
                        if (!state.isPlaying) {
                            onPlayerError?.invoke(null)
                            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        }
                        updateNotification() // Ensure notification is shown when ready
                    }

                    Player.STATE_BUFFERING -> {
                        updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING)
                    }

                    Player.STATE_IDLE -> {
                        onPlayerError?.invoke(null)
                        updatePlaybackState(PlaybackStateCompat.STATE_NONE)
                    }
                }
                if (state.error != null) {
                    onPlayerError?.invoke(state.error)
                    updatePlaybackState(PlaybackStateCompat.STATE_ERROR, state.error)
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
        updateStoredWatchState: (Long?) -> Unit,
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

            HlsPlayerUtil.dispatch(
                PlayerAction.SetMedia(
                    videoData = complement.sources,
                    lastTimestamp = complement.lastTimestamp,
                    onReady = { onPlayerReady() },
                    onError = { onPlayerError(it) }
                )
            )

            episodeDetailComplement?.let {
                val duration = HlsPlayerUtil.getPlayer()?.duration?.takeIf { it > 0 } ?: 0
                mediaSession?.setMetadata(
                    MediaMetadataCompat.Builder()
                        .putString(
                            MediaMetadataCompat.METADATA_KEY_TITLE,
                            "Eps. ${it.number}, ${it.episodeTitle}"
                        )
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, it.animeTitle)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                        .build()
                )
            }
            updateNotification()
        }
    }

    fun isForegroundService(): Boolean {
        val isForegroundValue = isForeground.get() && HlsPlayerUtil.state.value.isReady
        Log.d(
            "MediaPlaybackService",
            "isForegroundService called, returning $isForegroundValue (isForeground=${isForeground.get()}, isReady=${HlsPlayerUtil.state.value.isReady})"
        )
        return isForegroundValue
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("MediaPlaybackService", "Task removed, stopping service")
        HlsPlayerUtil.dispatch(PlayerAction.Pause)
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
        HlsPlayerUtil.getPlayer()?.let { player ->
            val position = player.currentPosition
            val duration = player.duration
            if (player.playbackState == Player.STATE_READY && position > 0 && position < duration) {
                coroutineScope.launch {
                    try {
                        withTimeout(5_000) { updateStoredWatchState?.invoke(position) }
                    } catch (e: Exception) {
                        Log.e("MediaPlaybackService", "Failed to save watch state on destroy", e)
                    }
                }
            }
        }
        HlsPlayerUtil.dispatch(PlayerAction.Release)
        mediaSession?.release()
        mediaSession = null
        handler.removeCallbacksAndMessages(null)
        coroutineScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE) // Ensure notification is removed
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
            HlsPlayerUtil.dispatch(PlayerAction.Play)
            updateNotification()
        }

        override fun onPause() {
            HlsPlayerUtil.dispatch(PlayerAction.Pause)
            updateNotification() // Update notification to show pause state, don't remove
        }

        override fun onRewind() {
            HlsPlayerUtil.dispatch(PlayerAction.Rewind)
            updateNotification()
        }

        override fun onFastForward() {
            HlsPlayerUtil.dispatch(PlayerAction.FastForward)
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
            HlsPlayerUtil.dispatch(PlayerAction.Pause)
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground.set(false)
            Log.d(
                "MediaPlaybackService",
                "Foreground stopped on stop, isForeground=${isForeground.get()}"
            )
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            HlsPlayerUtil.dispatch(PlayerAction.SeekTo(pos))
            if (pos > 0 && pos < (HlsPlayerUtil.getPlayer()?.duration ?: Long.MAX_VALUE)) {
                updateStoredWatchState?.invoke(pos)
            }
            updateNotification()
        }

        override fun onSetPlaybackSpeed(speed: Float) {
            HlsPlayerUtil.dispatch(PlayerAction.SetPlaybackSpeed(speed))
            updateNotification()
        }
    }

    fun stopService() {
        Log.d("MediaPlaybackService", "stopService called")
        HlsPlayerUtil.dispatch(PlayerAction.Pause)
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground.set(false)
        Log.d(
            "MediaPlaybackService",
            "Foreground stopped in stopService, isForeground=${isForeground.get()}"
        )
        stopSelf()
    }
}