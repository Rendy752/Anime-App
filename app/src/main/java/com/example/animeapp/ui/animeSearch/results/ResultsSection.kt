package com.example.animeapp.ui.animeSearch.results

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.animeapp.models.AnimeSearchResponse
import com.example.animeapp.models.Genre
import com.example.animeapp.models.GenresResponse
import com.example.animeapp.ui.common_ui.AnimeSearchItem
import com.example.animeapp.ui.common_ui.AnimeSearchItemSkeleton
import com.example.animeapp.ui.common_ui.MessageDisplay
import com.example.animeapp.utils.Navigation.navigateToAnimeDetail
import com.example.animeapp.utils.Resource

@Composable
fun ResultsSection(
    modifier: Modifier = Modifier,
    navController: NavController,
    animeSearchResults: Resource<AnimeSearchResponse>,
    selectedGenres: List<Genre>,
    genres: Resource<GenresResponse>,
    onGenreClick: (Genre) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (animeSearchResults) {
            is Resource.Loading -> LazyColumn(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(3) { AnimeSearchItemSkeleton() }
            }

            is Resource.Success -> {
                if (animeSearchResults.data.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MessageDisplay("No Results Found")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(animeSearchResults.data.data) { animeDetail ->
                            AnimeSearchItem(
                                animeDetail = animeDetail,
                                selectedGenres = selectedGenres,
                                onGenreClick = { genre ->
                                    val genre =
                                        genres.data?.data?.find { it.mal_id == genre.mal_id }
                                    genre?.let {
                                        onGenreClick(genre)
                                    }
                                },
                                onItemClick = {
                                    navController.navigateToAnimeDetail(animeDetail.mal_id)
                                }
                            )
                        }
                    }
                }
            }

            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MessageDisplay("Error Loading Data")
                }
            }
        }
    }
}