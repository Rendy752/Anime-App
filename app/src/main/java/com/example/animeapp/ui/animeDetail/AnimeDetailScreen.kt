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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.animeapp.BuildConfig.YOUTUBE_URL
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.EpisodeDetailComplement
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
    list?.map { NameAndUrl(it, "$YOUTUBE_URL/results?search_query=${Uri.encode(it)}") }

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AnimeDetailScreen(
    id: Int = 20,
    navController: NavHostController = rememberNavController(),
    mainState: MainState = MainState()
) {
    val viewModel: AnimeDetailViewModel = hiltViewModel()

    val animeDetail by viewModel.animeDetail.collectAsStateWithLifecycle()
    val animeDetailComplement by viewModel.animeDetailComplement.collectAsStateWithLifecycle()
    val defaultEpisode by viewModel.defaultEpisode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val leftScrollState = rememberLazyListState()
    val rightScrollState = rememberLazyListState()

    val currentAnimeIdState = rememberSaveable { mutableIntStateOf(id) }
    val currentAnimeId = currentAnimeIdState.intValue

    LaunchedEffect(mainState.isConnected) {
        if (mainState.isConnected && animeDetail is Resource.Error) viewModel.handleAnimeDetail(
            currentAnimeId
        )
    }

    LaunchedEffect(currentAnimeId) { viewModel.handleAnimeDetail(currentAnimeId) }

    LaunchedEffect(animeDetail) {
        if (animeDetail is Resource.Success) viewModel.handleEpisodes()
    }

    Scaffold(topBar = {
        AnimeDetailTopBar(
            animeDetail = animeDetail,
            animeDetailComplement = animeDetailComplement,
            defaultEpisode = defaultEpisode,
            navController = navController,
            onFavoriteToggle = { viewModel.handleToggleFavorite(it) }
        )
    }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize()) {
            when (animeDetail) {
                is Resource.Loading -> LoadingContent(paddingValues, mainState.isLandscape)
                is Resource.Success -> {
                    SuccessContent(
                        paddingValues,
                        animeDetail?.data?.data,
                        animeDetailComplement,
                        defaultEpisode,
                        navController,
                        context,
                        mainState.isLandscape,
                        leftScrollState,
                        rightScrollState,
                        viewModel
                    ) { newAnimeId -> currentAnimeIdState.intValue = newAnimeId }
                }

                is Resource.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { MessageDisplay(animeDetail?.message ?: "Error") }

                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { MessageDisplay("Empty") }
            }
        }
    }
}

@Composable
private fun LoadingContent(paddingValues: PaddingValues, isLandscape: Boolean) {
    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
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
    animeDetailComplement: Resource<AnimeDetailComplement?>?,
    defaultEpisode: EpisodeDetailComplement?,
    navController: NavController,
    context: Context,
    isLandscape: Boolean,
    leftScrollState: LazyListState,
    rightScrollState: LazyListState,
    viewModel: AnimeDetailViewModel,
    onAnimeIdChange: (Int) -> Unit
) {
    animeDetailData?.let { data ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                LazyColumn(modifier = Modifier.weight(1f), state = leftScrollState) {
                    item { LeftColumnContent(data, navController) }
                }
                LazyColumn(modifier = Modifier.weight(1f), state = rightScrollState) {
                    item {
                        RightColumnContent(
                            viewModel,
                            data,
                            animeDetailComplement,
                            defaultEpisode,
                            navController,
                            context,
                            onAnimeIdChange
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                item {
                    VerticalColumnContent(
                        viewModel,
                        data,
                        animeDetailComplement,
                        defaultEpisode,
                        navController,
                        context,
                        onAnimeIdChange
                    )
                }
            }
        }
    }
}

@Composable
private fun LeftColumnContent(data: AnimeDetail, navController: NavController) {
    Column(modifier = Modifier.padding(8.dp)) {
        AnimeHeader(data)
        NumericDetailSection(data)
        YoutubePreview(data.trailer.embed_url)
        DetailBodySection(data, navController)
    }
}

@Composable
private fun RightColumnContent(
    viewModel: AnimeDetailViewModel,
    data: AnimeDetail,
    animeDetailComplement: Resource<AnimeDetailComplement?>?,
    defaultEpisode: EpisodeDetailComplement?,
    navController: NavController,
    context: Context,
    onAnimeIdChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        listOf(
            "Background" to data.background,
            "Synopsis" to data.synopsis,
        ).forEach { DetailCommonBody(it.first, it.second) }
        RelationSection(
            navController,
            data.relations,
            { animeId -> viewModel.getAnimeDetail(animeId) },
            { animeId -> onAnimeIdChange(animeId) })
        EpisodesDetailSection(
            animeDetailComplement,
            { viewModel.getCachedEpisodeDetailComplement(it) },
            viewModel::handleEpisodes,
            { episodeId ->
                defaultEpisode?.let { defaultEpisode ->
                    if (animeDetailComplement is Resource.Success) {
                        animeDetailComplement.data?.let { animeDetailComplement ->
                            navController.navigateToAnimeWatch(
                                animeDetail = data,
                                animeDetailComplement = animeDetailComplement,
                                episodeId = episodeId,
                                episodes = animeDetailComplement.episodes,
                                defaultEpisode = defaultEpisode
                            )
                        }
                    }
                }
            }
        )
        CommonListContent(data, context)
    }
}

@Composable
private fun VerticalColumnContent(
    viewModel: AnimeDetailViewModel,
    data: AnimeDetail,
    animeDetailComplement: Resource<AnimeDetailComplement?>?,
    defaultEpisode: EpisodeDetailComplement?,
    navController: NavController,
    context: Context,
    onAnimeIdChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        LeftColumnContent(data, navController)
        RightColumnContent(
            viewModel,
            data,
            animeDetailComplement,
            defaultEpisode,
            navController,
            context,
            onAnimeIdChange
        )
    }
}

@Composable
private fun CommonListContent(data: AnimeDetail, context: Context) {
    listOf(
        "Openings" to convertToNameAndUrl(data.theme?.openings),
        "Endings" to convertToNameAndUrl(data.theme?.endings),
        "Externals" to data.external,
        "Streamings" to data.streaming
    ).forEach { (title, items) ->
        ClickableListSection(title, items) { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, url.normalizeTitle().toUri()))
        }
    }
}