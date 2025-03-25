package com.example.animeapp.ui.animeWatch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery

@Composable
fun EpisodeNavigation(
    episodeDetailComplement: EpisodeDetailComplement,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    val currentEpisodeNo = episodeDetailComplement.servers.episodeNo
    val previousEpisode = remember(currentEpisodeNo) {
        episodes.find { it.episodeNo == currentEpisodeNo - 1 }
    }
    val nextEpisode = remember(currentEpisodeNo) {
        episodes.find { it.episodeNo == currentEpisodeNo + 1 }
    }

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
                EpisodeNavigationButton(
                    modifier = Modifier.weight(1f),
                    episode = it,
                    isPrevious = isPrevious,
                    episodeSourcesQuery = episodeSourcesQuery,
                    handleSelectedEpisodeServer = handleSelectedEpisodeServer
                )
            }
        }
    }
}