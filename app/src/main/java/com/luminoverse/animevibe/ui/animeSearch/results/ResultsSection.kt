package com.luminoverse.animevibe.ui.animeSearch.results

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
import com.luminoverse.animevibe.models.AnimeSearchResponse
import com.luminoverse.animevibe.models.Genre
import com.luminoverse.animevibe.models.GenresResponse
import com.luminoverse.animevibe.ui.common.AnimeSearchItem
import com.luminoverse.animevibe.ui.common.AnimeSearchItemSkeleton
import com.luminoverse.animevibe.ui.common.MessageDisplay
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import com.luminoverse.animevibe.utils.Resource

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
                    AnimeSearchItemSkeleton(modifier = Modifier.padding(horizontal = 8.dp))
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
                                modifier = Modifier.padding(horizontal = 8.dp),
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