package com.example.animeapp.ui.animeWatch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.ui.animeWatch.infoContent.InfoContentSection
import com.example.animeapp.ui.animeWatch.videoPlayer.VideoPlayerSection
import com.example.animeapp.ui.animeWatch.watchContent.WatchContentSection
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.Resource

@Composable
fun AnimeWatchContent(
    animeDetail: AnimeDetail,
    isFavorite: Boolean,
    updateStoredWatchState: (EpisodeDetailComplement, Long?) -> Unit,
    getCachedEpisodeDetailComplement: suspend (String) -> EpisodeDetailComplement?,
    episodes: List<Episode>?,
    episodeDetailComplement: Resource<EpisodeDetailComplement>,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    isConnected: Boolean,
    isLandscape: Boolean,
    isPipMode: Boolean,
    isFullscreen: Boolean,
    scrollState: LazyListState,
    isScreenOn: Boolean,
    onEnterPipMode: () -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    onPlayerError: (String?) -> Unit,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
    selectedContentIndex: Int,
    modifier: Modifier,
    videoSize: Modifier
) {
    episodes?.let { episodeList ->
        episodeSourcesQuery?.let { query ->
            Row(modifier = Modifier.fillMaxWidth()) {
                if (episodeDetailComplement is Resource.Success && isConnected) {
                    VideoPlayerSection(
                        updateStoredWatchState = { seekPosition ->
                            updateStoredWatchState(episodeDetailComplement.data, seekPosition)
                        },
                        episodeDetailComplement = episodeDetailComplement.data,
                        episodes = episodeList,
                        episodeSourcesQuery = query,
                        handleSelectedEpisodeServer = handleSelectedEpisodeServer,
                        isPipMode = isPipMode,
                        onEnterPipMode = onEnterPipMode,
                        isFullscreen = isFullscreen,
                        onFullscreenChange = onFullscreenChange,
                        isScreenOn = isScreenOn,
                        isLandscape = isLandscape,
                        onPlayerError = onPlayerError,
                        modifier = modifier,
                        videoSize = videoSize
                    )
                } else {
                    Box(modifier = modifier.then(videoSize)) {
                        SkeletonBox(modifier = Modifier.fillMaxSize())
                    }
                }

                if (isLandscape && !isPipMode && !isFullscreen) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(0.5f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        state = scrollState
                    ) {
                        item {
                            if (selectedContentIndex == 0) {
                                WatchContentSection(
                                    animeDetail,
                                    isFavorite,
                                    getCachedEpisodeDetailComplement,
                                    episodeDetailComplement,
                                    episodes,
                                    episodeSourcesQuery
                                ) {
                                    handleSelectedEpisodeServer(it)
                                }
                            } else {
                                InfoContentSection(animeDetail)
                            }
                        }
                    }
                }
            }
            if (!isLandscape && !isPipMode && !isFullscreen) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    state = scrollState
                ) {
                    item {
                        WatchContentSection(
                            animeDetail,
                            isFavorite,
                            getCachedEpisodeDetailComplement,
                            episodeDetailComplement,
                            episodes,
                            episodeSourcesQuery
                        ) { handleSelectedEpisodeServer(it) }
                        InfoContentSection(animeDetail)
                    }
                }
            }
        }
    }
}