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
    onFavoriteToggle: (EpisodeDetailComplement) -> Unit,
    isFavorite: Boolean,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    onLoadEpisodeDetailComplement: (String) -> Unit,
    episodeDetailComplement: Resource<EpisodeDetailComplement>,
    episodes: List<Episode>,
    newEpisodeCount: Int,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    serverScrollState: ScrollState,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (episodeDetailComplement is Resource.Success) {
            episodeDetailComplement.data.let { episodeDetail ->
                val currentEpisode =
                    episodes.find { it.episodeId == episodeDetail.servers.episodeId }
                currentEpisode?.let { currentEpisode ->
                    WatchHeader(
                        title = animeDetail?.title,
                        networkStatus = networkStatus,
                        onFavoriteToggle = onFavoriteToggle,
                        isFavorite = isFavorite,
                        episode = currentEpisode,
                        episodeDetailComplement = episodeDetail,
                        episodeSourcesQuery = episodeSourcesQuery,
                        serverScrollState = serverScrollState,
                        onServerSelected = { handleSelectedEpisodeServer(it) }
                    )
                }
            }
        } else {
            WatchHeaderSkeleton(
                episode = episodes.first(),
                episodeDetailComplement = episodeDetailComplement.data
                    ?: episodeDetailComplementPlaceholder,
                networkStatus = networkStatus
            )
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