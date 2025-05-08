package com.example.animeapp.ui.animeWatch.watchContent

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
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.basicContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchEpisode(
    getCachedEpisodeDetailComplement: suspend (String) -> EpisodeDetailComplement?,
    episodeDetailComplement: Resource<EpisodeDetailComplement>,
    episodes: List<Episode>,
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

        if (episodeDetailComplement is Resource.Success) {
            EpisodeNavigation(
                episodeDetailComplement = episodeDetailComplement.data,
                getCachedEpisodeDetailComplement = getCachedEpisodeDetailComplement,
                episodes = episodes,
                episodeSourcesQuery = episodeSourcesQuery,
                handleSelectedEpisodeServer = handleSelectedEpisodeServer,
            )
        } else EpisodeNavigationSkeleton()

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        EpisodeSelectionGrid(
            episodes = episodes,
            getCachedEpisodeDetailComplement = getCachedEpisodeDetailComplement,
            episodeDetailComplement = if (episodeDetailComplement is Resource.Success) episodeDetailComplement.data else null,
            episodeSourcesQuery = episodeSourcesQuery,
            handleSelectedEpisodeServer = handleSelectedEpisodeServer,
            gridState = gridState
        )
    }
}