package com.luminoverse.animevibe.utils.media

import android.annotation.SuppressLint
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
import androidx.compose.runtime.Stable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.luminoverse.animevibe.models.EpisodeSources
import com.luminoverse.animevibe.models.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Stable
data class PlayerCoreState(
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val isLoading: Boolean = false,
    val error: PlaybackException? = null
)

data class ControlsState(
    val isControlsVisible: Boolean = false,
    val isLocked: Boolean = false,
    val selectedSubtitle: Track? = null,
    val playbackSpeed: Float = 1f,
    val zoom: Float = 1f
)

sealed class HlsPlayerAction {
    data class SetMedia(
        val videoData: EpisodeSources,
        val isAutoPlayVideo: Boolean,
        val currentPosition: Long,
        val duration: Long,
        val onError: (String) -> Unit
    ) : HlsPlayerAction()

    data object Play : HlsPlayerAction()
    data object Pause : HlsPlayerAction()
    data class SeekTo(val positionMs: Long) : HlsPlayerAction()
    data object FastForward : HlsPlayerAction()
    data object Rewind : HlsPlayerAction()
    data class SetPlaybackSpeed(val speed: Float) : HlsPlayerAction()
    data object Release : HlsPlayerAction()
    data class SetVideoSurface(val surface: Any?) : HlsPlayerAction()
    data class SetSubtitle(val track: Track?) : HlsPlayerAction()
    data class RequestToggleControlsVisibility(val isVisible: Boolean? = null) : HlsPlayerAction()
    data class ToggleLock(val isLocked: Boolean) : HlsPlayerAction()
    data class SetZoom(val zoom: Float) : HlsPlayerAction()
}

private const val CONTROLS_AUTO_HIDE_DELAY_MS = 3_000L

@Singleton
@OptIn(UnstableApi::class)
class HlsPlayerUtils @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val okHttpClient: OkHttpClient
) {
    private var exoPlayer: ExoPlayer? = null
    private var playerListener: Player.Listener? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested: Boolean = false
    private var videoSurface: Any? = null

    private var currentVideoData: EpisodeSources? = null

    private val playerCoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var controlsHideJob: Job? = null

    private val _playerCoreState = MutableStateFlow(PlayerCoreState())
    val playerCoreState: StateFlow<PlayerCoreState> = _playerCoreState.asStateFlow()

    private val _controlsState = MutableStateFlow(ControlsState())
    val controlsState: StateFlow<ControlsState> = _controlsState.asStateFlow()

    init {
        initializePlayerInternal()

        combine(_controlsState.asStateFlow(), _playerCoreState.asStateFlow()) { controls, core ->
            Triple(controls.isControlsVisible, core.isPlaying, controls.isLocked)
        }.onEach { (isControlsVisible, isPlaying, isLocked) ->
            manageAutoHideControls(isControlsVisible, isPlaying, isLocked)
        }.launchIn(playerCoroutineScope)
    }

    @SuppressLint("MissingPermission")
    private fun initializePlayerInternal() {
        if (exoPlayer == null) {
            audioManager =
                applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val trackSelector = DefaultTrackSelector(applicationContext)
            val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)

            val mediaSourceFactory = DefaultMediaSourceFactory(applicationContext)
                .setDataSourceFactory(dataSourceFactory)

            exoPlayer = ExoPlayer.Builder(applicationContext)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    playerListener = createPlayerListener()
                    addListener(playerListener!!)
                    pause()
                    Log.d("HlsPlayerUtils", "ExoPlayer initialized by Hilt with custom OkHttpClient")
                }
            _playerCoreState.update {
                PlayerCoreState(isPlaying = false, playbackState = Player.STATE_IDLE)
            }
            _controlsState.update { ControlsState(isControlsVisible = true) }
        }
    }

    fun dispatch(action: HlsPlayerAction) {
        when (action) {
            is HlsPlayerAction.SetMedia -> setMedia(
                action.videoData,
                action.isAutoPlayVideo,
                action.currentPosition,
                action.duration,
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
            is HlsPlayerAction.SetSubtitle -> setSubtitle(action.track)
            is HlsPlayerAction.RequestToggleControlsVisibility -> {
                _controlsState.update {
                    it.copy(isControlsVisible = action.isVisible ?: !it.isControlsVisible)
                }
            }

            is HlsPlayerAction.ToggleLock -> {
                _controlsState.update {
                    it.copy(
                        isLocked = action.isLocked, isControlsVisible = !action.isLocked
                    )
                }
            }

            is HlsPlayerAction.SetZoom -> {
                _controlsState.update { it.copy(zoom = action.zoom) }
            }
        }
    }

    private fun manageAutoHideControls(
        isControlsVisible: Boolean,
        isPlaying: Boolean,
        isLocked: Boolean
    ) {
        controlsHideJob?.cancel()
        if (isControlsVisible && isPlaying && !isLocked) {
            controlsHideJob = playerCoroutineScope.launch {
                delay(CONTROLS_AUTO_HIDE_DELAY_MS)
                _controlsState.update { it.copy(isControlsVisible = false) }
                Log.d("HlsPlayerUtils", "Controls auto-hidden")
            }
            Log.d("HlsPlayerUtils", "Auto-hide timer started/reset")
        } else {
            Log.d(
                "HlsPlayerUtils",
                "Auto-hide timer stopped (conditions not met: isVisible=$isControlsVisible, isPlaying=$isPlaying, isLocked=$isLocked)"
            )
        }
    }

    private fun createPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d("HlsPlayerUtils", "onIsPlayingChanged: $isPlaying")
                _playerCoreState.update { it.copy(isPlaying = isPlaying) }
                if (!isPlaying) dispatch(HlsPlayerAction.RequestToggleControlsVisibility(isVisible = true))
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d("HlsPlayerUtils", "onPlaybackStateChanged: $playbackState")
                _playerCoreState.update {
                    it.copy(
                        playbackState = playbackState,
                        isLoading = playbackState == Player.STATE_BUFFERING,
                        error = null
                    )
                }
                if (playbackState == Player.STATE_READY) {
                    _playerCoreState.update { it.copy(isLoading = false, error = null) }
                }
                if (playbackState == Player.STATE_ENDED) {
                    dispatch(HlsPlayerAction.ToggleLock(false))
                    _playerCoreState.update {
                        it.copy(isPlaying = false, isLoading = false, error = null)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playerCoreState.update {
                    it.copy(
                        isPlaying = false,
                        isLoading = false,
                        error = error
                    )
                }
                Log.e("HlsPlayerUtils", "PlayerError: ${error.message}", error)
            }
        }
    }

    fun getPlayer(): ExoPlayer? {
        if (exoPlayer == null) {
            Log.w("HlsPlayerUtils", "Player was null. Re-initializing.")
            initializePlayerInternal()
            videoSurface?.let { setVideoSurface(it) }
        }
        return exoPlayer
    }

    private fun captureFrame(): Bitmap? {
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
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            bitmap.recycle()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to capture screenshot", e)
            null
        }
    }

    private fun setMedia(
        videoData: EpisodeSources,
        isAutoPlayVideo: Boolean,
        currentPosition: Long,
        duration: Long,
        onError: (String) -> Unit
    ) {
        try {
            _controlsState.update { ControlsState() }
            currentVideoData = videoData
            setPlaybackSpeed(1f)

            exoPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()
                _playerCoreState.update {
                    it.copy(isPlaying = false, playbackState = Player.STATE_IDLE)
                }

                if (videoData.link.file.isNotEmpty() && videoData.link.type == "hls") {
                    val mediaItemUri = videoData.link.file.toUri()
                    val mediaItemBuilder = MediaItem.Builder().setUri(mediaItemUri)
                    player.setMediaItem(mediaItemBuilder.build())
                    player.prepare()
                    player.trackSelectionParameters = TrackSelectionParameters.Builder().build()
                    setSubtitle(videoData.tracks.firstOrNull { it.kind == "captions" && it.default == true })

                    if (isAutoPlayVideo) {
                        if (currentPosition == duration) seekTo(0)
                        else if (currentPosition < duration) seekTo(currentPosition)
                        dispatch(HlsPlayerAction.Play)
                    }
                } else {
                    onError("Invalid HLS source")
                }
            } ?: run {
                onError("Player not initialized")
            }
            dispatch(
                HlsPlayerAction.RequestToggleControlsVisibility(isVisible = true)
            )
        } catch (e: Exception) {
            onError("Failed to set media: ${e.message ?: "Unknown error"}")
        }
    }


    private fun play() {
        exoPlayer?.let {
            it.play()
            requestAudioFocus()
            Log.d("HlsPlayerUtils", "play: called")
        }
    }

    private fun pause() {
        exoPlayer?.let {
            it.pause()
            abandonAudioFocus()
        }
    }

    private fun seekTo(positionMs: Long) {
        exoPlayer?.let {
            val duration = it.duration
            val clampedPos = positionMs.coerceAtLeast(0)
                .coerceAtMost(if (duration > 0) duration else Long.MAX_VALUE)
            it.seekTo(clampedPos)
        }
    }

    private fun fastForward() {
        exoPlayer?.let {
            val currentPosition = it.currentPosition
            val duration = it.duration
            val newPosition =
                (currentPosition + 10_000).coerceAtMost(if (duration > 0) duration else Long.MAX_VALUE)
            seekTo(newPosition)
        }
    }

    private fun rewind() {
        exoPlayer?.let {
            try {
                val currentPosition = it.currentPosition
                val newPosition = (currentPosition - 10_000).coerceAtLeast(0)
                seekTo(newPosition)
            } catch (e: Exception) {
                Log.e("HlsPlayer", "Rewind failed", e)
            }
        }
    }

    private fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        _controlsState.update { it.copy(playbackSpeed = speed, isControlsVisible = false) }
    }

    private fun setVideoSurface(surface: Any?) {
        videoSurface = surface
        try {
            when (surface) {
                is TextureView -> exoPlayer?.setVideoTextureView(surface)
                is SurfaceView -> exoPlayer?.setVideoSurfaceView(surface)
                null -> exoPlayer?.clearVideoSurface()
                else -> Log.w(
                    "HlsPlayerUtils",
                    "Unsupported video surface type: ${surface.javaClass}"
                )
            }
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to set video surface", e)
        }
    }

    private fun setSubtitle(track: Track?) {
        try {
            exoPlayer?.let { player ->
                val builder = player.trackSelectionParameters.buildUpon()
                if (track == null || track.label == "None") {
                    player.trackSelectionParameters = builder.setPreferredTextLanguages().build()
                    _controlsState.update { it.copy(selectedSubtitle = null) }
                } else {
                    val language = track.label?.substringBefore("-")?.trim() ?: ""
                    player.trackSelectionParameters =
                        builder.setPreferredTextLanguages(language).build()
                    _controlsState.update { it.copy(selectedSubtitle = track) }
                }
            }
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to set subtitle", e)
        }
    }

    private fun requestAudioFocus() {
        try {
            if (!audioFocusRequested) {
                if (audioFocusRequest == null) {
                    val audioFocusChangeListener =
                        AudioManager.OnAudioFocusChangeListener { focusChange ->
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
                    } else {
                        Log.w("HlsPlayerUtils", "Audio focus request failed")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to request audio focus", e)
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (audioFocusRequested) {
                audioManager?.let { manager ->
                    audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
                    audioFocusRequested = false
                }
            }
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to abandon audio focus", e)
        }
    }

    private fun release() {
        try {
            exoPlayer?.let { player ->
                playerListener?.let { player.removeListener(it) }
                player.stop()
                player.release()
            }
            exoPlayer = null
            playerListener = null
            audioManager = null
            audioFocusRequest = null
            audioFocusRequested = false
            videoSurface = null
            currentVideoData = null

            controlsHideJob?.cancel()
            controlsHideJob = null

            playerCoroutineScope.cancel()
            _playerCoreState.update { PlayerCoreState() }
            _controlsState.update { ControlsState() }
            Log.d("HlsPlayerUtils", "HlsPlayerUtils completely released and state reset")
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to release HlsPlayerUtils", e)
        }
    }
}