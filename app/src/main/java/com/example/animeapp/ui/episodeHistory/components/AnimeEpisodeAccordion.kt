package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.common_ui.EpisodeInfoRow
import com.example.animeapp.ui.common_ui.AsyncImageWithPlaceholder
import com.example.animeapp.ui.common_ui.ImageRoundedCorner
import com.example.animeapp.ui.common_ui.highlightText
import com.example.animeapp.utils.basicContainer

@Composable
fun AnimeEpisodeAccordion(
    searchQuery: String,
    anime: AnimeDetailComplement,
    episodes: List<EpisodeDetailComplement>,
    onAnimeTitleClick: () -> Unit,
    onAnimeFavoriteToggle: (Boolean) -> Unit,
    onEpisodeClick: (EpisodeDetailComplement) -> Unit,
    onEpisodeFavoriteToggle: (String, Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val representativeEpisode = episodes.firstOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .semantics { stateDescription = if (isExpanded) "Expanded" else "Collapsed" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .basicContainer(
                        outerPadding = PaddingValues(0.dp),
                        onItemClick = { isExpanded = !isExpanded }
                    )
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImageWithPlaceholder(
                    model = representativeEpisode?.imageUrl,
                    contentDescription = "Anime Image",
                    modifier = Modifier.size(72.dp, 108.dp),
                    roundedCorners = ImageRoundedCorner.ALL
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        modifier = Modifier.clickable(onClick = onAnimeTitleClick),
                        text = highlightText(
                            representativeEpisode?.animeTitle ?: "Anime ID: ${anime.malId}",
                            searchQuery
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (anime.episodes?.isNotEmpty() == true) {
                            EpisodeInfoRow(
                                subCount = anime.sub,
                                dubCount = anime.dub,
                                epsCount = anime.eps,
                            )
                        }
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onAnimeFavoriteToggle(!anime.isFavorite) },
                        modifier = Modifier.semantics {
                            stateDescription =
                                if (anime.isFavorite) "Remove from favorites" else "Add to favorites"
                        }
                    ) {
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
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    episodes.forEachIndexed { index, episode ->
                        EpisodeHistoryItem(
                            searchQuery = searchQuery,
                            isFirstItem = index == 0,
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