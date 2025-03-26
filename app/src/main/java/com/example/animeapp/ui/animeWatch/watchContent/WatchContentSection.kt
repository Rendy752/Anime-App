package com.example.animeapp.ui.animeWatch.watchContent

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
        WatchHeader(
            title = animeDetail.title,
            episode = currentEpisode,
            episodeDetailComplement = episodeDetailComplement,
            episodeSourcesQuery = episodeSourcesQuery
        ) { handleSelectedEpisodeServer(it) }
    }
    if (episodes.size > 1) WatchEpisode(
        animeDetail = animeDetail,
        episodeDetailComplement = episodeDetailComplement,
        episodes = episodes,
        episodeSourcesQuery = episodeSourcesQuery,
        handleSelectedEpisodeServer = handleSelectedEpisodeServer
    )
}

@Composable
fun WatchContentSectionSkeleton(episodesSize: Int?) {
    WatchHeaderSkeleton()
    if (episodesSize != null && episodesSize > 1) {
        WatchEpisodeSkeleton(episodesSize)
    }
}