package com.luminoverse.animevibe.ui.common_ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.animeDetailPlaceholder
import com.luminoverse.animevibe.models.episodeDetailComplementPlaceholder
import com.luminoverse.animevibe.models.episodePlaceholder
import com.luminoverse.animevibe.ui.animeDetail.DetailAction
import com.luminoverse.animevibe.utils.WatchUtils.getEpisodeBackgroundColor
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun EpisodeDetailItem(
    modifier: Modifier = Modifier,
    animeDetail: AnimeDetail = animeDetailPlaceholder,
    lastEpisodeWatchedId: String? = null,
    episode: Episode = episodePlaceholder,
    episodeDetailComplement: EpisodeDetailComplement? = episodeDetailComplementPlaceholder,
    query: String = "",
    onAction: (DetailAction) -> Unit = {},
    onClick: (String) -> Unit = {},
    navBackStackEntry: NavBackStackEntry? = null,
    titleMaxLines: Int? = null
) {
    val lifecycleState by navBackStackEntry?.lifecycle?.currentStateFlow?.collectAsStateWithLifecycle()
        ?: return

    LaunchedEffect(episode.episodeId, lifecycleState) {
        if (episodeDetailComplement == null || lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
            onAction(DetailAction.LoadEpisodeDetailComplement(episode.episodeId))
        }
    }

    val progress = episodeDetailComplement?.let { complement ->
        if (complement.lastTimestamp != null && complement.duration != null && complement.lastTimestamp < complement.duration) {
            (complement.lastTimestamp.toFloat() / complement.duration).coerceIn(0f, 1f)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .basicContainer(
                onItemClick = { onClick(episode.episodeId) },
                backgroundBrush = getEpisodeBackgroundColor(
                    episode.filler,
                    episodeDetailComplement
                ),
                roundedCornerShape = RoundedCornerShape(8.dp),
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(0.dp)
            )
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenshotDisplay(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.4f),
                imageUrl = animeDetail.images.webp.large_image_url,
                screenshot = episodeDetailComplement?.screenshot
            )

            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = highlightText(episode.name, query),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = titleMaxLines ?: Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ep. ${episode.episodeNo}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    lastEpisodeWatchedId?.let {
                        if (episode.episodeId == it) {
                            Text(
                                text = "Last Watched",
                                modifier = Modifier
                                    .basicContainer(
                                        outerPadding = PaddingValues(0.dp),
                                        innerPadding = PaddingValues(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        ),
                                        backgroundBrush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        )
                                    ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        progress?.let {
            LinearProgressIndicator(
                progress = { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Preview
@Composable
fun EpisodeDetailItemSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .basicContainer(
                roundedCornerShape = RoundedCornerShape(8.dp),
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(0.dp)
            )
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .height(76.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .aspectRatio(16f / 9f)
                    .weight(0.4f)
            )

            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(
                        modifier = Modifier
                            .width(40.dp)
                            .height(16.dp)
                    )
                    SkeletonBox(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .width(88.dp)
                            .height(20.dp)
                    )
                }
            }
        }

        SkeletonBox(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
        )
    }
}