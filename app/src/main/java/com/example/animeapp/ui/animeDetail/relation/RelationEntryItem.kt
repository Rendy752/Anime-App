package com.example.animeapp.ui.animeDetail.relation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.ui.common_ui.AnimeSearchItem
import com.example.animeapp.ui.common_ui.AnimeSearchItemSkeleton
import com.example.animeapp.utils.Navigation.navigateWithFilter
import com.example.animeapp.utils.Resource

@Composable
fun RelationEntryItem(
    entryId: Int,
    entryName: String,
    getAnimeDetail: suspend (Int) -> Resource<AnimeDetailResponse>,
    navController: NavController,
    onItemClickListener: (Int) -> Unit
) {
    var animeDetail: AnimeDetail? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error: String? by remember { mutableStateOf(null) }

    LaunchedEffect(entryId) {
        isLoading = true
        when (val result = getAnimeDetail(entryId)) {
            is Resource.Success -> {
                animeDetail = result.data.data
                isLoading = false
            }

            is Resource.Error -> {
                error = result.message
                isLoading = false
            }

            is Resource.Loading -> {
                isLoading = true
            }
        }
    }

    when {
        isLoading -> AnimeSearchItemSkeleton()
        error != null || animeDetail == null -> AnimeSearchItem(errorTitle = entryName)
        else -> AnimeSearchItem(
            anime = animeDetail,
            onGenreClick = { genre ->
                navController.navigateWithFilter(genre, genreFilter = true)
            },
            onItemClick = { onItemClickListener(entryId) }
        )
    }
}