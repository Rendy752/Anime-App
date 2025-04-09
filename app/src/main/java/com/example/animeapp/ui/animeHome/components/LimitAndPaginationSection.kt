package com.example.animeapp.ui.animeHome.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import com.example.animeapp.models.AnimeSeasonNowResponse
import com.example.animeapp.models.AnimeSeasonNowSearchQueryState
import com.example.animeapp.ui.animeSearch.limitAndPagination.LimitAndPaginationHorizontalPager
import com.example.animeapp.utils.Resource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LimitAndPaginationSection(
    animeSeasonNow: Resource<AnimeSeasonNowResponse>,
    query: AnimeSeasonNowSearchQueryState,
    onQueryChanged: (AnimeSeasonNowSearchQueryState) -> Unit,
) {
    var selectedLimit by remember { mutableIntStateOf(query.limit ?: 10) }

    AnimatedVisibility(
        visible = animeSeasonNow is Resource.Success,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 1000, easing = EaseInOut)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 1000, easing = EaseInOut)
        )
    ) {
        LimitAndPaginationHorizontalPager(
            pagination = animeSeasonNow.data?.pagination,
            selectedLimit = selectedLimit,
            onPageChange = { onQueryChanged(query.copy(page = it)) },
            onLimitChange = {
                selectedLimit = it
                onQueryChanged(query.copy(limit = it, page = 1))
            }
        )
    }
}
