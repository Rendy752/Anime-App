package com.example.animeapp.ui.animeSearch.limitAndPagination

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import com.example.animeapp.models.AnimeSearchQueryState
import com.example.animeapp.models.CompletePagination

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LimitAndPaginationSection(
    query: AnimeSearchQueryState,
    pagination: CompletePagination?,
    onQueryChanged: (AnimeSearchQueryState) -> Unit,
    useHorizontalPager: Boolean = true
) {
    var selectedLimit by remember { mutableIntStateOf(query.limit ?: 10) }

    if (useHorizontalPager) {
        LimitAndPaginationHorizontalPager(
            pagination = pagination,
            selectedLimit = selectedLimit,
            onPageChange = { onQueryChanged(query.copy(page = it)) },
            onLimitChange = {
                selectedLimit = it
                onQueryChanged(query.copy(limit = it, page = 1))
            }
        )
    } else {
        LimitAndPaginationTopBottom(
            pagination = pagination,
            selectedLimit = selectedLimit,
            onPageChange = { onQueryChanged(query.copy(page = it)) },
            onLimitChange = {
                selectedLimit = it
                onQueryChanged(query.copy(limit = it, page = 1))
            }
        )
    }
}
