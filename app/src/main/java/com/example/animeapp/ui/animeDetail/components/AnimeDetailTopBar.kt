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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.ui.common_ui.DebouncedIconButton
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.ui.main.navigation.NavRoute
import com.example.animeapp.ui.main.navigation.navigateTo
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailTopBar(
    animeDetail: Resource<AnimeDetailResponse>?,
    animeDetailComplement: Resource<AnimeDetailComplement?>?,
    defaultEpisodeId: String?,
    navController: NavController,
    onFavoriteToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val isFavorite = remember { mutableStateOf(false) }
    animeDetailComplement?.data?.isFavorite?.let {
        if (animeDetailComplement is Resource.Success) isFavorite.value = it
    }

    Column {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = {
                when (animeDetail) {
                    is Resource.Loading -> SkeletonBox(
                        width = 200.dp,
                        height = 40.dp
                    )

                    is Resource.Success -> Text(
                        text = animeDetail.data.data.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    is Resource.Error -> Text(
                        text = "Error",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    else -> Text(
                        text = "Empty",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            actions = {
                animeDetail?.data?.data?.let { animeDetailData ->
                    if (animeDetailComplement is Resource.Success) {
                        DebouncedIconButton(
                            onClick = {
                                isFavorite.value = !isFavorite.value
                                animeDetailComplement.data?.let {
                                    onFavoriteToggle(isFavorite.value)
                                }
                            },
                            modifier = Modifier.semantics {
                                contentDescription =
                                    if (isFavorite.value) "Remove from favorites" else "Add to favorites"
                            }
                        ) {
                            Icon(
                                imageVector = if (isFavorite.value) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    if (animeDetailComplement is Resource.Success &&
                        animeDetailComplement.data?.episodes?.isNotEmpty() == true &&
                        defaultEpisodeId != null
                    ) {
                        animeDetailComplement.data.let { animeDetailComplement ->
                            animeDetailComplement.episodes?.let { episodes ->
                                IconButton(onClick = {
                                    navController.navigateTo(
                                        NavRoute.AnimeWatch.fromParams(
                                            malId = animeDetailData.mal_id,
                                            episodeId = animeDetailComplement.lastEpisodeWatchedId
                                                ?: defaultEpisodeId
                                        )
                                    )
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.LiveTv,
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentDescription = "Watch"
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = {
                        ShareUtils.shareAnimeDetail(context, animeDetailData)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = "Share"
                        )
                    }
                }
            }
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceContainer,
            thickness = 2.dp
        )
    }
}