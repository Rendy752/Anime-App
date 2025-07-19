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
import android.view.Surface
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
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
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
import java.io.File
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Stable
data class PlayerCoreState(
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val duration: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ControlsState(
    val isControlsVisible: Boolean = false,
    val isLocked: Boolean = false,
    val selectedSubtitle: Track? = null,
    val playbackSpeed: Float = 1f,
    val zoom: Float = 1f
)

sealed class PlayerAction {
    data class SetMedia(val videoData: EpisodeSources) : PlayerAction()
    data object Reset : PlayerAction()
    data object Play : PlayerAction()
    data object Pause : PlayerAction()
    data class SeekTo(val positionMs: Long) : PlayerAction()
    data class SetPlaybackSpeed(val speed: Float) : PlayerAction()
    data class SetVideoSurface(val surface: Any?) : PlayerAction()
    data class SetSubtitle(val track: Track?) : PlayerAction()
    data class RequestToggleControlsVisibility(val isVisible: Boolean? = null) : PlayerAction()
    data class ToggleLock(val isLocked: Boolean) : PlayerAction()
    data class SetZoom(val zoom: Float) : PlayerAction()
}

private const val CONTROLS_AUTO_HIDE_DELAY_MS = 3_000L

@Singleton
@OptIn(UnstableApi::class)
class HlsPlayerUtils @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val okHttpClient: OkHttpClient,
    private val exoPlayerCache: SimpleCache
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
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(exoPlayerCache)
                .setUpstreamDataSourceFactory(dataSourceFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(applicationContext)
                .setDataSourceFactory(cacheDataSourceFactory)
            exoPlayer = ExoPlayer.Builder(applicationContext)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    playerListener = createPlayerListener()
                    addListener(playerListener!!)
                    pause()
                }
            _playerCoreState.update {
                PlayerCoreState(isPlaying = false, playbackState = Player.STATE_IDLE)
            }
            _controlsState.update { ControlsState(isControlsVisible = true) }
        }
    }

    fun dispatch(action: PlayerAction) {
        when (action) {
            is PlayerAction.SetMedia -> setMedia(action.videoData)
            is PlayerAction.Reset -> reset()
            is PlayerAction.Play -> play()
            is PlayerAction.Pause -> pause()
            is PlayerAction.SeekTo -> seekTo(action.positionMs)
            is PlayerAction.SetPlaybackSpeed -> setPlaybackSpeed(action.speed)
            is PlayerAction.SetVideoSurface -> setVideoSurface(action.surface)
            is PlayerAction.SetSubtitle -> setSubtitle(action.track)
            is PlayerAction.RequestToggleControlsVisibility -> {
                _controlsState.update {
                    it.copy(isControlsVisible = action.isVisible ?: !it.isControlsVisible)
                }
            }

            is PlayerAction.ToggleLock -> {
                _controlsState.update {
                    it.copy(
                        isLocked = action.isLocked, isControlsVisible = !action.isLocked
                    )
                }
            }

            is PlayerAction.SetZoom -> {
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
            }
        }
    }

    private fun getReadableErrorMessage(error: PlaybackException): String {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                val code = cause.responseCode
                return when (code) {
                    403, 410 -> "This video link may have expired. Try another server or refresh."
                    404 -> "Video not found. It might be an issue with the server."
                    in 500..599 -> "The video server is having issues. Please try again later."
                    else -> "A network error occurred (Code: $code). Check connection or try another server."
                }
            }
            if (cause is UnknownHostException) {
                return "Cannot connect to the video server. Please check your internet connection."
            }
            cause = cause.cause
        }
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "Network connection failed. Please check your internet connection."

            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
                "Failed to play video. The format may not be supported on this device."

            PlaybackException.ERROR_CODE_DRM_UNSPECIFIED,
            PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED ->
                "This video is protected and cannot be played."

            else -> "An unexpected error occurred during playback."
        }
    }

    fun updateCoreState(newState: PlayerCoreState) {
        _playerCoreState.update { newState }
    }

    private fun createPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerCoreState.update { it.copy(isPlaying = isPlaying) }
                if (!isPlaying) {
                    dispatch(PlayerAction.RequestToggleControlsVisibility(isVisible = true))
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _playerCoreState.update { it.copy(playbackState = playbackState) }
                if (playbackState == Player.STATE_BUFFERING) _playerCoreState.update {
                    it.copy(isLoading = true, error = null)
                }
                if (playbackState == Player.STATE_READY) _playerCoreState.update {
                    it.copy(isLoading = false, error = null)
                }
                if (playbackState == Player.STATE_ENDED) {
                    dispatch(PlayerAction.ToggleLock(false))
                    _playerCoreState.update {
                        it.copy(isPlaying = false, isLoading = false, error = null)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val errorMessage = getReadableErrorMessage(error)
                _playerCoreState.update {
                    it.copy(
                        isPlaying = false,
                        isLoading = false,
                        error = null
                    )
                }
                _playerCoreState.update { it.copy(error = errorMessage) }
                Log.e("HlsPlayerUtils", "PlayerError: $errorMessage", error)
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
                    val surfaceToCopy: Surface? = surface.holder.surface
                    if (surfaceToCopy == null || !surfaceToCopy.isValid) {
                        Log.w("HlsPlayerUtils", "SurfaceView's surface is not valid for capture.")
                        return null
                    }

                    val bitmap = createBitmap(512, 288)
                    val latch = CountDownLatch(1)
                    PixelCopy.request(surfaceToCopy, bitmap, { result ->
                        if (result == PixelCopy.SUCCESS) {
                            latch.countDown()
                        } else {
                            Log.e("HlsPlayerUtils", "PixelCopy failed with result: $result")
                            latch.countDown()
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

    private fun setMedia(videoData: EpisodeSources) {
        try {
            _controlsState.update { ControlsState() }
            currentVideoData = videoData
            setPlaybackSpeed(1f)
            exoPlayer?.let { player ->
                pause()
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
                    _playerCoreState.update { it.copy(duration = player.duration) }
                }
            }
            dispatch(
                PlayerAction.RequestToggleControlsVisibility(isVisible = true)
            )
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to set media", e)
        }
    }

    private fun play() {
        exoPlayer?.let { player ->
            if (player.playerError != null) {
                _playerCoreState.update { it.copy(error = null) }
                player.prepare()
            }
            player.play()
            requestAudioFocus()
        }
    }

    private fun pause() {
        exoPlayer?.let {
            it.pause()
            abandonAudioFocus()
        }
    }

    private fun seekTo(positionMs: Long) {
        exoPlayer?.let { player ->
            val duration = _playerCoreState.value.duration
            val clampedPos = positionMs.coerceAtLeast(0)
                .coerceAtMost(if (duration > 0) duration else Long.MAX_VALUE)
            if (player.playerError != null) {
                player.prepare()
            }
            player.seekTo(clampedPos)
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

    fun getCacheSize(): Long {
        return exoPlayerCache.cacheSpace
    }

    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                for (key in exoPlayerCache.keys) {
                    exoPlayerCache.removeResource(key)
                }
                Log.d("HlsPlayerUtils", "All resources removed from ExoPlayer cache successfully.")
            } catch (e: Exception) {
                Log.e("HlsPlayerUtils", "Failed to clear ExoPlayer cache resources", e)
            }

            try {
                val subtitlesDir = File(applicationContext.cacheDir, "subtitles")
                if (subtitlesDir.exists()) {
                    subtitlesDir.deleteRecursively()
                    Log.d("HlsPlayerUtils", "Subtitles cache directory deleted successfully.")
                }
            } catch (e: Exception) {
                Log.e("HlsPlayerUtils", "Failed to delete subtitles cache directory", e)
            }
        }
    }

    private fun reset() {
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
        }
        currentVideoData = null
        _playerCoreState.update { PlayerCoreState() }
        _controlsState.update { ControlsState() }
        abandonAudioFocus()
    }
}