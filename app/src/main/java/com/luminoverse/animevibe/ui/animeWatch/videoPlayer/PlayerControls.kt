package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.luminoverse.animevibe.utils.TimeUtils.formatTimestamp
import com.luminoverse.animevibe.utils.media.HlsPlayerState

@Composable
fun PlayerControls(
    hlsPlayerState: HlsPlayerState,
    onHandleBackPress: () -> Unit,
    episodeDetailComplement: EpisodeDetailComplement,
    episodes: List<Episode>,
    isFullscreen: Boolean,
    isShowSpeedUp: Boolean,
    handlePlay: () -> Unit,
    handlePause: () -> Unit,
    onPlayPauseRestart: () -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onSeekTo: (Long) -> Unit,
    seekAmount: Long,
    isShowSeekIndicator: Int,
    dragSeekPosition: Long,
    isDraggingSeekBar: Boolean,
    onDraggingSeekBarChange: (Boolean, Long) -> Unit,
    onPipClick: () -> Unit,
    onLockClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onFullscreenToggle: () -> Unit
) {
    val currentEpisode = episodeDetailComplement.servers.episodeNo
    val hasPreviousEpisode = episodes.any { it.episodeNo == currentEpisode - 1 }
    val hasNextEpisode = episodes.any { it.episodeNo == currentEpisode + 1 }
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
                    IconButton(onClick = onHandleBackPress) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Return back",
                            tint = Color.White
                        )
                    }
                    if (isFullscreen) Column {
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
                Row {
                    IconButton(onClick = onPipClick) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture in Picture",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onPlaybackSpeedClick) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onSubtitleClick) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Subtitles",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onLockClick) {
                        Icon(
                            imageVector = if (hlsPlayerState.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (hlsPlayerState.isLocked) "Unlock" else "Lock",
                            tint = Color.White
                        )
                    }
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
                            enabled = hlsPlayerState.playbackState != Player.STATE_BUFFERING && hlsPlayerState.playbackState != Player.STATE_IDLE,
                            onClick = onPlayPauseRestart
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
                        targetState = when (hlsPlayerState.playbackState) {
                            Player.STATE_BUFFERING -> "buffering"
                            Player.STATE_IDLE -> "idle"
                            Player.STATE_ENDED -> "ended"
                            else -> if (hlsPlayerState.isPlaying) "playing" else "paused"
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
                        .clickable(enabled = hasNextEpisode, onClick = onNextEpisode)
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Episode",
                        tint = if (hasNextEpisode) Color.White else Color.Gray,
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
                Text(
                    text = formatTimestamp(dragSeekPosition),
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
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(formatTimestamp(hlsPlayerState.currentPosition))
                            }
                            append(" / ")
                            append(if (hlsPlayerState.duration > 0) formatTimestamp(hlsPlayerState.duration) else "--:--")
                        },
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = onFullscreenToggle) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }
            CustomSeekBar(
                currentPosition = hlsPlayerState.currentPosition,
                duration = hlsPlayerState.duration,
                introStart = episodeDetailComplement.sources.intro?.start?.times(1000L) ?: 0L,
                introEnd = episodeDetailComplement.sources.intro?.end?.times(1000L) ?: 0L,
                outroStart = episodeDetailComplement.sources.outro?.start?.times(1000L) ?: 0L,
                outroEnd = episodeDetailComplement.sources.outro?.end?.times(1000L) ?: 0L,
                handlePlay = handlePlay,
                handlePause = handlePause,
                onSeekTo = onSeekTo,
                onDraggingSeekBarChange = onDraggingSeekBarChange,
                seekAmount = seekAmount,
                isShowSeekIndicator = isShowSeekIndicator
            )
        }
    }
}