package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.luminoverse.animevibe.utils.TimeUtils.formatTimestamp
import com.luminoverse.animevibe.utils.media.CaptionCue
import com.luminoverse.animevibe.utils.media.vttTextToAnnotatedString
import java.util.Locale
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
fun SyncSubtitleOverlay(
    isVisible: Boolean,
    allCues: List<CaptionCue>,
    activeCues: List<CaptionCue>,
    currentOffset: Long,
    playerCurrentPosition: Long,
    onSyncToCue: (startTimeMs: Long) -> Unit,
    onAdjustOffset: (adjustmentMs: Long) -> Unit,
    onResetOffset: () -> Unit,
    onDismiss: () -> Unit,
    playbackState: Int,
    isPlaying: Boolean,
    isRefreshing: Boolean,
    onSeekTo: (positionMs: Long) -> Unit,
    handlePause: () -> Unit,
    handlePlay: () -> Unit
) {
    val lazyListState = rememberLazyListState()

    LaunchedEffect(activeCues, isVisible) {
        if (isVisible && activeCues.isNotEmpty() && allCues.isNotEmpty()) {
            val targetIndex = allCues.indexOfFirst { it.startTime == activeCues.first().startTime }
                .coerceAtLeast(0)

            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            if (visibleItems.none { it.index == targetIndex }) {
                val firstVisibleIndex = lazyListState.firstVisibleItemIndex
                val distance = abs(targetIndex - firstVisibleIndex)
                if (distance < 20) {
                    lazyListState.animateScrollToItem(targetIndex)
                } else {
                    lazyListState.scrollToItem(targetIndex)
                }
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(500.dp)
                    .align(Alignment.CenterEnd)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {}
            ) {
                Header(
                    onDismiss = onDismiss,
                    playbackState = playbackState,
                    isRefreshing = isRefreshing,
                    isPlaying = isPlaying,
                    currentPositionMs = playerCurrentPosition,
                    onSeekTo = onSeekTo,
                    handlePause = handlePause,
                    handlePlay = handlePlay
                )

                ManualAdjustControls(
                    currentOffset = currentOffset,
                    onAdjustOffset = onAdjustOffset,
                    onResetOffset = onResetOffset
                )

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            allCues,
                            key = { index, cue -> "${cue.startTime}-${cue.text}-$index" }) { _, cue ->
                            val isActive =
                                activeCues.any { it.startTime == cue.startTime && it.text == cue.text }
                            SyncCueItem(
                                cue = cue,
                                isActive = isActive,
                                onClick = { onSyncToCue(cue.startTime) }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    onDismiss: () -> Unit,
    playbackState: Int,
    isRefreshing: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    handlePause: () -> Unit,
    handlePlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Adjust Subtitle Sync",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.weight(1f))
        PlaybackControls(
            playbackState = playbackState,
            isRefreshing = isRefreshing,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            onSeekTo = onSeekTo,
            handlePause = handlePause,
            handlePlay = handlePlay
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss)
                .background(color = Color.Black.copy(alpha = 0.4f), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Sync Screen",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    playbackState: Int,
    isRefreshing: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    handlePause: () -> Unit,
    handlePlay: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = { onSeekTo(currentPositionMs - 5000) })
                .background(color = Color.Black.copy(alpha = 0.4f), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Replay5,
                contentDescription = "Seek Back 5s",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        PlayPauseLoadingButton(
            playbackState = playbackState,
            isRefreshing = isRefreshing,
            isPlaying = isPlaying,
            handleRestart = { onSeekTo(0) },
            handlePause = handlePause,
            handlePlay = handlePlay,
            size = 40.dp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = { onSeekTo(currentPositionMs + 5000) })
                .background(color = Color.Black.copy(alpha = 0.4f), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Forward5,
                contentDescription = "Seek Forward 5s",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun ManualAdjustControls(
    currentOffset: Long,
    onAdjustOffset: (Long) -> Unit,
    onResetOffset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Manual Offset:", style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onResetOffset) {
                Icon(Icons.Default.RestartAlt, "Reset Offset", tint = Color.White)
            }
            IconButton(onClick = { onAdjustOffset(-100L) }) {
                Icon(Icons.Default.Remove, "Decrease Offset", tint = Color.White)
            }
            Text(
                text = String.format(Locale.US, "%.1fs", currentOffset / 1000.0),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.width(60.dp)
            )
            IconButton(onClick = { onAdjustOffset(100L) }) {
                Icon(Icons.Default.Add, "Increase Offset", tint = Color.White)
            }
        }
    }
}

@Composable
private fun TextWithOutline(text: String, modifier: Modifier = Modifier) {
    Box(modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 14.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                drawStyle = Stroke(width = 4f, join = StrokeJoin.Round)
            )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun SyncCueItem(cue: CaptionCue, isActive: Boolean, onClick: () -> Unit) {
    val animatedBackground by animateDpAsState(
        targetValue = if (isActive) 4.dp else 0.dp,
        label = "activeCueBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = animatedBackground,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextWithOutline(text = formatTimestamp(cue.startTime))
        Text(
            text = vttTextToAnnotatedString(cue.text),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}