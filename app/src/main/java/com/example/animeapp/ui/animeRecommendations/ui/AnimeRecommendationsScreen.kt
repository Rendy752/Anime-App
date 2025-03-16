package com.example.animeapp.ui.animeRecommendations.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.animeapp.R
import com.example.animeapp.ui.animeRecommendations.components.AnimeRecommendationItem
import com.example.animeapp.ui.animeRecommendations.components.AnimeRecommendationItemSkeleton
import com.example.animeapp.ui.animeRecommendations.viewmodel.AnimeRecommendationsViewModel
import com.example.animeapp.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeRecommendationsScreen(navController: NavController) {
    val viewModel: AnimeRecommendationsViewModel = hiltViewModel()
    val animeRecommendationsState by viewModel.animeRecommendations.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Anime Recommendations")
                }
            )
        },
        floatingActionButton = {
            if (animeRecommendationsState is Resource.Success) {
                FloatingActionButton(onClick = { viewModel.getAnimeRecommendations() }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(id = R.string.refresh_button)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (animeRecommendationsState) {
                is Resource.Loading -> {
                    repeat(3) { AnimeRecommendationItemSkeleton() }
                }

                is Resource.Success -> {
                    val animeRecommendations =
                        (animeRecommendationsState as Resource.Success).data?.data ?: emptyList()
                    LazyColumn {
                        items(animeRecommendations) { recommendation ->
                            AnimeRecommendationItem(
                                recommendation = recommendation,
                                onItemClick = { animeId -> navController.navigate("animeDetail/$animeId") }
                            )
                        }
                    }
                }

                is Resource.Error -> {
                    Text(
                        text = stringResource(id = R.string.no_internet_connection),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}