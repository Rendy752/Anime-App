package com.example.animeapp.ui.animeDetail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.Episode
import com.example.animeapp.ui.animeDetail.components.EpisodeInfoRow
import com.example.animeapp.ui.animeDetail.components.EpisodeItem
import com.example.animeapp.ui.common_ui.ErrorMessage
import com.example.animeapp.ui.common_ui.SearchView
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.basicContainer

@Composable
fun EpisodesDetailSection(
    animeDetailComplement: Resource<AnimeDetailComplement?>?,
    onEpisodeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .basicContainer()
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Episodes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (animeDetailComplement is Resource.Success) {
                animeDetailComplement.data?.let { data ->
                    if (data.episodes.isNotEmpty()) {
                        EpisodeInfoRow(
                            subCount = data.sub,
                            dubCount = data.dub,
                            epsCount = data.eps,
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        when (animeDetailComplement) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is Resource.Success -> {
                animeDetailComplement.data?.let { data ->
                    if (data.episodes.isNotEmpty()) {
                        var searchQuery by rememberSaveable { mutableStateOf("") }
                        val reversedEpisodes = data.episodes.reversed()

                        val filteredEpisodes by remember(reversedEpisodes, searchQuery) {
                            derivedStateOf { filterEpisodes(reversedEpisodes, searchQuery) }
                        }

                        SearchView(
                            query = searchQuery,
                            onQueryChange = {
                                searchQuery = it
                            },
                            placeholder = "Search",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            if (filteredEpisodes.isEmpty() && searchQuery.isNotEmpty()) {
                                item {
                                    ErrorMessage(message = "No episodes found")
                                }
                            } else {
                                items(filteredEpisodes) { episode ->
                                    EpisodeItem(
                                        episode = episode,
                                        query = searchQuery,
                                        onClick = onEpisodeClick
                                    )
                                }
                            }
                        }
                    } else {
                        ErrorMessage(message = "No episodes found")
                    }
                }
            }

            is Resource.Error -> {
                ErrorMessage(
                    message = animeDetailComplement.message ?: "Error loading episodes"
                )
            }

            else -> {
                ErrorMessage(message = "Episode data not available")
            }
        }
    }
}

private fun filterEpisodes(episodes: List<Episode>, query: String): List<Episode> {
    return if (query.isBlank()) {
        episodes
    } else {
        episodes.filter { episode ->
            episode.episodeNo.toString().contains(query, ignoreCase = true) ||
                    episode.name.contains(query, ignoreCase = true)
        }
    }
}