package com.example.animeapp.ui.animeWatch.ui

import androidx.compose.material3.Text
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
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    val currentEpisode =
        episodes.find { it.episodeId == episodeDetailComplement.servers.episodeId }
    if (currentEpisode != null) {
        WatchHeaderSection(
            title = animeDetail.title,
            episode = currentEpisode,
            episodeDetailComplement = episodeDetailComplement,
            onServerSelected = { server ->
                episodeDetailComplement.servers.let { servers ->
                    val category = when (server) {
                        in servers.sub -> "sub"
                        in servers.dub -> "dub"
                        in servers.raw -> "raw"
                        else -> "sub"
                    }
                    handleSelectedEpisodeServer(
                        EpisodeSourcesQuery(
                            servers.episodeId,
                            server.serverName,
                            category
                        )
                    )
                }
            }
        )
    } else {
        Text("Episode not found")
    }
}