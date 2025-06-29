package com.luminoverse.animevibe.ui.animeDetail.relation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.luminoverse.animevibe.models.AnimeDetail
import com.luminoverse.animevibe.ui.animeDetail.DetailAction
import com.luminoverse.animevibe.ui.common.AnimeSearchItem
import com.luminoverse.animevibe.ui.common.AnimeSearchItemSkeleton
import com.luminoverse.animevibe.ui.common.SharedImageState
import com.luminoverse.animevibe.ui.main.navigation.NavRoute
import com.luminoverse.animevibe.ui.main.navigation.navigateTo
import com.luminoverse.animevibe.utils.resource.Resource

@Composable
fun RelationEntryItem(
    entryId: Int,
    entryName: String,
    relationDetail: Resource<AnimeDetail>?,
    navController: NavController,
    onAction: (DetailAction) -> Unit,
    showImagePreview: (SharedImageState) -> Unit,
    onItemClickListener: (Int) -> Unit
) {
    LaunchedEffect(entryId) {
        if (relationDetail == null) {
            onAction(DetailAction.LoadRelationAnimeDetail(entryId))
        }
    }


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
            onItemClick = { onItemClickListener(entryId) },
            showImagePreview = showImagePreview
        )

        null -> AnimeSearchItemSkeleton()
    }
}