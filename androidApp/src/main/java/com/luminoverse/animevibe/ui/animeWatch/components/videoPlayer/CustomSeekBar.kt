package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.TimeRange
import com.luminoverse.animevibe.utils.media.PositionState

@Composable
fun CustomSeekBar(
    modifier: Modifier,
    positionState: PositionState,
    intro: TimeRange,
    outro: TimeRange,
    handlePlay: () -> Unit,
    handlePause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDraggingSeekBarChange: (Boolean, Long) -> Unit,
    seekAmount: Long,
    isShowSeekIndicator: Int
) {
    val introStart = intro.start.times(1000L)
    val introEnd = intro.end.times(1000L)
    val outroStart = outro.start.times(1000L)
    val outroEnd = outro.end.times(1000L)

    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(positionState.currentPosition.toFloat()) }
    var initialDragX by remember { mutableFloatStateOf(0f) }
    var initialThumbPositionOnDragStart by remember { mutableFloatStateOf(0f) }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val progress =
        if (positionState.duration > 0) positionState.currentPosition.toFloat() / positionState.duration else 0f
    val bufferedProgressRatio =
        if (positionState.duration > 0) positionState.bufferedPosition.toFloat() / positionState.duration else 0f
    val density = LocalDensity.current
    val dragSensitivityFactor = 1f

    val touchTargetHeight: Dp = 24.dp
    val thumbSize: Dp = 10.dp
    val trackHeight: Dp = 4.dp

    val animatedThumbSize by animateDpAsState(
        targetValue = if (isDragging) thumbSize * 1.8f else thumbSize,
        label = "thumbSizeAnimation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shimmerTransition")
    val shimmerPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPosition"
    )

    LaunchedEffect(positionState.currentPosition, isDragging, seekAmount, isShowSeekIndicator) {
        if (!isDragging) {
            if (isShowSeekIndicator != 0) {
                val targetPosition =
                    (positionState.currentPosition + (seekAmount * isShowSeekIndicator)).coerceIn(
                        0L, positionState.duration
                    )
                dragPosition = targetPosition.toFloat()
            } else {
                dragPosition = positionState.currentPosition.toFloat()
            }
        }
    }

    LaunchedEffect(isDragging, dragPosition, isShowSeekIndicator) {
        val isSeeking = isDragging || isShowSeekIndicator != 0
        onDraggingSeekBarChange(isSeeking, dragPosition.toLong())
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(touchTargetHeight)
            .onSizeChanged { size ->
                trackWidthPx = size.width.toFloat()
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        initialDragX = offset.x
                        initialThumbPositionOnDragStart = dragPosition
                        handlePause()
                    },
                    onDragEnd = {
                        isDragging = false
                        if (positionState.duration > 0 && trackWidthPx > 0) {
                            val finalSeekPosition =
                                dragPosition.toLong().coerceIn(0, positionState.duration)
                            onSeekTo(finalSeekPosition)
                            handlePlay()
                        }
                    },
                    onDrag = { change, _ ->
                        if (positionState.duration > 0 && trackWidthPx > 0) {
                            val dragDeltaX = change.position.x - initialDragX
                            val timeDelta =
                                (dragDeltaX / trackWidthPx) * positionState.duration * dragSensitivityFactor
                            val newPosition =
                                (initialThumbPositionOnDragStart + timeDelta).coerceIn(
                                    0f, positionState.duration.toFloat()
                                )
                            dragPosition = newPosition
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Gray.copy(alpha = 0.3f))
                .align(Alignment.Center)
        )

        if (positionState.duration > 0 && bufferedProgressRatio > progress) {
            val bufferedSegmentStartPx = progress * trackWidthPx
            val bufferedSegmentWidthPx = (bufferedProgressRatio - progress) * trackWidthPx
            Box(
                modifier = Modifier
                    .offset(x = with(density) { bufferedSegmentStartPx.toDp() })
                    .width(with(density) { bufferedSegmentWidthPx.toDp() })
                    .height(trackHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.5f))
                    .align(Alignment.CenterStart)
            )
        }

        if (introStart >= 0 && introEnd > introStart && positionState.duration > 0) {
            val introStartProgress =
                (introStart.toFloat() / positionState.duration).coerceIn(0f, 1f)
            val introEndProgress = (introEnd.toFloat() / positionState.duration).coerceIn(0f, 1f)
            val introWidthPx = (introEndProgress - introStartProgress) * trackWidthPx
            Box(
                modifier = Modifier
                    .offset(x = with(density) { (introStartProgress * trackWidthPx).toDp() })
                    .width(with(density) { introWidthPx.toDp() })
                    .height(trackHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .align(Alignment.CenterStart)
            )
        }

        if (outroStart >= 0 && outroEnd > outroStart && positionState.duration > 0) {
            val outroStartProgress =
                (outroStart.toFloat() / positionState.duration).coerceIn(0f, 1f)
            val outroEndProgress = (outroEnd.toFloat() / positionState.duration).coerceIn(0f, 1f)
            val outroWidthPx = (outroEndProgress - outroStartProgress) * trackWidthPx
            Box(
                modifier = Modifier
                    .offset(x = with(density) { (outroStartProgress * trackWidthPx).toDp() })
                    .width(with(density) { outroWidthPx.toDp() })
                    .height(trackHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .align(Alignment.CenterStart)
            )
        }

        val progressWidth = if (isDragging || isShowSeekIndicator != 0) {
            (dragPosition / positionState.duration).coerceIn(0f, 1f)
        } else {
            progress
        }

        val primaryColor = MaterialTheme.colorScheme.primary
        val progressBrush = if (isDragging) {
            val shimmerWidth = 150f
            val startX =
                (trackWidthPx * progressWidth + shimmerWidth) * shimmerPosition - shimmerWidth
            Brush.horizontalGradient(
                colors = listOf(
                    primaryColor,
                    primaryColor.copy(alpha = 0.5f),
                    primaryColor
                ),
                startX = startX,
                endX = startX + shimmerWidth
            )
        } else {
            SolidColor(primaryColor)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(progressWidth)
                .height(trackHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(progressBrush)
                .align(Alignment.CenterStart)
        )

        val thumbOffset = with(density) {
            val currentThumbPositionPx = progressWidth * trackWidthPx
            (currentThumbPositionPx - (animatedThumbSize.toPx() / 2f)).toDp()
        }

        val thumbBrush = if (isDragging) {
            with(density) {
                Brush.radialGradient(
                    colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                    center = Offset.Unspecified,
                    radius = animatedThumbSize.toPx() / 2
                )
            }
        } else {
            SolidColor(Color.White)
        }

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(animatedThumbSize)
                .clip(CircleShape)
                .background(brush = thumbBrush)
                .align(Alignment.CenterStart)
        )
    }
}