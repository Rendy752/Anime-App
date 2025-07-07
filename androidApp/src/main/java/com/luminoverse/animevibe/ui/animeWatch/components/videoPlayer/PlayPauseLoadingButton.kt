package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player

@Composable
fun PlayPauseLoadingButton(
    playbackErrorMessage: String?,
    playbackState: Int,
    isRefreshing: Boolean,
    isPlaying: Boolean,
    onSeekTo: (Long) -> Unit,
    handlePause: () -> Unit,
    handlePlay: () -> Unit,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    var mutableIsPlaying by remember { mutableStateOf(isPlaying) }
    val infiniteTransition = rememberInfiniteTransition(label = "loading_border_transition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
        ),
        label = "loading_border_angle"
    )

    LaunchedEffect(isPlaying, playbackState) {
        mutableIsPlaying = if (playbackState == Player.STATE_IDLE) false else isPlaying
    }

    val borderModifier =
        if ((playbackState == Player.STATE_BUFFERING || isRefreshing) && playbackErrorMessage == null) {
            Modifier.drawBehind {
                val strokeWidth = 3.dp.toPx()
                val brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White,
                        Color.White.copy(alpha = 0.1f)
                    )
                )
                drawArc(
                    brush = brush,
                    startAngle = angle,
                    sweepAngle = 150f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        } else {
            Modifier
        }

    Box(
        modifier = modifier
            .size(size)
            .then(borderModifier)
            .clip(CircleShape)
            .clickable(
                onClick = {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            onSeekTo(0)
                            handlePlay()
                            mutableIsPlaying = true
                        }

                        else -> if (mutableIsPlaying) {
                            handlePause(); mutableIsPlaying = false
                        } else {
                            handlePlay(); mutableIsPlaying = true
                        }
                    }
                }
            )
            .background(
                color = Color.Black.copy(alpha = 0.4f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            modifier = Modifier.size(size - 16.dp),
            targetState = when {
                playbackState == Player.STATE_ENDED -> "ended"
                mutableIsPlaying -> "playing"
                else -> "paused"
            },
            transitionSpec = {
                (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f))
                    .togetherWith(fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.8f))
            },
            label = "PlayerStateAnimation"
        ) { state ->
            when (state) {
                "ended" -> Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Replay",
                    tint = Color.White
                )

                "playing" -> Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color.White
                )

                "paused" -> Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White
                )
            }
        }
    }
}