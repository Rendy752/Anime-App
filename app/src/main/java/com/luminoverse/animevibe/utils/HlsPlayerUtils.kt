package com.luminoverse.animevibe.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ExoPlaybackException
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class HlsPlayerState(
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val error: String? = null,
    val isReady: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val playbackSpeed: Float = 1f
)

sealed class HlsPlayerAction {
    data class InitializeHlsPlayer(val context: Context) : HlsPlayerAction()
    data class SetMedia(
        val videoData: EpisodeSourcesResponse,
        val lastTimestamp: Long? = null,
        val onReady: () -> Unit = {},
        val onError: (String) -> Unit = {}
    ) : HlsPlayerAction()

    data object Play : HlsPlayerAction()
    data object Pause : HlsPlayerAction()
    data class SeekTo(val positionMs: Long) : HlsPlayerAction()
    data object FastForward : HlsPlayerAction()
    data object Rewind : HlsPlayerAction()
    data class SetPlaybackSpeed(val speed: Float) : HlsPlayerAction()
    data object Release : HlsPlayerAction()
    data class SetVideoSurface(val surface: Any?) : HlsPlayerAction()
    data class UpdateWatchState(
        val complement: EpisodeDetailComplement,
        val episodes: List<Episode>,
        val query: EpisodeSourcesQuery,
        val updateStoredWatchState: (Long?, Long?, String?) -> Unit
    ) : HlsPlayerAction()
}

object HlsPlayerUtils {
    private var exoPlayer: ExoPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested: Boolean = false
    private var videoSurface: Any? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var watchStateUpdateJob: Job? = null

    private val _state = MutableStateFlow(HlsPlayerState())
    val state: StateFlow<HlsPlayerState> = _state.asStateFlow()

    private const val WATCH_STATE_UPDATE_INTERVAL_MS = 5_000L

    @OptIn(UnstableApi::class)
    fun dispatch(action: HlsPlayerAction) {
        when (action) {
            is HlsPlayerAction.InitializeHlsPlayer -> initializePlayer(action.context)
            is HlsPlayerAction.SetMedia -> setMedia(
                action.videoData,
                action.lastTimestamp,
                action.onReady,
                action.onError
            )

            is HlsPlayerAction.Play -> play()
            is HlsPlayerAction.Pause -> pause()
            is HlsPlayerAction.SeekTo -> seekTo(action.positionMs)
            is HlsPlayerAction.FastForward -> fastForward()
            is HlsPlayerAction.Rewind -> rewind()
            is HlsPlayerAction.SetPlaybackSpeed -> setPlaybackSpeed(action.speed)
            is HlsPlayerAction.Release -> release()
            is HlsPlayerAction.SetVideoSurface -> setVideoSurface(action.surface)
            is HlsPlayerAction.UpdateWatchState -> updateWatchState(
                action.updateStoredWatchState
            )
        }
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun captureFrame(): Bitmap? {
        return when (val surface = videoSurface) {
            is TextureView -> {
                try {
                    surface.bitmap?.let { bitmap ->
                        val scaledBitmap = bitmap.scale(512, 288)
                        if (scaledBitmap != bitmap) bitmap.recycle()
                        scaledBitmap
                    }
                } catch (e: Exception) {
                    Log.e("HlsPlayerUtils", "Failed to capture frame from TextureView", e)
                    null
                }
            }

            is SurfaceView -> {
                try {
                    val bitmap = createBitmap(512, 288)
                    val latch = CountDownLatch(1)
                    PixelCopy.request(surface.holder.surface, bitmap, { result ->
                        if (result == PixelCopy.SUCCESS) {
                            latch.countDown()
                        } else {
                            Log.e("HlsPlayerUtils", "PixelCopy failed: $result")
                        }
                    }, Handler(Looper.getMainLooper()))
                    latch.await(1, TimeUnit.SECONDS)
                    bitmap
                } catch (e: Exception) {
                    Log.e("HlsPlayerUtils", "Failed to capture frame from SurfaceView", e)
                    null
                }
            }

            else -> {
                Log.w("HlsPlayerUtils", "No video surface available for frame capture")
                null
            }
        }
    }

    suspend fun captureScreenshot(): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = captureFrame() ?: return@withContext null
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            bitmap.recycle()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to capture screenshot", e)
            null
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer(context: Context) {
        if (exoPlayer == null) {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Log.d("HlsPlayerUtils", "onIsPlayingChanged: isPlaying=$isPlaying")
                        _state.update { it.copy(isPlaying = isPlaying, error = null) }
                        if (isPlaying) {
                            requestAudioFocus()
                        } else {
                            abandonAudioFocus()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        Log.d("HlsPlayerUtils", "onPlaybackStateChanged: state=$state")
                        val isReady = state == Player.STATE_READY || state == Player.STATE_BUFFERING
                        _state.update {
                            it.copy(
                                playbackState = state,
                                isReady = isReady,
                                currentPosition = currentPosition,
                                duration = duration
                            )
                        }
                        when (state) {
                            Player.STATE_ENDED -> {
                                _state.update { it.copy(isPlaying = false) }
                                abandonAudioFocus()
                            }

                            Player.STATE_READY -> {
                                if (!_state.value.isPlaying) {
                                    _state.update { it.copy(error = null) }
                                }
                            }

                            Player.STATE_BUFFERING -> {
                                _state.update { it.copy(error = null) }
                            }

                            Player.STATE_IDLE -> {
                                _state.update { it.copy(error = null) }
                                abandonAudioFocus()
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val message = when {
                            error is ExoPlaybackException && error.type == ExoPlaybackException.TYPE_SOURCE -> {
                                "Playback error, try a different server."
                            }

                            error.message != null -> "Playback error: ${error.message}"
                            else -> "Unknown playback error"
                        }
                        Log.e("HlsPlayerUtils", "onPlayerError: $message", error)
                        _state.update { it.copy(error = message) }
                        abandonAudioFocus()
                    }
                })
                pause()
                Log.d("HlsPlayerUtils", "ExoPlayer initialized")
            }
            _state.update { it.copy(playbackState = Player.STATE_IDLE, isReady = false) }
        }
    }

    @OptIn(UnstableApi::class)
    private fun setMedia(
        videoData: EpisodeSourcesResponse,
        lastTimestamp: Long? = null,
        onReady: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            player.pause()

            if (videoData.sources.isNotEmpty() && videoData.sources[0].type == "hls") {
                val mediaItemUri = videoData.sources[0].url.toUri()
                val mediaItemBuilder = MediaItem.Builder().setUri(mediaItemUri)

                if (videoData.tracks.any { it.kind == "captions" }) {
                    val subtitleConfigurations = mutableListOf<MediaItem.SubtitleConfiguration>()
                    videoData.tracks.filter { it.kind == "captions" }.forEach { track ->
                        val subtitleConfiguration =
                            MediaItem.SubtitleConfiguration.Builder(track.file.toUri())
                                .setMimeType(MimeTypes.TEXT_VTT)
                                .setLanguage(track.label?.substringBefore("-")?.trim())
                                .setSelectionFlags(if (track.default == true) C.SELECTION_FLAG_DEFAULT else 0)
                                .setLabel(track.label?.substringBefore("-")?.trim())
                                .build()
                        subtitleConfigurations.add(subtitleConfiguration)
                    }
                    mediaItemBuilder.setSubtitleConfigurations(subtitleConfigurations)
                }

                player.setMediaItem(mediaItemBuilder.build())
                player.prepare()
                lastTimestamp?.let { if (it > 0 && it < player.duration) player.seekTo(it) }
                player.pause()
                Log.d(
                    "HlsPlayerUtils",
                    "Media set: url=${mediaItemUri}, lastTimestamp=$lastTimestamp"
                )

                val readyListener = object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            Log.d("HlsPlayerUtils", "Player ready for media")
                            onReady()
                            player.removeListener(this)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val message =
                            "Playback failed: ${error.message ?: "Unknown error"}. Please refresh or try a different server."
                        Log.e("HlsPlayerUtils", message, error)
                        onError(message)
                        player.removeListener(this)
                    }
                }
                player.addListener(readyListener)
            } else {
                val message = "Invalid HLS source"
                Log.e("HlsPlayerUtils", message)
                _state.update { it.copy(error = message) }
                onError(message)
            }
        }
    }

    private fun play() {
        exoPlayer?.let {
            if (_state.value.isReady && !_state.value.isPlaying) it.play()
        }
    }

    private fun pause() {
        exoPlayer?.pause()
        Log.d("HlsPlayerUtils", "pause called")
    }

    private fun seekTo(positionMs: Long) {
        exoPlayer?.let {
            val duration = it.duration
            val clampedPos = positionMs.coerceAtLeast(0)
                .coerceAtMost(if (duration > 0) duration else Long.MAX_VALUE)
            it.seekTo(clampedPos)
            Log.d("HlsPlayerUtils", "seekTo: position=$clampedPos, duration=$duration")
        }
    }

    private fun fastForward() {
        exoPlayer?.let {
            val currentPosition = it.currentPosition
            val duration = it.duration
            val newPosition =
                (currentPosition + 10_000).coerceAtMost(if (duration > 0) duration else Long.MAX_VALUE)
            it.seekTo(newPosition)
            Log.d("HlsPlayerUtils", "fastForward: from=$currentPosition to=$newPosition")
        }
    }

    private fun rewind() {
        exoPlayer?.let {
            val currentPosition = it.currentPosition
            val newPosition = (currentPosition - 10_000).coerceAtLeast(0)
            it.seekTo(newPosition)
            Log.d("HlsPlayerUtils", "rewind: from=$currentPosition to=$newPosition")
        }
    }

    private fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        _state.update { it.copy(playbackSpeed = speed) }
        Log.d("HlsPlayerUtils", "setPlaybackSpeed: speed=$speed")
    }

    private fun setVideoSurface(surface: Any?) {
        videoSurface = surface
        when (surface) {
            is TextureView -> exoPlayer?.setVideoTextureView(surface)
            is SurfaceView -> exoPlayer?.setVideoSurfaceView(surface)
            null -> {
                exoPlayer?.clearVideoSurface()
                Log.d("HlsPlayerUtils", "Video surface cleared")
            }

            else -> Log.w("HlsPlayerUtils", "Unsupported video surface type: ${surface.javaClass}")
        }
        Log.d("HlsPlayerUtils", "Video surface set: ${surface?.javaClass?.simpleName}")
    }

    private fun updateWatchState(updateStoredWatchState: (Long?, Long?, String?) -> Unit) {
        startPeriodicWatchStateUpdates(updateStoredWatchState)
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    coroutineScope.launch {
                        try {
                            withTimeout(5_000) {
                                val player = getPlayer() ?: return@withTimeout
                                val duration = player.duration.takeIf { it > 0 }
                                val screenshot = captureScreenshot()
                                updateStoredWatchState(duration, duration, screenshot)
                                Log.d(
                                    "HlsPlayerUtils",
                                    "Video ended: saved watch state with position=$duration, duration=$duration, screenshot=${
                                        screenshot?.take(
                                            20
                                        )
                                    }..."
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("HlsPlayerUtils", "Failed to save watch state on video end", e)
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startPeriodicWatchStateUpdates(updateStoredWatchState)
                } else {
                    stopPeriodicWatchStateUpdates()
                }
            }
        })
    }

    private fun startPeriodicWatchStateUpdates(
        updateStoredWatchState: (Long?, Long?, String?) -> Unit
    ) {
        watchStateUpdateJob?.cancel()
        watchStateUpdateJob = coroutineScope.launch {
            while (true) {
                val state = _state.value
                if (state.isPlaying) {
                    exoPlayer?.let { player ->
                        val position = player.currentPosition
                        val duration = player.duration
                        if (position > 10_000 && position < duration) {
                            try {
                                withTimeout(5_000) {
                                    val screenshot = captureScreenshot()
                                    updateStoredWatchState(position, duration, screenshot)
                                    Log.d(
                                        "HlsPlayerUtils",
                                        "Periodic watch state update: position=$position, screenshot=${
                                            screenshot?.take(
                                                20
                                            )
                                        }..."
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("HlsPlayerUtils", "Failed to save periodic watch state", e)
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
        Log.d("HlsPlayerUtils", "Stopped periodic watch state updates")
    }

    private fun requestAudioFocus() {
        if (!audioFocusRequested) {
            if (audioFocusRequest == null) {
                val audioFocusChangeListener =
                    AudioManager.OnAudioFocusChangeListener { focusChange ->
                        Log.d("HlsPlayerUtils", "Audio focus changed: $focusChange")
                        when (focusChange) {
                            AudioManager.AUDIOFOCUS_LOSS -> dispatch(HlsPlayerAction.Pause)
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> dispatch(HlsPlayerAction.Pause)
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                                exoPlayer?.volume = 0.5f
                            }
                        }
                    }
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
            }

            audioManager?.let { manager ->
                val result = audioFocusRequest?.let { manager.requestAudioFocus(it) }
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioFocusRequested = true
                    Log.d("HlsPlayerUtils", "Audio focus granted")
                } else {
                    Log.w("HlsPlayerUtils", "Audio focus request failed")
                }
            }
        }
    }

    private fun abandonAudioFocus() {
        if (audioFocusRequested) {
            audioManager?.let { manager ->
                audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
                audioFocusRequested = false
                Log.d("HlsPlayerUtils", "Audio focus abandoned")
            }
        }
    }

    private fun release() {
        exoPlayer?.let { player ->
            player.stop()
            player.release()
            Log.d("HlsPlayerUtils", "ExoPlayer released")
        }
        exoPlayer = null
        audioManager = null
        audioFocusRequest = null
        audioFocusRequested = false
        videoSurface = null
        stopPeriodicWatchStateUpdates()
        coroutineScope.cancel()
        _state.update {
            it.copy(
                isPlaying = false,
                playbackState = Player.STATE_IDLE,
                error = null,
                isReady = false,
                currentPosition = 0,
                duration = 0,
                playbackSpeed = 1f
            )
        }
    }
}