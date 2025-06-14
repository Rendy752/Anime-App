package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.utils.media.PositionState

@Composable
fun CustomSeekBar(
    positionState: PositionState,
    introStart: Long,
    introEnd: Long,
    outroStart: Long,
    outroEnd: Long,
    handlePlay: () -> Unit,
    handlePause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDraggingSeekBarChange: (Boolean, Long) -> Unit,
    seekAmount: Long,
    isShowSeekIndicator: Int,
    touchTargetHeight: Dp = 24.dp,
    thumbSize: Dp = 10.dp,
    trackHeight: Dp = 4.dp
) {
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
        modifier = Modifier
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

        Box(
            modifier = Modifier
                .fillMaxWidth(
                    if (isDragging || isShowSeekIndicator != 0) (dragPosition / positionState.duration).coerceIn(
                        0f, 1f
                    )
                    else progress
                )
                .height(trackHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary)
                .align(Alignment.CenterStart)
        )

        Box(
            modifier = Modifier
                .offset(x = with(density) {
                    val currentThumbPositionPx =
                        (if (isDragging || isShowSeekIndicator != 0) (dragPosition / positionState.duration).coerceIn(
                            0f, 1f
                        )
                        else progress) * trackWidthPx
                    (currentThumbPositionPx - (thumbSize.toPx() / 2f)).toDp()
                })
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
                .align(Alignment.CenterStart)
        )
    }
}