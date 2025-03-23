package com.example.animeapp.ui.animeDetail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.animeapp.R
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.utils.Navigation.navigateToAnimeWatch
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailTopBar(
    animeTitle: String,
    animeDetail: Resource<AnimeDetailResponse>?,
    animeDetailComplement: Resource<AnimeDetailComplement?>?,
    defaultEpisode: EpisodeDetailComplement?,
    navController: NavController
) {
    val context = LocalContext.current
    Column {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = {
                Text(
                    text = animeDetail?.data?.data?.title ?: animeTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Companion.Ellipsis
                )
            },
            actions = {
                animeDetail?.data?.data?.let { animeDetailData ->
                    if (animeDetailComplement is Resource.Success &&
                        animeDetailComplement.data?.episodes?.isNotEmpty() == true &&
                        defaultEpisode != null
                    ) {
                        animeDetailComplement.data.let { animeDetailComplementData ->
                            IconButton(onClick = {
                                navController.navigateToAnimeWatch(
                                    animeDetail = animeDetailData,
                                    episodeId = animeDetailComplementData.episodes[0].episodeId,
                                    episodes = animeDetailComplementData.episodes,
                                    defaultEpisode = defaultEpisode
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.LiveTv,
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = stringResource(id = R.string.watch)
                                )
                            }
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
}