package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.EpisodeHistoryQueryState
import com.example.animeapp.ui.common_ui.SearchView
import com.example.animeapp.ui.episodeHistory.EpisodeHistoryAction
import com.example.animeapp.utils.Debounce

@Composable
fun FilterContent(
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isFavoriteFilter == true,
                onClick = {
                    isFavoriteFilter = if (isFavoriteFilter == true) null else true
                    onAction(
                        EpisodeHistoryAction.ApplyFilters(
                            queryState.copy(isFavorite = isFavoriteFilter, page = 1)
                        )
                    )
                },
                label = { Text("Favorites Only") },
                leadingIcon = {
                    Icon(
                        if (isFavoriteFilter == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite Filter"
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilterChip(
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
                        sortBy.name.replace("([A-Z])".toRegex(), " $1").trim()
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