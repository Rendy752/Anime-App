package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import com.luminoverse.animevibe.utils.media.BoundsUtils
import com.luminoverse.animevibe.utils.media.HlsPlayerAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val LONG_PRESS_THRESHOLD_MILLIS = 500L
private const val DOUBLE_TAP_THRESHOLD_MILLIS = 300L

/**
 * A suspend function that detects and handles all pointer gestures for the video player within a
 * `AwaitPointerEventScope` (from a `pointerInput` modifier). It manages single taps, double taps,
 * long presses, vertical drags, and multi-touch zoom/pan gestures.
 *
 * @param state The [VideoPlayerState] instance to mutate based on gestures.
 * @param isPlayerDisplayFullscreen Whether the player is in a fullscreen display mode.
 * @param isLandscape Whether the device is in landscape orientation.
 * @param topPaddingPx The height of any top padding/system bar in pixels, to create a no-drag zone.
 * @param onVerticalDrag A callback for vertical drag events, used for PIP transitions.
 * @param onDragEnd A callback for when a drag gesture ends, used for PIP transitions.
 */
suspend fun AwaitPointerEventScope.handleGestures(
    state: VideoPlayerState,
    isPlayerDisplayFullscreen: Boolean,
    isLandscape: Boolean,
    topPaddingPx: Float,
    onVerticalDrag: (Float) -> Unit,
    onDragEnd: (flingVelocity: Float) -> Unit
) {
    val down: PointerInputChange = awaitFirstDown()
    val isDragInBarrier = isLandscape && down.position.y < topPaddingPx // Prevent drag in status bar area

    // Start a job to detect a long press for speed-up.
    state.longPressJob?.cancel()
    state.longPressJob = state.coroutineScope.launch {
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

    // Main gesture detection loop
    while (true) {
        val event = awaitPointerEvent()
        val pointers = event.changes
        // Exit loop if all pointers are up
        if (pointers.none { it.pressed }) {
            state.longPressJob?.cancel()
            break
        }

        // If locked, consume all events and do nothing.
        if (state.controlsState.value.isLocked) {
            pointers.forEach { it.consume() }
            continue
        }

        // --- Multi-touch (Zoom/Pan) Logic ---
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
                state.onPlayerAction(HlsPlayerAction.RequestToggleControlsVisibility(false))
            }
            val zoomChange = event.calculateZoom()
            state.zoomScale = (state.zoomScale * zoomChange).coerceIn(1f, MAX_ZOOM_SCALE)

            // Clamp offsets while zooming
            if (state.videoResolution.width > 0 && state.videoResolution.height > 0) {
                val containerWidth = size.width.toFloat()
                val containerHeight = size.height.toFloat()
                val containerAspect = containerWidth / containerHeight
                val videoAspect = state.videoResolution.width.toFloat() / state.videoResolution.height.toFloat()
                val fittedVideoSize = if (videoAspect > containerAspect) {
                    Size(width = containerWidth, height = containerWidth / videoAspect)
                } else {
                    Size(width = containerHeight * videoAspect, height = containerHeight)
                }
                val (maxOffsetX, maxOffsetY) = BoundsUtils.calculateOffsetBounds(size, fittedVideoSize, state.zoomScale)
                state.panOffsetX = state.panOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                state.panOffsetY = state.panOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
            }
            pointers.forEach { it.consume() }
            continue
        }

        // --- Single-touch (Pan/Drag) Logic ---
        if (pointers.size == 1 && !isMultiTouch && isPlayerDisplayFullscreen) {
            val change = pointers.first()

            // Pan logic when already zoomed in
            if (state.controlsState.value.zoom > 1f) {
                val pan = event.calculatePan()
                if (pan.x != 0f || pan.y != 0f) {
                    if (!isPanning) {
                        isPanning = true
                        state.onPlayerAction(HlsPlayerAction.RequestToggleControlsVisibility(false))
                        state.longPressJob?.cancel()
                        state.handleLongPressEnd()
                    }
                    if (state.videoResolution.width > 0 && state.videoResolution.height > 0) {
                        val containerWidth = size.width.toFloat()
                        val containerHeight = size.height.toFloat()
                        val containerAspect = containerWidth / containerHeight
                        val videoAspect = state.videoResolution.width.toFloat() / state.videoResolution.height.toFloat()
                        val fittedVideoSize = if (videoAspect > containerAspect) {
                            Size(width = containerWidth, height = containerWidth / videoAspect)
                        } else {
                            Size(width = containerHeight * videoAspect, height = containerHeight)
                        }
                        val (maxOffsetX, maxOffsetY) = BoundsUtils.calculateOffsetBounds(size, fittedVideoSize, state.zoomScale)
                        state.panOffsetX = (state.panOffsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                        state.panOffsetY = (state.panOffsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    }
                    change.consume()
                }
            } else {
                // Vertical drag logic for PIP transition
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
                    if (dt > 0) lastVelocityY = dy / dt // Calculate fling velocity
                    onVerticalDrag(dy)
                    change.consume()
                }
            }
        }
    }

    // --- Gesture End Logic ---
    if (verticalDragConsumed) {
        onDragEnd(lastVelocityY)
    } else if (isMultiTouch) {
        // Snap zoom to a logical value (original, fill, or current) on release
        val halfWayRatio = (1f + state.zoomToFillRatio) / 2
        val finalZoom = if (state.zoomScale < halfWayRatio) 1f
        else if (state.zoomScale in halfWayRatio..state.zoomToFillRatio) state.zoomToFillRatio
        else state.zoomScale

        state.onPlayerAction(HlsPlayerAction.SetZoom(finalZoom))
        state.zoomScale = finalZoom
        if (state.zoomScale <= 1.01f) { // Reset pan on zoom out
            state.panOffsetX = 0f
            state.panOffsetY = 0f
        }
        state.isZooming = false
    } else if (!isPanning && isPlayerDisplayFullscreen) {
        // Handle taps (single or double)
        state.longPressJob?.cancel()
        val currentTime = System.currentTimeMillis()
        val tapX = down.position.x
        if (state.isSpeedingUpWithLongPress) {
            state.handleLongPressEnd()
        } else {
            // Check for double tap
            if (currentTime - state.lastTapTimestamp < DOUBLE_TAP_THRESHOLD_MILLIS &&
                state.lastTapPositionX != null &&
                abs(tapX - state.lastTapPositionX!!) < 100f // Ensure taps are close
            ) {
                state.handleDoubleTap(tapX, size.width.toFloat(), state.controlsState.value.isLocked)
            } else {
                state.handleSingleTap(state.controlsState.value.isLocked)
            }
            state.lastTapTimestamp = currentTime
            state.lastTapPositionX = tapX
        }
    }
}