package com.luminoverse.animevibe.ui.episodeHistory.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.EpisodeHistoryQueryState
import com.luminoverse.animevibe.ui.common.SearchView
import com.luminoverse.animevibe.ui.episodeHistory.EpisodeHistoryAction
import com.luminoverse.animevibe.utils.Debounce

@Composable
fun FilterContent(
    modifier: Modifier = Modifier,
    queryState: EpisodeHistoryQueryState,
    onAction: (EpisodeHistoryAction) -> Unit
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember(queryState) { mutableStateOf(queryState.searchQuery) }
    val debounce = remember(queryState) {
        Debounce(scope, 500L) { newQuery ->
            onAction(
                EpisodeHistoryAction.ApplyFilters(queryState.copy(searchQuery = newQuery, page = 1))
            )
        }
    }
    var isFavoriteFilter by remember { mutableStateOf(queryState.isFavorite) }
    var sortBy by remember { mutableStateOf(queryState.sortBy) }

    LaunchedEffect(queryState) {
        searchQuery = queryState.searchQuery
        isFavoriteFilter = queryState.isFavorite
        sortBy = queryState.sortBy
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SearchView(
            query = searchQuery,
            onQueryChange = {
                searchQuery = it
                debounce.query(it)
            },
            placeholder = "Search Anime or Episode",
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                modifier = Modifier.weight(1f),
                selected = isFavoriteFilter == true,
                onClick = {
                    isFavoriteFilter = if (isFavoriteFilter == true) null else true
                    onAction(
                        EpisodeHistoryAction.ApplyFilters(
                            queryState.copy(isFavorite = isFavoriteFilter, page = 1)
                        )
                    )
                },
                label = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Favorites Only",
                        textAlign = TextAlign.Center
                    )
                },
                leadingIcon = {
                    Icon(
                        if (isFavoriteFilter == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite Filter",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            FilterChip(
                modifier = Modifier.weight(1f),
                selected = true,
                onClick = {
                    val sortOptions = EpisodeHistoryQueryState.SortBy.entries
                    val currentIndex = sortOptions.indexOf(sortBy)
                    val nextIndex = (currentIndex + 1) % sortOptions.size
                    sortBy = sortOptions[nextIndex]
                    onAction(
                        EpisodeHistoryAction.ApplyFilters(queryState.copy(sortBy = sortBy))
                    )
                },
                label = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = sortBy.name.replace("([A-Z])".toRegex(), " $1").trim(),
                        textAlign = TextAlign.Center
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}