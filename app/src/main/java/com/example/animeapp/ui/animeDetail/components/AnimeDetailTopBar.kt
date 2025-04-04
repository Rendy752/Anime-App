package com.example.animeapp.ui.animeDetail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailTopBar(
    animeTitle: String,
    animeDetail: Resource<AnimeDetailResponse>?,
    animeDetailComplement: Resource<AnimeDetailComplement?>?,
    defaultEpisode: EpisodeDetailComplement?,
    navController: NavController,
    onFavoriteToggle: (AnimeDetailComplement) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val debounceJob = remember { mutableStateOf<Job?>(null) }
    val isFavorite = remember { mutableStateOf(false) }
    animeDetailComplement?.data?.isFavorite?.let {
        if (animeDetailComplement is Resource.Success) isFavorite.value = it
    }

    DisposableEffect(Unit) {
        onDispose {
            debounceJob.value?.cancel()
        }
    }

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
                    overflow = TextOverflow.Ellipsis
                )
            },
            actions = {
                animeDetail?.data?.data?.let { animeDetailData ->
                    if (animeDetailComplement is Resource.Success) {
                        IconButton(onClick = {
                            isFavorite.value = !isFavorite.value
                            debounceJob.value?.cancel()
                            debounceJob.value = scope.launch {
                                delay(300)
                                animeDetailComplement.data?.let {
                                    onFavoriteToggle(it.copy(isFavorite = isFavorite.value))
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (isFavorite.value) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite.value) "Remove from favorites" else "Add to favorites",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    if (animeDetailComplement is Resource.Success &&
                        animeDetailComplement.data?.episodes?.isNotEmpty() == true &&
                        defaultEpisode != null
                    ) {
                        animeDetailComplement.data.let { animeDetailComplement ->
                            IconButton(onClick = {
                                navController.navigateToAnimeWatch(
                                    animeDetail = animeDetailData,
                                    animeDetailComplement = animeDetailComplement,
                                    episodeId = animeDetailComplement.lastEpisodeWatchedId
                                        ?: animeDetailComplement.episodes[0].episodeId,
                                    episodes = animeDetailComplement.episodes,
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