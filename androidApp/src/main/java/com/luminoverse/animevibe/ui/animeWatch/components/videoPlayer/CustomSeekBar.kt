package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.TimeRange

@Composable
fun CustomSeekBar(
    modifier: Modifier,
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    intro: TimeRange,
    outro: TimeRange,
    handlePlay: () -> Unit,
    handlePause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    dragCancelTrigger: Int,
    onDraggingSeekBarChange: (Boolean, Long) -> Unit,
    seekAmount: Long,
    isShowSeekIndicator: Int
) {
    val introStart = intro.start.times(1000L)
    val introEnd = intro.end.times(1000L)
    val outroStart = outro.start.times(1000L)
    val outroEnd = outro.end.times(1000L)

    var isDragging by remember { mutableStateOf(false) }
    var isHolding by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(currentPosition.toFloat()) }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val progress =
        if (duration > 0) currentPosition.toFloat() / duration else 0f
    val bufferedProgressRatio =
        if (duration > 0) bufferedPosition.toFloat() / duration else 0f
    val density = LocalDensity.current

    val touchTargetHeight: Dp = 24.dp
    val thumbSize: Dp = 10.dp
    val trackHeight: Dp = 4.dp

    val animatedThumbSize by animateDpAsState(
        targetValue = if (isDragging || isHolding) thumbSize * 1.8f else thumbSize,
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

    LaunchedEffect(currentPosition, isDragging, seekAmount, isShowSeekIndicator) {
        if (!isDragging) {
            if (isShowSeekIndicator != 0) {
                val targetPosition =
                    (currentPosition + (seekAmount * isShowSeekIndicator)).coerceIn(
                        0L,
                        duration
                    )
                dragPosition = targetPosition.toFloat()
            } else {
                dragPosition = currentPosition.toFloat()
            }
        }
    }

    LaunchedEffect(isDragging, dragPosition, isShowSeekIndicator) {
        val isSeeking = isDragging || isShowSeekIndicator != 0
        onDraggingSeekBarChange(isSeeking, dragPosition.toLong())
    }

    LaunchedEffect(dragCancelTrigger) {
        if (dragCancelTrigger > 0 && isDragging) {
            isDragging = false
            isHolding = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(touchTargetHeight)
            .onSizeChanged { size ->
                trackWidthPx = size.width.toFloat()
            }
            .pointerInput(dragCancelTrigger) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isHolding = true

                    val dragStart = awaitDragOrCancellation(down.id)

                    if (dragStart != null) {
                        isDragging = true
                        handlePause()
                        val initialThumbPosOnDrag = dragPosition

                        drag(dragStart.id) { change: PointerInputChange ->
                            isHolding = false
                            if (duration > 0 && trackWidthPx > 0) {
                                val dragAmount = change.position.x - dragStart.position.x
                                val timeDelta = (dragAmount / trackWidthPx) * duration
                                val newPosition = (initialThumbPosOnDrag + timeDelta)
                                    .coerceIn(0f, duration.toFloat())
                                dragPosition = newPosition
                            }
                            change.consume()
                        }

                        isDragging = false
                        val finalSeekPosition = dragPosition.toLong().coerceIn(0, duration)
                        if (!isHolding) onSeekTo(finalSeekPosition)
                        handlePlay()
                    }

                    isHolding = false
                }
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

        if (duration > 0 && bufferedProgressRatio > progress) {
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

        if (introStart >= 0 && introEnd > introStart && duration > 0) {
            val introStartProgress =
                (introStart.toFloat() / duration).coerceIn(0f, 1f)
            val introEndProgress = (introEnd.toFloat() / duration).coerceIn(0f, 1f)
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

        if (outroStart >= 0 && outroEnd > outroStart && duration > 0) {
            val outroStartProgress =
                (outroStart.toFloat() / duration).coerceIn(0f, 1f)
            val outroEndProgress = (outroEnd.toFloat() / duration).coerceIn(0f, 1f)
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
            (dragPosition / duration).coerceIn(0f, 1f)
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

        val thumbBrush = if (isDragging || isHolding) {
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