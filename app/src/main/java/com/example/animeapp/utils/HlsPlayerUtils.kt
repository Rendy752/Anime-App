package com.example.animeapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ExoPlaybackException
import com.example.animeapp.models.EpisodeSourcesResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

data class PlayerState(
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val error: String? = null,
    val isReady: Boolean = false
)

sealed class PlayerAction {
    data class InitializePlayer(val context: Context) : PlayerAction()
    data class SetMedia(
        val videoData: EpisodeSourcesResponse,
        val lastTimestamp: Long? = null,
        val onReady: () -> Unit = {},
        val onError: (String) -> Unit = {}
    ) : PlayerAction()

    data object Play : PlayerAction()
    data object Pause : PlayerAction()
    data class SeekTo(val positionMs: Long) : PlayerAction()
    data object FastForward : PlayerAction()
    data object Rewind : PlayerAction()
    data class SetPlaybackSpeed(val speed: Float) : PlayerAction()
    data object Release : PlayerAction()
}

object HlsPlayerUtils {
    private var exoPlayer: ExoPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusChangeListener: OnAudioFocusChangeListener? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested: Boolean = false
    private var videoSurface: Any? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    @OptIn(UnstableApi::class)
    fun dispatch(action: PlayerAction) {
        when (action) {
            is PlayerAction.InitializePlayer -> initializePlayer(action.context)
            is PlayerAction.SetMedia -> setMedia(
                action.videoData,
                action.lastTimestamp,
                action.onReady,
                action.onError
            )

            is PlayerAction.Play -> play()
            is PlayerAction.Pause -> pause()
            is PlayerAction.SeekTo -> seekTo(action.positionMs)
            is PlayerAction.FastForward -> fastForward()
            is PlayerAction.Rewind -> rewind()
            is PlayerAction.SetPlaybackSpeed -> setPlaybackSpeed(action.speed)
            is PlayerAction.Release -> release()
        }
    }

    fun setVideoSurface(surface: Any?) {
        videoSurface = surface
        when (surface) {
            is TextureView -> exoPlayer?.setVideoTextureView(surface)
            is SurfaceView -> exoPlayer?.setVideoSurfaceView(surface)
            null -> {
                exoPlayer?.clearVideoSurface()
                Log.d("HlsPlayerUtil", "Video surface cleared")
            }

            else -> Log.w("HlsPlayerUtil", "Unsupported video surface type: ${surface.javaClass}")
        }
        Log.d("HlsPlayerUtil", "Video surface set: ${surface?.javaClass?.simpleName}")
    }

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
                    Log.e("HlsPlayerUtil", "Failed to capture frame from TextureView", e)
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
                            Log.e("HlsPlayerUtil", "PixelCopy failed: $result")
                        }
                    }, Handler(Looper.getMainLooper()))
                    latch.await(1, TimeUnit.SECONDS)
                    bitmap
                } catch (e: Exception) {
                    Log.e("HlsPlayerUtil", "Failed to capture frame from SurfaceView", e)
                    null
                }
            }

            else -> {
                Log.w("HlsPlayerUtil", "No video surface available for frame capture")
                null
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer(context: Context) {
        if (exoPlayer == null) {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Log.d("HlsPlayerUtil", "onIsPlayingChanged: isPlaying=$isPlaying")
                        _state.update { it.copy(isPlaying = isPlaying, error = null) }
                        if (isPlaying) {
                            requestAudioFocus()
                        } else {
                            abandonAudioFocus()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        Log.d("HlsPlayerUtil", "onPlaybackStateChanged: state=$state")
                        val isReady = state == Player.STATE_READY || state == Player.STATE_BUFFERING
                        _state.update { it.copy(playbackState = state, isReady = isReady) }
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
                        Log.e("HlsPlayerUtil", "onPlayerError: $message", error)
                        _state.update { it.copy(error = message) }
                        abandonAudioFocus()
                    }
                })
                pause()
                Log.d("HlsPlayerUtil", "ExoPlayer initialized")
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
                    "HlsPlayerUtil",
                    "Media set: url=${mediaItemUri}, lastTimestamp=$lastTimestamp"
                )

                val readyListener = object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            Log.d("HlsPlayerUtil", "Player ready for media")
                            onReady()
                            player.removeListener(this)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val message = "Failed to initialize player: ${error.message}"
                        Log.e("HlsPlayerUtil", message, error)
                        onError(message)
                        player.removeListener(this)
                    }
                }
                player.addListener(readyListener)
            } else {
                val message = "Invalid HLS source"
                Log.e("HlsPlayerUtil", message)
                _state.update { it.copy(error = message) }
                onError(message)
            }
        }
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    private fun play() {
        exoPlayer?.play()
        Log.d("HlsPlayerUtil", "play called")
    }

    private fun pause() {
        exoPlayer?.pause()
        Log.d("HlsPlayerUtil", "pause called")
    }

    private fun seekTo(positionMs: Long) {
        exoPlayer?.let {
            val duration = it.duration
            val clampedPos = positionMs.coerceAtLeast(0)
                .coerceAtMost(if (duration > 0) duration else Long.MAX_VALUE)
            it.seekTo(clampedPos)
            Log.d("HlsPlayerUtil", "seekTo: position=$clampedPos, duration=$duration")
        }
    }

    private fun fastForward() {
        exoPlayer?.let {
            val currentPosition = it.currentPosition
            val duration = it.duration
            val newPosition =
                (currentPosition + 10_000).coerceAtMost(if (duration > 0) duration else Long.MAX_VALUE)
            it.seekTo(newPosition)
            Log.d("HlsPlayerUtil", "fastForward: from=$currentPosition to=$newPosition")
        }
    }

    private fun rewind() {
        exoPlayer?.let {
            val currentPosition = it.currentPosition
            val newPosition = (currentPosition - 10_000).coerceAtLeast(0)
            it.seekTo(newPosition)
            Log.d("HlsPlayerUtil", "rewind: from=$currentPosition to=$newPosition")
        }
    }

    private fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        Log.d("HlsPlayerUtil", "setPlaybackSpeed: speed=$speed")
    }

    private fun requestAudioFocus() {
        if (!audioFocusRequested) {
            if (audioFocusRequest == null) {
                audioFocusChangeListener = OnAudioFocusChangeListener { focusChange ->
                    Log.d("HlsPlayerUtil", "Audio focus changed: $focusChange")
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> dispatch(PlayerAction.Pause)
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> dispatch(PlayerAction.Pause)
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
                    .setOnAudioFocusChangeListener(audioFocusChangeListener!!)
                    .build()
            }

            audioManager?.let { manager ->
                val result = audioFocusRequest?.let { manager.requestAudioFocus(it) }
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioFocusRequested = true
                    Log.d("HlsPlayerUtil", "Audio focus granted")
                } else {
                    Log.w("HlsPlayerUtil", "Audio focus request failed")
                }
            }
        }
    }

    private fun abandonAudioFocus() {
        if (audioFocusRequested) {
            audioManager?.let { manager ->
                audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
                audioFocusRequested = false
                Log.d("HlsPlayerUtil", "Audio focus abandoned")
            }
        }
    }

    private fun release() {
        exoPlayer?.let { player ->
            player.stop()
            player.release()
            Log.d("HlsPlayerUtil", "ExoPlayer released")
        }
        exoPlayer = null
        audioManager = null
        audioFocusRequest = null
        audioFocusChangeListener = null
        audioFocusRequested = false
        videoSurface = null
        _state.update {
            it.copy(
                isPlaying = false,
                playbackState = Player.STATE_IDLE,
                error = null,
                isReady = false
            )
        }
    }
}