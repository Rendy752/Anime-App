package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun EpisodeSelectionGrid(
    episodes: List<Episode>,
    newEpisodeCount: Int,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    onLoadEpisodeDetailComplement: (String) -> Unit,
    episodeDetailComplement: EpisodeDetailComplement?,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    gridState: LazyGridState,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    val (selectedEpisodeId, setSelectedEpisodeId) = remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val (hasScrolledInitially, setHasScrolledInitially) = remember { mutableStateOf(false) }

    LaunchedEffect(episodeDetailComplement, episodes) {
        val targetEpisodeId = episodeDetailComplement?.servers?.episodeId
        val targetEpisodeNo = episodeDetailComplement?.servers?.episodeNo

        if (targetEpisodeId != null) {
            setSelectedEpisodeId(targetEpisodeId)
        }

        if (targetEpisodeNo != null && !hasScrolledInitially) {
            val targetIndex = episodes.indexOfFirst { it.episodeNo == targetEpisodeNo }
            if (targetIndex != -1) {
                val currentFirstVisibleIndex = gridState.firstVisibleItemIndex
                val scrollThreshold = 50

                if (kotlin.math.abs(targetIndex - currentFirstVisibleIndex) > scrollThreshold) {
                    gridState.scrollToItem(targetIndex)
                } else {
                    gridState.animateScrollToItem(targetIndex)
                }
                setHasScrolledInitially(true)
            }
        }
    }


    val episodeSelectionDebounce = remember(coroutineScope) {
        Debounce(coroutineScope) { episodeId ->
            handleSelectedEpisodeServer(
                episodeSourcesQuery?.copy(id = episodeId)
                    ?: EpisodeSourcesQuery.create(
                        id = episodeId,
                        rawServer = "vidsrc",
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
        itemsIndexed(episodes, key = { _, episode -> episode.episodeId }) { index, episode ->
            val (isItemVisible, setIsItemVisible) = remember { mutableStateOf(false) }

            LaunchedEffect(gridState, index) {
                snapshotFlow {
                    val first = gridState.firstVisibleItemIndex
                    val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                        ?: gridState.firstVisibleItemIndex
                    val buffer = 10
                    index in (first - buffer)..(last + buffer)
                }
                    .distinctUntilChanged()
                    .collect { isVisibleNow ->
                        setIsItemVisible(isVisibleNow)
                    }
            }

            if (isItemVisible && episodeDetailComplements[episode.episodeId] == null) {
                LaunchedEffect(episode.episodeId) {
                    onLoadEpisodeDetailComplement(episode.episodeId)
                }
            }

            val complementResource = episodeDetailComplements[episode.episodeId]
            WatchEpisodeItem(
                currentEpisode = episodeDetailComplement,
                episode = episode,
                isNewEpisode = index >= (episodes.size - newEpisodeCount),
                episodeDetailComplement = if (complementResource is Resource.Success) complementResource.data else null,
                onEpisodeClick = { episodeId ->
                    setSelectedEpisodeId(episodeId)
                    episodeSelectionDebounce.query(episodeId)
                },
                isSelected = episode.episodeId == selectedEpisodeId
            )
        }
    }
}