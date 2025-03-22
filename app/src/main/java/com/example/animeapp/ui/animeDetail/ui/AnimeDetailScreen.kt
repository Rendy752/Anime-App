package com.example.animeapp.ui.animeDetail.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.animeapp.BuildConfig.YOUTUBE_URL
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.animeapp.R
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ShareUtils
import com.example.animeapp.ui.animeDetail.viewmodel.AnimeDetailViewModel
import com.example.animeapp.ui.common_ui.AnimeHeader
import com.example.animeapp.ui.common_ui.DetailCommonBody
import com.example.animeapp.ui.common_ui.YoutubePreview
import androidx.core.net.toUri
import com.example.animeapp.models.NameAndUrl
import com.example.animeapp.ui.common_ui.ErrorMessage


private fun convertToNameAndUrl(list: List<String>?): List<NameAndUrl>? {
    return list?.map { item ->
        val encodedItem = Uri.encode(item)
        val youtubeSearchUrl = "${YOUTUBE_URL}/results?search_query=$encodedItem"
        NameAndUrl(name = item, url = youtubeSearchUrl)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(
    animeTitle: String,
    animeId: Int,
    navController: NavController
) {
    val viewModel: AnimeDetailViewModel = hiltViewModel()
    val animeDetail by viewModel.animeDetail.collectAsState()
    val animeDetailComplement by viewModel.animeDetailComplement.collectAsState()
    val defaultEpisode by viewModel.defaultEpisode.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberLazyListState()

    LaunchedEffect(animeId) {
        viewModel.handleAnimeDetail(animeId)
        scrollState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Text(
                            text = animeTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        animeDetail?.data?.data?.let { animeDetailData ->
                            if (animeDetailComplement is Resource.Success &&
                                (animeDetailComplement as Resource.Success).data?.episodes?.isNotEmpty() == true &&
                                defaultEpisode != null
                            ) {
                                IconButton(onClick = {
                                    navController.navigate(
                                        "animeWatch/${animeDetailData.mal_id}/${
                                            (animeDetailComplement as Resource.Success).data?.episodes?.get(
                                                0
                                            )?.episodeId
                                        }/${defaultEpisode!!.id}"
                                    )
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.LiveTv,
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentDescription = stringResource(id = R.string.watch)
                                    )
                                }
                            }
                            IconButton(onClick = {
                                ShareUtils.shareAnimeDetail(context, animeDetailData)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = stringResource(id = R.string.filter)
                                )
                            }
                        }
                    },

                    colors = TopAppBarDefaults.topAppBarColors(
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    thickness = 2.dp
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = scrollState
        ) {
            item {
                when (animeDetail) {
                    is Resource.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }

                    is Resource.Success -> {
                        animeDetail?.data?.data?.let { animeDetailData ->
                            LaunchedEffect(animeDetailData.mal_id) { viewModel.handleEpisodes() }
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                AnimeHeader(animeDetailData)
                                NumberDetailSection(animeDetailData)
                                YoutubePreview(animeDetailData.trailer.embed_url)
                                DetailBodySection(animeDetailData, navController)
                                DetailCommonBody("Background", animeDetailData.background)
                                DetailCommonBody("Synopsis", animeDetailData.synopsis)
                                RelationSection(
                                    navController,
                                    animeDetailData.relations,
                                    { animeId -> viewModel.getAnimeDetail(animeId) },
                                    { animeId -> viewModel.handleAnimeDetail(animeId) })


                                EpisodesDetailSection(
                                    animeDetailComplement = animeDetailComplement,
                                    onEpisodeClick = { episodeId ->
                                        navController.navigate("animeWatch/$animeId/$episodeId/$defaultEpisode")
                                    }
                                )

                                val openingNameAndUrls =
                                    convertToNameAndUrl(animeDetailData.theme.openings)
                                ClickableListSection(
                                    title = "Openings",
                                    items = openingNameAndUrls,
                                    onClick = { url ->
                                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                        context.startActivity(intent)
                                    },
                                )

                                val endingNameAndUrls =
                                    convertToNameAndUrl(animeDetailData.theme.endings)
                                ClickableListSection(
                                    title = "Endings",
                                    items = endingNameAndUrls,
                                    onClick = { url ->
                                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                        context.startActivity(intent)
                                    },
                                )

                                ClickableListSection(
                                    title = "Externals",
                                    items = animeDetailData.external,
                                    onClick = { external ->
                                        val intent =
                                            Intent(Intent.ACTION_VIEW, external.toUri())
                                        context.startActivity(intent)
                                    },
                                )

                                ClickableListSection(
                                    title = "Streamings",
                                    items = animeDetailData.streaming,
                                    onClick = { streaming ->
                                        val intent =
                                            Intent(Intent.ACTION_VIEW, streaming.toUri())
                                        context.startActivity(intent)
                                    },
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            ErrorMessage(
                                message = (animeDetail as Resource.Error).message ?: "Error"
                            )
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            ErrorMessage(message = "Empty")
                        }
                    }
                }
            }
        }
    }
}