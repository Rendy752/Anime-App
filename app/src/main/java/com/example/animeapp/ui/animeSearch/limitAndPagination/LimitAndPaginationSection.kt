package com.example.animeapp.ui.animeSearch.limitAndPagination

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeSearchQueryState
import com.example.animeapp.models.CompletePagination

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LimitAndPaginationSection(
    query: AnimeSearchQueryState,
    pagination: CompletePagination?,
    onQueryChanged: (AnimeSearchQueryState) -> Unit,
    useHorizontalPager: Boolean = true
) {
    var selectedLimit by remember { mutableIntStateOf(query.limit ?: 10) }

    if (pagination != null && useHorizontalPager) HorizontalDivider()

    if (useHorizontalPager) {
        HorizontalPager(state = rememberPagerState { 2 }, Modifier.padding(8.dp)) { page ->
            LimitAndPaginationContent(
                page = page,
                paginationState = pagination,
                selectedLimit = selectedLimit,
                onPageChange = { onQueryChanged(query.copy(page = it)) },
                onLimitChange = {
                    selectedLimit = it
                    onQueryChanged(query.copy(limit = it, page = 1))
                }
            )
        }
    } else {
        LimitAndPaginationContent(
            page = 0,
            paginationState = pagination,
            selectedLimit = selectedLimit,
            onPageChange = { onQueryChanged(query.copy(page = it)) },
            onLimitChange = {
                selectedLimit = it
                onQueryChanged(query.copy(limit = it, page = 1))
            },
            isPager = false
        )
    }
}