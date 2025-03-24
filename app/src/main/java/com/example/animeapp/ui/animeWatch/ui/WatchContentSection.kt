package com.example.animeapp.ui.animeWatch.ui

import androidx.compose.runtime.Composable
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery

@Composable
fun WatchContentSection(
    animeDetail: AnimeDetail,
    episodeDetailComplement: EpisodeDetailComplement,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    val currentEpisode =
        episodes.find { it.episodeId == episodeDetailComplement.servers.episodeId }
    currentEpisode?.let { currentEpisode ->
        WatchHeaderSection(
            title = animeDetail.title,
            episode = currentEpisode,
            episodeDetailComplement = episodeDetailComplement,
            episodeSourcesQuery = episodeSourcesQuery
        ) { handleSelectedEpisodeServer(it) }
    }
    WatchEpisodeSection(
        animeDetail = animeDetail,
        episodeDetailComplement = episodeDetailComplement,
        episodes = episodes,
        episodeSourcesQuery = episodeSourcesQuery,
        handleSelectedEpisodeServer = handleSelectedEpisodeServer
    )
}