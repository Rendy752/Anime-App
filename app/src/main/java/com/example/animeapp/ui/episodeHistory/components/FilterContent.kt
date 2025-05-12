package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.EpisodeHistoryQueryState
import com.example.animeapp.ui.episodeHistory.EpisodeHistoryAction

@Composable
fun FilterContent(
    queryState: EpisodeHistoryQueryState,
    onAction: (EpisodeHistoryAction) -> Unit
) {
    var searchQuery by remember { mutableStateOf(queryState.searchQuery) }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                onAction(
                    EpisodeHistoryAction.ApplyFilters(
                        queryState.copy(searchQuery = query, page = 1)
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search Anime or Episode") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true
        )

        // Filters and Sort
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Favorite Filter
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
                }
            )

            // Sort Dropdown
            var sortExpanded by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = false,
                    onClick = { sortExpanded = true },
                    label = { Text("Sort: ${sortBy.name}") }
                )
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    EpisodeHistoryQueryState.SortBy.entries.forEach { sortOption ->
                        DropdownMenuItem(
                            text = { Text(sortOption.name) },
                            onClick = {
                                sortBy = sortOption
                                onAction(
                                    EpisodeHistoryAction.ApplyFilters(
                                        queryState.copy(sortBy = sortOption, page = 1)
                                    )
                                )
                                sortExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
