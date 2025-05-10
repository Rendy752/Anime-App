package com.example.animeapp.ui.animeWatch.watchContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.models.episodeDetailComplementPlaceholder
import com.example.animeapp.models.episodePlaceholder
import com.example.animeapp.utils.WatchUtils.getEpisodeBackgroundColor
import com.example.animeapp.utils.basicContainer

@Composable
fun WatchHeader(
    title: String?,
    isFavorite: Boolean,
    episode: Episode,
    episodeDetailComplement: EpisodeDetailComplement,
    episodeSourcesQuery: EpisodeSourcesQuery?,
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
                CurrentlyWatchingHeader()
                EpisodeInfo(title, episode, servers.episodeNo, episodeSourcesQuery)
                ServerSelection(episodeSourcesQuery, servers, onServerSelected)
            }
        }
    }
}

@Preview
@Composable
fun WatchHeaderSkeleton(
    episode: Episode = episodePlaceholder,
    episodeDetailComplement: EpisodeDetailComplement = episodeDetailComplementPlaceholder
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
        CurrentlyWatchingHeader()
        EpisodeInfoSkeleton()
        ServerSelectionSkeleton()
    }
}