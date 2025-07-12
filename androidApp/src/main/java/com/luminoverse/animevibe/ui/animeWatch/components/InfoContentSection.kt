package com.luminoverse.animevibe.ui.animeWatch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
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
import com.luminoverse.animevibe.ui.main.PlayerDisplayMode
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo

@Composable
fun InfoContentSection(
    modifier: Modifier = Modifier,
    rememberedBottomPadding: Dp = 0.dp,
    animeDetail: AnimeDetail?,
    navController: NavController,
    setPlayerDisplayMode: (PlayerDisplayMode) -> Unit,
) {
    Column(
        modifier = modifier.padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (animeDetail != null) {
            AnimeHeader(
                modifier = Modifier.padding(top = 8.dp),
                animeDetail = animeDetail, onClick = {
                    navController.navigateTo(
                        NavRoute.AnimeDetail.fromId(animeDetail.mal_id)
                    )
                    setPlayerDisplayMode(PlayerDisplayMode.PIP)
                }
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
            DetailCommonBody(title = "Background", body = animeDetail.background)
            DetailCommonBody(
                modifier = Modifier.padding(bottom = rememberedBottomPadding),
                title = "Synopsis",
                body = animeDetail.synopsis
            )
        } else {
            AnimeHeaderSkeleton()
            NumericDetailSectionSkeleton()
            YoutubePreviewSkeleton()
            DetailCommonBodySkeleton(title = "Background")
            DetailCommonBodySkeleton(
                modifier = Modifier.padding(bottom = rememberedBottomPadding),
                title = "Synopsis"
            )
        }
    }
}