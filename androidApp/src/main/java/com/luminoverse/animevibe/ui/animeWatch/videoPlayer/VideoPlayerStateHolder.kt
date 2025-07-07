package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import android.app.Activity
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.OrientationEventListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import coil.imageLoader
import com.luminoverse.animevibe.data.remote.api.NetworkDataSource
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.ui.main.PlayerDisplayMode
import com.luminoverse.animevibe.utils.SystemBarsUtils
import com.luminoverse.animevibe.utils.media.BoundsUtils
import com.luminoverse.animevibe.utils.media.ControlsState
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import com.luminoverse.animevibe.utils.media.PlayerCoreState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive

/**
 * Enum representing the physical orientation of the device.
 */
private enum class PhysicalOrientation {
    PORTRAIT, LANDSCAPE, REVERSE_LANDSCAPE, UNKNOWN;

    fun isLandscape(): Boolean = this == LANDSCAPE || this == REVERSE_LANDSCAPE
}

/**
 * A composable function that creates, remembers, and manages the lifecycle of a [VideoPlayerState].
 * It encapsulates all the `LaunchedEffect` and `DisposableEffect` logic required to keep the state
 * synchronized with the player, system settings, and user interactions.
 *
 * @param key A key to restart the state holder. Typically the episode or source URL.
 * @param player The ExoPlayer instance.
 * @param onPlayerAction Lambda to dispatch actions to the player.
 * @param networkDataSource Data source for fetching network resources.
 * @param episodeDetails Complementary details of the current episode.
 * @param controlsStateFlow A flow of the shared player controls state.
 * @param coreState The core state of the player (isPlaying, playbackState).
 * @param displayMode The current display mode of the player (e.g., Fullscreen, PIP).
 * @param setPlayerDisplayMode Lambda to change the player's display mode.
 * @param setSideSheetVisibility Lambda to control the visibility of side sheets.
 * @param isLandscape True if the device is in landscape orientation.
 * @param pipEndDestinationPx The destination offset for the PIP animation.
 * @param pipEndSizePx The final size of the PIP window.
 * @param onMaxDragAmountCalculated Callback with the maximum vertical drag distance for PIP transition.
 * @param updateStoredWatchState Lambda to save the current watch progress.
 * @param captureScreenshot Suspend function to capture a screenshot of the player.
 * @param coroutineScope The coroutine scope for managing effects.
 * @return A remembered instance of [VideoPlayerState].
 */
@Composable
fun rememberVideoPlayerState(
    key: Any?,
    player: ExoPlayer,
    onPlayerAction: (HlsPlayerAction) -> Unit,
    networkDataSource: NetworkDataSource,
    episodeDetails: EpisodeDetailComplement,
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
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): VideoPlayerState {
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    val activity = context as? Activity

    val controlsState = controlsStateFlow.collectAsStateWithLifecycle()

    // Create and remember the VideoPlayerState instance. It's keyed to the episode URL.
    val state = remember(key) {
        VideoPlayerState(
            context = context,
            player = player,
            onPlayerAction = onPlayerAction,
            coroutineScope = coroutineScope,
            imageLoader = imageLoader,
            networkDataSource = networkDataSource,
            episodeDetails = episodeDetails,
            controlsState = controlsState
        )
    }

    // Effect to calculate the maximum vertical drag distance for the PIP animation.
    LaunchedEffect(state.playerContainerSize) {
        val playerHeight = state.playerContainerSize.height.toFloat()
        val finalTranslationY = if (playerHeight > 0f) {
            (pipEndDestinationPx.y + pipEndSizePx.height / 2f) - (playerHeight / 2f)
        } else {
            Float.POSITIVE_INFINITY
        }
        if (finalTranslationY != Float.POSITIVE_INFINITY) {
            onMaxDragAmountCalculated(finalTranslationY)
        }
    }

    // Effect to load thumbnails and captions when episode or subtitle selection changes.
    LaunchedEffect(episodeDetails, controlsState.value.selectedSubtitle) {
        // Load thumbnail cues
        val thumbnailTrackUrl = episodeDetails.sources.tracks.find { it.kind == "thumbnails" }?.file
        if (thumbnailTrackUrl != null) {
            state.loadAndCacheThumbnails(context, thumbnailTrackUrl)
        }
        // Load caption cues
        controlsState.value.selectedSubtitle?.file?.let {
            state.loadAndCacheCaptions(it)
        }
        // Show resume overlay if there's a last known timestamp
        state.shouldShowResumeOverlay = episodeDetails.lastTimestamp != null
    }

    // Effect to auto-hide the autoplay status message after a few seconds.
    LaunchedEffect(state.autoplayStatusId) {
        if (state.autoplayStatusId > 0) {
            delay(3000)
            state.autoplayStatusText = null
        }
    }

    // Effect for managing player state during playback.
    LaunchedEffect(coreState.isPlaying, coreState.playbackState) {
        val duration = player.duration.coerceAtLeast(0L)
        if (coreState.isPlaying) {
            // Hide overlays when playback starts
            state.shouldShowResumeOverlay = false
            state.shouldShowNextEpisodeOverlay = false
            if (state.isInitialLoading && coreState.playbackState == Player.STATE_READY) {
                state.isInitialLoading = false
            }
            // Periodically update current position and save progress
            while (isActive) {
                val currentPosition = player.currentPosition
                state.updatePosition(currentPosition)
                if (duration > 0 && currentPosition >= 10_000) { // Start saving after 10s
                    updateStoredWatchState(currentPosition.coerceAtLeast(0L), duration, captureScreenshot())
                }
                delay(500)
            }
        } else {
            // Handle state when playback ends
            if (coreState.playbackState == Player.STATE_ENDED) {
                state.shouldShowResumeOverlay = false
                state.shouldShowNextEpisodeOverlay = true
                state.updatePosition(duration)
                updateStoredWatchState(duration, duration, captureScreenshot())
            }
        }
    }

    // Effect to auto-hide the lock reminder icon.
    LaunchedEffect(state.showLockReminder, controlsState.value.isLocked) {
        if (state.showLockReminder && controlsState.value.isLocked) {
            delay(3000)
            state.showLockReminder = false
        }
    }

    // Effect to manage the buffering indicator specifically for seek bar dragging.
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

    // Effect to re-calculate and clamp pan offsets when zoom, size, or orientation changes.
    LaunchedEffect(isLandscape, state.playerContainerSize, state.zoomScale) {
        val playerSize = state.playerContainerSize
        val videoSize = state.videoResolution
        val scale = state.zoomScale

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

            val (maxOffsetX, maxOffsetY) = BoundsUtils.calculateOffsetBounds(
                containerSize = playerSize,
                imageSize = fittedVideoSize,
                scale = scale
            )

            state.panOffsetX = state.panOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
            state.panOffsetY = state.panOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
        }
    }

    // State and effect for observing system auto-rotate setting.
    val contentResolver = context.contentResolver
    var isSystemAutoRotateEnabled by remember {
        mutableStateOf(Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1)
    }
    DisposableEffect(contentResolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                isSystemAutoRotateEnabled = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
            }
        }
        contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true, observer)
        onDispose { contentResolver.unregisterContentObserver(observer) }
    }

    // State and effect for listening to physical device orientation changes.
    var physicalOrientation by remember { mutableStateOf(PhysicalOrientation.UNKNOWN) }
    DisposableEffect(Unit) {
        val orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                physicalOrientation = when (orientation) {
                    in 345..359, in 0..15 -> PhysicalOrientation.PORTRAIT
                    in 75..105 -> PhysicalOrientation.REVERSE_LANDSCAPE
                    in 255..285 -> PhysicalOrientation.LANDSCAPE
                    else -> physicalOrientation
                }
            }
        }
        orientationEventListener.enable()
        onDispose { orientationEventListener.disable() }
    }

    // Effect to manage the screen orientation based on player display mode and settings.
    LaunchedEffect(displayMode, controlsState.value.isLocked, isSystemAutoRotateEnabled) {
        setSideSheetVisibility(false)
        activity ?: return@LaunchedEffect
        val newOrientation = when (displayMode) {
            PlayerDisplayMode.FULLSCREEN_LANDSCAPE -> when {
                controlsState.value.isLocked -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                isSystemAutoRotateEnabled -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            PlayerDisplayMode.FULLSCREEN_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> if (isSystemAutoRotateEnabled) ActivityInfo.SCREEN_ORIENTATION_SENSOR else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (activity.requestedOrientation != newOrientation) {
            activity.requestedOrientation = newOrientation
        }
    }

    // Effect to automatically switch between portrait and landscape based on physical orientation.
    LaunchedEffect(physicalOrientation, controlsState.value.isLocked, isSystemAutoRotateEnabled) {
        if (controlsState.value.isLocked || !isSystemAutoRotateEnabled) return@LaunchedEffect

        if (physicalOrientation.isLandscape() && displayMode == PlayerDisplayMode.FULLSCREEN_PORTRAIT) {
            setPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN_LANDSCAPE)
        } else if (physicalOrientation == PhysicalOrientation.PORTRAIT && displayMode == PlayerDisplayMode.FULLSCREEN_LANDSCAPE) {
            setPlayerDisplayMode(PlayerDisplayMode.FULLSCREEN_PORTRAIT)
        }
    }

    // Effect to manage system UI (status/navigation bars) visibility.
    LaunchedEffect(isLandscape, displayMode) {
        if (displayMode in listOf(PlayerDisplayMode.PIP, PlayerDisplayMode.SYSTEM_PIP)) {
            SystemBarsUtils.setSystemBarsVisibility(activity, false)
            return@LaunchedEffect
        }
        if (isLandscape) {
            SystemBarsUtils.setSystemBarsVisibility(activity, true) // Hide bars in landscape
        } else {
            SystemBarsUtils.setSystemBarsVisibility(activity, false) // Show bars in portrait
            state.landscapeDragOffset.snapTo(0f)
        }
    }

    // Disposable effect to register/unregister player listeners and clean up state.
    DisposableEffect(key) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(newVideoSize: VideoSize) {
                state.videoResolution = newVideoSize
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
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