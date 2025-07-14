package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.NetworkStatus
import com.luminoverse.animevibe.utils.resource.Resource

@Composable
fun WatchContentSection(
    animeDetail: Resource<AnimeDetail>,
    animeDetailComplement: Resource<AnimeDetailComplement>,
    networkStatus: NetworkStatus,
    onFavoriteToggle: (Boolean) -> Unit,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    onLoadEpisodeDetailComplement: (String) -> Unit,
    episodeDetailComplement: Resource<EpisodeDetailComplement>,
    newEpisodeIdList: List<String>,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    episodeJumpNumber: Int?,
    setEpisodeJumpNumber: (Int) -> Unit,
    serverScrollState: ScrollState,
    isError: Boolean,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WatchHeader(
            title = animeDetail.data?.title,
            networkStatus = networkStatus,
            onFavoriteToggle = onFavoriteToggle,
            animeDetailComplement = animeDetailComplement,
            episodeDetailComplement = episodeDetailComplement,
            episodeSourcesQuery = episodeSourcesQuery,
            serverScrollState = serverScrollState,
            isError = isError,
            isRefreshing = episodeDetailComplement is Resource.Loading || animeDetail is Resource.Loading || animeDetailComplement is Resource.Loading,
            onRefresh = {
                episodeSourcesQuery?.let {
                    handleSelectedEpisodeServer(it, true)
                }
            },
            onServerSelected = { episodeSourcesQuery ->
                handleSelectedEpisodeServer(episodeSourcesQuery, false)
            }
        )

        WatchEpisode(
            imageUrl = animeDetail.data?.images?.webp?.large_image_url,
            episodeDetailComplements = episodeDetailComplements,
            onLoadEpisodeDetailComplement = onLoadEpisodeDetailComplement,
            animeDetailComplement = animeDetailComplement,
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