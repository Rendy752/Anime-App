package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.utils.TimeUtils.formatTimestamp
import com.luminoverse.animevibe.utils.media.HlsPlayerState

@Composable
fun PlayerControls(
    hlsPlayerState: HlsPlayerState,
    episodeDetailComplement: EpisodeDetailComplement,
    episodes: List<Episode>,
    isLandscape: Boolean,
    isFullscreen: Boolean,
    onPlayPauseRestart: () -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPipClick: () -> Unit,
    onLockToggle: () -> Unit,
    onSubtitleClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onClick: () -> Unit
) {
    val currentEpisode = episodeDetailComplement.servers.episodeNo
    val hasPreviousEpisode = episodes.any { it.episodeNo == currentEpisode - 1 }
    val hasNextEpisode = episodes.any { it.episodeNo == currentEpisode + 1 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episodeDetailComplement.episodeTitle,
                        style = MaterialTheme.typography.titleLarge,
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
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Row {
                    IconButton(onClick = onPipClick) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture in Picture",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onLockToggle) {
                        Icon(
                            imageVector = if (hlsPlayerState.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (hlsPlayerState.isLocked) "Unlock" else "Lock",
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
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }

            // Middle controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier.background(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                    onClick = onPreviousEpisode,
                    enabled = hasPreviousEpisode
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Episode",
                        tint = if (hasPreviousEpisode) Color.White else Color.Gray
                    )
                }

                AnimatedVisibility(
                    visible = (hlsPlayerState.playbackState != Player.STATE_BUFFERING && hlsPlayerState.playbackState != Player.STATE_IDLE),
                    modifier = Modifier.background(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    IconButton(onClick = onPlayPauseRestart) {
                        AnimatedContent(
                            targetState = when (hlsPlayerState.playbackState) {
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
                            }
                        }
                    }
                }

                IconButton(
                    modifier = Modifier.background(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                    onClick = onNextEpisode,
                    enabled = hasNextEpisode
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Episode",
                        tint = if (hasNextEpisode) Color.White else Color.Gray
                    )
                }
            }

            // Bottom controls
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatTimestamp(hlsPlayerState.currentPosition)} / ${
                            formatTimestamp(hlsPlayerState.duration)
                        }",
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
                LinearProgressIndicator(
                    progress = {
                        if (hlsPlayerState.duration > 0) {
                            hlsPlayerState.currentPosition.toFloat() / hlsPlayerState.duration
                        } else {
                            0f
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Handle progress bar click to seek
                        }
                )
            }
        }
    }
}