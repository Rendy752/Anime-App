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
import com.example.animeapp.ui.animeWatch.infoContent.InfoContentSection
import com.example.animeapp.ui.animeWatch.watchContent.WatchContentSectionSkeleton
import com.example.animeapp.ui.common_ui.SkeletonBox

@Composable
fun AnimeWatchSkeleton(
    animeDetail: AnimeDetail,
    episodes: List<Episode>?,
    selectedContentIndex: Int,
    isLandscape: Boolean,
    isPipMode: Boolean,
    isFullscreen: Boolean,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    videoSize: Modifier = Modifier
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = modifier.then(videoSize)) {
            SkeletonBox(modifier = Modifier.fillMaxSize())
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
                        WatchContentSectionSkeleton(episodes?.size)
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
                WatchContentSectionSkeleton(episodes?.size)
                InfoContentSection(animeDetail)
            }
        }
    }
}