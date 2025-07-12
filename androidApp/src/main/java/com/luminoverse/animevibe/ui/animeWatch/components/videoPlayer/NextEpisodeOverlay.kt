package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.ui.common.EpisodeDetailItem
import com.luminoverse.animevibe.utils.basicContainer
import kotlinx.coroutines.delay

@Composable
fun NextEpisodeOverlay(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isOnlyShowEpisodeDetail: Boolean,
    isLandscape: Boolean,
    animeImage: String?,
    nextEpisode: Episode,
    nextEpisodeDetailComplement: EpisodeDetailComplement?,
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
    onPlayNext: () -> Unit,
    isAutoplayNextEpisode: Boolean
) {
    AnimatedVisibility(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isVisible) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                } else Modifier
            ),
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        var countdown by remember { mutableIntStateOf(3) }

        LaunchedEffect(isVisible, isAutoplayNextEpisode) {
            if (isVisible && isAutoplayNextEpisode) {
                countdown = 3
                while (countdown > 0) {
                    delay(1000)
                    countdown--
                }
                if (countdown == 0) {
                    onPlayNext()
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
            verticalArrangement = if (isLandscape) Arrangement.SpaceBetween else Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isOnlyShowEpisodeDetail) Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isAutoplayNextEpisode && countdown > 0) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("Next Episode in ")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            ) { append("$countdown") }
                        },
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    val nextEpisodeText = if (isAutoplayNextEpisode && countdown == 0) {
                        "Playing next episode..."
                    } else {
                        "Next Episode"
                    }
                    Text(
                        text = nextEpisodeText,
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Close,
                    modifier = Modifier
                        .basicContainer(
                            outerPadding = PaddingValues(0.dp),
                            innerPadding = PaddingValues(8.dp),
                            roundedCornerShape = CircleShape,
                            backgroundBrush = null,
                            onItemClick = onDismiss
                        ),
                    contentDescription = "Dismiss overlay",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!isOnlyShowEpisodeDetail) Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isLandscape) Modifier.weight(1f) else Modifier.wrapContentHeight()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    4.dp,
                    if (isOnlyShowEpisodeDetail) Alignment.CenterVertically else Alignment.Bottom
                )
            ) {
                EpisodeDetailItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .aspectRatio(3.25f),
                    animeImage = animeImage,
                    episode = nextEpisode,
                    episodeDetailComplement = nextEpisodeDetailComplement,
                    onClick = { onPlayNext() },
                    titleMaxLines = 4,
                    isSameWidthContent = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .basicContainer(
                                onItemClick = onRestart,
                                roundedCornerShape = CircleShape,
                                outerPadding = PaddingValues(0.dp),
                                innerPadding = PaddingValues(8.dp)
                            )
                            .weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Restart",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.RestartAlt,
                            contentDescription = "Restart current episode",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Row(
                        modifier = Modifier
                            .basicContainer(
                                isPrimary = true,
                                onItemClick = onPlayNext,
                                roundedCornerShape = CircleShape,
                                outerPadding = PaddingValues(0.dp),
                                innerPadding = PaddingValues(8.dp)
                            )
                            .weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Play now",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Play next episode",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}