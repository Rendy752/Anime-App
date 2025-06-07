// PlayerControls.kt
package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.ui.common.CircularLoadingIndicator
import com.luminoverse.animevibe.ui.common.EpisodeDetailItem
import com.luminoverse.animevibe.utils.TimeUtils.formatTimestamp
import com.luminoverse.animevibe.utils.media.PositionState

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    playbackState: Int,
    positionState: PositionState,
    onHandleBackPress: () -> Unit,
    episodeDetailComplement: EpisodeDetailComplement,
    hasPreviousEpisode: Boolean,
    nextEpisode: Episode?,
    nextEpisodeDetailComplement: EpisodeDetailComplement?,
    isFullscreen: Boolean,
    isShowSpeedUp: Boolean,
    handlePlay: () -> Unit,
    handlePause: () -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onSeekTo: (Long) -> Unit,
    seekAmount: Long,
    isShowSeekIndicator: Int,
    dragSeekPosition: Long,
    onDraggingSeekBarChange: (Boolean, Long) -> Unit,
    isDraggingSeekBar: Boolean,
    showRemainingTime: Boolean,
    setShowRemainingTime: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onFullscreenToggle: () -> Unit,
) {
    val shouldShowControls = isShowSeekIndicator == 0 && !isDraggingSeekBar && !isShowSpeedUp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        AnimatedVisibility(
            visible = shouldShowControls,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onHandleBackPress() }
                            .padding(8.dp),
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Return back",
                        tint = Color.White
                    )
                    AnimatedVisibility(
                        visible = isFullscreen && (playbackState != Player.STATE_ENDED || nextEpisode == null),
                        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 300)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = episodeDetailComplement.episodeTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Text(
                                text = episodeDetailComplement.animeTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    nextEpisode?.let { nextEpisode ->
                        AnimatedVisibility(
                            visible = playbackState == Player.STATE_ENDED,
                            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 300)),
                        ) {
                            EpisodeDetailItem(
                                modifier = Modifier
                                    .heightIn(max = 60.dp)
                                    .aspectRatio(3.25f),
                                animeImage = episodeDetailComplement.imageUrl,
                                episode = nextEpisode,
                                episodeDetailComplement = nextEpisodeDetailComplement,
                                onClick = { onNextEpisode() },
                                titleMaxLines = 4,
                                isSameWidthContent = true
                            )
                        }
                    }
                }
                Row {
                    Icon(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSettingsClick() }
                            .padding(8.dp),
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = shouldShowControls,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(64.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(enabled = hasPreviousEpisode, onClick = onPreviousEpisode)
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Episode",
                        tint = if (hasPreviousEpisode) Color.White else Color.Gray,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = playbackState != Player.STATE_BUFFERING && playbackState != Player.STATE_IDLE,
                            onClick = {
                                when (playbackState) {
                                    Player.STATE_ENDED -> onSeekTo(0)
                                    else -> if (isPlaying) {
                                        handlePause()
                                    } else {
                                        handlePlay()
                                    }
                                }
                            }
                        )
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                ) {
                    AnimatedContent(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center),
                        targetState = when (playbackState) {
                            Player.STATE_BUFFERING -> "buffering"
                            Player.STATE_IDLE -> "idle"
                            Player.STATE_ENDED -> "ended"
                            else -> if (isPlaying) "playing" else "paused"
                        },
                        transitionSpec = {
                            (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f))
                                .togetherWith(
                                    fadeOut(tween(300)) + scaleOut(
                                        tween(300),
                                        targetScale = 0.8f
                                    )
                                )
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

                            else -> CircularLoadingIndicator()
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(enabled = nextEpisode != null, onClick = onNextEpisode)
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Episode",
                        tint = if (nextEpisode != null) Color.White else Color.Gray,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isShowSeekIndicator != 0 || isDraggingSeekBar,
            enter = slideInHorizontally(
                animationSpec = tween(durationMillis = 300),
                initialOffsetX = { fullWidth -> -fullWidth }
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = slideOutHorizontally(
                animationSpec = tween(durationMillis = 300),
                targetOffsetX = { fullWidth -> fullWidth }
            ) + fadeOut(animationSpec = tween(durationMillis = 300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val remainingTime = positionState.duration - dragSeekPosition
                Text(
                    text = if (showRemainingTime && positionState.duration > 0) {
                        "-${formatTimestamp(remainingTime)}"
                    } else {
                        formatTimestamp(dragSeekPosition)
                    },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            AnimatedVisibility(
                visible = shouldShowControls,
                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { setShowRemainingTime(!showRemainingTime) }
                            .padding(8.dp),
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                ),
                            ) {
                                if (showRemainingTime && positionState.duration > 0) {
                                    val remainingTime =
                                        positionState.duration - positionState.currentPosition
                                    append("-${formatTimestamp(remainingTime)}")
                                } else {
                                    append(formatTimestamp(positionState.currentPosition))
                                }
                            }
                            withStyle(style = SpanStyle(color = Color.White.copy(alpha = 0.8f))) {
                                append(" / ")
                                append(if (positionState.duration > 0) formatTimestamp(positionState.duration) else "--:--")
                            }
                        },
                        fontSize = 14.sp
                    )
                    Icon(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onFullscreenToggle() }
                            .padding(8.dp),
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                        tint = Color.White
                    )
                }
            }
            CustomSeekBar(
                positionState = positionState,
                introStart = episodeDetailComplement.sources.intro?.start?.times(1000L) ?: 0L,
                introEnd = episodeDetailComplement.sources.intro?.end?.times(1000L) ?: 0L,
                outroStart = episodeDetailComplement.sources.outro?.start?.times(1000L) ?: 0L,
                outroEnd = episodeDetailComplement.sources.outro?.end?.times(1000L) ?: 0L,
                handlePlay = handlePlay,
                handlePause = handlePause,
                onSeekTo = onSeekTo,
                onDraggingSeekBarChange = onDraggingSeekBarChange,
                seekAmount = seekAmount,
                isShowSeekIndicator = isShowSeekIndicator,
            )
        }
    }
}