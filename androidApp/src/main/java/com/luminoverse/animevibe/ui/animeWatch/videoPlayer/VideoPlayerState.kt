package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.utils.media.CaptionCue
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.media.ThumbnailCue
import com.luminoverse.animevibe.utils.media.parseCaptionCues
import com.luminoverse.animevibe.utils.media.parseThumbnailCues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import kotlin.math.abs
import kotlin.math.max

private const val FAST_FORWARD_REWIND_DEBOUNCE_MILLIS = 1000L
private const val DEFAULT_SEEK_INCREMENT = 10000L
private const val LONG_PRESS_THRESHOLD_MILLIS = 500L
private const val DOUBLE_TAP_THRESHOLD_MILLIS = 300L
private const val MAX_ZOOM_RATIO = 8f

@Stable
class VideoPlayerState(
    val player: ExoPlayer,
    val playerAction: (HlsPlayerAction) -> Unit,
    val scope: CoroutineScope,
    private val imageLoader: ImageLoader,
    private val networkDataSource: NetworkDataSource
) {
    var isFirstLoad by mutableStateOf(true)
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    var isShowNextEpisodeOverlay by mutableStateOf(false)
    var isShowSeekIndicator by mutableIntStateOf(0)
    var seekAmount by mutableLongStateOf(0L)
    var isSeeking by mutableStateOf(false)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var zoomScaleProgress by mutableFloatStateOf(1f)
    var isZooming by mutableStateOf(false)
    var speedUpText by mutableStateOf("")
    var isHolding by mutableStateOf(false)
    var showLockReminder by mutableStateOf(false)
    var isDraggingSeekBar by mutableStateOf(false)
    var dragCancelTrigger by mutableIntStateOf(0)
        private set
    var dragSeekPosition by mutableLongStateOf(0L)
    var showSettingsSheet by mutableStateOf(false)
    var showSubtitleSheet by mutableStateOf(false)
    var showPlaybackSpeedSheet by mutableStateOf(false)
    var playerSize by mutableStateOf(IntSize.Zero)
    var videoSize by mutableStateOf(player.videoSize)
    var bottomBarHeight by mutableFloatStateOf(0f)
    var showRemainingTime by mutableStateOf(false)

    var longPressJob by mutableStateOf<Job?>(null)
    private var fastForwardRewindCounter by mutableIntStateOf(0)
    private var previousPlaybackSpeed by mutableFloatStateOf(1f)
    var lastTapTime by mutableLongStateOf(0L)
    var lastTapX by mutableStateOf<Float?>(null)
    var thumbnailCues by mutableStateOf<Map<String, List<ThumbnailCue>>>(emptyMap())
        private set
    var captionCues by mutableStateOf<Map<String, List<CaptionCue>>>(emptyMap())
        private set

    fun updatePosition(position: Long) {
        _currentPosition.value = position
    }

    fun loadAndCacheThumbnails(context: Context, thumbnailUrl: String) {
        scope.launch {
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

                cues.map { it.imageUrl }.distinct().forEach { imageUrl ->
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .build()
                    imageLoader.enqueue(request)
                }
                Log.d("VideoPlayerState", "Thumbnails loaded and cached for $thumbnailUrl")

            } catch (e: Exception) {
                Log.e("VideoPlayerState", "Failed to load or cache thumbnails: ${e.message}", e)
            }
        }
    }

    fun loadAndCacheCaptions(captionUrl: String) {
        scope.launch {
            if (captionCues.containsKey(captionUrl)) return@launch
            try {
                val vttContent = networkDataSource.fetchText(captionUrl)
                if (vttContent.isNullOrEmpty()) {
                    throw IOException("Failed to fetch caption VTT content or content is empty.")
                }

                val cues = parseCaptionCues(vttContent)
                captionCues = captionCues + (captionUrl to cues)
                Log.d("VideoPlayerState", "Captions loaded and cached for $captionUrl")
            } catch (e: Exception) {
                Log.e("VideoPlayerState", "Failed to load captions: ${e.message}", e)
            }
        }
    }

    private val seekDisplayHandler = Handler(Looper.getMainLooper())
    private val seekDisplayRunnable = Runnable {
        Log.d("PlayerView", "Debounce period ended. Performing accumulated seeks.")
        val totalSeekSeconds = fastForwardRewindCounter
        val fixedSeekPerCall = (DEFAULT_SEEK_INCREMENT / 1000L).toInt()

        if (totalSeekSeconds != 0) {
            val numCalls = abs(totalSeekSeconds) / fixedSeekPerCall
            repeat(numCalls) {
                if (totalSeekSeconds > 0) playerAction(HlsPlayerAction.FastForward)
                else playerAction(HlsPlayerAction.Rewind)
            }
        }

        isShowSeekIndicator = 0
        seekAmount = 0L
        isSeeking = false
        fastForwardRewindCounter = 0
        playerAction(HlsPlayerAction.Play)
        Log.d("PlayerView", "Seek actions completed and states reset.")
    }

    val zoomToFillRatio by derivedStateOf {
        if (playerSize.width > 0 && playerSize.height > 0 && videoSize.width > 0 && videoSize.height > 0) {
            val containerAspect = playerSize.width.toFloat() / playerSize.height.toFloat()
            val videoAspect = videoSize.width.toFloat() / videoSize.height.toFloat()
            val (fittedWidth, fittedHeight) = if (videoAspect > containerAspect) {
                playerSize.width.toFloat() to playerSize.width.toFloat() / videoAspect
            } else {
                playerSize.height.toFloat() * videoAspect to playerSize.height.toFloat()
            }
            max(playerSize.width / fittedWidth, playerSize.height / fittedHeight).coerceAtLeast(1f)
        } else {
            1f
        }
    }

    fun cancelSeekBarDrag() {
        if (isDraggingSeekBar) {
            isShowSeekIndicator = 0
            seekAmount = 0L
            isSeeking = false
            fastForwardRewindCounter = 0
            isDraggingSeekBar = false
            dragCancelTrigger++
        }
    }

    fun getZoomRatioText(): String = when {
        abs(zoomScaleProgress - zoomToFillRatio) < 0.01f && zoomToFillRatio > 1f -> "Full View"
        zoomScaleProgress > 1f -> String.format(Locale.US, "%.1fx", zoomScaleProgress)
        else -> "Original"
    }

    fun resetZoom() {
        zoomScaleProgress = 1f
        playerAction(HlsPlayerAction.SetZoom(1f))
        offsetX = 0f
        offsetY = 0f
    }

    fun onDispose() {
        seekDisplayHandler.removeCallbacksAndMessages(null)
        longPressJob?.cancel()
        if (isHolding) {
            isHolding = false
            playerAction(HlsPlayerAction.SetPlaybackSpeed(previousPlaybackSpeed))
        }
        Log.d("PlayerView", "VideoPlayerState disposed, cleaning up resources.")
    }

    fun handleSingleTap(isLocked: Boolean) {
        if (!isLocked && !isHolding) {
            playerAction(HlsPlayerAction.RequestToggleControlsVisibility())
            Log.d("PlayerView", "Single tap: Toggled controls visibility")
        } else if (isLocked) {
            showLockReminder = true
            Log.d("PlayerView", "Single tap: Player is locked, showing lock reminder.")
        }
    }

    fun handleDoubleTap(
        x: Float,
        screenWidth: Float,
        coreState: PlayerCoreState,
        isLocked: Boolean
    ) {
        if (!isLocked && coreState.playbackState != Player.STATE_IDLE && !isFirstLoad) {
            Log.d("PlayerView", "Double tap at x=$x")
            val newSeekDirection = when {
                x < screenWidth * 0.4 -> -1
                x > screenWidth * 0.6 -> 1
                else -> 0
            }

            if (newSeekDirection != 0) {
                seekDisplayHandler.removeCallbacks(seekDisplayRunnable)
                playerAction(HlsPlayerAction.Pause)
                val seekIncrementSeconds = (DEFAULT_SEEK_INCREMENT / 1000L)

                if (isShowSeekIndicator != newSeekDirection && isShowSeekIndicator != 0) {
                    seekAmount = seekIncrementSeconds
                    fastForwardRewindCounter = newSeekDirection * seekIncrementSeconds.toInt()
                } else {
                    seekAmount += seekIncrementSeconds
                    fastForwardRewindCounter += newSeekDirection * seekIncrementSeconds.toInt()
                }

                isShowSeekIndicator = newSeekDirection
                isSeeking = true
                seekDisplayHandler.postDelayed(
                    seekDisplayRunnable,
                    FAST_FORWARD_REWIND_DEBOUNCE_MILLIS
                )
            } else {
                playerAction(HlsPlayerAction.Play)
                seekDisplayHandler.removeCallbacks(seekDisplayRunnable)
                if (fastForwardRewindCounter == 0) {
                    isShowSeekIndicator = 0
                    seekAmount = 0L
                    isSeeking = false
                }
                fastForwardRewindCounter = 0
            }
        }
    }

    fun handleLongPressStart(currentSpeed: Float) {
        isHolding = true
        speedUpText = "2x speed"
        previousPlaybackSpeed = currentSpeed
        playerAction(HlsPlayerAction.SetPlaybackSpeed(2f))
        Log.d("PlayerView", "Long press started: Speed set to 2x")
    }

    fun handleLongPressEnd() {
        if (isHolding) {
            isHolding = false
            speedUpText = ""
            playerAction(HlsPlayerAction.SetPlaybackSpeed(previousPlaybackSpeed))
            Log.d("PlayerView", "Long press ended")
        }
    }
}

@Composable
fun rememberVideoPlayerState(
    key: Any?,
    player: ExoPlayer,
    playerAction: (HlsPlayerAction) -> Unit,
    networkDataSource: NetworkDataSource,
    scope: CoroutineScope = rememberCoroutineScope()
): VideoPlayerState {
    val imageLoader = LocalContext.current.imageLoader
    val state = remember(key) {
        VideoPlayerState(
            player = player,
            playerAction = playerAction,
            scope = scope,
            imageLoader = imageLoader,
            networkDataSource = networkDataSource
        )
    }

    DisposableEffect(key) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(newVideoSize: VideoSize) {
                state.videoSize = newVideoSize
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                state.updatePosition(newPosition.positionMs)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            state.onDispose()
        }
    }
    return state
}

suspend fun AwaitPointerEventScope.handleGestures(
    state: VideoPlayerState,
    updatedControlsState: State<ControlsState>,
    updatedCoreState: State<PlayerCoreState>
) {
    val down = awaitFirstDown()

    state.longPressJob?.cancel()
    state.longPressJob = state.scope.launch {
        delay(LONG_PRESS_THRESHOLD_MILLIS)
        if (state.player.isPlaying && !updatedControlsState.value.isLocked) {
            state.handleLongPressStart(updatedControlsState.value.playbackSpeed)
            down.consume()
        }
    }

    var isMultiTouch = false
    var isDragging = false

    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.none { it.pressed }) {
            state.longPressJob?.cancel()
            break
        }

        if (event.changes.size > 1 && !updatedControlsState.value.isLocked) {
            if (!isMultiTouch) {
                isMultiTouch = true
                state.isZooming = true
                state.playerAction(
                    HlsPlayerAction.RequestToggleControlsVisibility(false)
                )
                state.longPressJob?.cancel()
                state.handleLongPressEnd()
            }
            val zoomChange = event.calculateZoom()
            state.zoomScaleProgress =
                (state.zoomScaleProgress * zoomChange).coerceIn(1f, MAX_ZOOM_RATIO)

            val (maxOffsetX, maxOffsetY) = calculateOffsetBounds(
                size,
                state.videoSize,
                state.zoomScaleProgress
            )
            state.offsetX = state.offsetX.coerceIn(-maxOffsetX, maxOffsetX)
            state.offsetY = state.offsetY.coerceIn(-maxOffsetY, maxOffsetY)
            event.changes.forEach { it.consume() }
        } else if (event.changes.size == 1 && !isMultiTouch && updatedControlsState.value.zoom > 1f && !updatedControlsState.value.isLocked) {
            val pan = event.calculatePan()
            if (pan.x != 0f || pan.y != 0f) {
                if (!isDragging) {
                    isDragging = true
                    state.playerAction(
                        HlsPlayerAction.RequestToggleControlsVisibility(false)
                    )
                    state.longPressJob?.cancel()
                    state.handleLongPressEnd()
                }
                val (maxOffsetX, maxOffsetY) = calculateOffsetBounds(
                    size,
                    state.videoSize,
                    state.zoomScaleProgress
                )
                state.offsetX =
                    (state.offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                state.offsetY =
                    (state.offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                event.changes.forEach { it.consume() }
            }
        }
    }

    if (isMultiTouch) {
        val halfWayRatio = (1f + state.zoomToFillRatio) / 2
        val finalZoom = if (state.zoomScaleProgress < halfWayRatio) 1f
        else if (state.zoomScaleProgress in halfWayRatio..state.zoomToFillRatio) state.zoomToFillRatio
        else state.zoomScaleProgress

        state.playerAction(HlsPlayerAction.SetZoom(finalZoom))
        state.zoomScaleProgress = finalZoom
        if (state.zoomScaleProgress <= 1.01f) {
            state.offsetX = 0f
            state.offsetY = 0f
        }
        state.isZooming = false
    } else if (!isDragging) {
        state.longPressJob?.cancel()
        val currentTime = System.currentTimeMillis()
        val tapX = down.position.x
        if (state.isHolding) {
            state.handleLongPressEnd()
        } else {
            if (currentTime - state.lastTapTime < DOUBLE_TAP_THRESHOLD_MILLIS && state.lastTapX != null && abs(
                    tapX - state.lastTapX!!
                ) < 100f
            ) {
                state.handleDoubleTap(
                    tapX,
                    size.width.toFloat(),
                    updatedCoreState.value,
                    updatedControlsState.value.isLocked
                )
            } else {
                state.handleSingleTap(updatedControlsState.value.isLocked)
            }
            state.lastTapTime = currentTime
            state.lastTapX = tapX
        }
    }
}

/**
 * A helper function to calculate the maximum allowed pan offsets based on the container size,
 * the video's intrinsic size, and the current zoom scale. This prevents panning into blank areas.
 */
fun calculateOffsetBounds(
    containerSize: IntSize,
    videoSize: VideoSize,
    scale: Float
): Pair<Float, Float> {
    if (containerSize.width == 0 || containerSize.height == 0 || videoSize.width == 0 || videoSize.height == 0 || scale <= 1f) {
        return 0f to 0f
    }

    val containerWidth = containerSize.width.toFloat()
    val containerHeight = containerSize.height.toFloat()

    val vWidth = videoSize.width.toFloat() * videoSize.pixelWidthHeightRatio
    val vHeight = videoSize.height.toFloat()

    val containerAspect = containerWidth / containerHeight
    val videoAspect = vWidth / vHeight

    val (fittedWidth, fittedHeight) = if (videoAspect > containerAspect) {
        containerWidth to (containerWidth / videoAspect)
    } else {
        (containerHeight * videoAspect) to containerHeight
    }

    val overflowX = (fittedWidth * scale - containerWidth).coerceAtLeast(0f)
    val overflowY = (fittedHeight * scale - containerHeight).coerceAtLeast(0f)

    return (overflowX / 2f) to (overflowY / 2f)
}