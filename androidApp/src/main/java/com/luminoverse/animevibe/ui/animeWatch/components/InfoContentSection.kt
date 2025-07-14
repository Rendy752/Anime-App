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
import com.luminoverse.animevibe.ui.common.SomethingWentWrongDisplay
import com.luminoverse.animevibe.ui.common.YoutubePreview
import com.luminoverse.animevibe.ui.common.YoutubePreviewSkeleton
import com.luminoverse.animevibe.ui.main.PlayerDisplayMode
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import com.luminoverse.animevibe.utils.resource.Resource

@Composable
fun InfoContentSection(
    modifier: Modifier = Modifier,
    rememberedBottomPadding: Dp = 0.dp,
    animeDetail: Resource<AnimeDetail>,
    navController: NavController,
    setPlayerDisplayMode: (PlayerDisplayMode) -> Unit,
) {
    Column(
        modifier = modifier.padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (animeDetail) {
            is Resource.Success -> {
                AnimeHeader(
                    modifier = Modifier.padding(top = 8.dp),
                    animeDetail = animeDetail.data, onClick = {
                        navController.navigateTo(
                            NavRoute.AnimeDetail.fromId(animeDetail.data.mal_id)
                        )
                        setPlayerDisplayMode(PlayerDisplayMode.PIP)
                    }
                )
                NumericDetailSection(
                    score = animeDetail.data.score,
                    scoredBy = animeDetail.data.scored_by,
                    rank = animeDetail.data.rank,
                    popularity = animeDetail.data.popularity,
                    members = animeDetail.data.members,
                    favorites = animeDetail.data.favorites
                )
                YoutubePreview(embedUrl = animeDetail.data.trailer.embed_url)
                DetailCommonBody(title = "Background", body = animeDetail.data.background)
                DetailCommonBody(
                    modifier = Modifier.padding(bottom = rememberedBottomPadding),
                    title = "Synopsis",
                    body = animeDetail.data.synopsis
                )
            }

            is Resource.Loading -> {
                AnimeHeaderSkeleton()
                NumericDetailSectionSkeleton()
                YoutubePreviewSkeleton()
                DetailCommonBodySkeleton(title = "Background")
                DetailCommonBodySkeleton(
                    modifier = Modifier.padding(bottom = rememberedBottomPadding),
                    title = "Synopsis"
                )
            }

            is Resource.Error -> {
                SomethingWentWrongDisplay(
                    message = animeDetail.message,
                    suggestion = "Please try again later"
                )
            }
        }
    }
}