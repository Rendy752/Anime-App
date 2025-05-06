package com.example.animeapp.ui.animeDetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.animeapp.BuildConfig.YOUTUBE_URL
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.NameAndUrl
import com.example.animeapp.ui.animeDetail.components.AnimeDetailTopBar
import com.example.animeapp.ui.animeDetail.clickableList.ClickableListSection
import com.example.animeapp.ui.animeDetail.detailBody.DetailBodySection
import com.example.animeapp.ui.animeDetail.detailBody.DetailBodySectionSkeleton
import com.example.animeapp.ui.animeDetail.episodeDetail.EpisodesDetailSection
import com.example.animeapp.ui.animeDetail.numericDetail.NumericDetailSection
import com.example.animeapp.ui.animeDetail.numericDetail.NumericDetailSectionSkeleton
import com.example.animeapp.ui.animeDetail.relation.RelationSection
import com.example.animeapp.ui.common_ui.AnimeHeader
import com.example.animeapp.ui.common_ui.AnimeHeaderSkeleton
import com.example.animeapp.ui.common_ui.DetailCommonBody
import com.example.animeapp.ui.common_ui.DetailCommonBodySkeleton
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.ui.common_ui.YoutubePreview
import com.example.animeapp.ui.common_ui.YoutubePreviewSkeleton
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.utils.AnimeTitleFinder.normalizeTitle
import com.example.animeapp.utils.Navigation.navigateToAnimeWatch
import com.example.animeapp.utils.Resource

private fun convertToNameAndUrl(list: List<String>?): List<NameAndUrl>? =
    list?.map {
        NameAndUrl(
            it, "$YOUTUBE_URL/results?search_query=${Uri.encode(it.normalizeTitle())}"
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(
    id: Int,
    navController: NavHostController,
    mainState: MainState,
    detailState: DetailState,
    episodeFilterState: EpisodeFilterState,
    onAction: (DetailAction) -> Unit
) {
    val context = LocalContext.current
    val portraitScrollState = rememberLazyListState()
    val landscapeScrollState = rememberLazyListState()

    val currentAnimeIdState = rememberSaveable { mutableIntStateOf(id) }
    val currentAnimeId = currentAnimeIdState.intValue

    LaunchedEffect(mainState.isConnected) {
        if (mainState.isConnected && detailState.animeDetail is Resource.Error) {
            onAction(DetailAction.LoadAnimeDetail(currentAnimeId))
        }
    }

    LaunchedEffect(currentAnimeId) {
        onAction(DetailAction.LoadAnimeDetail(currentAnimeId))
        portraitScrollState.animateScrollToItem(0)
        landscapeScrollState.animateScrollToItem(0)
    }

    Scaffold(topBar = {
        AnimeDetailTopBar(
            animeDetail = detailState.animeDetail,
            animeDetailComplement = detailState.animeDetailComplement,
            defaultEpisodeId = detailState.defaultEpisodeId,
            navController = navController,
            onFavoriteToggle = { onAction(DetailAction.ToggleFavorite(it)) }
        )
    }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize()) {
            when (detailState.animeDetail) {
                is Resource.Loading -> LoadingContent(
                    paddingValues = paddingValues,
                    isLandscape = mainState.isLandscape,
                    scrollState = portraitScrollState,
                )

                is Resource.Success -> {
                    SuccessContent(
                        paddingValues = paddingValues,
                        animeDetailData = detailState.animeDetail.data.data,
                        detailState = detailState,
                        episodeFilterState = episodeFilterState,
                        navController = navController,
                        context = context,
                        isLandscape = mainState.isLandscape,
                        portraitScrollState = portraitScrollState,
                        landscapeScrollState = landscapeScrollState,
                        onAction = onAction,
                        onAnimeIdChange = { newAnimeId ->
                            currentAnimeIdState.intValue = newAnimeId
                        }
                    )
                }

                is Resource.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { MessageDisplay(detailState.animeDetail.message ?: "Error") }

                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { MessageDisplay("Empty") }
            }
        }
    }
}

@Composable
private fun LoadingContent(
    paddingValues: PaddingValues,
    isLandscape: Boolean,
    scrollState: LazyListState
) {
    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(),
        state = scrollState
    ) {
        if (isLandscape) {
            item {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        LeftColumnContentSkeleton()
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        RightColumnContentSkeleton()
                    }
                }
            }
        } else {
            item {
                VerticalColumnContentSkeleton()
            }
        }
    }
}

@Composable
private fun VerticalColumnContentSkeleton() {
    Column(modifier = Modifier.padding(8.dp)) {
        LeftColumnContentSkeleton()
        RightColumnContentSkeleton()
    }
}

@Composable
private fun LeftColumnContentSkeleton() {
    Column(modifier = Modifier.padding(8.dp)) {
        AnimeHeaderSkeleton()
        NumericDetailSectionSkeleton()
        YoutubePreviewSkeleton()
        DetailBodySectionSkeleton()
    }
}

@Composable
private fun RightColumnContentSkeleton() {
    Column(modifier = Modifier.padding(8.dp)) {
        listOf<String>("Background", "Synopsis").forEach { DetailCommonBodySkeleton(it) }
        listOf<String>(
            "Openings",
            "Endings",
            "Externals",
            "Streamings"
        ).forEach { DetailCommonBodySkeleton(it) }
    }
}

@Composable
private fun SuccessContent(
    paddingValues: PaddingValues,
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
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
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
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
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
private fun LeftColumnContent(animeDetail: AnimeDetail, navController: NavController) {
    Column(modifier = Modifier.padding(8.dp)) {
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
    Column(modifier = Modifier.padding(8.dp)) {
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
            onEpisodeClick = { episodeId ->
                detailState.defaultEpisodeId?.let {
                    if (detailState.animeDetailComplement is Resource.Success) {
                        navController.navigateToAnimeWatch(
                            malId = animeDetail.mal_id,
                            episodeId = episodeId,
                        )
                    }
                }
            },
            onAction = onAction
        )
        CommonListContent(animeDetail, context)
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
    Column(modifier = Modifier.padding(8.dp)) {
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
private fun CommonListContent(animeDetail: AnimeDetail, context: Context) {
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