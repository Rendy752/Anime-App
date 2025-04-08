package com.example.animeapp.ui.animeRecommendations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.animeapp.R
import com.example.animeapp.ui.animeRecommendations.recommendations.RecommendationItem
import com.example.animeapp.ui.animeRecommendations.recommendations.RecommendationItemSkeleton
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.ui.main.BottomScreen
import com.example.animeapp.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeRecommendationsScreen(
    navController: NavController,
    isConnected: Boolean,
    isLandscape: Boolean
) {
    val viewModel: AnimeRecommendationsViewModel = hiltViewModel()

    val animeRecommendations by viewModel.animeRecommendations.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val state = rememberPullToRefreshState()

    LaunchedEffect(isConnected) {
        if (isConnected && animeRecommendations is Resource.Error) viewModel.getAnimeRecommendations()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = BottomScreen.Recommendations.label,
                            modifier = Modifier.padding(end = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.getAnimeRecommendations() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = state,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = state
                )
            },
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                when (animeRecommendations) {
                    is Resource.Loading -> {
                        if (!isLandscape) repeat(3) { RecommendationItemSkeleton() }
                        else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                repeat(2) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        repeat(2) { RecommendationItemSkeleton() }
                                    }
                                }
                            }
                        }
                    }

                    is Resource.Success -> {
                        animeRecommendations.data?.data?.let { animeRecommendations ->
                            if (!isLandscape) {
                                LazyColumn {
                                    items(animeRecommendations) {
                                        RecommendationItem(
                                            recommendation = it,
                                            onItemClick = { anime -> navController.navigate("animeDetail/${anime.title}/${anime.mal_id}") }
                                        )
                                    }
                                }
                            } else {
                                val listSize = animeRecommendations.size
                                val itemsPerColumn = (listSize + 1) / 2
                                Row(modifier = Modifier.fillMaxSize()) {
                                    repeat(2) { columnIndex ->
                                        val startIndex = columnIndex * itemsPerColumn
                                        val endIndex = minOf(startIndex + itemsPerColumn, listSize)
                                        val columnItems =
                                            animeRecommendations.subList(startIndex, endIndex)
                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            items(columnItems) {
                                                RecommendationItem(
                                                    recommendation = it,
                                                    onItemClick = { anime ->
                                                        navController.navigate(
                                                            "animeDetail/${anime.title}/${anime.mal_id}"
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is Resource.Error -> {
                        MessageDisplay(
                            animeRecommendations.message
                                ?: stringResource(R.string.error_loading_data)
                        )
                    }
                }
            }
        }
    }
}