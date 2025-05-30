package com.luminoverse.animevibe.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun shimmerAnimation(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    )

    val transition = rememberInfiniteTransition()
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ), repeatMode = RepeatMode.Reverse
        )
    )

    return linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(translateAnimation.value, translateAnimation.value)
    )
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, width: Dp = 0.dp, height: Dp = 100.dp) {
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(8.dp))
            .background(shimmerAnimation())
    )
}