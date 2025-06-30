package com.luminoverse.animevibe.utils.media

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize

object BoundsUtils {
    /**
    A helper function to calculate the maximum allowed pan offsets based on the container size,
    the image's intrinsic size, and the current zoom scale. This prevents panning into blank areas.
     */
    fun calculateOffsetBounds(
        containerSize: IntSize,
        imageSize: Size,
        scale: Float
    ): Pair<Float, Float> {

        val containerWidth = containerSize.width.toFloat()
        val containerHeight = containerSize.height.toFloat()
        val imageWidth = imageSize.width
        val imageHeight = imageSize.height

        val scaledImageWidth = imageWidth * scale
        val scaledImageHeight = imageHeight * scale

        val overflowWidth = (scaledImageWidth - containerWidth).coerceAtLeast(0f)
        val overflowHeight = (scaledImageHeight - containerHeight).coerceAtLeast(0f)

        return (overflowWidth / 2f) to (overflowHeight / 2f)
    }
}