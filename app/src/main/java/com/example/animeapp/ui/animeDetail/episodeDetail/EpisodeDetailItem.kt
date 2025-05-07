package com.example.animeapp.ui.animeDetail.episodeDetail

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.Episode
import com.example.animeapp.ui.animeDetail.DetailAction
import com.example.animeapp.ui.animeDetail.DetailState
import com.example.animeapp.ui.common_ui.ScreenshotDisplay
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.WatchUtils.getEpisodeBackgroundColor
import com.example.animeapp.utils.basicContainer

@Composable
fun EpisodeDetailItem(
    animeDetail: AnimeDetail,
    animeDetailComplement: AnimeDetailComplement,
    episode: Episode,
    detailState: DetailState,
    query: String,
    onAction: (DetailAction) -> Unit,
    onClick: (String) -> Unit,
    navBackStackEntry: NavBackStackEntry?
) {
    val lifecycleState by navBackStackEntry?.lifecycle?.currentStateFlow?.collectAsStateWithLifecycle()
        ?: return

    LaunchedEffect(episode.episodeId, lifecycleState) {
        if (detailState.episodeDetailComplements[episode.episodeId] == null || lifecycleState.isAtLeast(
                Lifecycle.State.RESUMED
            )
        ) {
            onAction(DetailAction.LoadEpisodeDetailComplement(episode.episodeId))
        }
    }

    val episodeDetailComplementResource = detailState.episodeDetailComplements[episode.episodeId]
    val episodeDetailComplement = (episodeDetailComplementResource as? Resource.Success)?.data

    Row(
        modifier = Modifier
            .basicContainer(
                onItemClick = { onClick(episode.episodeId) },
                backgroundBrush = getEpisodeBackgroundColor(
                    episode.filler,
                    episodeDetailComplement
                ),
                roundedCornerShape = RoundedCornerShape(8.dp),
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(8.dp)
            )
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenshotDisplay(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .weight(0.4f),
            imageUrl = animeDetail.images.webp.large_image_url,
            screenshot = episodeDetailComplement?.screenshot
        )

        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = highlightText(episode.name, query),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ep. ${episode.episodeNo}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                animeDetailComplement.lastEpisodeWatchedId?.let { lastEpisodeWatchedId ->
                    if (episodeDetailComplement?.id == lastEpisodeWatchedId) {
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
}

@Composable
fun highlightText(text: String, query: String): AnnotatedString {
    val annotatedString = AnnotatedString.Builder(text)
    if (query.isNotBlank()) {
        val startIndex = text.indexOf(query, ignoreCase = true)
        if (startIndex != -1) {
            annotatedString.addStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                ),
                start = startIndex,
                end = startIndex + query.length
            )
        }
    }
    return annotatedString.toAnnotatedString()
}

@Preview
@Composable
fun EpisodeDetailItemSkeleton() {
    Row(
        modifier = Modifier
            .basicContainer(
                roundedCornerShape = RoundedCornerShape(8.dp),
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(8.dp)
            )
            .fillMaxWidth()
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonBox(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .weight(0.4f)
        )

        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
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
}