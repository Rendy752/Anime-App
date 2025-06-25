package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.NetworkStatus
import com.luminoverse.animevibe.utils.resource.Resource

@Composable
fun WatchContentSection(
    animeDetail: AnimeDetail?,
    networkStatus: NetworkStatus,
    onFavoriteToggle: (Boolean) -> Unit,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    onLoadEpisodeDetailComplement: (String) -> Unit,
    episodeDetailComplement: EpisodeDetailComplement?,
    episodes: List<Episode>,
    newEpisodeCount: Int,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    serverScrollState: ScrollState,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        episodeDetailComplement?.let { episodeDetailComplement ->
            episodes.find { it.id == episodeDetailComplement.id }
                ?.let { currentEpisode ->
                    WatchHeader(
                        title = animeDetail?.title,
                        networkStatus = networkStatus,
                        onFavoriteToggle = onFavoriteToggle,
                        episode = currentEpisode,
                        episodeDetailComplement = episodeDetailComplement,
                        episodeSourcesQuery = episodeSourcesQuery,
                        serverScrollState = serverScrollState,
                        onServerSelected = { handleSelectedEpisodeServer(it) }
                    )
                }
        }
        if (episodes.size > 1) WatchEpisode(
            episodeDetailComplements = episodeDetailComplements,
            onLoadEpisodeDetailComplement = onLoadEpisodeDetailComplement,
            episodeDetailComplement = episodeDetailComplement,
            episodes = episodes,
            newEpisodeCount = newEpisodeCount,
            episodeSourcesQuery = episodeSourcesQuery,
            handleSelectedEpisodeServer = handleSelectedEpisodeServer
        )
    }
}