package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.basicContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchEpisode(
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    onLoadEpisodeDetailComplement: (String) -> Unit,
    isRefreshing: Boolean,
    episodeDetailComplement: EpisodeDetailComplement?,
    episodes: List<Episode>,
    newEpisodeCount: Int,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    val gridState = rememberLazyGridState()
    Column(
        modifier = Modifier
            .basicContainer(
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(8.dp)
            )
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EpisodeJump(episodes = episodes, gridState = gridState)

        if (episodeDetailComplement != null && !isRefreshing) {
            EpisodeNavigation(
                episodeDetailComplement = episodeDetailComplement,
                episodeDetailComplements = episodeDetailComplements,
                onLoadEpisodeDetailComplement = onLoadEpisodeDetailComplement,
                episodes = episodes,
                episodeSourcesQuery = episodeSourcesQuery,
                handleSelectedEpisodeServer = handleSelectedEpisodeServer,
            )
        } else EpisodeNavigationSkeleton()

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        EpisodeSelectionGrid(
            episodes = episodes,
            newEpisodeCount = newEpisodeCount,
            episodeDetailComplements = episodeDetailComplements,
            onLoadEpisodeDetailComplement = onLoadEpisodeDetailComplement,
            episodeDetailComplement = episodeDetailComplement,
            episodeSourcesQuery = episodeSourcesQuery,
            handleSelectedEpisodeServer = handleSelectedEpisodeServer,
            gridState = gridState
        )
    }
}