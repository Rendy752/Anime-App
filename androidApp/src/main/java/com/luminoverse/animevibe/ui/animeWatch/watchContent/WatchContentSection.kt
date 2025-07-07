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
    newEpisodeIdList: List<String>,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    episodeJumpNumber: Int?,
    setEpisodeJumpNumber: (Int) -> Unit,
    serverScrollState: ScrollState,
    isRefreshing: Boolean,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WatchHeader(
            title = animeDetail?.title,
            networkStatus = networkStatus,
            onFavoriteToggle = onFavoriteToggle,
            episode = episodes.find { it.id == episodeDetailComplement?.id },
            episodeDetailComplement = episodeDetailComplement,
            episodeSourcesQuery = episodeSourcesQuery,
            serverScrollState = serverScrollState,
            isRefreshing = isRefreshing,
            onRefresh = { episodeSourcesQuery?.let { handleSelectedEpisodeServer(it, true) } },
            onServerSelected = { episodeSourcesQuery ->
                handleSelectedEpisodeServer(episodeSourcesQuery, false)
            }
        )

        WatchEpisode(
            imageUrl = animeDetail?.images?.webp?.large_image_url,
            episodeDetailComplements = episodeDetailComplements,
            onLoadEpisodeDetailComplement = onLoadEpisodeDetailComplement,
            episodeDetailComplement = episodeDetailComplement,
            episodes = episodes,
            newEpisodeIdList = newEpisodeIdList,
            episodeSourcesQuery = episodeSourcesQuery,
            episodeJumpNumber = episodeJumpNumber,
            setEpisodeJumpNumber = setEpisodeJumpNumber,
            handleSelectedEpisodeServer = { episodeSourcesQuery ->
                handleSelectedEpisodeServer(episodeSourcesQuery, false)
            }
        )
    }
}