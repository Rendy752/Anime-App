package com.luminoverse.animevibe.ui.animeDetail.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.luminoverse.animevibe.android.BuildConfig.YOUTUBE_URL
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.models.NameAndUrl
import com.luminoverse.animevibe.ui.animeDetail.DetailAction
import com.luminoverse.animevibe.ui.animeDetail.DetailState
import com.luminoverse.animevibe.ui.animeDetail.EpisodeFilterState
import com.luminoverse.animevibe.ui.animeDetail.clickableList.ClickableListSection
import com.luminoverse.animevibe.ui.animeDetail.detailBody.DetailBodySection
import com.luminoverse.animevibe.ui.animeDetail.episodeDetail.EpisodesDetailSection
import com.luminoverse.animevibe.ui.common.NumericDetailSection
import com.luminoverse.animevibe.ui.animeDetail.relation.RelationSection
import com.luminoverse.animevibe.ui.common.AnimeHeader
import com.luminoverse.animevibe.ui.common.DetailCommonBody
import com.luminoverse.animevibe.ui.common.YoutubePreview
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import com.luminoverse.animevibe.utils.watch.AnimeTitleFinder.normalizeTitle
import com.luminoverse.animevibe.utils.resource.Resource

@Composable
fun SuccessContent(
    detailState: DetailState,
    episodeFilterState: EpisodeFilterState,
    navController: NavController,
    context: Context,
    isLandscape: Boolean,
    portraitScrollState: LazyListState,
    landscapeScrollState: LazyListState,
    onAction: (DetailAction) -> Unit,
    onAnimeIdChange: (Int) -> Unit
) {
    detailState.animeDetail.data?.data?.let { animeDetail ->
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = portraitScrollState,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    leftColumnContent(
                        animeDetail = animeDetail,
                        navController = navController
                    )
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = landscapeScrollState,
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rightColumnContent(
                        animeDetail = animeDetail,
                        detailState = detailState,
                        episodeFilterState = episodeFilterState,
                        navController = navController,
                        context = context,
                        onAction = onAction,
                        onAnimeIdChange = onAnimeIdChange
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = portraitScrollState,
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                leftColumnContent(
                    animeDetail = animeDetail,
                    navController = navController
                )
                rightColumnContent(
                    animeDetail = animeDetail,
                    detailState = detailState,
                    episodeFilterState = episodeFilterState,
                    navController = navController,
                    context = context,
                    onAction = onAction,
                    onAnimeIdChange = onAnimeIdChange
                )
            }
        }
    }
}

private fun LazyListScope.leftColumnContent(
    animeDetail: AnimeDetail,
    navController: NavController
) {
    item { AnimeHeader(animeDetail = animeDetail) }
    item {
        NumericDetailSection(
            score = animeDetail.score,
            scoredBy = animeDetail.scored_by,
            rank = animeDetail.rank,
            popularity = animeDetail.popularity,
            members = animeDetail.members,
            favorites = animeDetail.favorites
        )
    }
    item { YoutubePreview(embedUrl = animeDetail.trailer.embed_url) }
    item {
        DetailBodySection(
            animeDetail = animeDetail, navController = navController
        )
    }
}

private fun LazyListScope.rightColumnContent(
    animeDetail: AnimeDetail,
    detailState: DetailState,
    episodeFilterState: EpisodeFilterState,
    navController: NavController,
    context: Context,
    onAction: (DetailAction) -> Unit,
    onAnimeIdChange: (Int) -> Unit
) {
    val commonBodyItems = listOf(
        "Background" to animeDetail.background,
        "Synopsis" to animeDetail.synopsis,
    )
    items(commonBodyItems.size) { index ->
        DetailCommonBody(
            title = commonBodyItems[index].first,
            body = commonBodyItems[index].second
        )
    }
    item {
        RelationSection(
            navController = navController,
            relations = animeDetail.relations,
            relationAnimeDetails = detailState.relationAnimeDetails,
            onAction = onAction,
            onItemClickListener = onAnimeIdChange
        )
    }
    item {
        EpisodesDetailSection(
            animeDetail = animeDetail,
            animeDetailComplement = detailState.animeDetailComplement,
            newEpisodeIdList = detailState.newEpisodeIdList,
            episodeDetailComplements = detailState.episodeDetailComplements,
            episodeFilterState = episodeFilterState,
            navBackStackEntry = navController.currentBackStackEntry,
            onEpisodeClick = { episodeId ->
                if (detailState.animeDetailComplement is Resource.Success) {
                    navController.navigateTo(
                        NavRoute.AnimeWatch.fromParams(
                            malId = animeDetail.mal_id,
                            episodeId = episodeId
                        )
                    )
                }
            },
            onAction = onAction
        )
    }
    item {
        CommonListContent(
            animeDetail = animeDetail,
            context = context
        )
    }
}

private fun convertToNameAndUrl(list: List<String>?): List<NameAndUrl>? =
    list?.map {
        NameAndUrl(
            it, "$YOUTUBE_URL/results?search_query=${Uri.encode(it.normalizeTitle())}"
        )
    }

@Composable
private fun CommonListContent(
    animeDetail: AnimeDetail,
    context: Context
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "Openings" to convertToNameAndUrl(animeDetail.theme?.openings),
            "Endings" to convertToNameAndUrl(animeDetail.theme?.endings),
            "Externals" to animeDetail.external,
            "Streamings" to animeDetail.streaming
        ).forEach { (title, items) ->
            ClickableListSection(title, items) { url ->
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
        }
    }
}