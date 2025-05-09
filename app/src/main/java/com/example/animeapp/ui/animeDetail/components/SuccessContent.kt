package com.example.animeapp.ui.animeDetail.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.example.animeapp.BuildConfig.YOUTUBE_URL
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.NameAndUrl
import com.example.animeapp.ui.animeDetail.DetailAction
import com.example.animeapp.ui.animeDetail.DetailState
import com.example.animeapp.ui.animeDetail.EpisodeFilterState
import com.example.animeapp.ui.animeDetail.clickableList.ClickableListSection
import com.example.animeapp.ui.animeDetail.detailBody.DetailBodySection
import com.example.animeapp.ui.animeDetail.episodeDetail.EpisodesDetailSection
import com.example.animeapp.ui.animeDetail.numericDetail.NumericDetailSection
import com.example.animeapp.ui.animeDetail.relation.RelationSection
import com.example.animeapp.ui.common_ui.AnimeHeader
import com.example.animeapp.ui.common_ui.DetailCommonBody
import com.example.animeapp.ui.common_ui.YoutubePreview
import com.example.animeapp.ui.main.navigation.NavRoute
import com.example.animeapp.ui.main.navigation.navigateTo
import com.example.animeapp.utils.AnimeTitleFinder.normalizeTitle
import com.example.animeapp.utils.Resource

@Composable
fun SuccessContent(
    animeDetailData: AnimeDetail?,
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
    animeDetailData?.let { animeDetail ->
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyColumn(modifier = Modifier.weight(1f), state = portraitScrollState) {
                    item {
                        LeftColumnContent(
                            animeDetail = animeDetail,
                            navController = navController
                        )
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f), state = landscapeScrollState) {
                    item {
                        RightColumnContent(
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = portraitScrollState
            ) {
                item {
                    VerticalColumnContent(
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
}

@Composable
private fun VerticalColumnContent(
    animeDetail: AnimeDetail,
    detailState: DetailState,
    episodeFilterState: EpisodeFilterState,
    navController: NavController,
    context: Context,
    onAction: (DetailAction) -> Unit,
    onAnimeIdChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LeftColumnContent(animeDetail = animeDetail, navController = navController)
        RightColumnContent(
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

@Composable
private fun LeftColumnContent(animeDetail: AnimeDetail, navController: NavController) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimeHeader(animeDetail = animeDetail)
        NumericDetailSection(animeDetail = animeDetail)
        YoutubePreview(embedUrl = animeDetail.trailer.embed_url)
        DetailBodySection(animeDetail = animeDetail, navController = navController)
    }
}

@Composable
private fun RightColumnContent(
    animeDetail: AnimeDetail,
    detailState: DetailState,
    episodeFilterState: EpisodeFilterState,
    navController: NavController,
    context: Context,
    onAction: (DetailAction) -> Unit,
    onAnimeIdChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "Background" to animeDetail.background,
            "Synopsis" to animeDetail.synopsis,
        ).forEach { DetailCommonBody(it.first, it.second) }
        RelationSection(
            navController = navController,
            relations = animeDetail.relations,
            detailState = detailState,
            onAction = onAction,
            onItemClickListener = onAnimeIdChange
        )
        EpisodesDetailSection(
            animeDetail = animeDetail,
            detailState = detailState,
            episodeFilterState = episodeFilterState,
            navBackStackEntry = navController.currentBackStackEntry,
            onEpisodeClick = { episodeId ->
                detailState.defaultEpisodeId?.let {
                    if (detailState.animeDetailComplement is Resource.Success) {
                        navController.navigateTo(
                            NavRoute.AnimeWatch.fromParams(
                                malId = animeDetail.mal_id,
                                episodeId = episodeId
                            )
                        )
                    }
                }
            },
            onAction = onAction
        )
        CommonListContent(animeDetail, context)
    }
}

private fun convertToNameAndUrl(list: List<String>?): List<NameAndUrl>? =
    list?.map {
        NameAndUrl(
            it, "$YOUTUBE_URL/results?search_query=${Uri.encode(it.normalizeTitle())}"
        )
    }

@Composable
private fun CommonListContent(animeDetail: AnimeDetail, context: Context) {
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