package com.example.animeapp.ui.episodeHistory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeHistoryQueryState
import com.example.animeapp.ui.common_ui.LimitAndPaginationQueryState
import com.example.animeapp.ui.common_ui.LimitAndPaginationSection
import com.example.animeapp.ui.main.MainState
import com.example.animeapp.ui.main.navigation.NavRoute
import com.example.animeapp.ui.main.navigation.navigateTo
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.TimeUtils.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeHistoryScreen(
    navController: NavHostController,
    mainState: MainState,
    historyState: EpisodeHistoryState,
    onAction: (EpisodeHistoryAction) -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()
    var searchQuery by remember { mutableStateOf(historyState.queryState.searchQuery) }
    var isFavoriteFilter by remember { mutableStateOf(historyState.queryState.isFavorite) }
    var sortBy by remember { mutableStateOf(historyState.queryState.sortBy) }

    LaunchedEffect(historyState.queryState) {
        searchQuery = historyState.queryState.searchQuery
        isFavoriteFilter = historyState.queryState.isFavorite
        sortBy = historyState.queryState.sortBy
    }

    LaunchedEffect(mainState.isConnected) {
        if (!mainState.isConnected) return@LaunchedEffect
        if (historyState.episodeHistoryResults is Resource.Error) {
            onAction(EpisodeHistoryAction.FetchHistory)
        }
    }

    Scaffold { paddingValues ->
        PullToRefreshBox(
            isRefreshing = historyState.isRefreshing,
            onRefresh = { onAction(EpisodeHistoryAction.ApplyFilters(historyState.queryState)) },
            modifier = Modifier.padding(paddingValues),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = historyState.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = pullToRefreshState
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        onAction(
                            EpisodeHistoryAction.ApplyFilters(
                                historyState.queryState.copy(
                                    searchQuery = query,
                                    page = 1
                                )
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = { Text("Search Anime or Episode") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )

                // Filters and Sort
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Filter
                    FilterChip(
                        selected = isFavoriteFilter == true,
                        onClick = {
                            isFavoriteFilter = if (isFavoriteFilter == true) null else true
                            onAction(
                                EpisodeHistoryAction.ApplyFilters(
                                    historyState.queryState.copy(
                                        isFavorite = isFavoriteFilter,
                                        page = 1
                                    )
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
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = false,
                            onClick = { expanded = true },
                            label = { Text("Sort: ${sortBy.name}") }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            EpisodeHistoryQueryState.SortBy.entries.forEach { sortOption ->
                                DropdownMenuItem(
                                    text = { Text(sortOption.name) },
                                    onClick = {
                                        sortBy = sortOption
                                        onAction(
                                            EpisodeHistoryAction.ApplyFilters(
                                                historyState.queryState.copy(
                                                    sortBy = sortOption,
                                                    page = 1
                                                )
                                            )
                                        )
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Episode List
                when (val results = historyState.episodeHistoryResults) {
                    is Resource.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    is Resource.Success -> {
                        if (results.data.isEmpty()) {
                            Text(
                                text = "No episodes found",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(results.data) { episode ->
                                    EpisodeHistoryItem(
                                        episode = episode,
                                        onClick = {
                                            navController.navigateTo(
                                                NavRoute.AnimeWatch.fromParams(
                                                    episode.malId,
                                                    episode.id
                                                )
                                            )
                                        },
                                        onToggleFavorite = { isFavorite ->
                                            onAction(
                                                EpisodeHistoryAction.ToggleFavorite(
                                                    episode.id,
                                                    isFavorite
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is Resource.Error -> {
                        Text(
                            text = results.message ?: "Error loading history",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                // Pagination
                LimitAndPaginationSection(
                    isVisible = historyState.episodeHistoryResults is Resource.Success,
                    pagination = historyState.pagination,
                    query = LimitAndPaginationQueryState(
                        page = historyState.queryState.page,
                        limit = historyState.queryState.limit
                    ),
                    onQueryChanged = { updatedQuery ->
                        onAction(
                            EpisodeHistoryAction.ApplyFilters(
                                historyState.queryState.copy(
                                    page = updatedQuery.page,
                                    limit = updatedQuery.limit ?: historyState.queryState.limit
                                )
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun EpisodeHistoryItem(
    episode: EpisodeDetailComplement,
    onClick: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = episode.animeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Episode ${episode.number}: ${episode.episodeTitle}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                episode.lastTimestamp?.let { timestamp ->
                    Text(
                        text = "Watched: ${formatTimestamp(timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = { onToggleFavorite(!episode.isFavorite) }
            ) {
                Icon(
                    imageVector = if (episode.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (episode.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}