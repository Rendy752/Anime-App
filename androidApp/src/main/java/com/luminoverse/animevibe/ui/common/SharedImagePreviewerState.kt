package com.luminoverse.animevibe.ui.common

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize

private const val DOUBLE_TAP_ZOOM_SCALE = 2f
private const val MAX_ZOOM_SCALE = 5f
private const val PAN_SENSITIVITY_FACTOR = 1.5f
internal const val DOUBLE_TAP_THRESHOLD_MILLIS = 300L

@Stable
class SharedImagePreviewerState {
    var zoomScale by mutableFloatStateOf(1f)
        private set
    var panOffset by mutableStateOf(Offset.Zero)
        private set
    var containerSize by mutableStateOf(IntSize.Zero)
        private set
    var imageSize by mutableStateOf(Size.Unspecified)
        private set
    var isZooming by mutableStateOf(false)
        private set
    internal var lastTapTime by mutableLongStateOf(0L)
    internal var lastTapPosition by mutableStateOf(Offset.Zero)

    private var fittedImageSize by mutableStateOf(Size.Zero)

    var isVisible by mutableStateOf(false)
        private set
    var isAnimatingOut by mutableStateOf(false)
        private set

    fun enter() {
        isVisible = true
    }

    fun exit() {
        resetZoom()
        isVisible = false
        isAnimatingOut = true
    }

    fun updateContainerSize(size: IntSize) {
        containerSize = size
        updateFittedImageSize()
        resetZoomAndPanIfNeeded()
    }

    fun updateImageSize(size: Size?) {
        imageSize = size ?: Size.Unspecified
        updateFittedImageSize()
        resetZoomAndPanIfNeeded()
    }

    private fun updateFittedImageSize() {
        if (containerSize == IntSize.Zero || imageSize == Size.Unspecified || imageSize.width <= 0f || imageSize.height <= 0f) {
            fittedImageSize = Size.Zero
            return
        }

        val containerWidth = containerSize.width.toFloat()
        val containerHeight = containerSize.height.toFloat()
        val imageWidth = imageSize.width
        val imageHeight = imageSize.height

        val containerAspect = containerWidth / containerHeight
        val imageAspect = imageWidth / imageHeight

        fittedImageSize = if (imageAspect > containerAspect) {
            Size(containerWidth, containerWidth / imageAspect)
        } else {
            Size(containerHeight * imageAspect, containerHeight)
        }
    }

    private fun resetZoomAndPanIfNeeded() {
        if (containerSize != IntSize.Zero && imageSize != Size.Unspecified && imageSize.width > 0 && imageSize.height > 0) {
            if (zoomScale == 1f) {
                panOffset = Offset.Zero
            } else {
                val centerXBefore = (containerSize.width / 2f + panOffset.x) / zoomScale
                val centerYBefore = (containerSize.height / 2f + panOffset.y) / zoomScale
                val newZoomScale = zoomScale
                panOffset = Offset(
                    (centerXBefore * newZoomScale - containerSize.width / 2f).coerceIn(panBoundsX),
                    (centerYBefore * newZoomScale - containerSize.height / 2f).coerceIn(panBoundsY)
                )
            }
        }
    }

    fun handleDoubleTap(position: Offset) {
        if (zoomScale > 1.01f) {
            resetZoom()
        } else {
            val targetZoom = DOUBLE_TAP_ZOOM_SCALE.coerceAtMost(MAX_ZOOM_SCALE)
            zoomScale = targetZoom

            val imageTopLeftX = (containerSize.width - fittedImageSize.width) / 2f
            val imageTopLeftY = (containerSize.height - fittedImageSize.height) / 2f

            val tapXInImage = position.x - imageTopLeftX
            val tapYInImage = position.y - imageTopLeftY

            val targetOffsetX = (fittedImageSize.width / 2f - tapXInImage) * (targetZoom - 1)
            val targetOffsetY = (fittedImageSize.height / 2f - tapYInImage) * (targetZoom - 1)

            panOffset =
                Offset(targetOffsetX.coerceIn(panBoundsX), targetOffsetY.coerceIn(panBoundsY))
        }
    }

    fun handleZoom(zoomFactor: Float) {
        isZooming = true
        zoomScale = (zoomScale * zoomFactor).coerceIn(1f, MAX_ZOOM_SCALE)
    }

    fun handlePan(delta: Offset) {
        val newOffsetX = (panOffset.x + (delta.x * PAN_SENSITIVITY_FACTOR)).coerceIn(panBoundsX)
        val newOffsetY = (panOffset.y + (delta.y * PAN_SENSITIVITY_FACTOR)).coerceIn(panBoundsY)
        panOffset = Offset(newOffsetX, newOffsetY)
    }

    internal fun onGestureEnd() {
        isZooming = false
    }

    private fun resetZoom() {
        zoomScale = 1f
        panOffset = Offset.Zero
        isZooming = false
    }

    private val panBoundsX: ClosedFloatingPointRange<Float>
        get() = if (containerSize.width.toFloat() == 0f || fittedImageSize.width == 0f || zoomScale <= 1f) {
            0f..0f
        } else {
            val scaledImageWidth = fittedImageSize.width * zoomScale
            val overflowWidth = (scaledImageWidth - containerSize.width).coerceAtLeast(0f)
            (-overflowWidth / 2f)..(overflowWidth / 2f)
        }

    private val panBoundsY: ClosedFloatingPointRange<Float>
        get() = if (containerSize.height.toFloat() == 0f || fittedImageSize.height == 0f || zoomScale <= 1f) {
            0f..0f
        } else {
            val scaledImageHeight = fittedImageSize.height * zoomScale
            val overflowHeight = (scaledImageHeight - containerSize.height).coerceAtLeast(0f)
            (-overflowHeight / 2f)..(overflowHeight / 2f)
        }
}

@Composable
fun rememberSharedImagePreviewerState() = remember { SharedImagePreviewerState() }