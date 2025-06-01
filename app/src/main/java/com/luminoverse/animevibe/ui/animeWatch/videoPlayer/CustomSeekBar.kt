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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect

@Composable
fun CustomSeekBar(
    currentPosition: Long,
    duration: Long,
    introStart: Long,
    introEnd: Long,
    outroStart: Long,
    outroEnd: Long,
    handlePlay: () -> Unit,
    handlePause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDraggingChange: (Boolean, Long) -> Unit,
    touchTargetHeight: Dp = 24.dp,
    thumbSize: Dp = 10.dp,
    trackHeight: Dp = 4.dp
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(currentPosition.toFloat()) }
    var initialDragX by remember { mutableFloatStateOf(0f) }
    var initialThumbPositionOnDragStart by remember { mutableFloatStateOf(0f) }

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val density = LocalDensity.current

    val dragSensitivityFactor = 1f

    LaunchedEffect(currentPosition, isDragging) {
        if (!isDragging) {
            dragPosition = currentPosition.toFloat()
        }
    }

    LaunchedEffect(isDragging, dragPosition) {
        onDraggingChange(isDragging, dragPosition.toLong())
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
                        if (duration > 0 && trackWidthPx > 0) {
                            val finalSeekPosition = dragPosition.toLong().coerceIn(0, duration)
                            onSeekTo(finalSeekPosition)
                            handlePlay()
                        }
                    },
                    onDrag = { change, _ ->
                        if (duration > 0 && trackWidthPx > 0) {
                            val dragDeltaX = change.position.x - initialDragX
                            val timeDelta =
                                (dragDeltaX / trackWidthPx) * duration * dragSensitivityFactor

                            val newPosition =
                                (initialThumbPositionOnDragStart + timeDelta).coerceIn(
                                    0f, duration.toFloat()
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

        if (introStart >= 0 && introEnd > introStart && duration > 0) {
            val introStartProgress = (introStart.toFloat() / duration).coerceIn(0f, 1f)
            val introEndProgress = (introEnd.toFloat() / duration).coerceIn(0f, 1f)
            val introWidthPx = (introEndProgress - introStartProgress) * trackWidthPx
            Box(
                modifier = Modifier
                    .offset(x = with(density) { (introStartProgress * trackWidthPx).toDp() })
                    .width(with(density) { introWidthPx.toDp() })
                    .height(trackHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .align(Alignment.CenterStart)
            )
        }

        if (outroStart >= 0 && outroEnd > outroStart && duration > 0) {
            val outroStartProgress = (outroStart.toFloat() / duration).coerceIn(0f, 1f)
            val outroEndProgress = (outroEnd.toFloat() / duration).coerceIn(0f, 1f)
            val outroWidthPx = (outroEndProgress - outroStartProgress) * trackWidthPx
            Box(
                modifier = Modifier
                    .offset(x = with(density) { (outroStartProgress * trackWidthPx).toDp() })
                    .width(with(density) { outroWidthPx.toDp() })
                    .height(trackHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .align(Alignment.CenterStart)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(
                    if (isDragging) (dragPosition / duration).coerceIn(0f, 1f)
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
                        (if (isDragging) (dragPosition / duration).coerceIn(0f, 1f)
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