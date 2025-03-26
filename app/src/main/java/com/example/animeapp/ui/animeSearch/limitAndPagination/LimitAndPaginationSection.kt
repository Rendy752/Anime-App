package com.example.animeapp.ui.animeSearch.limitAndPagination

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.ui.animeSearch.AnimeSearchViewModel

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LimitAndPaginationSection(
    viewModel: AnimeSearchViewModel,
    useHorizontalPager: Boolean = true
) {
    val animeSearchQueryState by viewModel.queryState.collectAsState()
    val animeSearchData by viewModel.animeSearchResults.collectAsState()
    var selectedLimit by remember { mutableIntStateOf(animeSearchQueryState.limit ?: 10) }
    val paginationState = animeSearchData.data?.pagination

    if (paginationState != null && useHorizontalPager) HorizontalDivider()

    if (useHorizontalPager) {
        HorizontalPager(state = rememberPagerState { 2 }, Modifier.padding(8.dp)) { page ->
            LimitAndPaginationContent(
                page = page,
                paginationState = paginationState,
                selectedLimit = selectedLimit,
                onPageChange = { viewModel.applyFilters(animeSearchQueryState.copy(page = it)) },
                onLimitChange = {
                    selectedLimit = it
                    viewModel.applyFilters(animeSearchQueryState.copy(limit = it, page = 1))
                }
            )
        }
    } else {
        LimitAndPaginationContent(
            page = 0,
            paginationState = paginationState,
            selectedLimit = selectedLimit,
            onPageChange = { viewModel.applyFilters(animeSearchQueryState.copy(page = it)) },
            onLimitChange = {
                selectedLimit = it
                viewModel.applyFilters(animeSearchQueryState.copy(limit = it, page = 1))
            },
            isPager = false
        )
    }
}