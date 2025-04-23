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
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ExoPlaybackException
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Size
import coil.transform.Transformation
import com.example.animeapp.R
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.URLEncoder
import androidx.core.net.toUri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.core.graphics.scale

private const val NOTIFICATION_ID = 123
private const val CHANNEL_ID = "anime_playback_channel"
private const val IMAGE_SIZE = 512

class MediaPlaybackService : MediaBrowserServiceCompat() {
    private var mediaSession: MediaSessionCompat? = null
    private var exoPlayer: ExoPlayer? = null
    private var episodeDetailComplement: EpisodeDetailComplement? = null
    private var episodes: List<Episode> = emptyList()
    private var episodeSourcesQuery: EpisodeSourcesQuery? = null
    private var handleSelectedEpisodeServer: ((EpisodeSourcesQuery) -> Unit)? = null
    private var updateStoredWatchState: ((Long?) -> Unit)? = null
    private var onPlayerError: ((String?) -> Unit)? = null
    private var onPlayerReady: (() -> Unit)? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private val savePositionRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                val position = player.currentPosition
                val duration = player.duration
                if (player.playbackState == Player.STATE_READY && position > 0 && position < duration) {
                    coroutineScope.launch {
                        try {
                            withTimeout(5_000) { updateStoredWatchState?.invoke(position) }
                        } catch (e: Exception) {
                            Log.e("MediaPlaybackService", "Failed to save watch state", e)
                        }
                    }
                }
            }
            handler.postDelayed(this, 1_000)
        }
    }

    private val binder = MediaPlaybackBinder()

    inner class MediaPlaybackBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return if (intent?.action == "android.media.browse.MediaBrowserService") {
            super.onBind(intent)
        } else {
            binder
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        initializeMediaSession()
        createNotificationChannel()
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                @OptIn(UnstableApi::class)
                override fun onPlayerError(error: PlaybackException) {
                    val message = if (error is ExoPlaybackException && error.type == ExoPlaybackException.TYPE_SOURCE) {
                        "Playback error, try a different server."
                    } else {
                        "Playback error: ${error.message}"
                    }
                    onPlayerError?.invoke(message)
                    Log.e("MediaPlaybackService", "Player error", error)
                    updatePlaybackState(PlaybackStateCompat.STATE_ERROR, errorMessage = message)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        onPlayerError?.invoke(null)
                        handler.post(savePositionRunnable)
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    } else {
                        handler.removeCallbacks(savePositionRunnable)
                        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        coroutineScope.launch {
                            try {
                                val duration = exoPlayer?.duration
                                if (duration != null && duration > 0) {
                                    withTimeout(5_000) { updateStoredWatchState?.invoke(duration) }
                                }
                            } catch (e: Exception) {
                                Log.e("MediaPlaybackService", "Failed to save watch state at end", e)
                            }
                        }
                        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                        val currentEpisodeNo = episodeDetailComplement?.servers?.episodeNo ?: return
                        val nextEpisode = episodes.find { it.episodeNo == currentEpisodeNo + 1 }
                        if (nextEpisode != null) {
                            episodeSourcesQuery = episodeSourcesQuery?.copy(id = nextEpisode.episodeId)
                            episodeDetailComplement = episodeDetailComplement?.copy(id = nextEpisode.episodeId)
                            handleSelectedEpisodeServer?.invoke(episodeSourcesQuery!!)
                        }
                    } else if (playbackState == Player.STATE_READY && !isPlaying) {
                        onPlayerError?.invoke(null)
                        onPlayerReady?.invoke()
                        updateNotification()
                    }
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    val duration = exoPlayer?.duration?.takeIf { it > 0 } ?: 0
                    episodeDetailComplement?.let {
                        mediaSession?.setMetadata(
                            MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Eps. ${it.number}, ${it.episodeTitle}")
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, it.animeTitle)
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                                .build()
                        )
                    }
                    updateNotification()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateNotification()
                }
            })
            // Ensure player is paused initially
            pause()
        }
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "MediaPlaybackService").apply {
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackStateCompat.ACTION_STOP or
                                PlaybackStateCompat.ACTION_SEEK_TO
                    )
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f)
                    .build()
            )
            setCallback(MediaSessionCallback())
            isActive = true
        }
        sessionToken = mediaSession?.sessionToken
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
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
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
        val player = exoPlayer ?: return
        val playbackState = mediaSession?.controller?.playbackState ?: return
        val mediaMetadata = mediaSession?.controller?.metadata
        val duration = player.duration.takeIf { it > 0 }?.toInt() ?: 0
        val position = player.currentPosition.takeIf { it >= 0 }?.toInt() ?: 0

        val currentEpisodeNo = episodeDetailComplement?.servers?.episodeNo ?: -1
        val hasPreviousEpisode = currentEpisodeNo > 1 && episodes.any { it.episodeNo == currentEpisodeNo - 1 }
        val hasNextEpisode = episodes.any { it.episodeNo == currentEpisodeNo + 1 }

        coroutineScope.launch {
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
                setContentTitle(mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "Anime Episode")
                setContentText(mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM) ?: "Anime Series")
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setOnlyAlertOnce(true)
                setProgress(duration, position, false)
                imageBitmap?.let { setLargeIcon(it) }

                if (hasPreviousEpisode) {
                    addAction(
                        NotificationCompat.Action(
                            androidx.media3.session.R.drawable.media3_icon_previous,
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
                        if (playbackState.state == PlaybackStateCompat.STATE_PLAYING)
                            androidx.media3.session.R.drawable.media3_icon_pause
                        else
                            androidx.media3.session.R.drawable.media3_icon_play,
                        "Play/Pause",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this@MediaPlaybackService,
                            PlaybackStateCompat.ACTION_PLAY_PAUSE
                        )
                    )
                )

                if (hasNextEpisode) {
                    addAction(
                        NotificationCompat.Action(
                            androidx.media3.session.R.drawable.media3_icon_next,
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
            startForeground(NOTIFICATION_ID, builder.build())
        }
    }

    private fun updatePlaybackState(state: Int, errorMessage: String? = null) {
        val position = exoPlayer?.currentPosition?.takeIf { it >= 0 } ?: 0
        val builder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
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
            exoPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()
                player.pause()

                if (query.id != episodeDetailComplement?.id) {
                    Log.w("MediaPlaybackService", "Mismatch in episodeSourcesQuery.id (${query.id}) and episodeDetailComplement.id (${episodeDetailComplement?.id})")
                    onPlayerError.invoke("Episode data mismatch")
                    return@launch
                }

                val readyListener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            onPlayerReady()
                            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                            player.removeListener(this)
                        } else if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                            onPlayerError("Player failed to initialize")
                            player.removeListener(this)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        onPlayerError("Player error during initialization: ${error.message}")
                        player.removeListener(this)
                    }
                }

                player.addListener(readyListener)

                try {
                    HlsPlayerUtil.initializePlayer(player, complement.sources)
                    if (player.playbackState == Player.STATE_READY) {
                        onPlayerReady()
                        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        player.removeListener(readyListener)
                    }
                    complement.lastTimestamp?.let { timestamp ->
                        if (timestamp > 0 && timestamp < player.duration) {
                            player.seekTo(timestamp)
                        }
                    }
                } catch (e: Exception) {
                    onPlayerError("Failed to initialize player: ${e.message}")
                    player.removeListener(readyListener)
                }
            } ?: run {
                onPlayerError("ExoPlayer is null")
            }
        }
    }

    fun getExoPlayer(): ExoPlayer? = exoPlayer

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.let { player ->
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
            player.release()
        }
        exoPlayer = null
        mediaSession?.release()
        mediaSession = null
        handler.removeCallbacks(savePositionRunnable)
        coroutineScope.cancel()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot("media_root_id", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem?>?>) {
        result.sendResult(mutableListOf())
    }

    inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            exoPlayer?.play()
        }

        override fun onPause() {
            exoPlayer?.pause()
        }

        override fun onSkipToNext() {
            val currentEpisodeNo = episodeDetailComplement?.servers?.episodeNo ?: return
            val nextEpisode = episodes.find { it.episodeNo == currentEpisodeNo + 1 }
            if (nextEpisode != null) {
                episodeSourcesQuery = episodeSourcesQuery?.copy(id = nextEpisode.episodeId)
                episodeDetailComplement = episodeDetailComplement?.copy(id = nextEpisode.episodeId)
                handleSelectedEpisodeServer?.invoke(episodeSourcesQuery!!)
            }
        }

        override fun onSkipToPrevious() {
            val currentEpisodeNo = episodeDetailComplement?.servers?.episodeNo ?: return
            val previousEpisode = episodes.find { it.episodeNo == currentEpisodeNo - 1 }
            if (previousEpisode != null) {
                episodeSourcesQuery = episodeSourcesQuery?.copy(id = previousEpisode.episodeId)
                episodeDetailComplement = episodeDetailComplement?.copy(id = previousEpisode.episodeId)
                handleSelectedEpisodeServer?.invoke(episodeSourcesQuery!!)
            }
        }

        override fun onStop() {
            exoPlayer?.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            val duration = exoPlayer?.duration ?: Long.MAX_VALUE
            val clampedPos = pos.coerceAtLeast(0).coerceAtMost(duration)
            exoPlayer?.seekTo(clampedPos)
            if (clampedPos > 0 && clampedPos < duration) {
                updateStoredWatchState?.invoke(clampedPos)
            }
            updatePlaybackState(
                when (exoPlayer?.playbackState) {
                    Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
                    Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
                    Player.STATE_READY -> if (exoPlayer?.isPlaying == true) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                    else -> PlaybackStateCompat.STATE_NONE
                }
            )
        }
    }
}