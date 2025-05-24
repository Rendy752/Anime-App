package com.luminoverse.animevibe.ui.animeDetail.episodeDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.navigation.NavBackStackEntry
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.ui.animeDetail.DetailAction
import com.luminoverse.animevibe.ui.animeDetail.DetailState
import com.luminoverse.animevibe.ui.animeDetail.EpisodeFilterState
import com.luminoverse.animevibe.ui.common.EpisodeDetailItem
import com.luminoverse.animevibe.ui.common.EpisodeDetailItemSkeleton
import com.luminoverse.animevibe.ui.common.EpisodeInfoRow
import com.luminoverse.animevibe.ui.common.EpisodeInfoRowSkeleton
import com.luminoverse.animevibe.ui.common.MessageDisplay
import com.luminoverse.animevibe.ui.common.RetryButton
import com.luminoverse.animevibe.ui.common.SearchView
import com.luminoverse.animevibe.ui.common.SearchViewSkeleton
import com.luminoverse.animevibe.ui.common.SkeletonBox
import com.luminoverse.animevibe.utils.FilterUtils
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun EpisodesDetailSection(
    modifier: Modifier = Modifier,
    animeDetail: AnimeDetail,
    detailState: DetailState,
    episodeFilterState: EpisodeFilterState,
    navBackStackEntry: NavBackStackEntry?,
    onEpisodeClick: (String) -> Unit,
    onAction: (DetailAction) -> Unit
) {
    Column(
        modifier = modifier
            .basicContainer(outerPadding = PaddingValues(0.dp))
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
                        height = 64.dp,
                        width = 64.dp
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (animeDetail.airing) RetryButton(
                                modifier = if (data.episodes.size >= 4) Modifier.size(64.dp)
                                else Modifier
                                    .fillMaxWidth()
                                    .height(64.dp),
                                onClick = { onAction(DetailAction.LoadEpisodes(true)) }
                            )
                        }
                        val filteredEpisodes = FilterUtils.filterEpisodes(
                            episodes = data.episodes.reversed(),
                            query = episodeFilterState.episodeQuery,
                            episodeDetailComplements = detailState.episodeDetailComplements,
                            lastEpisodeWatchedId = data.lastEpisodeWatchedId
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (filteredEpisodes.isEmpty() && episodeFilterState.episodeQuery.title.isNotEmpty()) {
                                item {
                                    MessageDisplay(message = "No episodes found")
                                }
                            } else {
                                items(filteredEpisodes) { episode ->
                                    EpisodeDetailItem(
                                        animeDetail = animeDetail,
                                        lastEpisodeWatchedId = data.lastEpisodeWatchedId,
                                        episode = episode,
                                        episodeDetailComplement = detailState.episodeDetailComplements[episode.episodeId]?.data,
                                        query = episodeFilterState.episodeQuery.title,
                                        onAction = onAction,
                                        onClick = onEpisodeClick,
                                        navBackStackEntry = navBackStackEntry
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
        }
    }
}