package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.EpisodeDetailComplement

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
            AccordionItem(
                anime = anime,
                representativeEpisode = representativeEpisode,
                searchQuery = searchQuery,
                isExpanded = isExpanded,
                onItemClick = { isExpanded = !isExpanded },
                onAnimeTitleClick = { onAnimeTitleClick() },
                onAnimeFavoriteToggle = { isFavorite ->
                    onAnimeFavoriteToggle(isFavorite)
                },
            )
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