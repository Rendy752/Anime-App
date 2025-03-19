package com.example.animeapp.ui.animeDetail.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ShareUtils
import com.example.animeapp.BuildConfig.YOUTUBE_URL
import com.example.animeapp.R
import com.example.animeapp.ui.animeDetail.AnimeDetailViewModel
import com.example.animeapp.ui.common_ui.AnimeHeader
import com.example.animeapp.utils.BindAnimeUtils

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

    LaunchedEffect(animeId) {
        viewModel.handleAnimeDetail(animeId)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = animeTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when (animeDetail) {
                is Resource.Loading -> {
                    CircularProgressIndicator()
                }

                is Resource.Success -> {
                    (animeDetail as Resource.Success<AnimeDetailResponse>).data?.data?.let { animeDetailData ->
                        Column {
                            AnimeHeader(animeDetailData)
//                        AnimeNumberDetail(animeDetailData)
//                        YoutubePreview(animeDetailData.trailer.embed_url)
//                        AnimeBody(animeDetailData)
//                        AnimeBackground(animeDetailData.background)
//                        AnimeSynopsis(animeDetailData.synopsis)
//                        AnimeRelation(animeDetailData.relations, { relatedAnimeId ->
//                            viewModel.handleAnimeDetail(relatedAnimeId)
//                        })
//                        AnimeOpening(animeDetailData.theme.openings, { opening ->
//                            val encodedOpening = Uri.encode(opening)
//                            val youtubeSearchUrl =
//                                "${YOUTUBE_URL}/results?search_query=$encodedOpening"
//                            val intent = Intent(Intent.ACTION_VIEW, youtubeSearchUrl.toUri())
//                            context.startActivity(intent)
//                        })
//                        AnimeEnding(animeDetailData.theme.endings, { ending ->
//                            val encodedEnding = Uri.encode(ending)
//                            val youtubeSearchUrl =
//                                "${YOUTUBE_URL}/results?search_query=$encodedEnding"
//                            val intent = Intent(Intent.ACTION_VIEW, youtubeSearchUrl.toUri())
//                            context.startActivity(intent)
//                        })
//                        AnimeExternal(animeDetailData.external)
//                        AnimeStreaming(animeDetailData.streaming)
//                        AnimeEpisodes(animeDetailComplement)
                            Button(onClick = {
                                if (animeDetailComplement is Resource.Success &&
                                    (animeDetailComplement as Resource.Success).data?.episodes?.isNotEmpty() == true &&
                                    defaultEpisode != null
                                ) {
                                    // Navigate using Compose Navigation
                                    navController.navigate(
                                        "animeWatch/${animeDetailData.mal_id}/${
                                            (animeDetailComplement as Resource.Success).data?.episodes?.get(
                                                0
                                            )?.episodeId
                                        }/${defaultEpisode!!.id}"
                                    )
                                }
                            }) {
                                Text("Watch Now")
                            }
                            Button(onClick = {
                                ShareUtils.shareAnimeDetail(context, animeDetailData)
                            }) {
                                Text("Share")
                            }
                        }
                    }
                }

                is Resource.Error -> {
                    Text(text = (animeDetail as Resource.Error).message ?: "Error")
                }

                else -> {
                    Text(text = "Empty")
                }
            }
        }
    }
}