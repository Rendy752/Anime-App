package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.common_ui.ScreenshotDisplay
import com.example.animeapp.utils.TimeUtils
import com.example.animeapp.utils.TimeUtils.formatTimestamp
import com.example.animeapp.utils.WatchUtils.getEpisodeBackgroundColor
import com.example.animeapp.utils.basicContainer
import kotlin.math.roundToInt

@Composable
fun EpisodeHistoryItem(
    episode: EpisodeDetailComplement,
    onClick: () -> Unit,
    onFavoriteToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .basicContainer(
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(8.dp),
                backgroundBrush = getEpisodeBackgroundColor(episode.isFiller),
                onItemClick = { onClick() }
            )
            .clip(RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScreenshotDisplay(
            modifier = Modifier
                .size(100.dp, 56.dp)
                .clip(RoundedCornerShape(4.dp)),
            imageUrl = episode.imageUrl,
            screenshot = episode.screenshot
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Ep ${episode.number}: ${episode.episodeTitle}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                episode.lastTimestamp?.let { timestamp ->
                    Text(
                        modifier = Modifier.padding(end = if (episode.lastWatched != null) 4.dp else 0.dp),
                        text = "${formatTimestamp(timestamp)} " +
                                "${episode.duration?.let { "/ ${formatTimestamp(it)}" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                episode.lastWatched?.let { lastWatched ->
                    Text(
                        modifier = Modifier.padding(start = if (episode.lastTimestamp != null) 4.dp else 0.dp),
                        text = "~ ${TimeUtils.formatDateToAgo(lastWatched)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val duration = episode.duration?.toFloat() ?: (24 * 60f)
            val progress = episode.lastTimestamp?.let { timestamp ->
                if (timestamp < duration) (timestamp.toFloat() / duration).coerceIn(0f, 1f) else 1f
            } ?: 0f
            val percentage = (progress * 100).roundToInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .semantics { contentDescription = "$percentage% watched" },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        IconButton(
            onClick = { onFavoriteToggle(!episode.isFavorite) },
            modifier = Modifier.semantics {
                contentDescription =
                    if (episode.isFavorite) "Remove episode from favorites" else "Add episode to favorites"
            }
        ) {
            Icon(
                imageVector = if (episode.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (episode.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}