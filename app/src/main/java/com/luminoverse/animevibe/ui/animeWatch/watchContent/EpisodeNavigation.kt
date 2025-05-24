package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.utils.resource.Resource

@Composable
fun EpisodeNavigation(
    episodeDetailComplement: EpisodeDetailComplement,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    onLoadEpisodeDetailComplement: (String) -> Unit,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    val currentEpisodeNo = episodeDetailComplement.servers.episodeNo
    val previousEpisode = episodes.find { it.episodeNo == currentEpisodeNo - 1 }
    val nextEpisode = episodes.find { it.episodeNo == currentEpisodeNo + 1 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            previousEpisode to true,
            nextEpisode to false
        ).forEach { (episode, isPrevious) ->
            episode?.let {
                LaunchedEffect(episode.episodeId) {
                    if (episodeDetailComplements[episode.episodeId] == null) {
                        onLoadEpisodeDetailComplement(episode.episodeId)
                    }
                }
                val complementResource = episodeDetailComplements[episode.episodeId]
                val complement = if (complementResource is Resource.Success) complementResource.data else null
                EpisodeNavigationButton(
                    modifier = Modifier.weight(1f),
                    episodeDetailComplement = complement,
                    episode = it,
                    isPrevious = isPrevious,
                    episodeSourcesQuery = episodeSourcesQuery,
                    handleSelectedEpisodeServer = handleSelectedEpisodeServer
                )
            }
        }
    }
}

@Preview
@Composable
fun EpisodeNavigationSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EpisodeNavigationButtonSkeleton(modifier = Modifier.weight(1f), isPrevious = true)
        EpisodeNavigationButtonSkeleton(modifier = Modifier.weight(1f), isPrevious = false)
    }
}