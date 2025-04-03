package com.example.animeapp.ui.animeWatch.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    getCachedEpisodeDetailComplement: suspend (String) -> EpisodeDetailComplement?,
    episodeDetailComplement: Resource<EpisodeDetailComplement>,
    updateEpisodeDetailComplement: (EpisodeDetailComplement) -> Unit,
    episodes: List<Episode>?,
    episodeSourcesQuery: EpisodeSourcesQuery?,
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
                if (episodeDetailComplement is Resource.Success) {
                    VideoPlayerSection(
                        episodeDetailComplement = episodeDetailComplement.data,
                        updateEpisodeDetailComplement = updateEpisodeDetailComplement,
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
                            .weight(0.5f)
                            .padding(8.dp),
                        state = scrollState
                    ) {
                        item {
                            if (selectedContentIndex == 0) {
                                WatchContentSection(
                                    animeDetail,
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
                    modifier = Modifier.padding(8.dp),
                    state = scrollState
                ) {
                    item {
                        WatchContentSection(
                            animeDetail,
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