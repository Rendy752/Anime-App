package com.example.animeapp.ui.animeWatch.watchContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.utils.Resource

@Composable
fun WatchContentSection(
    animeDetail: AnimeDetail?,
    isFavorite: Boolean,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    onLoadEpisodeDetailComplement: (String) -> Unit,
    episodeDetailComplement: Resource<EpisodeDetailComplement>,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery?,
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
                        isFavorite = isFavorite,
                        episode = currentEpisode,
                        episodeDetailComplement = episodeDetail,
                        episodeSourcesQuery = episodeSourcesQuery,
                        onServerSelected = { handleSelectedEpisodeServer(it) }
                    )
                }
            }
        } else {
            WatchHeaderSkeleton()
        }
        if (episodes.size > 1) WatchEpisode(
            episodeDetailComplements = episodeDetailComplements,
            onLoadEpisodeDetailComplement = onLoadEpisodeDetailComplement,
            episodeDetailComplement = episodeDetailComplement,
            episodes = episodes,
            episodeSourcesQuery = episodeSourcesQuery,
            handleSelectedEpisodeServer = handleSelectedEpisodeServer
        )
    }
}