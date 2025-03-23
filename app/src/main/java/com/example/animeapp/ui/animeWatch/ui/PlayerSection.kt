package com.example.animeapp.ui.animeWatch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.ui.animeWatch.AnimeWatchViewModel
import com.example.animeapp.ui.animeWatch.components.VideoPlayer
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.Resource

@Composable
fun PlayerSection(
    episodeDetailComplement: Resource<EpisodeDetailComplement>?,
    viewModel: AnimeWatchViewModel,
    isPipMode: Boolean,
    onEnterPipMode: () -> Unit,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    isScreenOn: Boolean,
    episodeId: String,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    modifier: Modifier = Modifier,
    videoSize: Modifier,
    onErrorMessageChange: (String?) -> Unit
) {
    when (episodeDetailComplement) {
        is Resource.Loading -> {
            SkeletonBox(modifier = modifier)
        }

        is Resource.Success -> {
            episodeDetailComplement.data?.let { episodeDetailComplement ->
                VideoPlayer(
                    episodeDetailComplement,
                    viewModel,
                    isPipMode,
                    onEnterPipMode,
                    isFullscreen,
                    onFullscreenChange,
                    isScreenOn,
                    onErrorMessageChange,
                    modifier,
                    videoSize
                )
            }
        }

        is Resource.Error -> {
            episodeSourcesQuery?.let { query ->
                viewModel.handleSelectedEpisodeServer(query.copy(id = episodeId))
            }
        }

        else -> {}
    }
}