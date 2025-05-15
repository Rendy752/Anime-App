package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.ui.episodeHistory.EpisodeHistoryAction
import com.example.animeapp.ui.episodeHistory.EpisodeHistoryState
import com.example.animeapp.ui.main.navigation.NavRoute
import com.example.animeapp.ui.main.navigation.navigateTo
import com.example.animeapp.utils.Resource

@Composable
fun HistoryContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    listState: LazyListState,
    state: EpisodeHistoryState,
    onAction: (EpisodeHistoryAction) -> Unit
) {
    when (val results = state.episodeHistoryResults) {
        is Resource.Loading -> {
            LazyColumn(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(2) { AnimeEpisodeAccordionSkeleton() }
            }
        }

        is Resource.Success -> {
            if (results.data.isEmpty()) {
                Box(
                    modifier = modifier,
                    contentAlignment = Alignment.Center
                ) { MessageDisplay("No animes or episodes found") }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    results.data.forEach { (anime, episodes) ->
                        item {
                            AnimeEpisodeAccordion(
                                searchQuery = state.queryState.searchQuery,
                                anime = anime,
                                episodes = episodes,
                                onAnimeTitleClick = {
                                    navController.navigateTo(
                                        NavRoute.AnimeDetail.fromId(anime.malId)
                                    )
                                },
                                onEpisodeClick = { episode ->
                                    navController.navigateTo(
                                        NavRoute.AnimeWatch.fromParams(
                                            episode.malId,
                                            episode.id
                                        )
                                    )
                                },
                                onAnimeFavoriteToggle = { isFavorite ->
                                    onAction(
                                        EpisodeHistoryAction.ToggleAnimeFavorite(
                                            anime.malId,
                                            isFavorite
                                        )
                                    )
                                },
                                onEpisodeFavoriteToggle = { episodeId, isFavorite ->
                                    onAction(
                                        EpisodeHistoryAction.ToggleEpisodeFavorite(
                                            episodeId,
                                            isFavorite
                                        )
                                    )
                                },
                                onAnimeDelete = { malId ->
                                    onAction(EpisodeHistoryAction.DeleteAnime(malId))
                                },
                                onEpisodeDelete = { episodeId ->
                                    onAction(EpisodeHistoryAction.DeleteEpisode(episodeId))
                                }
                            )
                        }
                    }
                }
            }
        }

        is Resource.Error -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) { MessageDisplay(results.message ?: "Error loading history") }
        }
    }
}