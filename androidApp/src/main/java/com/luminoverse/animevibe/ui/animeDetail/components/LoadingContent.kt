package com.luminoverse.animevibe.ui.animeDetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.ui.animeDetail.detailBody.DetailBodySectionSkeleton
import com.luminoverse.animevibe.ui.common.NumericDetailSectionSkeleton
import com.luminoverse.animevibe.ui.common.AnimeHeaderSkeleton
import com.luminoverse.animevibe.ui.common.DetailCommonBodySkeleton
import com.luminoverse.animevibe.ui.common.YoutubePreviewSkeleton

@Composable
fun LoadingContent(
    isLandscape: Boolean,
    navigationBarBottomPadding: Dp,
    portraitScrollState: LazyListState,
    landscapeScrollState: LazyListState,
) {
    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyColumn(modifier = Modifier.weight(1f), state = portraitScrollState) {
                item {
                    LeftColumnContentSkeleton()
                }
            }
            LazyColumn(modifier = Modifier.weight(1f), state = landscapeScrollState) {
                item {
                    RightColumnContentSkeleton()
                }
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = portraitScrollState) {
            item {
                VerticalColumnContentSkeleton(navigationBarBottomPadding)
            }
        }
    }
}

@Composable
private fun VerticalColumnContentSkeleton(navigationBarBottomPadding: Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = navigationBarBottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LeftColumnContentSkeleton()
        RightColumnContentSkeleton()
    }
}

@Composable
private fun LeftColumnContentSkeleton() {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimeHeaderSkeleton()
        NumericDetailSectionSkeleton()
        YoutubePreviewSkeleton()
        DetailBodySectionSkeleton()
    }
}

@Composable
private fun RightColumnContentSkeleton() {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf<String>("Background", "Synopsis").forEach { DetailCommonBodySkeleton(it) }
        listOf<String>(
            "Openings",
            "Endings",
            "Externals",
            "Streamings"
        ).forEach { DetailCommonBodySkeleton(it) }
    }
}