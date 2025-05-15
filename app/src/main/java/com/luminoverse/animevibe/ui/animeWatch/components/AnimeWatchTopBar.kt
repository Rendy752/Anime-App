package com.luminoverse.animevibe.ui.animeWatch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.ui.animeWatch.WatchState
import com.luminoverse.animevibe.ui.common_ui.DebouncedIconButton
import com.luminoverse.animevibe.ui.common_ui.SkeletonBox
import com.luminoverse.animevibe.ui.main.MainState
import com.luminoverse.animevibe.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeWatchTopBar(
    watchState: WatchState,
    mainState: MainState,
    onContentIndexChange: (Int) -> Unit,
    onHandleBackPress: () -> Unit,
    onFavoriteToggle: (EpisodeDetailComplement) -> Unit
) {
    Column {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onHandleBackPress) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            title = {
                if (watchState.animeDetail?.title != null) Text(
                    watchState.animeDetail.title,
                    modifier = Modifier.padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) else {
                    SkeletonBox(
                        width = 200.dp,
                        height = 40.dp
                    )
                }
            },
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mainState.networkStatus.let {
                        Row {
                            Text(
                                text = it.label,
                                color = if (it.iconColor == MaterialTheme.colorScheme.onError) MaterialTheme.colorScheme.onError
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = it.icon,
                                contentDescription = it.label,
                                tint = it.iconColor
                            )
                        }
                    }
                    if (mainState.isLandscape) ContentSegmentedButton(
                        selectedIndex = watchState.selectedContentIndex,
                        onSelectedIndexChange = onContentIndexChange
                    )
                    if (watchState.episodeDetailComplement is Resource.Success) {
                        DebouncedIconButton(
                            onClick = {
                                onFavoriteToggle(
                                    watchState.episodeDetailComplement.data.copy(
                                        isFavorite = !watchState.isFavorite
                                    )
                                )
                            },
                            modifier = Modifier.semantics {
                                contentDescription =
                                    if (watchState.isFavorite) "Remove from favorites" else "Add to favorites"
                            }
                        ) {
                            Icon(
                                imageVector = if (watchState.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
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