package com.luminoverse.animevibe.ui.animeWatch.watchContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.models.EpisodeSourcesQuery
import com.luminoverse.animevibe.ui.common.CircularLoadingIndicator
import com.luminoverse.animevibe.ui.common.SomethingWentWrongDisplay
import com.luminoverse.animevibe.utils.basicContainer
import com.luminoverse.animevibe.utils.resource.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchEpisode(
    imageUrl: String?,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    onLoadEpisodeDetailComplement: (String) -> Unit,
    animeDetailComplement: Resource<AnimeDetailComplement>,
    newEpisodeIdList: List<String>,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    episodeJumpNumber: Int?,
    setEpisodeJumpNumber: (Int) -> Unit,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    val gridState = rememberLazyGridState()
    Column(
        modifier = Modifier
            .basicContainer(
                outerPadding = PaddingValues(0.dp),
                innerPadding = PaddingValues(top = 8.dp, start = 8.dp, end = 8.dp)
            )
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (animeDetailComplement) {
            is Resource.Success -> animeDetailComplement.data.episodes?.let {
                EpisodeJump(
                    episodes = it,
                    episodeJumpNumber = episodeJumpNumber,
                    setEpisodeJumpNumber = setEpisodeJumpNumber,
                    gridState = gridState
                )
            }

            is Resource.Loading -> EpisodeJumpSkeleton()

            is Resource.Error -> {}
        }

        if (animeDetailComplement is Resource.Success) EpisodeNavigation(
            episodeDetailComplements = episodeDetailComplements,
            onLoadEpisodeDetailComplement = onLoadEpisodeDetailComplement,
            episodes = animeDetailComplement.data.episodes ?: emptyList(),
            episodeSourcesQuery = episodeSourcesQuery,
            handleSelectedEpisodeServer = handleSelectedEpisodeServer,
        ) else if (animeDetailComplement is Resource.Loading) EpisodeNavigationSkeleton()

        Column {
            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            when (animeDetailComplement) {
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularLoadingIndicator() }
                }

                is Resource.Success -> {
                    EpisodeSelectionGrid(
                        imageUrl = imageUrl,
                        episodes = animeDetailComplement.data.episodes ?: emptyList(),
                        episodeJumpNumber = episodeJumpNumber,
                        newEpisodeIdList = newEpisodeIdList,
                        episodeDetailComplements = episodeDetailComplements,
                        onLoadEpisodeDetailComplement = onLoadEpisodeDetailComplement,
                        episodeSourcesQuery = episodeSourcesQuery,
                        handleSelectedEpisodeServer = handleSelectedEpisodeServer,
                        gridState = gridState
                    )
                }

                is Resource.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SomethingWentWrongDisplay(
                            message = animeDetailComplement.message,
                            suggestion = "Please try again later"
                        )
                    }
                }
            }
        }
    }
}