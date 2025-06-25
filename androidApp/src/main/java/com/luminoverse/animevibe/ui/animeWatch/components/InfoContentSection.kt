package com.luminoverse.animevibe.ui.animeWatch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.ui.common.NumericDetailSection
import com.luminoverse.animevibe.ui.common.NumericDetailSectionSkeleton
import com.luminoverse.animevibe.ui.common.AnimeHeader
import com.luminoverse.animevibe.ui.common.AnimeHeaderSkeleton
import com.luminoverse.animevibe.ui.common.DetailCommonBody
import com.luminoverse.animevibe.ui.common.DetailCommonBodySkeleton
import com.luminoverse.animevibe.ui.common.YoutubePreview
import com.luminoverse.animevibe.ui.common.YoutubePreviewSkeleton

@Composable
fun InfoContentSection(
    animeDetail: AnimeDetail?,
    navController: NavController,
    isConnected: Boolean,
    isLandscape: Boolean
) {
    val density = LocalDensity.current
    val navigationBarPadding = with(density) {
        WindowInsets.systemBars.getBottom(density).toDp()
    }

    Column(
        modifier = Modifier.padding(
            bottom = if (isConnected) {
                if (isLandscape) 8.dp else navigationBarPadding
            } else 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (animeDetail != null) {
            AnimeHeader(
                modifier = Modifier.padding(top = 8.dp),
                animeDetail = animeDetail, navController = navController
            )
            NumericDetailSection(
                score = animeDetail.score,
                scoredBy = animeDetail.scored_by,
                rank = animeDetail.rank,
                popularity = animeDetail.popularity,
                members = animeDetail.members,
                favorites = animeDetail.favorites
            )
            YoutubePreview(embedUrl = animeDetail.trailer.embed_url)
            val commonBodyItems = listOf(
                "Background" to animeDetail.background,
                "Synopsis" to animeDetail.synopsis,
            )
            commonBodyItems.forEach { item ->
                DetailCommonBody(title = item.first, body = item.second)
            }
        } else {
            AnimeHeaderSkeleton()
            NumericDetailSectionSkeleton()
            YoutubePreviewSkeleton()
            listOf<String>(
                "Background",
                "Synopsis"
            ).forEach { DetailCommonBodySkeleton(title = it) }
        }
    }
}