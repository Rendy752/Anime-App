package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.models.NetworkStatus
import com.luminoverse.animevibe.models.episodeDetailComplementPlaceholder
import com.luminoverse.animevibe.models.episodePlaceholder
import com.luminoverse.animevibe.models.networkStatusPlaceholder
import com.luminoverse.animevibe.utils.watch.WatchUtils.getEpisodeBackgroundColor
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun WatchHeader(
    title: String?,
    networkStatus: NetworkStatus,
    onFavoriteToggle: (EpisodeDetailComplement) -> Unit,
    isFavorite: Boolean,
    episode: Episode,
    episodeDetailComplement: EpisodeDetailComplement,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    serverScrollState: ScrollState,
    onServerSelected: (EpisodeSourcesQuery) -> Unit,
) {
    Column(
        modifier = Modifier
            .basicContainer(
                backgroundBrush = getEpisodeBackgroundColor(
                    episode.filler,
                    episodeDetailComplement.copy(isFavorite = isFavorite)
                ),
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(0.dp)
            )
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        episodeDetailComplement.servers.let { servers ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurrentlyWatchingHeader(
                    episodeDetailComplement = episodeDetailComplement,
                    networkStatus = networkStatus,
                    onFavoriteToggle = onFavoriteToggle,
                    isFavorite = isFavorite
                )
                EpisodeInfo(
                    title = title,
                    episode = episode,
                    episodeNo = servers.episodeNo,
                    episodeSourcesQuery = episodeSourcesQuery
                )
                ServerSelection(
                    scrollState = serverScrollState,
                    episodeSourcesQuery = episodeSourcesQuery,
                    servers = servers,
                    onServerSelected = onServerSelected
                )
            }
        }
    }
}

@Preview
@Composable
fun WatchHeaderSkeleton(
    episode: Episode = episodePlaceholder,
    episodeDetailComplement: EpisodeDetailComplement = episodeDetailComplementPlaceholder,
    networkStatus: NetworkStatus = networkStatusPlaceholder
) {
    Column(
        modifier = Modifier
            .basicContainer(
                backgroundBrush = getEpisodeBackgroundColor(
                    episode.filler,
                    episodeDetailComplement
                ),
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(0.dp)
            )
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CurrentlyWatchingHeader(
            episodeDetailComplement = episodeDetailComplement,
            networkStatus = networkStatus,
            onFavoriteToggle = {},
            isFavorite = false
        )
        EpisodeInfoSkeleton()
        ServerSelectionSkeleton()
    }
}