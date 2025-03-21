package com.example.animeapp.ui.animeDetail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tv
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.animeapp.R
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ShareUtils
import com.example.animeapp.ui.animeDetail.AnimeDetailViewModel
import com.example.animeapp.ui.common_ui.AnimeHeader
import com.example.animeapp.ui.common_ui.DetailCommonBody
import com.example.animeapp.ui.common_ui.YoutubePreview

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
                                        imageVector = Icons.Filled.Tv,
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
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
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            AnimeHeader(animeDetailData)
                            NumberDetailSection(animeDetailData)
                            YoutubePreview(animeDetailData.trailer.embed_url)
                            DetailBodySection(animeDetailData, navController)
                            DetailCommonBody("Background", animeDetailData.background)
                            DetailCommonBody("Synopsis", animeDetailData.synopsis)
//                        AnimeRelation(animeDetailData.relations, { relatedAnimeId ->
//                            viewModel.handleAnimeDetail(relatedAnimeId)
//                        })
//                        AnimeEpisodes(animeDetailComplement)
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