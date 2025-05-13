package com.example.animeapp.ui.episodeHistory.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    var isAscending by remember { mutableStateOf(queryState.isAscending) }

    LaunchedEffect(queryState) {
        searchQuery = queryState.searchQuery
        isFavoriteFilter = queryState.isFavorite
        sortBy = queryState.sortBy
        isAscending = queryState.isAscending
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
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

            var sortExpanded by remember { mutableStateOf(false) }
            Box {
                TextButton(
                    onClick = { sortExpanded = true },
                    modifier = Modifier.semantics { contentDescription = "Sort by ${sortBy.name}" }
                ) {
                    Text("Sort: ${sortBy.name.replace("([A-Z])".toRegex(), " $1").trim()}")
                }
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    EpisodeHistoryQueryState.SortBy.entries.forEach { sortOption ->
                        DropdownMenuItem(
                            text = { Text(sortOption.name.replace("([A-Z])".toRegex(), " $1").trim()) },
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isAscending) "Asc" else "Desc",
                    style = MaterialTheme.typography.labelMedium
                )
                Switch(
                    checked = isAscending,
                    onCheckedChange = {
                        isAscending = it
                        onAction(
                            EpisodeHistoryAction.ApplyFilters(
                                queryState.copy(isAscending = it, page = 1)
                            )
                        )
                    },
                    modifier = Modifier.semantics { contentDescription = "Toggle sort order" }
                )
            }
        }
    }
}