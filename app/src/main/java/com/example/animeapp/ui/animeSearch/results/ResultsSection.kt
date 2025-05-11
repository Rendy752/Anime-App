package com.example.animeapp.ui.animeSearch.results

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.animeapp.ui.main.navigation.NavRoute
import com.example.animeapp.ui.main.navigation.navigateTo
import com.example.animeapp.utils.Resource

@Composable
fun ResultsSection(
    modifier: Modifier = Modifier,
    resultsSectionScrollState: LazyListState,
    navController: NavController,
    query: String,
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = resultsSectionScrollState
            ) {
                itemsIndexed((0 until 3).toList()) { index, _ ->
                    AnimeSearchItemSkeleton(
                        modifier = Modifier.padding(
                            top = if (index == 0) 8.dp else 0.dp, start = 8.dp, end = 8.dp
                        )
                    )
                }
            }

            is Resource.Success -> {
                if (animeSearchResults.data.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MessageDisplay("No Results Found")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = resultsSectionScrollState
                    ) {
                        itemsIndexed(animeSearchResults.data.data) { index, animeDetail ->
                            AnimeSearchItem(
                                modifier = Modifier.padding(
                                    top = if (index == 0) 8.dp else 0.dp,
                                    start = 8.dp,
                                    end = 8.dp
                                ),
                                animeDetail = animeDetail,
                                query = query,
                                selectedGenres = selectedGenres,
                                onGenreClick = { genreClickedId ->
                                    val genre =
                                        genres.data?.data?.find { it.mal_id == genreClickedId }
                                    genre?.let { onGenreClick(genre) }
                                },
                                onItemClick = {
                                    navController.navigateTo(NavRoute.AnimeDetail.fromId(animeDetail.mal_id))
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