package com.luminoverse.animevibe.ui.episodeHistory.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.luminoverse.animevibe.ui.common.SharedImageState
import com.luminoverse.animevibe.ui.common.SomethingWentWrongDisplay
import com.luminoverse.animevibe.ui.episodeHistory.EpisodeHistoryAction
import com.luminoverse.animevibe.ui.episodeHistory.EpisodeHistoryState
import com.luminoverse.animevibe.ui.main.SnackbarMessage
import com.luminoverse.animevibe.ui.main.SnackbarMessageType
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import com.luminoverse.animevibe.utils.resource.Resource

@Composable
fun HistoryContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    playEpisode: (Int, String) -> Unit,
    showSnackbar: (SnackbarMessage) -> Unit,
    showImagePreview: (SharedImageState) -> Unit,
    listState: LazyListState,
    state: EpisodeHistoryState,
    onAction: (EpisodeHistoryAction) -> Unit
) {
    if (state.isEpisodeHistoryEmpty) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            SomethingWentWrongDisplay(
                message = "No animes or episodes found",
                suggestion = "Episodes you watch will appear here."
            )
        }
    } else when (val results = state.paginatedHistory) {
        is Resource.Loading -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                items(3) { index ->
                    AnimeEpisodeAccordionSkeleton()
                }
            }
        }

        is Resource.Success -> {
            if (results.data.isEmpty()) {
                Box(
                    modifier = modifier,
                    contentAlignment = Alignment.Center
                ) {
                    SomethingWentWrongDisplay(
                        message = "No animes or episodes matched",
                        suggestion = "Try searching for something else."
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = modifier,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    results.data.forEach { (anime, episodes) ->
                        item {
                            AnimeEpisodeAccordion(
                                searchQuery = state.queryState.searchQuery,
                                anime = anime,
                                episodes = episodes,
                                showImagePreview = showImagePreview,
                                onAnimeTitleClick = {
                                    navController.navigateTo(
                                        NavRoute.AnimeDetail.fromId(anime.malId)
                                    )
                                },
                                onEpisodeClick = { episode ->
                                    playEpisode(anime.malId, episode.id)
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
                                onAnimeDelete = { malId, message ->
                                    onAction(EpisodeHistoryAction.DeleteAnime(malId))
                                    showSnackbar(
                                        SnackbarMessage(
                                            message = message, type = SnackbarMessageType.SUCCESS
                                        )
                                    )
                                },
                                onEpisodeDelete = { episodeId, message ->
                                    onAction(EpisodeHistoryAction.DeleteEpisode(episodeId))
                                    showSnackbar(
                                        SnackbarMessage(
                                            message = message, type = SnackbarMessageType.SUCCESS
                                        )
                                    )
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
            ) { SomethingWentWrongDisplay(message = results.message) }
        }
    }
}