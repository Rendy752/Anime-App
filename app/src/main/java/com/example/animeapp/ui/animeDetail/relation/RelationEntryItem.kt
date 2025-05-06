package com.example.animeapp.ui.animeDetail.relation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.example.animeapp.ui.animeDetail.DetailAction
import com.example.animeapp.ui.animeDetail.DetailState
import com.example.animeapp.ui.common_ui.AnimeSearchItem
import com.example.animeapp.ui.common_ui.AnimeSearchItemSkeleton
import com.example.animeapp.utils.Navigation.navigateWithFilter
import com.example.animeapp.utils.Resource

@Composable
fun RelationEntryItem(
    entryId: Int,
    entryName: String,
    detailState: DetailState,
    navController: NavController,
    onAction: (DetailAction) -> Unit,
    onItemClickListener: (Int) -> Unit
) {
    LaunchedEffect(entryId) {
        if (detailState.relationAnimeDetails[entryId] == null) {
            onAction(DetailAction.LoadRelationAnimeDetail(entryId))
        }
    }

    val relationDetail = detailState.relationAnimeDetails[entryId]

    when (relationDetail) {
        is Resource.Loading -> AnimeSearchItemSkeleton()
        is Resource.Error -> AnimeSearchItem(errorTitle = entryName)
        is Resource.Success -> AnimeSearchItem(
            animeDetail = relationDetail.data,
            onGenreClick = { genre ->
                navController.navigateWithFilter(genre, genreFilter = true)
            },
            onItemClick = { onItemClickListener(entryId) }
        )

        null -> AnimeSearchItemSkeleton()
    }
}