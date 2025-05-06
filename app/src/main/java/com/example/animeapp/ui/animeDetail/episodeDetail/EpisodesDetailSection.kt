package com.example.animeapp.ui.animeDetail.episodeDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.ui.animeDetail.DetailAction
import com.example.animeapp.ui.animeDetail.DetailState
import com.example.animeapp.ui.animeDetail.EpisodeFilterState
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.ui.common_ui.RetryButton
import com.example.animeapp.ui.common_ui.SearchView
import com.example.animeapp.ui.common_ui.SearchViewSkeleton
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.FilterUtils
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.basicContainer

@Composable
fun EpisodesDetailSection(
    modifier: Modifier = Modifier,
    animeDetail: AnimeDetail,
    detailState: DetailState,
    episodeFilterState: EpisodeFilterState,
    onEpisodeClick: (String) -> Unit,
    onAction: (DetailAction) -> Unit
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
            if (detailState.animeDetailComplement is Resource.Success) {
                detailState.animeDetailComplement.data?.let { data ->
                    if (data.episodes?.isNotEmpty() == true) {
                        EpisodeInfoRow(
                            subCount = data.sub,
                            dubCount = data.dub,
                            epsCount = data.eps,
                        )
                    }
                }
            } else if (detailState.animeDetailComplement is Resource.Loading) {
                EpisodeInfoRowSkeleton()
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        when (detailState.animeDetailComplement) {
            is Resource.Loading -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchViewSkeleton(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        )
                        if (animeDetail.airing) SkeletonBox(
                            modifier = modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            height = 56.dp,
                            width = 56.dp
                        )
                    }
                    repeat(3) {
                        EpisodeDetailItemSkeleton()
                    }
                }
            }

            is Resource.Success -> {
                detailState.animeDetailComplement.data?.let { data ->
                    if (animeDetail.type == "Music") {
                        MessageDisplay(message = "Anime is a music, no episodes available")
                    } else if (data.episodes?.isNotEmpty() == true) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (data.episodes.size >= 4) {
                                SearchView(
                                    query = episodeFilterState.episodeQuery.title,
                                    onQueryChange = {
                                        onAction(
                                            DetailAction.UpdateEpisodeQueryState(
                                                episodeFilterState.episodeQuery.copy(title = it)
                                            )
                                        )
                                    },
                                    placeholder = "Search",
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 4.dp)
                                )
                            }
                            if (animeDetail.airing) RetryButton(
                                modifier = if (data.episodes.size >= 4) Modifier else Modifier.fillMaxWidth(),
                                onClick = { onAction(DetailAction.LoadEpisodes(true)) }
                            )
                        }
                        val filteredEpisodes = FilterUtils.filterEpisodes(
                            episodes = data.episodes.reversed(),
                            query = episodeFilterState.episodeQuery,
                            episodeDetailComplements = detailState.episodeDetailComplements
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            if (filteredEpisodes.isEmpty() && episodeFilterState.episodeQuery.title.isNotEmpty()) {
                                item {
                                    MessageDisplay(message = "No episodes found")
                                }
                            } else {
                                items(filteredEpisodes) { episode ->
                                    EpisodeDetailItem(
                                        animeDetailComplement = data,
                                        episode = episode,
                                        detailState = detailState,
                                        query = episodeFilterState.episodeQuery.title,
                                        onAction = onAction,
                                        onClick = onEpisodeClick
                                    )
                                }
                            }
                        }
                    } else {
                        MessageDisplay(message = "No episodes found")
                    }
                }
            }

            is Resource.Error -> {
                MessageDisplay(
                    message = detailState.animeDetailComplement.message ?: "Error loading episodes"
                )
            }

            else -> {
                MessageDisplay(message = "Episode data not available")
            }
        }
    }
}