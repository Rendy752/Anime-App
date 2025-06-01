package com.luminoverse.animevibe.utils.media

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
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.luminoverse.animevibe.models.EpisodeSourcesResponse
import com.luminoverse.animevibe.models.Track
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
    val playbackSpeed: Float = 1f,
    val showIntroButton: Boolean = false,
    val showOutroButton: Boolean = false,
    val selectedSubtitle: Track? = null,
    val isControlsVisible: Boolean = true,
    val isLocked: Boolean = false
)

sealed class HlsPlayerAction {
    data class InitializeHlsPlayer(val context: Context) : HlsPlayerAction()
    data class SetMedia(
        val videoData: EpisodeSourcesResponse,
        val lastTimestamp: Long? = null,
        val isAutoPlayVideo: Boolean = true,
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
    data class UpdateWatchState(val updateStoredWatchState: (Long?, Long?, String?) -> Unit) :
        HlsPlayerAction()

    data class SkipIntro(val endTime: Long) : HlsPlayerAction()
    data class SkipOutro(val endTime: Long) : HlsPlayerAction()
    data class SetSubtitle(val track: Track?) : HlsPlayerAction()
    data class ToggleControlsVisibility(val isVisible: Boolean) : HlsPlayerAction()
    data class ToggleLock(val isLocked: Boolean) : HlsPlayerAction()
}

object HlsPlayerUtils {
    private var exoPlayer: ExoPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested: Boolean = false
    private var videoSurface: Any? = null

    private val playerCoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var watchStateUpdateJob: Job? = null
    private var introOutroJob: Job? = null
    private var positionUpdateJob: Job? = null

    private var introSkipped = false
    private var outroSkipped = true
    private var currentVideoData: EpisodeSourcesResponse? = null
    private var updateStoredWatchStateCallback: ((Long?, Long?, String?) -> Unit)? = null

    private val _state = MutableStateFlow(HlsPlayerState())
    val state: StateFlow<HlsPlayerState> = _state.asStateFlow()

    private const val WATCH_STATE_UPDATE_INTERVAL_MS = 1_000L
    private const val INTRO_OUTRO_CHECK_INTERVAL_MS = 1_000L
    private const val POSITION_UPDATE_INTERVAL_MS = 500L

    @OptIn(UnstableApi::class)
    fun dispatch(action: HlsPlayerAction) {
        when (action) {
            is HlsPlayerAction.InitializeHlsPlayer -> initializePlayer(action.context)
            is HlsPlayerAction.SetMedia -> setMedia(
                action.videoData,
                action.lastTimestamp,
                action.isAutoPlayVideo,
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
            is HlsPlayerAction.ToggleControlsVisibility -> toggleControlsVisibility(action.isVisible)
            is HlsPlayerAction.ToggleLock -> toggleLock(action.isLocked)
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
            val trackSelector = DefaultTrackSelector(context)
            exoPlayer = ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build()
                .apply {
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            Log.d("HlsPlayerUtils", "onIsPlayingChanged: isPlaying=$isPlaying")
                            _state.update { it.copy(isPlaying = isPlaying, error = null) }
                            if (isPlaying) {
                                requestAudioFocus()
                                startIntroOutroCheck()
                                startPositionUpdates()
                                startPeriodicWatchStateUpdates(updateStoredWatchStateCallback)
                            } else {
                                abandonAudioFocus()
                                stopIntroOutroCheck()
                                stopPositionUpdates()
                                stopPeriodicWatchStateUpdates()
                            }
                        }

                        override fun onPlaybackStateChanged(state: Int) {
                            val isReady =
                                state == Player.STATE_READY || state == Player.STATE_BUFFERING
                            _state.update {
                                it.copy(
                                    playbackState = state,
                                    isReady = isReady,
                                    duration = exoPlayer?.duration?.takeIf { it > 0 } ?: 0L
                                )
                            }

                            if (state == Player.STATE_READY) {
                                setSubtitle(currentVideoData?.tracks?.firstOrNull { track -> track.kind == "captions" && track.default == true })
                            }

                            when (state) {
                                Player.STATE_ENDED -> {
                                    _state.update { it.copy(isPlaying = false) }
                                    abandonAudioFocus()
                                    stopIntroOutroCheck()
                                    stopPositionUpdates()
                                    stopPeriodicWatchStateUpdates()
                                }

                                Player.STATE_IDLE -> {
                                    _state.update { it.copy(error = null) }
                                    abandonAudioFocus()
                                    stopIntroOutroCheck()
                                    stopPositionUpdates()
                                    stopPeriodicWatchStateUpdates()
                                }

                                Player.STATE_READY, Player.STATE_BUFFERING -> {
                                    _state.update { it.copy(error = null) }
                                }
                            }
                        }

                        override fun onPositionDiscontinuity(
                            oldPosition: Player.PositionInfo,
                            newPosition: Player.PositionInfo,
                            reason: Int
                        ) {
                            Log.d(
                                "HlsPlayerUtils",
                                "onPositionDiscontinuity: reason=$reason, newPosition=${newPosition.positionMs}ms"
                            )
                            _state.update {
                                it.copy(currentPosition = newPosition.positionMs)
                            }
                            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                                introOutroCheck()
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
                            stopIntroOutroCheck()
                            stopPositionUpdates()
                            stopPeriodicWatchStateUpdates()
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
        isAutoPlayVideo: Boolean = false,
        onReady: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            currentVideoData = videoData
            introSkipped = false
            outroSkipped = true
            _state.update {
                it.copy(
                    showIntroButton = false,
                    showOutroButton = false,
                    selectedSubtitle = null,
                    duration = 0,
                    currentPosition = 0
                )
            }
            stopIntroOutroCheck()
            stopPositionUpdates()
            stopPeriodicWatchStateUpdates()

            exoPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()
                pause()

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

                    if (!isAutoPlayVideo) {
                        pause()
                    }

                    Log.d(
                        "HlsPlayerUtils",
                        "Media set: url=$mediaItemUri, lastTimestamp=$lastTimestamp, isAutoPlayVideo=$isAutoPlayVideo"
                    )

                    val readyListener = object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) {
                                Log.d(
                                    "HlsPlayerUtils",
                                    "Player ready for media, duration=${player.duration}"
                                )
                                _state.update {
                                    it.copy(duration = player.duration.takeIf { it > 0 } ?: 0)
                                }
                                onReady()
                                if (isAutoPlayVideo) {
                                    lastTimestamp?.let { timestamp ->
                                        if (timestamp > 0 && timestamp < player.duration) {
                                            seekTo(timestamp)
                                            Log.d(
                                                "HlsPlayerUtils",
                                                "AutoPlay: seekTo timestamp=$timestamp"
                                            )
                                        }
                                    }
                                    player.play()
                                    Log.d("HlsPlayerUtils", "AutoPlay: play called")
                                }
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
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to set media", e)
            val message = "Failed to set media: ${e.message ?: "Unknown error"}"
            _state.update { it.copy(error = message) }
            onError(message)
        }
    }

    private fun play() {
        exoPlayer?.let {
            if (_state.value.isReady && !it.isPlaying) {
                it.play()
            }
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
            _state.update { it.copy(currentPosition = clampedPos) }
            introOutroCheck()
            Log.d("HlsPlayer", "seekTo: position=$clampedPos, duration=$duration")
        }
    }

    private fun fastForward() {
        exoPlayer?.let {
            val currentPosition = it.currentPosition
            val duration = it.duration
            val newPosition =
                (currentPosition + 10_000).coerceAtMost(if (duration > 0) duration else Long.MAX_VALUE)
            seekTo(newPosition)
            Log.d("HlsPlayer", "fastForward: from=$currentPosition to=$newPosition")
        }
    }

    private fun rewind() {
        exoPlayer?.let {
            try {
                val currentPosition = it.currentPosition
                val newPosition = (currentPosition - 10_000).coerceAtLeast(0)
                seekTo(newPosition)
                Log.d("HlsPlayer", "rewind: from=$currentPosition to=$newPosition")
            } catch (e: Exception) {
                Log.e("HlsPlayer", "Rewind failed", e)
            }
        }
    }

    private fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        _state.update { it.copy(playbackSpeed = speed) }
        Log.d("HlsPlayerUtils", "setPlaybackSpeed: speed=$speed")
    }

    private fun setVideoSurface(surface: Any?) {
        videoSurface = surface
        try {
            when (surface) {
                is TextureView -> exoPlayer?.setVideoTextureView(surface)
                is SurfaceView -> exoPlayer?.setVideoSurfaceView(surface)
                null -> {
                    exoPlayer?.clearVideoSurface()
                    Log.d("HlsPlayerUtils", "Video surface cleared")
                }

                else -> Log.w(
                    "HlsPlayerUtils",
                    "Unsupported video surface type: ${surface.javaClass}"
                )
            }
            Log.d("HlsPlayerUtils", "Video surface set: ${surface?.javaClass?.simpleName}")
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to set video surface", e)
        }
    }

    private fun updateWatchState(updateStoredWatchState: (Long?, Long?, String?) -> Unit) {
        this.updateStoredWatchStateCallback = updateStoredWatchState
    }

    private fun startPeriodicWatchStateUpdates(updateStoredWatchState: ((Long?, Long?, String?) -> Unit)?) {
        if (updateStoredWatchState == null) {
            Log.w(
                "HlsPlayerUtils",
                "updateStoredWatchStateCallback is null, cannot start watch state updates."
            )
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
                            Log.d(
                                "HlsPlayerUtils",
                                "Periodic watch state update: position=$position, duration=$duration, " +
                                        "screenshot=${screenshot?.take(20)}..."
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("HlsPlayerUtils", "Failed to save periodic watch state", e)
                    }
                }
                delay(WATCH_STATE_UPDATE_INTERVAL_MS)
            }
        }
        Log.d("HlsPlayerUtils", "Started periodic watch state updates")
    }

    private fun stopPeriodicWatchStateUpdates() {
        watchStateUpdateJob?.cancel()
        watchStateUpdateJob = null
        Log.d("HlsPlayerUtils", "Stopped periodic watch state updates")
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = playerCoroutineScope.launch {
            while (true) {
                val player = exoPlayer
                if (player != null && (player.isPlaying || player.playbackState == Player.STATE_BUFFERING)) {
                    val position = player.currentPosition
                    val duration = player.duration.takeIf { it > 0 } ?: 0
                    _state.update {
                        it.copy(
                            currentPosition = position,
                            duration = duration
                        )
                    }
                    Log.d(
                        "HlsPlayerUtils",
                        "Periodic Position update: position=$position, duration=$duration"
                    )
                }
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
        Log.d("HlsPlayerUtils", "Started position updates")
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
        Log.d("HlsPlayerUtils", "Stopped position updates")
    }

    private fun startIntroOutroCheck() {
        if (introOutroJob?.isActive != true && currentVideoData != null) {
            Log.d("HlsPlayerUtils", "Starting intro/outro periodic check")
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
                    val currentPositionSec = player.currentPosition / 1000
                    Log.d("HlsPlayerUtils", "Intro/Outro check: position=$currentPositionSec")

                    val intro = videoData.intro
                    val outro = videoData.outro

                    _state.update {
                        it.copy(
                            showIntroButton = intro != null && currentPositionSec in intro.start..intro.end && !introSkipped,
                            showOutroButton = outro != null && currentPositionSec in outro.start..outro.end && !outroSkipped
                        )
                    }

                    if (intro != null && (currentPositionSec < intro.start || currentPositionSec > intro.end)) {
                        introSkipped = false
                    }
                    if (outro != null && (currentPositionSec < outro.start || currentPositionSec > outro.end)) {
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
            Log.d("HlsPlayerUtils", "Stopping intro/outro periodic check")
            introOutroJob?.cancel()
            introOutroJob = null
        }
    }

    private fun skipIntro(endTime: Long) {
        try {
            seekTo(endTime * 1000L)
            introSkipped = true
            _state.update { it.copy(showIntroButton = false) }
            Log.d("HlsPlayerUtils", "Skipped intro to $endTime seconds")
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to skip intro", e)
        }
    }

    private fun skipOutro(endTime: Long) {
        try {
            seekTo(endTime * 1000L)
            outroSkipped = true
            _state.update { it.copy(showOutroButton = false) }
            Log.d("HlsPlayerUtils", "Skipped outro to $endTime seconds")
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to skip outro", e)
        }
    }

    @OptIn(UnstableApi::class)
    private fun setSubtitle(track: Track?) {
        try {
            exoPlayer?.let { player ->
                val builder = player.trackSelectionParameters.buildUpon()
                if (track == null || track.label == "None") {
                    player.trackSelectionParameters = builder.setPreferredTextLanguages().build()
                    _state.update { it.copy(selectedSubtitle = null) }
                    Log.d("HlsPlayerUtils", "Subtitles disabled")
                } else {
                    val language = track.label?.substringBefore("-")?.trim() ?: ""
                    player.trackSelectionParameters = builder.setPreferredTextLanguages(language)
                        .build()
                    _state.update { it.copy(selectedSubtitle = track) }
                    Log.d("HlsPlayerUtils", "Selected subtitle: ${track.label}")
                }
            }
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to set subtitle", e)
        }
    }

    private fun toggleControlsVisibility(isVisible: Boolean) {
        try {
            _state.update { it.copy(isControlsVisible = isVisible) }
            Log.d("HlsPlayerUtils", "Controls visibility set to $isVisible")
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to toggle controls visibility", e)
        }
    }

    private fun toggleLock(isLocked: Boolean) {
        try {
            _state.update { it.copy(isLocked = isLocked) }
            Log.d("HlsPlayerUtils", "Lock state set to $isLocked")
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to toggle lock", e)
        }
    }

    private fun requestAudioFocus() {
        try {
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
                    Log.d("HlsPlayerUtils", "Audio focus abandoned")
                }
            }
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to abandon audio focus", e)
        }
    }

    private fun release() {
        try {
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
            currentVideoData = null
            updateStoredWatchStateCallback = null
            introSkipped = false
            outroSkipped = true
            stopPeriodicWatchStateUpdates()
            stopIntroOutroCheck()
            stopPositionUpdates()
            playerCoroutineScope.cancel()
            _state.update {
                it.copy(
                    isPlaying = false,
                    playbackState = Player.STATE_IDLE,
                    error = null,
                    isReady = false,
                    currentPosition = 0,
                    duration = 0,
                    playbackSpeed = 1f,
                    showIntroButton = false,
                    showOutroButton = false,
                    selectedSubtitle = null,
                    isControlsVisible = true,
                    isLocked = false
                )
            }
        } catch (e: Exception) {
            Log.e("HlsPlayerUtils", "Failed to release player", e)
        }
    }
}