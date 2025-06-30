package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.animeDetailPlaceholder
import com.luminoverse.animevibe.models.episodeDetailComplementPlaceholder
import com.luminoverse.animevibe.models.episodePlaceholder
import com.luminoverse.animevibe.utils.basicContainer
import com.luminoverse.animevibe.utils.watch.WatchUtils.getEpisodeBackgroundColor

@Preview
@Composable
fun WatchEpisodeItemPreview() {
    WatchEpisodeItem(
        imageUrl = animeDetailPlaceholder.images.webp.large_image_url,
        currentEpisode = episodeDetailComplementPlaceholder.copy(number = 2),
        episode = episodePlaceholder,
        isHighlighted = true,
        isNew = true,
        episodeDetailComplement = episodeDetailComplementPlaceholder,
        onEpisodeClick = {},
        isSelected = false
    )
}

@Composable
fun WatchEpisodeItem(
    imageUrl: String?,
    currentEpisode: EpisodeDetailComplement?,
    episode: Episode,
    isHighlighted: Boolean,
    isNew: Boolean,
    episodeDetailComplement: EpisodeDetailComplement?,
    onEpisodeClick: (String) -> Unit,
    isSelected: Boolean
) {
    val isCurrentEpisode =
        if (currentEpisode != null) currentEpisode.number == episode.episode_no else false
    var showTooltip by remember { mutableStateOf(false) }
    val backgroundColor =
        getEpisodeBackgroundColor(
            episode.filler,
            episodeDetailComplement,
            if (isSelected) true else isCurrentEpisode,
        )

    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteGlow")

    val animatedScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulsingScale"
    )

    val animatedGlowRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulsingGlowRadius"
    )

    val scale = if (isHighlighted) animatedScale else 1f
    val glowRadius = if (isHighlighted) animatedGlowRadius else 0f

    Surface(
        modifier = Modifier
            .widthIn(min = 48.dp, max = 100.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .then(
                Modifier.border(
                    BorderStroke(
                        width = 1.5.dp,
                        color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            ),
        shadowElevation = 0.dp,
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .basicContainer(
                    useBorder = !isHighlighted,
                    backgroundBrush = backgroundColor,
                    outerPadding = PaddingValues(0.dp),
                    innerPadding = PaddingValues(0.dp),
                )
                .then(
                    if (!isCurrentEpisode) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onEpisodeClick(episode.id) },
                                onLongPress = { showTooltip = true },
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            val glowColor = MaterialTheme.colorScheme.primary

            Text(
                text = episode.episode_no.toString(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                style = if (isHighlighted) {
                    MaterialTheme.typography.bodyLarge.copy(
                        shadow = Shadow(
                            color = glowColor.copy(alpha = 0.9f),
                            offset = Offset.Zero,
                            blurRadius = glowRadius
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    MaterialTheme.typography.bodyLarge
                }
            )

            if (isNew) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {}
            }
        }

        if (showTooltip) {
            WatchEpisodePopup(
                onDismissRequest = { showTooltip = false },
                imageUrl = imageUrl,
                episode = episode,
                episodeDetailComplement = episodeDetailComplement
            )
        }
    }
}