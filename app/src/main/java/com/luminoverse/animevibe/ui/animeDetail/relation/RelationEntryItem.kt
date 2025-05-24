package com.luminoverse.animevibe.ui.animeDetail.relation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.luminoverse.animevibe.ui.animeDetail.DetailAction
import com.luminoverse.animevibe.ui.animeDetail.DetailState
import com.luminoverse.animevibe.ui.common.AnimeSearchItem
import com.luminoverse.animevibe.ui.common.AnimeSearchItemSkeleton
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import com.luminoverse.animevibe.utils.resource.Resource

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
            onGenreClick = { genreId ->
                navController.navigateTo(
                    NavRoute.SearchWithFilter.fromFilter(
                        genreId = genreId,
                        producerId = null
                    )
                )
            },
            onItemClick = { onItemClickListener(entryId) }
        )

        null -> AnimeSearchItemSkeleton()
    }
}