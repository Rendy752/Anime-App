package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.common_ui.ScreenshotDisplay
import com.example.animeapp.utils.TimeUtils.formatTimestamp
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
                onItemClick = { onClick() }),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScreenshotDisplay(
            modifier = Modifier.size(100.dp, 56.dp),
            imageUrl = episode.imageUrl,
            screenshot = episode.screenshot
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Episode ${episode.number}: ${episode.episodeTitle}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            episode.lastTimestamp?.let { timestamp ->
                Text(
                    text = "Watched: ${formatTimestamp(timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val duration = episode.duration?.toFloat() ?: (24 * 60f)
            val progress = episode.lastTimestamp?.let { timestamp ->
                if (timestamp < duration) (timestamp.toFloat() / duration).coerceIn(
                    0f,
                    1f
                ) else 1f
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
                        .height(4.dp),
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
        IconButton(onClick = { onFavoriteToggle(!episode.isFavorite) }) {
            Icon(
                imageVector = if (episode.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Toggle Episode Favorite",
                tint = if (episode.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}