package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.animeapp.ui.common_ui.LimitAndPaginationQueryState
import com.example.animeapp.ui.common_ui.LimitAndPaginationSection
import com.example.animeapp.ui.episodeHistory.EpisodeHistoryAction
import com.example.animeapp.ui.episodeHistory.EpisodeHistoryState
import com.example.animeapp.ui.main.navigation.NavRoute
import com.example.animeapp.ui.main.navigation.navigateTo
import com.example.animeapp.utils.Resource

@Composable
fun HistoryContent(
    navController: NavHostController,
    state: EpisodeHistoryState,
    onAction: (EpisodeHistoryAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        when (val results = state.episodeHistoryResults) {
            is Resource.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            is Resource.Success -> {
                if (results.data.isEmpty()) {
                    Text(
                        text = "No episodes found",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        results.data.forEach { (anime, episodes) ->
                            item {
                                AnimeEpisodeAccordion(
                                    anime = anime,
                                    episodes = episodes,
                                    onAnimeTitleClick = {
                                        navController.navigateTo(
                                            NavRoute.AnimeDetail.fromId(anime.malId)
                                        )
                                    },
                                    onAnimeFavoriteToggle = { isFavorite ->
                                        onAction(EpisodeHistoryAction.ToggleAnimeFavorite(anime.malId, isFavorite))
                                    },
                                    onEpisodeClick = { episode ->
                                        navController.navigateTo(
                                            NavRoute.AnimeWatch.fromParams(episode.malId, episode.id)
                                        )
                                    },
                                    onEpisodeFavoriteToggle = { episodeId, isFavorite ->
                                        onAction(EpisodeHistoryAction.ToggleEpisodeFavorite(episodeId, isFavorite))
                                    }
                                )
                            }
                        }
                    }
                }
            }
            is Resource.Error -> {
                Text(
                    text = results.message ?: "Error loading history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        LimitAndPaginationSection(
            isVisible = state.episodeHistoryResults is Resource.Success,
            pagination = state.pagination,
            query = LimitAndPaginationQueryState(
                page = state.queryState.page,
                limit = state.queryState.limit
            ),
            onQueryChanged = { updatedQuery ->
                onAction(
                    EpisodeHistoryAction.ApplyFilters(
                        state.queryState.copy(
                            page = updatedQuery.page,
                            limit = updatedQuery.limit ?: state.queryState.limit
                        )
                    )
                )
            }
        )
    }
}