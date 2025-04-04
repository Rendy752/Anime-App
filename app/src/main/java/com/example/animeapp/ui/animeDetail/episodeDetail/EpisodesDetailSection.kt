package com.example.animeapp.ui.animeDetail.episodeDetail

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
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.common_ui.ErrorMessage
import com.example.animeapp.ui.common_ui.SearchView
import com.example.animeapp.ui.common_ui.SearchViewSkeleton
import com.example.animeapp.utils.FilterUtils
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.basicContainer

@Composable
fun EpisodesDetailSection(
    animeDetailComplement: Resource<AnimeDetailComplement?>?,
    getCachedEpisodeDetailComplement: suspend (String) -> EpisodeDetailComplement?,
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
            } else if (animeDetailComplement is Resource.Loading) {
                EpisodeInfoRowSkeleton()
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        when (animeDetailComplement) {
            is Resource.Loading -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SearchViewSkeleton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    repeat(3) {
                        EpisodeDetailItemSkeleton()
                    }
                }
            }

            is Resource.Success -> {
                animeDetailComplement.data?.let { data ->
                    if (data.episodes.isNotEmpty()) {
                        var searchQuery by rememberSaveable { mutableStateOf("") }
                        val reversedEpisodes = data.episodes.reversed()

                        val filteredEpisodes by remember(reversedEpisodes, searchQuery) {
                            derivedStateOf {
                                FilterUtils.filterEpisodes(
                                    reversedEpisodes,
                                    searchQuery
                                )
                            }
                        }
                        if (data.episodes.size >= 4) SearchView(
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
                                    EpisodeDetailItem(
                                        animeDetailComplement = data,
                                        episode = episode,
                                        query = searchQuery,
                                        getCachedEpisodeDetailComplement = getCachedEpisodeDetailComplement,
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