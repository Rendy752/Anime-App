package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.NetworkStatus
import com.luminoverse.animevibe.ui.common.SomethingWentWrongDisplay
import com.luminoverse.animevibe.utils.watch.WatchUtils.getEpisodeBackgroundColor
import com.luminoverse.animevibe.utils.basicContainer
import com.luminoverse.animevibe.utils.resource.Resource

@Composable
fun WatchHeader(
    title: String?,
    networkStatus: NetworkStatus,
    onFavoriteToggle: (Boolean) -> Unit,
    animeDetailComplement: Resource<AnimeDetailComplement>,
    episodeDetailComplement: Resource<EpisodeDetailComplement>,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    serverScrollState: ScrollState,
    isError: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onServerSelected: (EpisodeSourcesQuery) -> Unit,
) {
    val currentEpisode by remember(animeDetailComplement) {
        mutableStateOf(animeDetailComplement.data?.episodes?.find { it.id == episodeSourcesQuery?.id })
    }
    var isFavorite by remember(episodeDetailComplement) {
        mutableStateOf(episodeDetailComplement.data?.isFavorite ?: false)
    }
    Column(
        modifier = Modifier
            .basicContainer(
                backgroundBrush = getEpisodeBackgroundColor(
                    currentEpisode?.filler ?: false,
                    episodeDetailComplement.data?.copy(isFavorite = isFavorite)
                ),
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(0.dp)
            )
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CurrentlyWatchingHeader(
                networkStatus = networkStatus,
                isError = isError,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                onFavoriteToggle = {
                    isFavorite = !isFavorite
                    onFavoriteToggle(isFavorite)
                },
                isFavorite = isFavorite
            )

            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (episodeDetailComplement !is Resource.Error) currentEpisode?.let {
                    EpisodeInfo(
                        title = title,
                        currentEpisode = it,
                        episodeSourcesQuery = episodeSourcesQuery
                    )
                } ?: EpisodeInfoSkeleton()
                else SomethingWentWrongDisplay(message = episodeDetailComplement.message)

                if (episodeDetailComplement is Resource.Success) ServerSelection(
                    scrollState = serverScrollState,
                    episodeSourcesQuery = episodeSourcesQuery,
                    servers = episodeDetailComplement.data.servers,
                    onServerSelected = onServerSelected
                ) else if (episodeDetailComplement is Resource.Loading) ServerSelectionSkeleton()
            }
        }
    }
}