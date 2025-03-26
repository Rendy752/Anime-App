package com.example.animeapp.ui.animeWatch.watchContent

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery

@Composable
fun EpisodeSelectionGrid(
    episodes: List<Episode>,
    episodeDetailComplement: EpisodeDetailComplement,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
    gridState: LazyGridState
) {
    val currentEpisodeNo = episodeDetailComplement.servers.episodeNo

    LaunchedEffect(Unit) {
        val index = episodes.indexOfFirst { it.episodeNo == currentEpisodeNo }
        if (index != -1) {
            gridState.animateScrollToItem(index)
        }
    }

    LazyVerticalGrid(
        state = gridState,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .wrapContentHeight(),
        columns = GridCells.Fixed(4),
    ) {
        items(episodes) { episode ->
            WatchEpisodeItem(
                episodeDetailComplement = episodeDetailComplement,
                episode = episode,
                onEpisodeClick = { episodeId ->
                    handleSelectedEpisodeServer(
                        episodeSourcesQuery?.copy(id = episodeId)
                            ?: EpisodeSourcesQuery(
                                id = episodeId,
                                server = "vidsrc",
                                category = "sub"
                            )
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun EpisodeSelectionGridSkeleton(episodesSize: Int = 12) {
    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .wrapContentHeight(),
        columns = GridCells.Fixed(4),
    ) {
        items(episodesSize) {
            WatchEpisodeItemSkeleton()
        }
    }
}