package com.example.animeapp.ui.animeWatch.watchContent

import androidx.compose.runtime.Composable
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.utils.Resource

@Composable
fun WatchContentSection(
    animeDetail: AnimeDetail,
    episodeDetailComplement: Resource<EpisodeDetailComplement>,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    if (episodeDetailComplement is Resource.Success) {
        episodeDetailComplement.data?.let { episodeDetail ->
            val currentEpisode =
                episodes.find { it.episodeId == episodeDetail.servers.episodeId }
            currentEpisode?.let { currentEpisode ->
                WatchHeader(
                    title = animeDetail.title,
                    episode = currentEpisode,
                    episodeDetailComplement = episodeDetail,
                    episodeSourcesQuery = episodeSourcesQuery
                ) { handleSelectedEpisodeServer(it) }
            }
        }
    } else {
        WatchHeaderSkeleton()
    }
    if (episodes.size > 1) WatchEpisode(
        animeDetail = animeDetail,
        episodeDetailComplement = episodeDetailComplement,
        episodes = episodes,
        episodeSourcesQuery = episodeSourcesQuery,
        handleSelectedEpisodeServer = handleSelectedEpisodeServer
    )
}