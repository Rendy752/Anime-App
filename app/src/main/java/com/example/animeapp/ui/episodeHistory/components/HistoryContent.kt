package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.animeapp.ui.common_ui.LimitAndPaginationQueryState
import com.example.animeapp.ui.common_ui.LimitAndPaginationSection
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.ui.episodeHistory.EpisodeHistoryAction
import com.example.animeapp.ui.episodeHistory.EpisodeHistoryState
import com.example.animeapp.ui.main.navigation.NavRoute
import com.example.animeapp.ui.main.navigation.navigateTo
import com.example.animeapp.utils.Resource

@Composable
fun HistoryContent(
    navController: NavHostController,
    listState: LazyListState,
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
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(2) { AnimeEpisodeAccordionSkeleton() }
                }
            }

            is Resource.Success -> {
                if (results.data.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { MessageDisplay("No episodes found") }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { MessageDisplay(results.message ?: "Error loading history") }
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
                if (updatedQuery.page != state.queryState.page) {
                    onAction(EpisodeHistoryAction.ChangePage(updatedQuery.page))
                } else if (updatedQuery.limit != state.queryState.limit) {
                    onAction(
                        EpisodeHistoryAction.ApplyFilters(
                            state.queryState.copy(
                                page = 1,
                                limit = updatedQuery.limit ?: state.queryState.limit
                            )
                        )
                    )
                }
            }
        )
    }
}