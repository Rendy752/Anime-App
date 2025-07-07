package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.OrientationEventListener
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.ui.main.PlayerDisplayMode
import com.luminoverse.animevibe.utils.SystemBarsUtils
import com.luminoverse.animevibe.utils.media.BoundsUtils.calculateOffsetBounds
import com.luminoverse.animevibe.utils.media.CaptionCue
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.media.ThumbnailCue
import com.luminoverse.animevibe.utils.media.findActiveCaptionCues
import com.luminoverse.animevibe.utils.media.parseCaptionCues
import com.luminoverse.animevibe.utils.media.parseThumbnailCues
import com.luminoverse.animevibe.utils.media.toSha256
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.abs
import kotlin.math.max

private const val FAST_FORWARD_REWIND_DEBOUNCE_MILLIS = 1000L
private const val DEFAULT_SEEK_INCREMENT = 10000L
private const val LONG_PRESS_THRESHOLD_MILLIS = 500L
private const val DOUBLE_TAP_THRESHOLD_MILLIS = 300L
private const val MAX_ZOOM_RATIO = 8f

private enum class PhysicalOrientation {
    PORTRAIT, LANDSCAPE, REVERSE_LANDSCAPE, UNKNOWN;

    fun isLandscape(): Boolean {
        return this == LANDSCAPE || this == REVERSE_LANDSCAPE
    }
}

@Stable
class VideoPlayerState(
    private val context: Context,
    val player: ExoPlayer,
    val playerAction: (HlsPlayerAction) -> Unit,
    val scope: CoroutineScope,
    private val imageLoader: ImageLoader,
    private val networkDataSource: NetworkDataSource,
    val episodeDetailComplement: EpisodeDetailComplement,
    val controlsState: State<ControlsState>
) {
    var isFirstLoad by mutableStateOf(true)
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    var isShowResume by mutableStateOf(false)
    var isShowNextEpisodeOverlay by mutableStateOf(false)
    var isShowSeekIndicator by mutableIntStateOf(0)
    var seekAmount by mutableLongStateOf(0L)
    var isSeeking by mutableStateOf(false)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var zoomScaleProgress by mutableFloatStateOf(1f)
    var isZooming by mutableStateOf(false)
    var speedUpText by mutableStateOf("")
    var autoplayNextEpisodeStatusText by mutableStateOf<String?>(null)
    var autoplayNextEpisodeStatusTrigger by mutableIntStateOf(0)
    var isHolding by mutableStateOf(false)
    var showLockReminder by mutableStateOf(false)
    var isBufferingFromSeeking by mutableStateOf(false)
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

    var isAutoplayNextEpisode by mutableStateOf(false)
    val landscapeDragOffset = Animatable(0f)

    val activeCaptionCue: List<CaptionCue>?
        @Composable get() {
            val currentPos = currentPosition.collectAsStateWithLifecycle().value
            return controlsState.value.selectedSubtitle?.file?.let { url ->
                captionCues[url]?.let { cues ->
                    findActiveCaptionCues(currentPos, cues)
                }
            }
        }

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

    private fun getSubtitlesCacheDir(): File {
        return File(context.cacheDir, "subtitles").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    private fun getSubtitleFile(url: String): File {
        val fileName = "${url.toSha256()}.vtt"
        return File(getSubtitlesCacheDir(), fileName)
    }

    fun loadAndCacheCaptions(captionUrl: String) {
        scope.launch {
            if (captionCues.containsKey(captionUrl)) return@launch

            val subtitleFile = getSubtitleFile(captionUrl)
            var vttContent: String? = null

            if (subtitleFile.exists()) {
                try {
                    vttContent = withContext(Dispatchers.IO) {
                        subtitleFile.readText()
                    }
                    Log.d("VideoPlayerState", "Loaded captions from local cache for $captionUrl")
                } catch (e: Exception) {
                    Log.e("VideoPlayerState", "Failed to read local caption file: ${e.message}", e)
                }
            }

            if (vttContent == null) {
                try {
                    vttContent = networkDataSource.fetchText(captionUrl)
                    if (!vttContent.isNullOrEmpty()) {
                        withContext(Dispatchers.IO) {
                            subtitleFile.writeText(vttContent)
                        }
                        Log.d(
                            "VideoPlayerState",
                            "Fetched captions from network and cached locally for $captionUrl"
                        )
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

            try {
                val cues = parseCaptionCues(vttContent)
                captionCues = captionCues + (captionUrl to cues)
            } catch (e: Exception) {
                Log.e("VideoPlayerState", "Failed to parse caption VTT content: ${e.message}", e)
            }
        }
    }

    private val seekDisplayHandler = Handler(Looper.getMainLooper())
    private val seekDisplayRunnable = Runnable {
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
    }

    fun handleSingleTap(isLocked: Boolean) {
        if (!isLocked && !isHolding) {
            playerAction(HlsPlayerAction.RequestToggleControlsVisibility())
        } else if (isLocked) {
            showLockReminder = true
        }
    }

    fun handleDoubleTap(
        x: Float,
        screenWidth: Float,
        isLocked: Boolean
    ) {
        if (!isLocked) {
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
    }

    fun handleLongPressEnd() {
        if (isHolding) {
            isHolding = false
            speedUpText = ""
            playerAction(HlsPlayerAction.SetPlaybackSpeed(previousPlaybackSpeed))
        }
    }

    fun onAutoplayNextEpisodeToggle(enabled: Boolean) {
        isAutoplayNextEpisode = enabled
        autoplayNextEpisodeStatusText = if (enabled) "Autoplay is ON" else "Autoplay is OFF"
        autoplayNextEpisodeStatusTrigger++
    }

    fun onVerticalDrag(dy: Float, isLandscape: Boolean, onVerticalDrag: (Float) -> Unit) {
        if (isLandscape) {
            scope.launch {
                val screenHeight = playerSize.height.toFloat()
                val maxDrag = screenHeight * 0.5f
                landscapeDragOffset.snapTo((landscapeDragOffset.value + dy).coerceIn(0f, maxDrag))
            }
        } else {
            onVerticalDrag(dy)
        }
    }

    fun onDragEnd(
        flingVelocity: Float,
        isLandscape: Boolean,
        onDragEnd: (flingVelocity: Float) -> Unit,
        setPlayerDisplayMode: (PlayerDisplayMode) -> Unit
    ) {
        if (isLandscape) {
            val screenHeight = playerSize.height.toFloat()
            val threshold = screenHeight * 0.5f
            if (landscapeDragOffset.value >= threshold) {
                setPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN_PORTRAIT)
            } else {
                scope.launch {
                    landscapeDragOffset.animateTo(0f, animationSpec = spring())
                }
            }
        } else {
            onDragEnd(flingVelocity)
        }
    }
}

@Composable
fun rememberVideoPlayerState(
    key: Any?,
    player: ExoPlayer,
    playerAction: (HlsPlayerAction) -> Unit,
    networkDataSource: NetworkDataSource,
    episodeDetailComplement: EpisodeDetailComplement,
    controlsStateFlow: StateFlow<ControlsState>,
    coreState: PlayerCoreState,
    displayMode: PlayerDisplayMode,
    setPlayerDisplayMode: (PlayerDisplayMode) -> Unit,
    setSideSheetVisibility: (Boolean) -> Unit,
    isLandscape: Boolean,
    pipEndDestinationPx: Offset,
    pipEndSizePx: IntSize,
    onMaxDragAmountCalculated: (Float) -> Unit,
    updateStoredWatchState: (Long?, Long?, String?) -> Unit,
    captureScreenshot: suspend () -> String?,
    scope: CoroutineScope = rememberCoroutineScope()
): VideoPlayerState {
    val imageLoader = LocalContext.current.imageLoader
    val context = LocalContext.current
    val activity = context as? Activity

    val controlsState = controlsStateFlow.collectAsStateWithLifecycle()

    val state = remember(key) {
        VideoPlayerState(
            context = context,
            player = player,
            playerAction = playerAction,
            scope = scope,
            imageLoader = imageLoader,
            networkDataSource = networkDataSource,
            episodeDetailComplement = episodeDetailComplement,
            controlsState = controlsState
        )
    }

    LaunchedEffect(state.playerSize) {
        val playerHeight = state.playerSize.height.toFloat()
        val finalTranslationY = if (playerHeight > 0f) {
            (pipEndDestinationPx.y + pipEndSizePx.height / 2f) - (playerHeight / 2f)
        } else {
            Float.POSITIVE_INFINITY
        }
        if (finalTranslationY != Float.POSITIVE_INFINITY) {
            onMaxDragAmountCalculated(finalTranslationY)
        }
    }

    LaunchedEffect(episodeDetailComplement) {
        val thumbnailTrackUrl =
            episodeDetailComplement.sources.tracks.find { it.kind == "thumbnails" }?.file
        if (thumbnailTrackUrl != null) {
            state.loadAndCacheThumbnails(context, thumbnailTrackUrl)
        }
        state.isShowResume = episodeDetailComplement.lastTimestamp != null
    }

    LaunchedEffect(controlsState.value.selectedSubtitle) {
        controlsState.value.selectedSubtitle?.file?.let {
            state.loadAndCacheCaptions(it)
        }
    }

    LaunchedEffect(state.autoplayNextEpisodeStatusTrigger) {
        if (state.autoplayNextEpisodeStatusTrigger > 0) {
            delay(3000)
            state.autoplayNextEpisodeStatusText = null
        }
    }

    LaunchedEffect(coreState.isPlaying, coreState.playbackState) {
        val duration = player.duration.coerceAtLeast(0L)
        if (coreState.isPlaying) {
            state.isShowResume = false
            state.isShowNextEpisodeOverlay = false
            if (state.isFirstLoad && coreState.playbackState == Player.STATE_READY) {
                state.isFirstLoad = false
            }
            while (isActive) {
                val currentPosition = player.currentPosition
                state.updatePosition(currentPosition)
                if (player.duration > 0 && currentPosition >= 10_000) {
                    val position = currentPosition.coerceAtLeast(0L)
                    updateStoredWatchState(position, duration, captureScreenshot())
                }
                delay(500)
            }
        } else {
            if (coreState.playbackState == Player.STATE_ENDED) {
                state.isShowResume = false
                state.isShowNextEpisodeOverlay = true
                state.updatePosition(duration)
                updateStoredWatchState(duration, duration, captureScreenshot())
            }
        }
    }

    LaunchedEffect(state.showLockReminder, controlsState.value.isLocked) {
        if (state.showLockReminder && controlsState.value.isLocked) {
            delay(3000)
            state.showLockReminder = false
        }
    }

    LaunchedEffect(state.isDraggingSeekBar, coreState.playbackState) {
        state.isBufferingFromSeeking =
            if (state.isDraggingSeekBar && (coreState.playbackState == Player.STATE_READY || coreState.playbackState == Player.STATE_BUFFERING)) {
                true
            } else if (coreState.playbackState != Player.STATE_BUFFERING) {
                false
            } else {
                state.isBufferingFromSeeking
            }
    }

    LaunchedEffect(isLandscape, state.playerSize, state.zoomScaleProgress) {
        val playerSize = state.playerSize
        val videoSize = state.videoSize
        val scale = state.zoomScaleProgress

        if (playerSize.width > 0 && playerSize.height > 0 && videoSize.width > 0 && videoSize.height > 0 && scale > 1f) {
            val containerWidth = playerSize.width.toFloat()
            val containerHeight = playerSize.height.toFloat()
            val containerAspect = containerWidth / containerHeight
            val videoAspect = videoSize.width.toFloat() / videoSize.height.toFloat()

            val fittedVideoSize = if (videoAspect > containerAspect) {
                Size(width = containerWidth, height = containerWidth / videoAspect)
            } else {
                Size(width = containerHeight * videoAspect, height = containerHeight)
            }

            val (maxOffsetX, maxOffsetY) = calculateOffsetBounds(
                containerSize = playerSize,
                imageSize = fittedVideoSize,
                scale = scale
            )

            state.offsetX = state.offsetX.coerceIn(-maxOffsetX, maxOffsetX)
            state.offsetY = state.offsetY.coerceIn(-maxOffsetY, maxOffsetY)
        }
    }

    var physicalOrientation by remember { mutableStateOf(PhysicalOrientation.UNKNOWN) }
    val contentResolver = context.contentResolver
    var isSystemAutoRotateEnabled by remember {
        mutableStateOf(
            Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        )
    }

    DisposableEffect(contentResolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                isSystemAutoRotateEnabled = Settings.System.getInt(
                    contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    0
                ) == 1
            }
        }
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
            true,
            observer
        )
        onDispose {
            contentResolver.unregisterContentObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        val orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }
                physicalOrientation = when (orientation) {
                    in 345..359, in 0..15 -> PhysicalOrientation.PORTRAIT
                    in 75..105 -> PhysicalOrientation.REVERSE_LANDSCAPE
                    in 255..285 -> PhysicalOrientation.LANDSCAPE
                    else -> physicalOrientation
                }
            }
        }
        orientationEventListener.enable()
        onDispose {
            orientationEventListener.disable()
        }
    }

    LaunchedEffect(displayMode, controlsState.value.isLocked, isSystemAutoRotateEnabled) {
        setSideSheetVisibility(false)
        activity ?: return@LaunchedEffect
        val newOrientation = when (displayMode) {
            PlayerDisplayMode.FULLSCREEN_LANDSCAPE -> {
                if (controlsState.value.isLocked) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else if (isSystemAutoRotateEnabled) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }

            PlayerDisplayMode.FULLSCREEN_PORTRAIT -> {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            else -> {
                if (isSystemAutoRotateEnabled) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }

        if (activity.requestedOrientation != newOrientation) {
            activity.requestedOrientation = newOrientation
        }
    }

    LaunchedEffect(physicalOrientation, controlsState.value.isLocked, isSystemAutoRotateEnabled) {
        if (controlsState.value.isLocked || !isSystemAutoRotateEnabled) {
            return@LaunchedEffect
        }

        if (physicalOrientation.isLandscape() && displayMode == PlayerDisplayMode.FULLSCREEN_PORTRAIT) {
            setPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN_LANDSCAPE)
        } else if (physicalOrientation == PhysicalOrientation.PORTRAIT && displayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE) {
            setPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN_PORTRAIT)
        }
    }

    LaunchedEffect(isLandscape, displayMode) {
        if (displayMode in listOf(PlayerDisplayMode.PIP, PlayerDisplayMode.SYSTEM_PIP)) {
            SystemBarsUtils.setSystemBarsVisibility(activity, false)
            return@LaunchedEffect
        }

        if (isLandscape) {
            SystemBarsUtils.setSystemBarsVisibility(activity, true)
        } else {
            SystemBarsUtils.setSystemBarsVisibility(activity, false)
            state.landscapeDragOffset.snapTo(0f)
        }
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
    isPlayerDisplayFullscreen: Boolean,
    isLandscape: Boolean,
    topPaddingPx: Float,
    onVerticalDrag: (Float) -> Unit,
    onDragEnd: (flingVelocity: Float) -> Unit
) {
    val down: PointerInputChange = awaitFirstDown()
    val isDragInBarrier = isLandscape && down.position.y < topPaddingPx

    state.longPressJob?.cancel()
    state.longPressJob = state.scope.launch {
        delay(LONG_PRESS_THRESHOLD_MILLIS)
        if (state.player.isPlaying && !state.controlsState.value.isLocked && isPlayerDisplayFullscreen) {
            state.handleLongPressStart(state.controlsState.value.playbackSpeed)
            down.consume()
        }
    }

    var isMultiTouch = false
    var isPanning = false
    var verticalDragConsumed = false
    var dragStarted = false
    var lastVelocityY = 0f

    while (true) {
        val event = awaitPointerEvent()
        val pointers = event.changes
        if (pointers.none { it.pressed }) {
            state.longPressJob?.cancel()
            break
        }

        if (state.controlsState.value.isLocked) {
            pointers.forEach { it.consume() }
            continue
        }

        if (pointers.size > 1 && isPlayerDisplayFullscreen) {
            if (verticalDragConsumed) {
                onDragEnd(0f)
                verticalDragConsumed = false
                dragStarted = false
            }

            if (!isMultiTouch) {
                isMultiTouch = true
                state.isZooming = true
                state.longPressJob?.cancel()
                state.handleLongPressEnd()
                isPanning = false
                state.playerAction(HlsPlayerAction.RequestToggleControlsVisibility(false))
            }
            val zoomChange = event.calculateZoom()
            state.zoomScaleProgress =
                (state.zoomScaleProgress * zoomChange).coerceIn(1f, MAX_ZOOM_RATIO)

            if (state.videoSize.width > 0 && state.videoSize.height > 0) {
                val containerWidth = size.width.toFloat()
                val containerHeight = size.height.toFloat()
                val containerAspect = containerWidth / containerHeight
                val videoAspect = state.videoSize.width.toFloat() / state.videoSize.height.toFloat()

                val fittedVideoSize = if (videoAspect > containerAspect) {
                    Size(width = containerWidth, height = containerWidth / videoAspect)
                } else {
                    Size(width = containerHeight * videoAspect, height = containerHeight)
                }

                val (maxOffsetX, maxOffsetY) = calculateOffsetBounds(
                    containerSize = size,
                    imageSize = fittedVideoSize,
                    scale = state.zoomScaleProgress
                )
                state.offsetX = state.offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                state.offsetY = state.offsetY.coerceIn(-maxOffsetY, maxOffsetY)
            }

            pointers.forEach { it.consume() }
            continue
        }

        if (pointers.size == 1 && !isMultiTouch && isPlayerDisplayFullscreen) {
            val change = pointers.first()

            if (state.controlsState.value.zoom > 1f) {
                val pan = event.calculatePan()
                if (pan.x != 0f || pan.y != 0f) {
                    if (!isPanning) {
                        isPanning = true
                        state.playerAction(HlsPlayerAction.RequestToggleControlsVisibility(false))
                        state.longPressJob?.cancel()
                        state.handleLongPressEnd()
                    }
                    if (state.videoSize.width > 0 && state.videoSize.height > 0) {
                        val containerWidth = size.width.toFloat()
                        val containerHeight = size.height.toFloat()
                        val containerAspect = containerWidth / containerHeight
                        val videoAspect =
                            state.videoSize.width.toFloat() / state.videoSize.height.toFloat()

                        val fittedVideoSize = if (videoAspect > containerAspect) {
                            Size(width = containerWidth, height = containerWidth / videoAspect)
                        } else {
                            Size(width = containerHeight * videoAspect, height = containerHeight)
                        }

                        val (maxOffsetX, maxOffsetY) = calculateOffsetBounds(
                            containerSize = size,
                            imageSize = fittedVideoSize,
                            scale = state.zoomScaleProgress
                        )
                        state.offsetX = (state.offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                        state.offsetY = (state.offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    }
                    change.consume()
                }
            } else {
                val positionChangeOffset = change.position - change.previousPosition
                val dy = positionChangeOffset.y
                val dx = positionChangeOffset.x

                if (!dragStarted && abs(dy) > abs(dx) && abs(dy) > 1f && !isDragInBarrier) {
                    dragStarted = true
                    verticalDragConsumed = true
                    state.longPressJob?.cancel()
                    state.handleLongPressEnd()
                }

                if (dragStarted) {
                    val dt = change.uptimeMillis - change.previousUptimeMillis
                    if (dt > 0) {
                        lastVelocityY = dy / dt
                    }
                    onVerticalDrag(dy)
                    change.consume()
                }
            }
        }
    }

    if (verticalDragConsumed) {
        onDragEnd(lastVelocityY)
    } else if (isMultiTouch) {
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
    } else if (!isPanning && isPlayerDisplayFullscreen) {
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
                    state.controlsState.value.isLocked
                )
            } else {
                state.handleSingleTap(state.controlsState.value.isLocked)
            }
            state.lastTapTime = currentTime
            state.lastTapX = tapX
        }
    }
}