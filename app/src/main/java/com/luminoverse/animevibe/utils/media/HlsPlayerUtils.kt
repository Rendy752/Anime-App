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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
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
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerCoreState(
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val isLoading: Boolean = false,
    val error: PlaybackException? = null
)

data class PositionState(
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val bufferedPosition: Long = 0
)

data class ControlsState(
    val isControlsVisible: Boolean = false,
    val showIntroButton: Boolean = false,
    val showOutroButton: Boolean = false,
    val isLocked: Boolean = false,
    val selectedSubtitle: Track? = null,
    val playbackSpeed: Float = 1f
)

sealed class HlsPlayerAction {
    data class SetMedia(
        val videoData: EpisodeSourcesResponse,
        val isAutoPlayVideo: Boolean,
        val positionState: PositionState,
        val onReady: () -> Unit,
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
    data class UpdateWatchState(val updateStoredWatchState: (Long?, Long?, String?) -> Unit) :
        HlsPlayerAction()

    data class SkipIntro(val endTime: Long) : HlsPlayerAction()
    data class SkipOutro(val endTime: Long) : HlsPlayerAction()
    data class SetSubtitle(val track: Track?) : HlsPlayerAction()
    data class RequestToggleControlsVisibility(val isVisible: Boolean? = null) : HlsPlayerAction()
    data class ToggleLock(val isLocked: Boolean) : HlsPlayerAction()
}

private const val CONTROLS_AUTO_HIDE_DELAY_MS = 3_000L
private const val WATCH_STATE_UPDATE_INTERVAL_MS = 1_000L
private const val INTRO_OUTRO_CHECK_INTERVAL_MS = 1_000L
private const val POSITION_UPDATE_INTERVAL_MS = 500L

@Singleton
@OptIn(UnstableApi::class)
class HlsPlayerUtils @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) {
    private var exoPlayer: ExoPlayer? = null
    private var playerListener: Player.Listener? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested: Boolean = false
    private var videoSurface: Any? = null

    private val playerCoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var watchStateUpdateJob: Job? = null
    private var introOutroJob: Job? = null
    private var positionUpdateJob: Job? = null
    private var controlsHideJob: Job? = null

    private var introSkipped = false
    private var outroSkipped = true
    private var currentVideoData: EpisodeSourcesResponse? = null
    private var updateStoredWatchStateCallback: ((Long?, Long?, String?) -> Unit)? = null

    private val _playerCoreState = MutableStateFlow(PlayerCoreState())
    val playerCoreState: StateFlow<PlayerCoreState> = _playerCoreState.asStateFlow()

    private val _positionState = MutableStateFlow(PositionState())
    val positionState: StateFlow<PositionState> = _positionState.asStateFlow()

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
            exoPlayer = ExoPlayer.Builder(applicationContext)
                .setTrackSelector(trackSelector)
                .build()
                .apply {
                    playerListener = createPlayerListener()
                    addListener(playerListener!!)
                    pause()
                    Log.d("HlsPlayerUtils", "ExoPlayer initialized by Hilt")
                }
            _playerCoreState.update {
                PlayerCoreState(isPlaying = false, playbackState = Player.STATE_IDLE)
            }
            _positionState.update { PositionState() }
            _controlsState.update { ControlsState(isControlsVisible = true) }
        }
    }

    fun dispatch(action: HlsPlayerAction) {
        when (action) {
            is HlsPlayerAction.SetMedia -> setMedia(
                action.videoData,
                action.isAutoPlayVideo,
                action.positionState,
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
            is HlsPlayerAction.UpdateWatchState -> updateWatchState(action.updateStoredWatchState)
            is HlsPlayerAction.SkipIntro -> skipIntro(action.endTime)
            is HlsPlayerAction.SkipOutro -> skipOutro(action.endTime)
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
                if (isPlaying) {
                    startPositionUpdates()
                    startIntroOutroCheck()
                    startPeriodicWatchStateUpdates(updateStoredWatchStateCallback)
                } else {
                    stopPositionUpdates()
                    stopIntroOutroCheck()
                    stopPeriodicWatchStateUpdates()
                    dispatch(
                        HlsPlayerAction.RequestToggleControlsVisibility(isVisible = true)
                    )
                }
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
                    _positionState.update { it.copy(duration = exoPlayer?.duration ?: 0) }
                }
                if (playbackState == Player.STATE_ENDED) {
                    dispatch(HlsPlayerAction.ToggleLock(false))
                    _playerCoreState.update {
                        it.copy(
                            isPlaying = false, isLoading = false, error = null
                        )
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
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            bitmap.recycle()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to capture screenshot", e)
            null
        }
    }

    private fun setMedia(
        videoData: EpisodeSourcesResponse,
        isAutoPlayVideo: Boolean,
        positionState: PositionState,
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            _positionState.update { PositionState() }
            _positionState.update { positionState }
            _controlsState.update { ControlsState() }
            currentVideoData = videoData
            introSkipped = false
            outroSkipped = true
            stopIntroOutroCheck()
            stopPositionUpdates()
            stopPeriodicWatchStateUpdates()

            exoPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()
                _playerCoreState.update {
                    it.copy(isPlaying = false, playbackState = Player.STATE_IDLE)
                }

                if (videoData.sources.isNotEmpty() && videoData.sources[0].type == "hls") {
                    val mediaItemUri = videoData.sources[0].url.toUri()
                    val mediaItemBuilder = MediaItem.Builder().setUri(mediaItemUri)

                    if (videoData.tracks.any { it.kind == "captions" }) {
                        val subtitleConfigurations =
                            mutableListOf<MediaItem.SubtitleConfiguration>()
                        videoData.tracks.filter { it.kind == "captions" }.forEach { track ->
                            val subtitleConfiguration =
                                MediaItem.SubtitleConfiguration.Builder(track.file.toUri())
                                    .setMimeType(MimeTypes.TEXT_VTT)
                                    .setLanguage(track.label?.substringBefore("-")?.trim() ?: "")
                                    .setLabel(track.label?.substringBefore("-")?.trim() ?: "")
                                    .build()
                            subtitleConfigurations.add(subtitleConfiguration)
                        }
                        mediaItemBuilder.setSubtitleConfigurations(subtitleConfigurations)
                    }

                    player.setMediaItem(mediaItemBuilder.build())
                    player.prepare()
                    player.trackSelectionParameters = TrackSelectionParameters.Builder().build()
                    setSubtitle(videoData.tracks.firstOrNull { it.kind == "captions" && it.default == true })

                    playerCoroutineScope.launch {
                        while (player.playbackState != Player.STATE_READY) {
                            delay(100)
                        }
                        if (isAutoPlayVideo && (positionState.currentPosition < positionState.duration)) {
                            seekTo(positionState.currentPosition)
                        }
                        if (isAutoPlayVideo) {
                            dispatch(HlsPlayerAction.Play)
                        }
                        _positionState.update {
                            it.copy(
                                currentPosition = player.currentPosition,
                                duration = player.duration,
                                bufferedPosition = player.bufferedPosition
                            )
                        }
                        onReady()
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
            _positionState.update { it.copy(currentPosition = clampedPos) }
            _controlsState.update { it.copy(isControlsVisible = true) }
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

    private fun updateWatchState(updateStoredWatchState: (Long?, Long?, String?) -> Unit) {
        this.updateStoredWatchStateCallback = updateStoredWatchState
    }

    private fun startPeriodicWatchStateUpdates(updateStoredWatchState: ((Long?, Long?, String?) -> Unit)?) {
        if (updateStoredWatchState == null) {
            return
        }
        stopPeriodicWatchStateUpdates()
        watchStateUpdateJob = playerCoroutineScope.launch {
            while (true) {
                val player = exoPlayer
                if (player != null && player.isPlaying && player.playbackState == Player.STATE_READY && player.duration > 0 && player.currentPosition > 10_000) {
                    try {
                        withTimeout(5_000L) {
                            val position = player.currentPosition
                            val duration = player.duration.takeIf { it > 0 } ?: 0
                            val screenshot = captureScreenshot()
                            updateStoredWatchState.invoke(position, duration, screenshot)
                        }
                    } catch (e: Exception) {
                        Log.e("HlsPlayerUtils", "Failed to save periodic watch state", e)
                    }
                }
                delay(WATCH_STATE_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopPeriodicWatchStateUpdates() {
        watchStateUpdateJob?.cancel()
        watchStateUpdateJob = null
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = playerCoroutineScope.launch {
            while (true) {
                val player = exoPlayer
                if (player != null && (player.isPlaying || player.playbackState == Player.STATE_BUFFERING)) {
                    val duration = player.duration.takeIf { it > 0 } ?: 0
                    val position = player.currentPosition.coerceAtMost(duration)
                    val bufferedPosition = player.bufferedPosition.coerceAtMost(duration)
                    _positionState.update {
                        it.copy(
                            currentPosition = position,
                            duration = duration,
                            bufferedPosition = bufferedPosition
                        )
                    }
                }
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun startIntroOutroCheck() {
        if (introOutroJob?.isActive != true && currentVideoData != null) {
            stopIntroOutroCheck()
            introOutroJob = playerCoroutineScope.launch(Dispatchers.Main) {
                while (true) {
                    introOutroCheck()
                    delay(INTRO_OUTRO_CHECK_INTERVAL_MS)
                }
            }
        }
    }

    private fun introOutroCheck() {
        try {
            currentVideoData?.let { videoData ->
                exoPlayer?.let { player ->
                    val currentPositionMs = player.currentPosition
                    val intro = videoData.intro
                    val outro = videoData.outro

                    val shouldShowIntroButton = intro != null &&
                            currentPositionMs >= intro.start * 1000L &&
                            currentPositionMs <= intro.end * 1000L &&
                            !introSkipped
                    val shouldShowOutroButton = outro != null &&
                            currentPositionMs >= outro.start * 1000L &&
                            currentPositionMs <= outro.end * 1000L &&
                            !outroSkipped

                    _controlsState.update {
                        it.copy(
                            showIntroButton = shouldShowIntroButton,
                            showOutroButton = shouldShowOutroButton
                        )
                    }

                    if (intro != null && (currentPositionMs < intro.start * 1000L || currentPositionMs > intro.end * 1000L)) {
                        introSkipped = false
                    }
                    if (outro != null && (currentPositionMs < outro.start * 1000L || currentPositionMs > outro.end * 1000L)) {
                        outroSkipped = false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Intro/Outro check failed", e)
        }
    }

    private fun stopIntroOutroCheck() {
        if (introOutroJob?.isActive == true) {
            introOutroJob?.cancel()
            introOutroJob = null
        }
    }

    private fun skipIntro(endTime: Long) {
        try {
            seekTo(endTime * 1000L)
            introSkipped = true
            _controlsState.update { it.copy(showIntroButton = false) }
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to skip intro", e)
        }
    }

    private fun skipOutro(endTime: Long) {
        try {
            seekTo(endTime * 1000L)
            outroSkipped = true
            _controlsState.update { it.copy(showOutroButton = false) }
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to skip outro", e)
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
            updateStoredWatchStateCallback = null
            introSkipped = false
            outroSkipped = true

            stopPeriodicWatchStateUpdates()
            stopIntroOutroCheck()
            stopPositionUpdates()
            controlsHideJob?.cancel()
            controlsHideJob = null

            playerCoroutineScope.cancel()
            _playerCoreState.update { PlayerCoreState() }
            _controlsState.update { ControlsState() }
            _positionState.update { PositionState() }
            Log.d("HlsPlayerUtils", "HlsPlayerUtils completely released and state reset")
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to release HlsPlayerUtils", e)
        }
    }
}