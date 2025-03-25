package com.example.animeapp.ui.animeWatch.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.ui.animeWatch.components.EpisodeJump
import com.example.animeapp.ui.animeWatch.components.EpisodeNavigation
import com.example.animeapp.ui.animeWatch.components.EpisodeSelectionGrid
import com.example.animeapp.utils.basicContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchEpisodeSection(
    animeDetail: AnimeDetail,
    episodeDetailComplement: EpisodeDetailComplement,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    val gridState = rememberLazyGridState()
    Column(
        modifier = Modifier
            .basicContainer()
            .fillMaxWidth()
    ) {
        EpisodeNavigation(
            episodeDetailComplement,
            episodes,
            episodeSourcesQuery,
            handleSelectedEpisodeServer,
        )
        EpisodeJump(animeDetail, episodes, gridState)
        HorizontalDivider(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp))
        EpisodeSelectionGrid(
            episodes,
            episodeDetailComplement,
            episodeSourcesQuery,
            handleSelectedEpisodeServer,
            gridState
        )
    }
}