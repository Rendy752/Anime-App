package com.luminoverse.animevibe.ui.animeWatch.infoContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.ui.common_ui.AnimeHeader
import com.luminoverse.animevibe.ui.common_ui.AnimeHeaderSkeleton
import com.luminoverse.animevibe.ui.common_ui.DetailCommonBody
import com.luminoverse.animevibe.ui.common_ui.DetailCommonBodySkeleton
import com.luminoverse.animevibe.ui.common_ui.YoutubePreview
import com.luminoverse.animevibe.ui.common_ui.YoutubePreviewSkeleton

@Composable
fun InfoContentSection(
    animeDetail: AnimeDetail?,
    navController: NavController
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (animeDetail != null) {
            AnimeHeader(animeDetail = animeDetail, navController = navController)
            YoutubePreview(embedUrl = animeDetail.trailer.embed_url)
            DetailCommonBody(title = "Synopsis", body = animeDetail.synopsis)
        } else {
            AnimeHeaderSkeleton()
            YoutubePreviewSkeleton()
            DetailCommonBodySkeleton()
        }
    }
}