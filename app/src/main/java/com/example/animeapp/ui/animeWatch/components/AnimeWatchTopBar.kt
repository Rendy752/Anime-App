package com.example.animeapp.ui.animeWatch.components

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.NetworkStatus
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.ui.theme.favoriteEpisode
import com.example.animeapp.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeWatchTopBar(
    title: String?,
    isFavorite: Boolean,
    isLandscape: Boolean,
    networkStatus: NetworkStatus?,
    selectedContentIndex: Int,
    onContentIndexChange: (Int) -> Unit,
    episodeDetailComplement: Resource<EpisodeDetailComplement>,
    onHandleBackPress: () -> Unit,
    onFavoriteToggle: (EpisodeDetailComplement) -> Unit
) {
    val scope = rememberCoroutineScope()
    val debounceJob = remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose { debounceJob.value?.cancel() }
    }

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
                if (title != null) Text(
                    title,
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
                    networkStatus?.let {
                        Row {
                            Text(
                                text = it.label,
                                color = if (it.color == MaterialTheme.colorScheme.onError) MaterialTheme.colorScheme.onError
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = it.icon,
                                contentDescription = it.label,
                                tint = it.color
                            )
                        }
                    }
                    if (isLandscape) ContentSegmentedButton(
                        selectedIndex = selectedContentIndex,
                        onSelectedIndexChange = onContentIndexChange
                    )
                    if (episodeDetailComplement is Resource.Success) {
                        IconButton(onClick = {
                            debounceJob.value?.cancel()
                            debounceJob.value = scope.launch {
                                delay(300)
                                onFavoriteToggle(episodeDetailComplement.data.copy(isFavorite = !isFavorite))
                            }
                        }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = favoriteEpisode
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