package com.luminoverse.animevibe.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A Modifier extension function that applies a sweeping shimmer effect to a Composable.
 *
 * This modifier uses `drawWithCache` to optimize performance by avoiding
 * recomposition of the Composable itself during the animation. The gradient is
 * drawn directly in the drawing phase.
 *
 * @param shape The shape to clip the shimmer effect to. Defaults to `RoundedCornerShape(8.dp)`.
 * @param colors The list of colors to use for the shimmer gradient. Defaults to a MaterialTheme-based shimmer.
 * @param durationMillis The duration of one sweep of the shimmer animation.
 * @param bandWidthDp The width of the bright band in the shimmer.
 * @param angle The angle of the shimmer sweep in degrees (0 is horizontal left-to-right, 90 is vertical top-to-bottom).
 * A 45-degree angle gives a common diagonal shimmer.
 * @param easing The easing function for the animation.
 */
@Composable
fun Modifier.sweepingShimmerBackground(
    shape: Shape = RoundedCornerShape(8.dp),
    colors: List<Color>? = null,
    durationMillis: Int = 1500,
    bandWidthDp: Dp = 200.dp,
    angle: Float = 20f,
    easing: Easing = LinearEasing
): Modifier {
    val shimmerColors = colors ?: listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    )

    val transition = rememberInfiniteTransition(label = "sweeping_shimmer_transition")

    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = easing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweeping_shimmer_translate"
    )

    return this
        .clip(shape)
        .drawWithCache {
            val componentWidth = size.width
            val componentHeight = size.height
            val bandWidthPx = bandWidthDp.toPx()

            val angleRad = Math.toRadians(angle.toDouble()).toFloat()

            val travelDistance = (componentWidth * kotlin.math.abs(kotlin.math.cos(angleRad)) +
                    componentHeight * kotlin.math.abs(kotlin.math.sin(angleRad)) +
                    bandWidthPx).toFloat()

            val bandOffset = translateAnimation * travelDistance - (bandWidthPx / 2f)

            val gradientAngleRad = Math.toRadians(angle - 90.0).toFloat()

            val offsetX = bandOffset * kotlin.math.cos(angleRad)
            val offsetY = bandOffset * kotlin.math.sin(angleRad)

            val perpCos = kotlin.math.cos(gradientAngleRad)
            val perpSin = kotlin.math.sin(gradientAngleRad)

            val start = Offset(
                x = offsetX - perpCos * componentWidth,
                y = offsetY - perpSin * componentHeight
            )
            val end = Offset(
                x = offsetX + perpCos * componentWidth,
                y = offsetY + perpSin * componentHeight
            )

            val brush = Brush.linearGradient(
                colors = shimmerColors,
                start = start,
                end = end
            )

            onDrawBehind {
                drawRect(brush = brush)
            }
        }
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    width: Dp = 0.dp, height: Dp = 100.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    shimmerAngle: Float = 20f
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .sweepingShimmerBackground(shape = shape, angle = shimmerAngle)
    )
}