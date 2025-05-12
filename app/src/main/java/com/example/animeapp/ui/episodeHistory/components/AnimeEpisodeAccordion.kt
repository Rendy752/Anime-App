package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.common_ui.AsyncImageWithPlaceholder
import com.example.animeapp.ui.common_ui.ImageRoundedCorner
import com.example.animeapp.utils.basicContainer

@Composable
fun AnimeEpisodeAccordion(
    anime: AnimeDetailComplement,
    episodes: List<EpisodeDetailComplement>,
    onAnimeFavoriteToggle: (Boolean) -> Unit,
    onEpisodeClick: (EpisodeDetailComplement) -> Unit,
    onEpisodeFavoriteToggle: (String, Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val representativeEpisode = episodes.firstOrNull()
    Card(modifier = Modifier.clip(RoundedCornerShape(16.dp)).fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .basicContainer(
                        outerPadding = PaddingValues(0.dp),
                        onItemClick = { isExpanded = !isExpanded })
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImageWithPlaceholder(
                    model = representativeEpisode?.imageUrl,
                    contentDescription = "Anime Image",
                    modifier = Modifier.size(64.dp, 96.dp),
                    isAiring = null,
                    roundedCorners = ImageRoundedCorner.ALL
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = representativeEpisode?.animeTitle ?: "Anime ID: ${anime.malId}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    anime.eps?.let {
                        Text(
                            text = "Episodes: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { onAnimeFavoriteToggle(!anime.isFavorite) }) {
                        Icon(
                            imageVector = if (anime.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Toggle Anime Favorite",
                            tint = if (anime.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    episodes.forEach { episode ->
                        EpisodeHistoryItem(
                            episode = episode,
                            onClick = { onEpisodeClick(episode) },
                            onFavoriteToggle = { isFavorite ->
                                onEpisodeFavoriteToggle(episode.id, isFavorite)
                            }
                        )
                    }
                }
            }
        }
    }
}