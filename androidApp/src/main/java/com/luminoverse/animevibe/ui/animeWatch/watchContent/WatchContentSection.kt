package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.NetworkStatus
import com.luminoverse.animevibe.models.episodeDetailComplementPlaceholder
import com.luminoverse.animevibe.utils.resource.Resource

@Composable
fun WatchContentSection(
    animeDetail: AnimeDetail?,
    networkStatus: NetworkStatus,
    onFavoriteToggle: (Boolean) -> Unit,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    onLoadEpisodeDetailComplement: (String) -> Unit,
    isRefreshing: Boolean,
    episodeDetailComplement: EpisodeDetailComplement?,
    episodes: List<Episode>,
    newEpisodeCount: Int,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    serverScrollState: ScrollState,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isRefreshing) {
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
        } else {
            WatchHeaderSkeleton(
                episode = episodes.first(),
                episodeDetailComplement = episodeDetailComplement
                    ?: episodeDetailComplementPlaceholder,
                networkStatus = networkStatus
            )
        }
        if (episodes.size > 1) WatchEpisode(
            episodeDetailComplements = episodeDetailComplements,
            onLoadEpisodeDetailComplement = onLoadEpisodeDetailComplement,
            isRefreshing = isRefreshing,
            episodeDetailComplement = episodeDetailComplement,
            episodes = episodes,
            newEpisodeCount = newEpisodeCount,
            episodeSourcesQuery = episodeSourcesQuery,
            handleSelectedEpisodeServer = handleSelectedEpisodeServer
        )
    }
}