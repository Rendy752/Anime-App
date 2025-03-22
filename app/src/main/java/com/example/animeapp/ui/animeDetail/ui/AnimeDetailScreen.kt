package com.example.animeapp.ui.animeDetail.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.animeapp.BuildConfig.YOUTUBE_URL
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.NameAndUrl
import com.example.animeapp.ui.animeDetail.components.AnimeDetailScreenErrorMessage
import com.example.animeapp.ui.animeDetail.components.AnimeDetailTopBar
import com.example.animeapp.ui.animeDetail.viewmodel.AnimeDetailViewModel
import com.example.animeapp.ui.common_ui.AnimeHeader
import com.example.animeapp.ui.common_ui.AnimeHeaderSkeleton
import com.example.animeapp.ui.common_ui.DetailCommonBody
import com.example.animeapp.ui.common_ui.DetailCommonBodySkeleton
import com.example.animeapp.ui.common_ui.YoutubePreview
import com.example.animeapp.ui.common_ui.YoutubePreviewSkeleton
import com.example.animeapp.utils.Navigation.navigateToAnimeWatch
import com.example.animeapp.utils.Resource

private fun convertToNameAndUrl(list: List<String>?): List<NameAndUrl>? =
    list?.map { NameAndUrl(it, "$YOUTUBE_URL/results?search_query=${Uri.encode(it)}") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(animeTitle: String, animeId: Int, navController: NavController) {
    val viewModel: AnimeDetailViewModel = hiltViewModel()
    val animeDetail by viewModel.animeDetail.collectAsState()
    val animeDetailComplement by viewModel.animeDetailComplement.collectAsState()
    val defaultEpisode by viewModel.defaultEpisode.collectAsState()
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val leftScrollState = rememberLazyListState()
    val rightScrollState = rememberLazyListState()

    LaunchedEffect(animeId) { viewModel.handleAnimeDetail(animeId) }

    Scaffold(topBar = {
        AnimeDetailTopBar(
            animeTitle,
            animeDetail,
            animeDetailComplement,
            defaultEpisode,
            navController
        )
    }) { paddingValues ->
        when (animeDetail) {
            is Resource.Loading -> LoadingContent(paddingValues, isLandscape)
            is Resource.Success -> {
                LaunchedEffect(Unit) { viewModel.handleEpisodes() }
                SuccessContent(
                    paddingValues,
                    animeDetail?.data?.data,
                    animeDetailComplement,
                    defaultEpisode,
                    navController,
                    context,
                    isLandscape,
                    leftScrollState,
                    rightScrollState,
                    viewModel
                )
            }

            is Resource.Error -> ErrorContent(paddingValues, animeDetail?.message ?: "Error")
            else -> ErrorContent(paddingValues, "Empty")
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
        NumberDetailSectionSkeleton()
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
    leftScrollState: androidx.compose.foundation.lazy.LazyListState,
    rightScrollState: androidx.compose.foundation.lazy.LazyListState,
    viewModel: AnimeDetailViewModel
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
                            context
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
                        context
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
        NumberDetailSection(data)
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
    context: Context
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
            { animeId -> viewModel.handleAnimeDetail(animeId) })
        EpisodesDetailSection(animeDetailComplement, { episodeId ->
            defaultEpisode?.let { defaultEpisode ->
                animeDetailComplement?.data?.episodes?.let { episodes ->
                    navController.navigateToAnimeWatch(
                        animeDetail = data,
                        episodeId = episodeId,
                        episodes = episodes,
                        defaultEpisode = defaultEpisode
                    )
                }
            }
        })
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
    context: Context
) {
    Column(modifier = Modifier.padding(8.dp)) {
        LeftColumnContent(data, navController)
        RightColumnContent(
            viewModel,
            data,
            animeDetailComplement,
            defaultEpisode,
            navController,
            context
        )
    }
}

@Composable
private fun CommonListContent(data: AnimeDetail, context: Context) {
    listOf(
        "Openings" to convertToNameAndUrl(data.theme.openings),
        "Endings" to convertToNameAndUrl(data.theme.endings),
        "Externals" to data.external,
        "Streamings" to data.streaming
    ).forEach { (title, items) ->
        ClickableListSection(title, items) { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
    }
}

@Composable
private fun ErrorContent(paddingValues: PaddingValues, message: String) {
    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
    ) { item { AnimeDetailScreenErrorMessage(message) } }
}