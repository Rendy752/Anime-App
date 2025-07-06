package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.OrientationEventListener
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.CustomSeekBar
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.CustomSubtitleView
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.LoadingIndicator
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.LockButton
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.NextEpisodeOverlay
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.PlaybackSpeedContent
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.PlayerControls
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.ResumePlaybackOverlay
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.SeekCancelPreview
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.SeekIndicator
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.SettingsContent
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.SkipButtonsContainer
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.SpeedUpIndicator
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.SubtitleContent
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.ThumbnailPreview
import com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer.ZoomIndicator
import com.luminoverse.animevibe.ui.common.BottomSheetConfig
import com.luminoverse.animevibe.ui.common.CustomModalBottomSheet
import com.luminoverse.animevibe.ui.common.ImageAspectRatio
import com.luminoverse.animevibe.ui.common.ImageDisplay
import com.luminoverse.animevibe.ui.common.ImageRoundedCorner
import com.luminoverse.animevibe.ui.main.PlayerDisplayMode
import com.luminoverse.animevibe.utils.FullscreenUtils
import com.luminoverse.animevibe.utils.media.BoundsUtils.calculateOffsetBounds
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import com.luminoverse.animevibe.utils.media.findActiveCaptionCues
import com.luminoverse.animevibe.utils.resource.Resource
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

private enum class PhysicalOrientation {
    PORTRAIT, LANDSCAPE, REVERSE_LANDSCAPE, UNKNOWN;

    fun isLandscape(): Boolean {
        return this == LANDSCAPE || this == REVERSE_LANDSCAPE
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    playerView: PlayerView,
    player: ExoPlayer,
    networkDataSource: NetworkDataSource,
    updateStoredWatchState: (Long?, Long?, String?) -> Unit,
    captureScreenshot: suspend () -> String?,
    coreState: PlayerCoreState,
    controlsStateFlow: StateFlow<ControlsState>,
    playerAction: (HlsPlayerAction) -> Unit,
    onHandleBackPress: () -> Unit,
    episodeDetailComplement: EpisodeDetailComplement,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery, Boolean) -> Unit,
    displayMode: PlayerDisplayMode,
    setPlayerDisplayMode: (PlayerDisplayMode) -> Unit,
    onEnterSystemPipMode: () -> Unit,
    isSideSheetVisible: Boolean,
    setSideSheetVisibility: (Boolean) -> Unit,
    isAutoplayEnabled: Boolean,
    isLandscape: Boolean,
    rememberedTopPadding: Dp,
    verticalDragOffset: Float,
    onVerticalDrag: (Float) -> Unit,
    onDragEnd: (flingVelocity: Float) -> Unit,
    pipEndDestinationPx: Offset,
    pipEndSizePx: IntSize,
    onMaxDragAmountCalculated: (Float) -> Unit
) {
    val context = LocalContext.current
    val topPaddingPx = with(LocalDensity.current) { rememberedTopPadding.toPx() }
    val activity = context as? Activity

    val videoPlayerState =
        rememberVideoPlayerState(
            key = episodeDetailComplement.sources.link.file,
            player = player,
            playerAction = playerAction,
            networkDataSource = networkDataSource
        )
    val controlsState by controlsStateFlow.collectAsStateWithLifecycle()
    val currentPosition by videoPlayerState.currentPosition.collectAsStateWithLifecycle()

    val playerHeight = videoPlayerState.playerSize.height.toFloat()

    val finalTranslationY = remember(playerHeight, pipEndDestinationPx, pipEndSizePx) {
        if (playerHeight > 0f) {
            (pipEndDestinationPx.y + pipEndSizePx.height / 2f) - (playerHeight / 2f)
        } else {
            Float.POSITIVE_INFINITY
        }
    }

    LaunchedEffect(finalTranslationY) {
        if (finalTranslationY != Float.POSITIVE_INFINITY) {
            onMaxDragAmountCalculated(finalTranslationY)
        }
    }

    LaunchedEffect(isLandscape, videoPlayerState.playerSize, videoPlayerState.zoomScaleProgress) {
        val playerSize = videoPlayerState.playerSize
        val videoSize = videoPlayerState.videoSize
        val scale = videoPlayerState.zoomScaleProgress

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

            videoPlayerState.offsetX = videoPlayerState.offsetX.coerceIn(-maxOffsetX, maxOffsetY)
            videoPlayerState.offsetY = videoPlayerState.offsetY.coerceIn(-maxOffsetY, maxOffsetY)
        }
    }

    val thumbnailTrackUrl =
        episodeDetailComplement.sources.tracks.find { it.kind == "thumbnails" }?.file
    LaunchedEffect(thumbnailTrackUrl) {
        if (thumbnailTrackUrl != null) {
            videoPlayerState.loadAndCacheThumbnails(context, thumbnailTrackUrl)
        }
    }

    LaunchedEffect(episodeDetailComplement.sources.link.file) {
        videoPlayerState.isShowResume = episodeDetailComplement.lastTimestamp != null
    }
    val selectedSubtitle = controlsState.selectedSubtitle
    LaunchedEffect(episodeDetailComplement.sources.link.file, selectedSubtitle) {
        if (selectedSubtitle != null) {
            videoPlayerState.loadAndCacheCaptions(selectedSubtitle.file)
        }
    }
    val activeCaptionCue = selectedSubtitle?.file?.let { url ->
        videoPlayerState.captionCues[url]?.let { cues ->
            findActiveCaptionCues(currentPosition, cues)
        }
    }

    val updatedControlsState = rememberUpdatedState(controlsState)
    val updatedCoreState = rememberUpdatedState(coreState)

    val animatedZoom by animateFloatAsState(
        targetValue = videoPlayerState.zoomScaleProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PlayerZoomAnimation"
    )

    val currentEpisode = episodeDetailComplement.number
    val prevEpisode = episodes.find { it.episode_no == currentEpisode - 1 }
    val nextEpisode = episodes.find { it.episode_no == currentEpisode + 1 }

    var isBufferingFromSeeking by remember { mutableStateOf(false) }

    val shouldShowResumeOverlay = !isAutoplayEnabled && videoPlayerState.isShowResume &&
            episodeDetailComplement.lastTimestamp != null &&
            updatedCoreState.value.playbackState == Player.STATE_READY && !updatedCoreState.value.isPlaying

    LaunchedEffect(updatedCoreState.value.isPlaying) {
        if (updatedCoreState.value.isPlaying) {
            videoPlayerState.isShowResume = false
            if (videoPlayerState.isFirstLoad && updatedCoreState.value.playbackState == Player.STATE_READY) {
                videoPlayerState.isFirstLoad = false
            }
            while (true) {
                val currentPosition = player.currentPosition
                videoPlayerState.updatePosition(currentPosition)
                if (player.duration > 0 && currentPosition >= 10_000) {
                    val position = currentPosition.coerceAtLeast(0L)
                    val duration = player.duration.coerceAtLeast(0L)
                    val screenshot = captureScreenshot()
                    updateStoredWatchState(position, duration, screenshot)
                }
                delay(500)
            }
        }
    }

    LaunchedEffect(videoPlayerState.showLockReminder, updatedControlsState.value.isLocked) {
        if (videoPlayerState.showLockReminder && updatedControlsState.value.isLocked) {
            delay(3000)
            videoPlayerState.showLockReminder = false
        }
    }

    val isPlayerDisplayFullscreen = displayMode in listOf(
        PlayerDisplayMode.FULLSCREEN_PORTRAIT,
        PlayerDisplayMode.FULLSCREEN_LANDSCAPE
    )
    val isPlayerDisplayPip = displayMode in listOf(
        PlayerDisplayMode.PIP,
        PlayerDisplayMode.SYSTEM_PIP
    )

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

    LaunchedEffect(displayMode, controlsState.isLocked, isSystemAutoRotateEnabled) {
        setSideSheetVisibility(false)
        activity ?: return@LaunchedEffect
        val newOrientation = when (displayMode) {
            PlayerDisplayMode.FULLSCREEN_LANDSCAPE -> {
                if (controlsState.isLocked) {
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

    LaunchedEffect(physicalOrientation, controlsState.isLocked, isSystemAutoRotateEnabled) {
        if (controlsState.isLocked || !isSystemAutoRotateEnabled) {
            return@LaunchedEffect
        }

        if (physicalOrientation.isLandscape() && displayMode == PlayerDisplayMode.FULLSCREEN_PORTRAIT) {
            setPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN_LANDSCAPE)
        } else if (physicalOrientation == PhysicalOrientation.PORTRAIT && displayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE) {
            setPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN_PORTRAIT)
        }
    }


    LaunchedEffect(isLandscape, displayMode) {
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        playerView.postDelayed({
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }, 1)

        val window = activity?.window ?: return@LaunchedEffect
        if (displayMode in listOf(PlayerDisplayMode.PIP, PlayerDisplayMode.SYSTEM_PIP)) {
            FullscreenUtils.setFullscreen(window, false)
            return@LaunchedEffect
        }

        if (isLandscape) {
            FullscreenUtils.setFullscreen(window, true)
        } else {
            FullscreenUtils.setFullscreen(window, false)
        }
    }

    LaunchedEffect(updatedCoreState.value.playbackState, videoPlayerState.isDraggingSeekBar) {
        if (updatedCoreState.value.playbackState == Player.STATE_ENDED) {
            videoPlayerState.isShowNextEpisodeOverlay = true
            val duration = player.duration.coerceAtLeast(0L)
            val screenshot = captureScreenshot()
            updateStoredWatchState(duration, duration, screenshot)
        }

        isBufferingFromSeeking =
            if (videoPlayerState.isDraggingSeekBar && (updatedCoreState.value.playbackState == Player.STATE_READY || updatedCoreState.value.playbackState == Player.STATE_BUFFERING)) {
                true
            } else if (updatedCoreState.value.playbackState != Player.STATE_BUFFERING) {
                false
            } else {
                isBufferingFromSeeking
            }
    }

    val isCommonPartVisible =
        !isPlayerDisplayPip && !updatedControlsState.value.isLocked && verticalDragOffset == 0f

    val animatedCornerRadius by animateDpAsState(
        targetValue = if (verticalDragOffset > 0) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "cornerRadiusAnimation"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { videoPlayerState.playerSize = it }
            .graphicsLayer {
                if (isPlayerDisplayFullscreen) {
                    val playerWidth = videoPlayerState.playerSize.width.toFloat()
                    val pHeight = videoPlayerState.playerSize.height.toFloat()

                    if (playerWidth > 0f && pHeight > 0f) {
                        val targetScale = min(
                            pipEndSizePx.width / playerWidth,
                            pipEndSizePx.height / pHeight
                        )

                        val finalTranslationX =
                            (pipEndDestinationPx.x + pipEndSizePx.width / 2f) - (playerWidth / 2f)

                        val progress = if (finalTranslationY > 0f) {
                            (verticalDragOffset / finalTranslationY).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        val currentScale = lerp(1f, targetScale, progress)
                        val currentTranslationX = lerp(0f, finalTranslationX, progress)

                        translationY = verticalDragOffset
                        translationX = currentTranslationX
                        scaleX = currentScale
                        scaleY = currentScale
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(animatedCornerRadius))
                .clipToBounds()
                .pointerInput(
                    episodeDetailComplement.sources.link.file,
                    displayMode,
                    updatedControlsState.value.zoom,
                    isLandscape
                ) {
                    awaitEachGesture {
                        try {
                            handleGestures(
                                state = videoPlayerState,
                                isPlayerDisplayFullscreen = isPlayerDisplayFullscreen,
                                updatedControlsState = updatedControlsState,
                                isLandscape = isLandscape,
                                topPaddingPx = topPaddingPx,
                                onVerticalDrag = onVerticalDrag,
                                onDragEnd = onDragEnd
                            )
                        } finally {
                            if (videoPlayerState.isHolding) {
                                videoPlayerState.handleLongPressEnd()
                            }
                            if (videoPlayerState.isZooming) {
                                videoPlayerState.isZooming = false
                            }
                        }
                    }
                }
        ) {
            val borderModifier =
                if (videoPlayerState.isZooming && isCommonPartVisible) Modifier.border(
                    16.dp,
                    Color.White.copy(alpha = 0.5f)
                )
                else Modifier

            if (videoPlayerState.isFirstLoad) {
                ImageDisplay(
                    modifier = Modifier.fillMaxSize(),
                    image = episodeDetailComplement.screenshot,
                    imagePlaceholder = episodeDetailComplement.imageUrl,
                    ratio = ImageAspectRatio.WIDESCREEN.ratio,
                    contentDescription = "Thumbnail Image",
                    roundedCorners = ImageRoundedCorner.NONE
                )
            } else {
                AndroidView(
                    factory = { playerView },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .then(borderModifier)
                        .then(
                            if (isPlayerDisplayFullscreen) {
                                Modifier.graphicsLayer(
                                    scaleX = animatedZoom,
                                    scaleY = animatedZoom,
                                    translationX = videoPlayerState.offsetX,
                                    translationY = videoPlayerState.offsetY
                                )
                            } else Modifier
                        ),
                    update = { view ->
                        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                )
            }
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = displayMode == PlayerDisplayMode.PIP, enter = fadeIn(), exit = fadeOut()
        ) {
            CustomSeekBar(
                currentPosition = currentPosition,
                bufferedPosition = player.bufferedPosition,
                duration = player.duration.takeIf { it > 0 }
                    ?: episodeDetailComplement.duration ?: 0,
                intro = episodeDetailComplement.sources.intro,
                outro = episodeDetailComplement.sources.outro,
                seekAmount = videoPlayerState.seekAmount * 1000L,
                dragCancelTrigger = videoPlayerState.dragCancelTrigger,
                isShowSeekIndicator = videoPlayerState.isShowSeekIndicator,
                touchTargetHeight = 2.dp,
                trackHeight = 2.dp
            )
        }

        val isPlayerControlsVisible =
            (updatedControlsState.value.isControlsVisible || videoPlayerState.isDraggingSeekBar) && !shouldShowResumeOverlay && isCommonPartVisible && isPlayerDisplayFullscreen

        thumbnailTrackUrl?.let { url ->
            val showThumbnail =
                isBufferingFromSeeking || (!updatedCoreState.value.isPlaying && videoPlayerState.isDraggingSeekBar && !videoPlayerState.isFirstLoad)
            val thumbnailSeekPositionKey =
                remember(videoPlayerState.dragSeekPosition) { (videoPlayerState.dragSeekPosition / 10000L) * 10000L }

            AnimatedVisibility(visible = showThumbnail, enter = fadeIn(), exit = fadeOut()) {
                ThumbnailPreview(
                    modifier = Modifier.fillMaxSize(),
                    seekPosition = thumbnailSeekPositionKey,
                    cues = videoPlayerState.thumbnailCues[url]
                )
            }
        }

        val animatedSubtitleTopPadding by animateDpAsState(
            targetValue = if (isCommonPartVisible && isPlayerControlsVisible && isLandscape && !isSideSheetVisible) {
                (videoPlayerState.bottomBarHeight.dp - 16.dp).coerceAtLeast(0.dp)
            } else {
                if (isSideSheetVisible) 16.dp else 8.dp
            },
            label = "SubtitleTopPadding"
        )

        val animatedSubtitleBottomPadding by animateDpAsState(
            targetValue = if (isCommonPartVisible && isPlayerControlsVisible && isLandscape) {
                videoPlayerState.bottomBarHeight.dp
            } else {
                if (isSideSheetVisible) 16.dp else 8.dp
            },
            label = "SubtitleBottomPadding"
        )
        CustomSubtitleView(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = animatedSubtitleTopPadding, bottom = animatedSubtitleBottomPadding,
                    start = 8.dp, end = 8.dp
                ),
            cues = activeCaptionCue,
            isLandscape = isLandscape,
            isPipMode = isPlayerDisplayPip
        )

        AnimatedVisibility(visible = isPlayerControlsVisible, enter = fadeIn(), exit = fadeOut()) {
            PlayerControls(
                isPlaying = updatedCoreState.value.isPlaying,
                currentPosition = currentPosition,
                bufferedPosition = player.bufferedPosition,
                duration = player.duration.takeIf { it > 0 }
                    ?: episodeDetailComplement.duration ?: 0,
                playbackState = updatedCoreState.value.playbackState,
                playbackErrorMessage = updatedCoreState.value.error,
                onHandleBackPress = onHandleBackPress,
                episodeDetailComplement = episodeDetailComplement,
                hasPreviousEpisode = prevEpisode != null,
                nextEpisode = nextEpisode,
                nextEpisodeDetailComplement = episodeDetailComplements[nextEpisode?.id]?.data,
                isSideSheetVisible = isSideSheetVisible,
                setSideSheetVisibility = setSideSheetVisibility,
                isLandscape = isLandscape,
                isShowSpeedUp = videoPlayerState.isHolding,
                zoomText = videoPlayerState.getZoomRatioText(),
                onZoomReset = { videoPlayerState.resetZoom() },
                handlePlay = { playerAction(HlsPlayerAction.Play) },
                handlePause = { playerAction(HlsPlayerAction.Pause) },
                onPreviousEpisode = {
                    prevEpisode?.let {
                        handleSelectedEpisodeServer(episodeSourcesQuery.copy(id = it.id), false)
                    }
                },
                onNextEpisode = {
                    nextEpisode?.let {
                        handleSelectedEpisodeServer(episodeSourcesQuery.copy(id = it.id), false)
                    }
                },
                onSeekTo = { position -> playerAction(HlsPlayerAction.SeekTo(position)) },
                seekAmount = videoPlayerState.seekAmount * 1000L,
                isShowSeekIndicator = videoPlayerState.isShowSeekIndicator,
                dragSeekPosition = videoPlayerState.dragSeekPosition,
                dragCancelTrigger = videoPlayerState.dragCancelTrigger,
                onDraggingSeekBarChange = { isDragging, position ->
                    videoPlayerState.isDraggingSeekBar = isDragging
                    videoPlayerState.dragSeekPosition = position
                },
                isDraggingSeekBar = videoPlayerState.isDraggingSeekBar,
                showRemainingTime = videoPlayerState.showRemainingTime,
                setShowRemainingTime = { videoPlayerState.showRemainingTime = it },
                onSettingsClick = { videoPlayerState.showSettingsSheet = true },
                onFullscreenToggle = {
                    setPlayerDisplayMode(
                        if (displayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE) {
                            PlayerDisplayMode.FULLSCREEN_PORTRAIT
                        } else {
                            PlayerDisplayMode.FULLSCREEN_LANDSCAPE
                        }
                    )
                },
                onBottomBarMeasured = { height -> videoPlayerState.bottomBarHeight = height }
            )
        }

        LoadingIndicator(
            modifier = Modifier.align(Alignment.Center),
            isVisible = (updatedCoreState.value.playbackState == Player.STATE_BUFFERING || updatedCoreState.value.playbackState == Player.STATE_IDLE)
                    && !isPlayerControlsVisible && updatedCoreState.value.error == null && isPlayerDisplayFullscreen
        )

        SeekIndicator(
            modifier = Modifier.align(Alignment.Center),
            seekDirection = videoPlayerState.isShowSeekIndicator,
            seekAmount = videoPlayerState.seekAmount,
            isLandscape = isLandscape,
            isFullscreen = isPlayerDisplayFullscreen
        )

        val isSkipVisible =
            displayMode != PlayerDisplayMode.SYSTEM_PIP && !updatedControlsState.value.isLocked && verticalDragOffset == 0f && !videoPlayerState.isHolding && !videoPlayerState.isDraggingSeekBar && updatedCoreState.value.playbackState != Player.STATE_ENDED && updatedCoreState.value.playbackState != Player.STATE_IDLE && !shouldShowResumeOverlay && !videoPlayerState.isFirstLoad

        SkipButtonsContainer(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = if (isPlayerDisplayFullscreen) 56.dp else 8.dp,
                    bottom = if (isPlayerDisplayFullscreen) 56.dp else 8.dp
                ),
            currentPosition = currentPosition,
            duration = player.duration,
            intro = episodeDetailComplement.sources.intro,
            outro = episodeDetailComplement.sources.outro,
            isSkipVisible = isSkipVisible,
            onSkip = { seekPosition -> playerAction(HlsPlayerAction.SeekTo(seekPosition)) }
        )

        SpeedUpIndicator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            isVisible = videoPlayerState.isHolding && isCommonPartVisible,
            speedText = videoPlayerState.speedUpText
        )

        ZoomIndicator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            visible = videoPlayerState.isZooming && isCommonPartVisible,
            isShowSpeedUp = videoPlayerState.isHolding,
            zoomText = videoPlayerState.getZoomRatioText(),
            isClickable = videoPlayerState.zoomScaleProgress > 1f,
            onClick = { videoPlayerState.resetZoom() }
        )

        SeekCancelPreview(
            modifier = Modifier.align(Alignment.TopEnd),
            visible = videoPlayerState.isDraggingSeekBar && isCommonPartVisible && !videoPlayerState.isHolding,
            captureScreenshot = captureScreenshot,
            imageUrl = episodeDetailComplement.imageUrl,
            onCancelSeekBarDrag = {
                videoPlayerState.cancelSeekBarDrag()
                playerAction(HlsPlayerAction.Play)
                playerAction(HlsPlayerAction.RequestToggleControlsVisibility(false))
            },
        )

        LockButton(
            visible = updatedControlsState.value.isLocked && !isPlayerDisplayPip && videoPlayerState.showLockReminder,
            onClick = {
                playerAction(HlsPlayerAction.ToggleLock(false))
            },
            modifier = Modifier.align(Alignment.TopEnd)
        )

        episodeDetailComplement.lastTimestamp?.let { lastTimestamp ->
            ResumePlaybackOverlay(
                modifier = Modifier.align(Alignment.Center),
                isVisible = shouldShowResumeOverlay,
                isPipMode = isPlayerDisplayPip,
                lastTimestamp = lastTimestamp,
                onDismiss = {
                    videoPlayerState.isShowResume = false
                    playerAction(HlsPlayerAction.RequestToggleControlsVisibility(true))
                },
                onRestart = {
                    playerAction(HlsPlayerAction.SeekTo(0))
                    playerAction(HlsPlayerAction.Play)
                    videoPlayerState.isShowResume = false
                },
                onResume = {
                    playerAction(HlsPlayerAction.SeekTo(it))
                    playerAction(HlsPlayerAction.Play)
                    videoPlayerState.isShowResume = false
                }
            )
        }

        nextEpisode?.let {
            NextEpisodeOverlay(
                modifier = Modifier.align(Alignment.Center),
                isVisible = updatedCoreState.value.playbackState == Player.STATE_ENDED && videoPlayerState.isShowNextEpisodeOverlay,
                isOnlyShowEpisodeDetail = isPlayerDisplayPip,
                isLandscape = isLandscape,
                animeImage = episodeDetailComplement.imageUrl,
                nextEpisode = it,
                nextEpisodeDetailComplement = episodeDetailComplements[it.id]?.data,
                onDismiss = {
                    videoPlayerState.isShowNextEpisodeOverlay = false
                    playerAction(HlsPlayerAction.RequestToggleControlsVisibility(true))
                },
                onRestart = {
                    playerAction(HlsPlayerAction.SeekTo(0))
                    playerAction(HlsPlayerAction.Play)
                    videoPlayerState.isShowNextEpisodeOverlay = false
                },
                onPlayNext = {
                    handleSelectedEpisodeServer(episodeSourcesQuery.copy(id = it.id), false)
                    videoPlayerState.isShowNextEpisodeOverlay = false
                }
            )
        }

        val settingsSheetConfig = BottomSheetConfig(
            landscapeWidthFraction = 0.4f,
            landscapeHeightFraction = 0.7f
        )
        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = videoPlayerState.showSettingsSheet && isCommonPartVisible,
            isLandscape = isLandscape,
            config = settingsSheetConfig,
            onDismiss = { videoPlayerState.showSettingsSheet = false }
        ) {
            SettingsContent(
                onDismiss = { videoPlayerState.showSettingsSheet = false },
                onLockClick = {
                    videoPlayerState.showLockReminder = true
                    setPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN_LANDSCAPE)
                    if (updatedCoreState.value.playbackState == Player.STATE_ENDED) playerAction(
                        HlsPlayerAction.SeekTo(0)
                    )
                    playerAction(HlsPlayerAction.Play)
                    playerAction(HlsPlayerAction.ToggleLock(true))
                },
                onPipClick = { onEnterSystemPipMode() },
                selectedPlaybackSpeed = updatedControlsState.value.playbackSpeed,
                onPlaybackSpeedClick = { videoPlayerState.showPlaybackSpeedSheet = true },
                isSubtitleAvailable = episodeDetailComplement.sources.tracks.any { it.kind == "captions" },
                selectedSubtitle = updatedControlsState.value.selectedSubtitle,
                onSubtitleClick = { videoPlayerState.showSubtitleSheet = true }
            )
        }

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = videoPlayerState.showSubtitleSheet && isCommonPartVisible,
            isLandscape = isLandscape,
            config = settingsSheetConfig,
            onDismiss = { videoPlayerState.showSubtitleSheet = false }
        ) {
            SubtitleContent(
                tracks = episodeDetailComplement.sources.tracks,
                selectedSubtitle = updatedControlsState.value.selectedSubtitle,
                onSubtitleSelected = { subtitleTrack ->
                    playerAction(HlsPlayerAction.SetSubtitle(subtitleTrack))
                    videoPlayerState.showSubtitleSheet = false
                }
            )
        }

        CustomModalBottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = videoPlayerState.showPlaybackSpeedSheet && isCommonPartVisible,
            isLandscape = isLandscape,
            config = settingsSheetConfig,
            onDismiss = { videoPlayerState.showPlaybackSpeedSheet = false }
        ) {
            PlaybackSpeedContent(
                selectedPlaybackSpeed = updatedControlsState.value.playbackSpeed,
                onSpeedChange = { speed ->
                    playerAction(HlsPlayerAction.SetPlaybackSpeed(speed))
                    videoPlayerState.showPlaybackSpeedSheet = false
                }
            )
        }
    }
}
