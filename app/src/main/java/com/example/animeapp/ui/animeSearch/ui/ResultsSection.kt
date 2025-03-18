package com.example.animeapp.ui.animeSearch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.animeapp.R
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.ui.animeSearch.components.AnimeSearchItem
import com.example.animeapp.ui.common_ui.ErrorMessage
import com.example.animeapp.utils.Resource

@Composable
fun ResultsSection(navController: NavController, viewModel: AnimeSearchViewModel) {
    val animeList = viewModel.animeSearchResults.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        when (animeList) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is Resource.Success -> {
                if (animeList.data?.data.isNullOrEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorMessage(stringResource(R.string.no_results_found))
                    }
                } else {
                    LazyColumn {
                        items(animeList.data.data) { anime ->
                            AnimeSearchItem(anime = anime, onGenreClick = { genre ->
                                val genreMalId = anime.genres?.find { it.name == genre }?.mal_id
                                genreMalId?.let {
                                    viewModel.setSelectedGenreId(it)
                                    viewModel.applyGenreFilters()
                                }
                            }) { animeId ->
                                navController.navigate("animeDetail/$animeId")
                            }
                        }
                    }
                }
            }

            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorMessage(stringResource(R.string.error_loading_data))
                }
            }
        }
    }
}