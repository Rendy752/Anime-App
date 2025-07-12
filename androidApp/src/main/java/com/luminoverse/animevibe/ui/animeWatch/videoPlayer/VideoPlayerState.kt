package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.request.ImageRequest
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.utils.media.CaptionCue
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.ThumbnailCue
import com.luminoverse.animevibe.utils.media.findActiveCaptionCues
import com.luminoverse.animevibe.utils.media.parseCaptionCues
import com.luminoverse.animevibe.utils.media.parseThumbnailCues
import com.luminoverse.animevibe.utils.media.toSha256
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private const val FAST_FORWARD_REWIND_DEBOUNCE_MILLIS = 1000L
private const val DEFAULT_SEEK_INCREMENT_MS = 10000L
const val MAX_ZOOM_SCALE = 8f

/**
 * A state holder for the video player, responsible for managing UI state, handling user interactions,
 * and coordinating with the underlying ExoPlayer instance.
 *
 * @param context The Android context.
 * @param player The ExoPlayer instance for media playback.
 * @param onPlayerAction A lambda to dispatch actions to the player (e.g., play, pause, seek).
 * @param coroutineScope The coroutine scope for launching async operations like data fetching.
 * @param imageLoader The Coil image loader for fetching and caching thumbnails.
 * @param networkDataSource The data source for fetching remote data like VTT files.
 * @param episodeDetails The complementary details of the current episode.
 * @param controlsState A state object representing the shared state of player controls.
 */
@Stable
class VideoPlayerState(
    private val context: Context,
    val player: ExoPlayer,
    val onPlayerAction: (HlsPlayerAction) -> Unit,
    val coroutineScope: CoroutineScope,
    private val imageLoader: ImageLoader,
    private val networkDataSource: NetworkDataSource,
    val episodeDetails: EpisodeDetailComplement,
    val controlsState: State<ControlsState>
) {

    // region Core Playback State
    /** Tracks if the player is loading for the very first time. */
    var isInitialLoading by mutableStateOf(true)
    private val _currentPositionMs = MutableStateFlow(0L)

    /** The current playback position in milliseconds, exposed as a StateFlow. */
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    /** True if the seek bar is being dragged, used to show a buffering indicator. */
    var isBufferingFromSeeking by mutableStateOf(false)

    /** The active caption cues for the current playback position. */
    val activeCaptionCue: List<CaptionCue>?
        @Composable get() {
            val currentPosition = currentPositionMs.collectAsStateWithLifecycle().value
            return controlsState.value.selectedSubtitle?.file?.let { url ->
                captionCues[url]?.let { cues ->
                    findActiveCaptionCues(
                        if (isDraggingSeekBar) dragSeekPositionMs else currentPosition,
                        cues
                    )
                }
            }
        }
    // endregion

    // region UI Overlays & Sheets State
    /** Manages the visibility of the "Resume Playback" overlay. */
    var shouldShowResumeOverlay by mutableStateOf(false)

    /** Manages the visibility of the landscape episode list. */
    var showLandscapeEpisodeList by mutableStateOf(false)

    /** Manages the visibility of the "Next Episode" overlay. */
    var shouldShowNextEpisodeOverlay by mutableStateOf(false)

    /** Manages the visibility of the settings bottom sheet. */
    var showSettingsSheet by mutableStateOf(false)

    /** Manages the visibility of the subtitle selection bottom sheet. */
    var showSubtitleSheet by mutableStateOf(false)

    /** Manages the visibility of the playback speed selection bottom sheet. */
    var showPlaybackSpeedSheet by mutableStateOf(false)

    /** Manages the visibility of the lock reminder icon. */
    var showLockReminder by mutableStateOf(false)

    /** Toggles between showing remaining time vs. total duration in the seek bar. */
    var showRemainingTime by mutableStateOf(false)
    // endregion

    // region Gesture & Interaction State
    /** The direction of the seek indicator: -1 for rewind, 1 for forward, 0 for hidden. */
    var seekIndicatorDirection by mutableIntStateOf(0)

    /** The accumulated seek amount in seconds during a double-tap seek gesture. */
    var seekAmountSeconds by mutableLongStateOf(0L)

    /** True if a seek operation (e.g., from double-tap) is in progress. */
    var isSeeking by mutableStateOf(false)

    /** The horizontal offset for pan gestures when zoomed in. */
    var panOffsetX by mutableFloatStateOf(0f)

    /** The vertical offset for pan gestures when zoomed in. */
    var panOffsetY by mutableFloatStateOf(0f)

    /** The current zoom scale of the player view. */
    var zoomScale by mutableFloatStateOf(1f)

    /** True if a zoom gesture is currently active. */
    var isZooming by mutableStateOf(false)

    /** The text displayed in the speed-up indicator (e.g., "2x speed"). */
    var speedUpIndicatorText by mutableStateOf("")

    /** True if the user is performing a long press to speed up playback. */
    var isSpeedingUpWithLongPress by mutableStateOf(false)

    /** True if the user is currently dragging the seek bar. */
    var isDraggingSeekBar by mutableStateOf(false)

    /** The seek position from the drag gesture, used for thumbnail previews. */
    var dragSeekPositionMs by mutableLongStateOf(0L)

    /** A trigger to notify observers that a seek bar drag has been cancelled. Incremented on each cancellation. */
    var seekBarDragCancellationId by mutableIntStateOf(0)
        private set

    /** The status text for autoplay (e.g., "Autoplay is ON"). Disappears after a delay. */
    var autoplayStatusText by mutableStateOf<String?>(null)

    /** A trigger to show the autoplay status text. Incremented on each toggle. */
    var autoplayStatusId by mutableIntStateOf(0)

    // endregion

    // region Private Interaction Logic State
    private var fastForwardRewindCounterSeconds by mutableIntStateOf(0)
    private var previousPlaybackSpeed by mutableFloatStateOf(1f)
    internal var lastTapTimestamp by mutableLongStateOf(0L)
    internal var lastTapPositionX by mutableStateOf<Float?>(null)
    internal var longPressJob by mutableStateOf<Job?>(null)
    // endregion

    // region Data & Resource State
    var thumbnailCues by mutableStateOf<Map<String, List<ThumbnailCue>>>(emptyMap())
    private var captionCues by mutableStateOf<Map<String, List<CaptionCue>>>(emptyMap())
    // endregion

    // region Player & Video Dimensions
    /** The size of the player container View in pixels. */
    var playerContainerSize by mutableStateOf(IntSize.Zero)

    /** The size of the decoded video in pixels. */
    var videoResolution by mutableStateOf(player.videoSize)

    /** The measured height of the bottom control bar, used for subtitle padding. */
    var bottomBarHeight by mutableFloatStateOf(0f)

    /** The calculated zoom ratio required to make the video fill the container. */
    val zoomToFillRatio by derivedStateOf {
        if (playerContainerSize.width > 0 && playerContainerSize.height > 0 && videoResolution.width > 0 && videoResolution.height > 0) {
            val containerAspect =
                playerContainerSize.width.toFloat() / playerContainerSize.height.toFloat()
            val videoAspect = videoResolution.width.toFloat() / videoResolution.height.toFloat()
            val (fittedWidth, fittedHeight) = if (videoAspect > containerAspect) {
                playerContainerSize.width.toFloat() to playerContainerSize.width.toFloat() / videoAspect
            } else {
                playerContainerSize.height.toFloat() * videoAspect to playerContainerSize.height.toFloat()
            }
            max(
                playerContainerSize.width / fittedWidth,
                playerContainerSize.height / fittedHeight
            ).coerceAtLeast(1f)
        } else {
            1f
        }
    }
    // endregion

    // region Internal Handlers
    private val seekDisplayHandler = Handler(Looper.getMainLooper())
    private val seekDisplayRunnable = Runnable {
        val totalSeekSeconds = fastForwardRewindCounterSeconds
        val fixedSeekPerCall = (DEFAULT_SEEK_INCREMENT_MS / 1000L).toInt()

        if (totalSeekSeconds != 0) {
            val numCalls = abs(totalSeekSeconds) / fixedSeekPerCall
            repeat(numCalls) {
                if (totalSeekSeconds > 0) onPlayerAction(HlsPlayerAction.FastForward)
                else onPlayerAction(HlsPlayerAction.Rewind)
            }
        }

        seekIndicatorDirection = 0
        seekAmountSeconds = 0L
        isSeeking = false
        fastForwardRewindCounterSeconds = 0
        onPlayerAction(HlsPlayerAction.Play)
    }
    // endregion

    // region Public Functions

    /** Updates the current playback position. Called from a listener. */
    fun updatePosition(positionMs: Long) {
        _currentPositionMs.value = positionMs
    }

    /**
     * Downloads and parses a VTT thumbnail file, then pre-caches the associated thumbnail images.
     * Cues are stored in memory to prevent re-fetching.
     *
     * @param context The Android context.
     * @param thumbnailUrl The URL of the VTT thumbnail file.
     */
    fun loadAndCacheThumbnails(context: Context, thumbnailUrl: String) {
        coroutineScope.launch {
            if (thumbnailCues.containsKey(thumbnailUrl)) return@launch

            try {
                val vttContent = networkDataSource.fetchText(thumbnailUrl)
                if (vttContent.isNullOrEmpty()) {
                    throw IOException("Failed to fetch thumbnail VTT content or content is empty.")
                }

                val cues = withContext(Dispatchers.Default) {
                    val baseUrl = thumbnailUrl.substringBeforeLast("/") + "/"
                    parseThumbnailCues(vttContent, baseUrl)
                }

                thumbnailCues = thumbnailCues + (thumbnailUrl to cues)

                // Pre-warm the Coil cache with all thumbnail images
                cues.map { it.imageUrl }.distinct().forEach { imageUrl ->
                    val request = ImageRequest.Builder(context).data(imageUrl).build()
                    imageLoader.enqueue(request)
                }
                Log.d("VideoPlayerState", "Thumbnails loaded and cached for $thumbnailUrl")
            } catch (e: Exception) {
                Log.e("VideoPlayerState", "Failed to load or cache thumbnails: ${e.message}", e)
            }
        }
    }

    /**
     * Downloads and parses a VTT caption file. The file is cached locally on disk
     * to avoid re-fetching on subsequent loads.
     *
     * @param captionUrl The URL of the VTT caption file.
     */
    fun loadAndCacheCaptions(captionUrl: String) {
        coroutineScope.launch {
            if (captionCues.containsKey(captionUrl)) return@launch

            val subtitleFile = getSubtitleFile(captionUrl)
            var vttContent: String? = null

            // Try reading from local cache first
            if (subtitleFile.exists()) {
                try {
                    vttContent = withContext(Dispatchers.IO) { subtitleFile.readText() }
                } catch (e: Exception) {
                    Log.e("VideoPlayerState", "Failed to read local caption file: ${e.message}", e)
                }
            }

            // If not in cache, fetch from network and save to cache
            if (vttContent == null) {
                try {
                    vttContent = networkDataSource.fetchText(captionUrl)
                    if (!vttContent.isNullOrEmpty()) {
                        withContext(Dispatchers.IO) { subtitleFile.writeText(vttContent) }
                    } else {
                        throw IOException("Fetched caption content is null or empty.")
                    }
                } catch (e: Exception) {
                    Log.e(
                        "VideoPlayerState",
                        "Failed to fetch captions from network: ${e.message}",
                        e
                    )
                    return@launch
                }
            }

            // Parse the VTT content into cues
            try {
                val cues = parseCaptionCues(vttContent)
                captionCues = captionCues + (captionUrl to cues)
            } catch (e: Exception) {
                Log.e("VideoPlayerState", "Failed to parse caption VTT content: ${e.message}", e)
            }
        }
    }

    /** Resets the zoom and pan to their default state (1x scale, no offset). */
    fun resetZoom() {
        zoomScale = 1f
        onPlayerAction(HlsPlayerAction.SetZoom(1f))
        panOffsetX = 0f
        panOffsetY = 0f
    }

    /** Returns a user-friendly string representation of the current zoom ratio. */
    fun getZoomRatioText(): String = when {
        abs(zoomScale - zoomToFillRatio) < 0.01f && zoomToFillRatio > 1f -> "Full View"
        zoomScale > 1f -> String.format(Locale.US, "%.1fx", zoomScale)
        else -> "Original"
    }

    /** Immediately hides indicators and resets state related to seek bar dragging. */
    fun cancelSeekBarDrag() {
        if (isDraggingSeekBar) {
            seekIndicatorDirection = 0
            seekAmountSeconds = 0L
            isSeeking = false
            fastForwardRewindCounterSeconds = 0
            isDraggingSeekBar = false
            seekBarDragCancellationId++
        }
    }

    /** Toggles the state of autoplay for the next episode and triggers a status message. */
    fun onAutoplayNextEpisodeToggle(enabled: Boolean) {
        autoplayStatusText = if (enabled) "Autoplay is ON" else "Autoplay is OFF"
        autoplayStatusId++
    }

    /**
     * Cleans up resources like coroutine jobs and pending handler messages
     * when the state holder is disposed.
     */
    fun onDispose() {
        seekDisplayHandler.removeCallbacksAndMessages(null)
        longPressJob?.cancel()
        if (isSpeedingUpWithLongPress) {
            isSpeedingUpWithLongPress = false
            onPlayerAction(HlsPlayerAction.SetPlaybackSpeed(previousPlaybackSpeed))
        }
    }

    // endregion

    // region Gesture Handling Logic (called from pointer input handler)

    /** Handles a single tap gesture, typically toggling control visibility. */
    internal fun handleSingleTap(isLocked: Boolean) {
        if (!isLocked && !isSpeedingUpWithLongPress) {
            onPlayerAction(HlsPlayerAction.RequestToggleControlsVisibility())
        } else if (isLocked) {
            showLockReminder = true
        }
    }

    /** Handles a double tap gesture for fast forward or rewind. */
    internal fun handleDoubleTap(tapPositionX: Float, screenWidth: Float, isLocked: Boolean) {
        if (isLocked) return

        val newSeekDirection = when {
            tapPositionX < screenWidth * 0.4 -> -1 // Rewind
            tapPositionX > screenWidth * 0.6 -> 1  // Forward
            else -> 0
        }

        if (newSeekDirection != 0) {
            seekDisplayHandler.removeCallbacks(seekDisplayRunnable)
            onPlayerAction(HlsPlayerAction.Pause)
            val seekIncrementSeconds = (DEFAULT_SEEK_INCREMENT_MS / 1000L)

            // If direction changes, reset the counter. Otherwise, accumulate.
            if (seekIndicatorDirection != newSeekDirection && seekIndicatorDirection != 0) {
                seekAmountSeconds = seekIncrementSeconds
                fastForwardRewindCounterSeconds = newSeekDirection * seekIncrementSeconds.toInt()
            } else {
                seekAmountSeconds += seekIncrementSeconds
                fastForwardRewindCounterSeconds += newSeekDirection * seekIncrementSeconds.toInt()
            }

            seekIndicatorDirection = newSeekDirection
            isSeeking = true
            seekDisplayHandler.postDelayed(seekDisplayRunnable, FAST_FORWARD_REWIND_DEBOUNCE_MILLIS)
        } else {
            // Tapped in the center, cancel any pending seek
            onPlayerAction(HlsPlayerAction.Play)
            seekDisplayHandler.removeCallbacks(seekDisplayRunnable)
            if (fastForwardRewindCounterSeconds == 0) {
                seekIndicatorDirection = 0
                seekAmountSeconds = 0L
                isSeeking = false
            }
            fastForwardRewindCounterSeconds = 0
        }
    }

    /** Initiates the 2x speed-up on a long press gesture. */
    internal fun handleLongPressStart(currentSpeed: Float) {
        isSpeedingUpWithLongPress = true
        speedUpIndicatorText = "2x speed"
        previousPlaybackSpeed = currentSpeed
        onPlayerAction(HlsPlayerAction.SetPlaybackSpeed(2f))
    }

    /** Reverts the playback speed when the long press is released. */
    internal fun handleLongPressEnd() {
        if (isSpeedingUpWithLongPress) {
            isSpeedingUpWithLongPress = false
            speedUpIndicatorText = ""
            onPlayerAction(HlsPlayerAction.SetPlaybackSpeed(previousPlaybackSpeed))
        }
    }

    // endregion

    // region Private Helper Functions
    private fun getSubtitlesCacheDir(): File {
        return File(context.cacheDir, "subtitles").also { it.mkdirs() }
    }

    private fun getSubtitleFile(url: String): File {
        val fileName = "${url.toSha256()}.vtt"
        return File(getSubtitlesCacheDir(), fileName)
    }
    // endregion
}