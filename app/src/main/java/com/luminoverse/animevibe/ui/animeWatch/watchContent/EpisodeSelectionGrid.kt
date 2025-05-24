package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.utils.Debounce
import com.luminoverse.animevibe.utils.resource.Resource

@Composable
fun EpisodeSelectionGrid(
    episodes: List<Episode>,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    onLoadEpisodeDetailComplement: (String) -> Unit,
    episodeDetailComplement: EpisodeDetailComplement?,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
    gridState: LazyGridState
) {
    val currentEpisodeNo = episodeDetailComplement?.servers?.episodeNo
    val (selectedEpisodeId, setSelectedEpisodeId) = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(episodeDetailComplement) {
        if (episodeDetailComplement?.servers?.episodeId != null) {
            setSelectedEpisodeId(episodeDetailComplement.servers.episodeId)
        }
        if (currentEpisodeNo != null) {
            val index = episodes.indexOfFirst { it.episodeNo == currentEpisodeNo }
            if (index != -1) {
                gridState.animateScrollToItem(index)
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val debounce = remember(coroutineScope) {
        Debounce(coroutineScope) { episodeId ->
            handleSelectedEpisodeServer(
                episodeSourcesQuery?.copy(id = episodeId)
                    ?: EpisodeSourcesQuery(
                        id = episodeId,
                        server = "vidsrc",
                        category = "sub"
                    )
            )
        }
    }

    LazyVerticalGrid(
        state = gridState,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .wrapContentHeight(),
        columns = GridCells.Adaptive(minSize = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(episodes) { episode ->
            LaunchedEffect(episode.episodeId) {
                if (episodeDetailComplements[episode.episodeId] == null) {
                    onLoadEpisodeDetailComplement(episode.episodeId)
                }
            }
            val complementResource = episodeDetailComplements[episode.episodeId]
            WatchEpisodeItem(
                currentEpisode = episodeDetailComplement,
                episode = episode,
                episodeDetailComplement = if (complementResource is Resource.Success) complementResource.data else null,
                onEpisodeClick = { episodeId ->
                    setSelectedEpisodeId(episodeId)
                    debounce.query(episodeId)
                },
                isSelected = episode.episodeId == selectedEpisodeId
            )
        }
    }
}