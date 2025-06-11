package com.luminoverse.animevibe.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CircularLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Int = 36,
    strokeWidth: Float = 4f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LoadingRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAnimation"
    )

    CircularProgressIndicator(
        modifier = modifier
            .size(size.dp)
            .rotate(rotation),
        color = Color.White,
        strokeWidth = strokeWidth.dp,
        trackColor = Color.White.copy(alpha = 0.3f)
    )
}