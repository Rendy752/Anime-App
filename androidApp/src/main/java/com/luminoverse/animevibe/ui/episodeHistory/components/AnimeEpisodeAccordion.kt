package com.luminoverse.animevibe.ui.episodeHistory.components

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.animeDetailComplementPlaceholder
import com.luminoverse.animevibe.ui.common.ConfirmationAlert
import com.luminoverse.animevibe.ui.common.SharedImageState

@Preview
@Composable
fun AnimeEpisodeAccordion(
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    anime: AnimeDetailComplement = animeDetailComplementPlaceholder,
    episodes: List<EpisodeDetailComplement> = emptyList(),
    showImagePreview: (SharedImageState) -> Unit = {},
    onAnimeTitleClick: () -> Unit = {},
    onEpisodeClick: (EpisodeDetailComplement) -> Unit = {},
    onAnimeFavoriteToggle: (Boolean) -> Unit = {},
    onEpisodeFavoriteToggle: (String, Boolean) -> Unit = { _, _ -> },
    onAnimeDelete: (Int, String) -> Unit = { _, _ -> },
    onEpisodeDelete: (String, String) -> Unit = { _, _ -> }
) {
    var isExpanded by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val representativeEpisode = episodes.firstOrNull()

    if (showDeleteDialog) {
        representativeEpisode?.let {
            ConfirmationAlert(
                title = "Delete Anime",
                message = "Are you sure you want to delete ${it.animeTitle} and all its episode history?",
                confirmText = "Delete",
                onConfirm = {
                    onAnimeDelete(
                        anime.malId,
                        "Successfully deleted ${it.animeTitle} and all its episode history"
                    )
                },
                cancelText = "Cancel",
                onCancel = { showDeleteDialog = false }
            )
        }
    }

    Card(
        modifier = modifier
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
                onDeleteClick = { showDeleteDialog = true }
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
                            showImagePreview = showImagePreview,
                            onClick = { onEpisodeClick(episode) },
                            onFavoriteToggle = { isFavorite ->
                                onEpisodeFavoriteToggle(episode.id, isFavorite)
                            },
                            onDelete = { message -> onEpisodeDelete(episode.id, message) }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AnimeEpisodeAccordionSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AccordionItemSkeleton()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(2) { index ->
                    EpisodeHistoryItemSkeleton(isFirstItem = index == 0)
                }
            }
        }
    }
}