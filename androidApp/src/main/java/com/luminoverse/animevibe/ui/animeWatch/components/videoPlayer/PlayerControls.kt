package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.ui.common.EpisodeDetailItem
import com.luminoverse.animevibe.utils.TimeUtils.formatTimestamp

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    currentPosition: Long,
    cachedPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    playbackState: Int,
    playbackErrorMessage: String?,
    onHandleBackPress: () -> Unit,
    episodeDetailComplement: EpisodeDetailComplement,
    hasPreviousEpisode: Boolean,
    nextEpisode: Episode?,
    nextEpisodeDetailComplement: EpisodeDetailComplement?,
    isSideSheetVisible: Boolean,
    setSideSheetVisibility: (Boolean) -> Unit,
    isLandscape: Boolean,
    isShowSpeedUp: Boolean,
    zoomText: String,
    onZoomReset: () -> Unit,
    handlePlay: () -> Unit,
    handlePause: () -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onSeekTo: (Long) -> Unit,
    seekAmount: Long,
    isShowSeekIndicator: Int,
    dragSeekPosition: Long,
    dragCancelTrigger: Int,
    onDraggingSeekBarChange: (Boolean, Long) -> Unit,
    isDraggingSeekBar: Boolean,
    showRemainingTime: Boolean,
    setShowRemainingTime: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onBottomBarMeasured: (Float) -> Unit
) {
    val shouldShowControls = isShowSeekIndicator == 0 && !isDraggingSeekBar && !isShowSpeedUp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        TopSection(
            modifier = Modifier.align(Alignment.TopCenter),
            shouldShowControls = shouldShowControls,
            onHandleBackPress = onHandleBackPress,
            isLandscape = isLandscape,
            playbackState = playbackState,
            episodeDetailComplement = episodeDetailComplement,
            nextEpisode = nextEpisode,
            nextEpisodeDetailComplement = nextEpisodeDetailComplement,
            isSideSheetVisible = isSideSheetVisible,
            setSideSheetVisibility = setSideSheetVisibility,
            zoomText = zoomText,
            onZoomReset = onZoomReset,
            onSettingsClick = onSettingsClick,
            onNextEpisode = onNextEpisode
        )

        MiddleSection(
            modifier = Modifier.align(Alignment.Center),
            playbackErrorMessage = playbackErrorMessage,
            shouldShowControls = shouldShowControls,
            hasPreviousEpisode = hasPreviousEpisode,
            playbackState = playbackState,
            handlePlay = handlePlay,
            handlePause = handlePause,
            onPreviousEpisode = onPreviousEpisode,
            onNextEpisode = onNextEpisode,
            onSeekTo = onSeekTo,
            nextEpisode = nextEpisode,
            isPlaying = isPlaying,
            isShowSeekIndicator = isShowSeekIndicator,
            isDraggingSeekBar = isDraggingSeekBar,
            dragSeekPosition = dragSeekPosition,
            showRemainingTime = showRemainingTime,
            duration = duration
        )

        BottomSection(
            modifier = Modifier.align(Alignment.BottomCenter),
            shouldShowControls = shouldShowControls,
            isShowSeekIndicator = isShowSeekIndicator,
            onFullscreenToggle = onFullscreenToggle,
            isLandscape = isLandscape,
            handlePlay = handlePlay,
            handlePause = handlePause,
            onSeekTo = onSeekTo,
            seekAmount = seekAmount,
            dragCancelTrigger = dragCancelTrigger,
            onDraggingSeekBarChange = onDraggingSeekBarChange,
            showRemainingTime = showRemainingTime,
            setShowRemainingTime = setShowRemainingTime,
            currentPosition = currentPosition,
            cachedPosition = cachedPosition,
            duration = duration,
            bufferedPosition = bufferedPosition,
            episodeDetailComplement = episodeDetailComplement,
            onBottomBarMeasured = onBottomBarMeasured,
        )
    }
}

@Composable
private fun TopSection(
    modifier: Modifier,
    shouldShowControls: Boolean,
    onHandleBackPress: () -> Unit,
    isLandscape: Boolean,
    playbackState: Int,
    episodeDetailComplement: EpisodeDetailComplement,
    nextEpisode: Episode?,
    nextEpisodeDetailComplement: EpisodeDetailComplement?,
    isSideSheetVisible: Boolean,
    setSideSheetVisibility: (Boolean) -> Unit,
    zoomText: String,
    onZoomReset: () -> Unit,
    onSettingsClick: () -> Unit,
    onNextEpisode: () -> Unit
) {
    AnimatedVisibility(
        visible = shouldShowControls,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
                    imageVector = if (isLandscape) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Return back",
                    tint = Color.White
                )
                AnimatedVisibility(
                    visible = isLandscape && (playbackState != Player.STATE_ENDED || nextEpisode == null) && !isSideSheetVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Row(
                        modifier = Modifier.clickable { setSideSheetVisibility(true) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(start = 4.dp)) {
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
                        Icon(
                            modifier = Modifier
                                .clip(CircleShape)
                                .padding(8.dp),
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Open currently watching anime info",
                            tint = Color.White
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(visible = zoomText != "Original") {
                    Text(
                        text = zoomText,
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                            .clickable { onZoomReset() }
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
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
}

@Composable
fun PlayPauseLoadingButton(
    playbackErrorMessage: String?,
    playbackState: Int,
    isPlaying: Boolean,
    onSeekTo: (Long) -> Unit,
    handlePause: () -> Unit,
    handlePlay: () -> Unit,
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

    LaunchedEffect(isPlaying) {
        mutableIsPlaying = isPlaying
    }

    val borderModifier = if (playbackState == Player.STATE_BUFFERING && playbackErrorMessage == null) {
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
            .size(56.dp)
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
            modifier = Modifier.size(40.dp),
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

@Composable
private fun MiddleSection(
    modifier: Modifier,
    playbackErrorMessage: String?,
    shouldShowControls: Boolean,
    hasPreviousEpisode: Boolean,
    playbackState: Int,
    handlePlay: () -> Unit,
    handlePause: () -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onSeekTo: (Long) -> Unit,
    nextEpisode: Episode?,
    isPlaying: Boolean,
    isShowSeekIndicator: Int,
    isDraggingSeekBar: Boolean,
    dragSeekPosition: Long,
    showRemainingTime: Boolean,
    duration: Long
) {
    AnimatedVisibility(
        visible = shouldShowControls,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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

            PlayPauseLoadingButton(
                playbackErrorMessage = playbackErrorMessage,
                playbackState = playbackState,
                isPlaying = isPlaying,
                onSeekTo = onSeekTo,
                handlePause = handlePause,
                handlePlay = handlePlay
            )

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
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(vertical = 8.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val remainingTime = duration - dragSeekPosition
            Text(
                text = if (showRemainingTime && duration > 0) {
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
}

@Composable
private fun BottomSection(
    modifier: Modifier,
    shouldShowControls: Boolean,
    isShowSeekIndicator: Int,
    onFullscreenToggle: () -> Unit,
    isLandscape: Boolean,
    handlePlay: () -> Unit,
    handlePause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    seekAmount: Long,
    dragCancelTrigger: Int,
    onDraggingSeekBarChange: (Boolean, Long) -> Unit,
    showRemainingTime: Boolean,
    setShowRemainingTime: (Boolean) -> Unit,
    currentPosition: Long,
    cachedPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    episodeDetailComplement: EpisodeDetailComplement,
    onBottomBarMeasured: (Float) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = if (isLandscape) 8.dp else 0.dp)
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
                            if (showRemainingTime && duration > 0) {
                                val remainingTime = duration - currentPosition
                                append("-${formatTimestamp(remainingTime)}")
                            } else {
                                append(formatTimestamp(currentPosition))
                            }
                        }
                        withStyle(style = SpanStyle(color = Color.White.copy(alpha = 0.8f))) {
                            append(" / ")
                            append(
                                if (duration > 0) formatTimestamp(
                                    duration
                                ) else "--:--"
                            )
                        }
                    },
                    fontSize = 14.sp
                )
                Icon(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onFullscreenToggle() }
                        .padding(8.dp),
                    imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isLandscape) "Exit Fullscreen" else "Enter Fullscreen",
                    tint = Color.White
                )
            }
        }
        CustomSeekBar(
            modifier = Modifier.onGloballyPositioned {
                val heightInPx = it.size.height.toFloat()
                onBottomBarMeasured(heightInPx)
            },
            currentPosition = currentPosition,
            cachedPosition = cachedPosition,
            duration = duration,
            bufferedPosition = bufferedPosition,
            intro = episodeDetailComplement.sources.intro,
            outro = episodeDetailComplement.sources.outro,
            handlePlay = handlePlay,
            handlePause = handlePause,
            onSeekTo = onSeekTo,
            dragCancelTrigger = dragCancelTrigger,
            onDraggingSeekBarChange = onDraggingSeekBarChange,
            seekAmount = seekAmount,
            isShowSeekIndicator = isShowSeekIndicator,
        )
    }
}