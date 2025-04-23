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
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private val savePositionRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.currentPosition > 10_000) {
                    coroutineScope.launch {
                        try {
                            withTimeout(5_000) { updateStoredWatchState?.invoke(player.currentPosition) }
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
        Log.d("MediaPlaybackService", "onBind: action=${intent?.action}")
        return if (intent?.action == "android.media.browse.MediaBrowserService") {
            super.onBind(intent)
        } else {
            binder
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MediaPlaybackService", "onCreate")
        initializePlayer()
        initializeMediaSession()
        createNotificationChannel()
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                @OptIn(UnstableApi::class)
                override fun onPlayerError(error: PlaybackException) {
                    if (error is ExoPlaybackException && error.type == ExoPlaybackException.TYPE_SOURCE) {
                        onPlayerError?.invoke("Playback error, try a different server.")
                        Log.e("MediaPlaybackService", "Source error: ${error.message}")
                    } else {
                        onPlayerError?.invoke("Playback error: ${error.message}")
                        Log.e("MediaPlaybackService", "Player error: ${error.message}")
                    }
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
                    Log.d("MediaPlaybackService", "onPlaybackStateChanged: state=$playbackState")
                    if (playbackState == Player.STATE_ENDED) {
                        coroutineScope.launch {
                            try {
                                withTimeout(5_000) { updateStoredWatchState?.invoke(exoPlayer?.duration) }
                            } catch (e: Exception) {
                                Log.e(
                                    "MediaPlaybackService",
                                    "Failed to save watch state at end",
                                    e
                                )
                            }
                        }
                        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                        val currentEpisodeNo = episodeDetailComplement?.servers?.episodeNo ?: return
                        val nextEpisode = episodes.find { it.episodeNo == currentEpisodeNo + 1 }
                        if (nextEpisode != null) {
                            episodeSourcesQuery =
                                episodeSourcesQuery?.copy(id = nextEpisode.episodeId)
                            episodeDetailComplement =
                                episodeDetailComplement?.copy(id = nextEpisode.episodeId)
                            handleSelectedEpisodeServer?.invoke(episodeSourcesQuery!!)
                        }
                    } else if (playbackState == Player.STATE_READY && !isPlaying) {
                        onPlayerError?.invoke(null)
                        onPlayerReady?.invoke()
                        updateNotification()
                    }
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    val duration = duration.takeIf { it > 0 } ?: 0
                    Log.d("MediaPlaybackService", "onTimelineChanged: duration=$duration")
                    episodeDetailComplement?.let {
                        mediaSession?.setMetadata(
                            MediaMetadataCompat.Builder()
                                .putString(
                                    MediaMetadataCompat.METADATA_KEY_TITLE,
                                    "Eps. ${it.number}, ${it.episodeTitle}"
                                )
                                .putString(
                                    MediaMetadataCompat.METADATA_KEY_ALBUM,
                                    it.animeTitle
                                )
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                                .build()
                        )
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateNotification()
                }
            })
        }
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "MediaPlaybackService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
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
        Log.d("MediaPlaybackService", "MediaSession initialized")
    }

    private fun createNotificationChannel() {
        val name = "Anime Video Playback"
        val descriptionText = "Channel for anime video playback controls"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setShowBadge(false)
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d("MediaPlaybackService", "Notification channel created")
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
                    coil.memory.MemoryCache.Builder(this@MediaPlaybackService)
                        .maxSizePercent(0.25)
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
            if (result is SuccessResult) {
                (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MediaPlaybackService", "Failed to load image: $url", e)
            null
        }
    }

    private fun updateNotification() {
        val mediaMetadata = mediaSession?.controller?.metadata
        val playbackState = mediaSession?.controller?.playbackState ?: return
        val player = exoPlayer ?: return
        val duration = player.duration.takeIf { it > 0 }?.toInt() ?: 0
        val position = player.currentPosition.takeIf { it >= 0 }?.toInt() ?: 0

        coroutineScope.launch {
            val imageBitmap = loadImageBitmap(episodeDetailComplement?.imageUrl)

            val builder = NotificationCompat.Builder(this@MediaPlaybackService, CHANNEL_ID).apply {
                setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(sessionToken)
                        .setShowActionsInCompactView(0, 1, 2)
                )
                setSmallIcon(R.mipmap.ic_app_icon)
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

                val malId = episodeDetailComplement?.malId ?: return@apply
                val episodeId = episodeDetailComplement?.id ?: return@apply
                val encodedMalId = URLEncoder.encode(malId.toString(), "UTF-8")
                val encodedEpisodeId = URLEncoder.encode(episodeId, "UTF-8")
                val deepLinkUri = "animeapp://anime/watch/$encodedMalId/$encodedEpisodeId"
                val openAppIntent = Intent(Intent.ACTION_VIEW, deepLinkUri.toUri()).apply {
                    setClass(this@MediaPlaybackService, MainActivity::class.java)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                val pendingIntent = PendingIntent.getActivity(
                    this@MediaPlaybackService,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                setContentIntent(pendingIntent)
            }

            Log.d(
                "MediaPlaybackService",
                "Updating notification: duration=$duration, position=$position"
            )
            startForeground(NOTIFICATION_ID, builder.build())
        }
    }

    private fun updatePlaybackState(state: Int) {
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, exoPlayer?.currentPosition ?: 0, 1.0f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_STOP or
                            PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        )
        Log.d(
            "MediaPlaybackService",
            "Playback state updated: state=$state, position=${exoPlayer?.currentPosition}"
        )
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
                Log.d(
                    "MediaPlaybackService",
                    "Initializing player with sources: ${complement.sources}"
                )
                HlsPlayerUtil.initializePlayer(player, complement.sources)
                updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
            }
        }
    }

    fun getExoPlayer(): ExoPlayer? {
        Log.d("MediaPlaybackService", "getExoPlayer: player=${exoPlayer != null}")
        return exoPlayer
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer?.stop()
        stopSelf()
        Log.d("MediaPlaybackService", "onTaskRemoved")
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.let { player ->
            if (player.currentPosition > 10_000) {
                coroutineScope.launch {
                    try {
                        withTimeout(5_000) { updateStoredWatchState?.invoke(player.currentPosition) }
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
        Log.d("MediaPlaybackService", "onDestroy")
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        Log.d("MediaPlaybackService", "onGetRoot: client=$clientPackageName")
        return BrowserRoot("media_root_id", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem?>?>
    ) {
        Log.d("MediaPlaybackService", "onLoadChildren: parentId=$parentId")
        result.sendResult(mutableListOf())
    }

    inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            exoPlayer?.play()
            Log.d("MediaPlaybackService", "onPlay")
        }

        override fun onPause() {
            exoPlayer?.pause()
            Log.d("MediaPlaybackService", "onPause")
        }

        override fun onSkipToNext() {
            val currentEpisodeNo = episodeDetailComplement?.servers?.episodeNo ?: return
            val nextEpisode = episodes.find { it.episodeNo == currentEpisodeNo + 1 }
            if (nextEpisode != null) {
                episodeSourcesQuery = episodeSourcesQuery?.copy(id = nextEpisode.episodeId)
                episodeDetailComplement = episodeDetailComplement?.copy(id = nextEpisode.episodeId)
                handleSelectedEpisodeServer?.invoke(episodeSourcesQuery!!)
                Log.d("MediaPlaybackService", "onSkipToNext: episodeNo=${nextEpisode.episodeNo}")
            }
        }

        override fun onSkipToPrevious() {
            val currentEpisodeNo = episodeDetailComplement?.servers?.episodeNo ?: return
            val previousEpisode = episodes.find { it.episodeNo == currentEpisodeNo - 1 }
            if (previousEpisode != null) {
                episodeSourcesQuery = episodeSourcesQuery?.copy(id = previousEpisode.episodeId)
                episodeDetailComplement =
                    episodeDetailComplement?.copy(id = previousEpisode.episodeId)
                handleSelectedEpisodeServer?.invoke(episodeSourcesQuery!!)
                Log.d(
                    "MediaPlaybackService",
                    "onSkipToPrevious: episodeNo=${previousEpisode.episodeNo}"
                )
            }
        }

        override fun onStop() {
            exoPlayer?.stop()
            stopForeground(true)
            stopSelf()
            Log.d("MediaPlaybackService", "onStop")
        }

        override fun onSeekTo(pos: Long) {
            val clampedPos =
                pos.coerceAtLeast(0).coerceAtMost(exoPlayer?.duration ?: Long.MAX_VALUE)
            exoPlayer?.seekTo(clampedPos)
            updateStoredWatchState?.invoke(clampedPos)
            updatePlaybackState(exoPlayer?.playbackState?.let {
                when (it) {
                    Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
                    Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
                    Player.STATE_READY -> if (exoPlayer?.isPlaying == true) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                    else -> PlaybackStateCompat.STATE_NONE
                }
            } ?: PlaybackStateCompat.STATE_NONE)
            Log.d("MediaPlaybackService", "onSeekTo: requested=$pos, clamped=$clampedPos")
        }

        override fun onSetPlaybackSpeed(speed: Float) {
            exoPlayer?.setPlaybackSpeed(speed)
            Log.d("MediaPlaybackService", "onSetPlaybackSpeed: speed=$speed")
        }
    }
}