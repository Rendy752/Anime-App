package com.example.animeapp.ui.animeSearch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.animeapp.R
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.ui.common_ui.AnimeSearchItem
import com.example.animeapp.ui.common_ui.AnimeSearchItemSkeleton
import com.example.animeapp.ui.common_ui.ErrorMessage
import com.example.animeapp.utils.Resource

@Composable
fun ResultsSection(navController: NavController, viewModel: AnimeSearchViewModel) {
    val animeList = viewModel.animeSearchResults.collectAsState().value
    val selectedGenres = viewModel.selectedGenres.collectAsState().value
    val genres = viewModel.genres.collectAsState().value

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (animeList) {
            is Resource.Loading -> {
                repeat(3) { AnimeSearchItemSkeleton() }
            }

            is Resource.Success -> {
                if (animeList.data?.data.isNullOrEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorMessage(stringResource(R.string.no_results_found))
                    }
                } else {
                    LazyColumn {
                        items(animeList.data.data) { anime ->
                            AnimeSearchItem(
                                anime = anime,
                                selectedGenres = selectedGenres,
                                onGenreClick = { genre ->
                                    val genre =
                                        genres.data?.data?.find { it.mal_id == genre.mal_id }
                                    genre?.let {
                                        viewModel.setSelectedGenre(it)
                                        viewModel.applyGenreFilters()
                                    }
                                }) { animeId ->
                                navController.navigate("animeDetail/${anime.title}/$animeId")
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