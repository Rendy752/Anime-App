package com.luminoverse.animevibe.ui.animeDetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.luminoverse.animevibe.models.AnimeDetailComplement
import com.luminoverse.animevibe.models.AnimeDetailResponse
import com.luminoverse.animevibe.ui.common.DebouncedIconButton
import com.luminoverse.animevibe.ui.common.SkeletonBox
import com.luminoverse.animevibe.utils.resource.Resource
import com.luminoverse.animevibe.utils.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailTopBar(
    animeDetail: Resource<AnimeDetailResponse>?,
    animeDetailComplement: Resource<AnimeDetailComplement?>?,
    navController: NavController,
    playEpisode: (Int, String) -> Unit,
    onFavoriteToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val isFavorite = remember { mutableStateOf(false) }
    animeDetailComplement?.data?.isFavorite?.let {
        if (animeDetailComplement is Resource.Success) isFavorite.value = it
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            when (animeDetail) {
                is Resource.Loading -> SkeletonBox(
                    width = 200.dp,
                    height = 40.dp
                )

                is Resource.Success -> Text(
                    text = animeDetail.data.data.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                is Resource.Error -> Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                else -> Text(
                    text = "Anime not found",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

                if ((animeDetailComplement is Resource.Success || animeDetailComplement is Resource.Loading) && animeDetailComplement.data?.episodes?.isNotEmpty() == true) {
                    animeDetailComplement.data.let { animeDetailComplement ->
                        animeDetailComplement?.episodes?.let { episodes ->
                            IconButton(onClick = {
                                playEpisode(
                                    animeDetailData.mal_id,
                                    animeDetailComplement.lastEpisodeWatchedId
                                        ?: episodes.first().id
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

                IconButton(
                    onClick = {
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
    }
}