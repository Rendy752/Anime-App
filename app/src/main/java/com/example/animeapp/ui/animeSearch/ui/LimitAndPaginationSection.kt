package com.example.animeapp.ui.animeSearch.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.utils.Limit
import com.example.animeapp.ui.animeSearch.components.PaginationButtons
import com.example.animeapp.ui.common_ui.DropdownInputField

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LimitAndPaginationSection(
    viewModel: AnimeSearchViewModel,
    useHorizontalPager: Boolean = true
) {
    val animeSearchQueryState by viewModel.queryState.collectAsState()
    val animeSearchData by viewModel.animeSearchResults.collectAsState()
    var selectedLimit by remember { mutableIntStateOf(animeSearchQueryState.limit ?: 10) }
    val pagerState = rememberPagerState { 2 }
    val paginationState = animeSearchData.data?.pagination

    if (paginationState != null && useHorizontalPager) HorizontalDivider()

    if (useHorizontalPager) {
        HorizontalPager(state = pagerState, Modifier.padding(8.dp)) { page ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (page == 0) {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (paginationState != null) {
                            PaginationButtons(paginationState) { pageNumber ->
                                viewModel.applyFilters(animeSearchQueryState.copy(page = pageNumber))
                            }
                        }
                    }
                } else {
                    DropdownInputField(
                        label = "Limit",
                        options = Limit.limitOptions.map { it.toString() },
                        selectedValue = selectedLimit.toString(),
                        onValueChange = {
                            selectedLimit = it.toInt()
                            if (animeSearchQueryState.limit != selectedLimit) {
                                val updatedQueryState = animeSearchQueryState.copy(
                                    limit = selectedLimit, page = 1
                                )
                                viewModel.applyFilters(updatedQueryState)
                            }
                        },
                        modifier = Modifier.wrapContentSize()
                    )
                }
            }
        }
    } else {
        Column(Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (paginationState != null) {
                    PaginationButtons(paginationState) { pageNumber ->
                        viewModel.applyFilters(animeSearchQueryState.copy(page = pageNumber))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            DropdownInputField(
                label = "Limit",
                options = Limit.limitOptions.map { it.toString() },
                selectedValue = selectedLimit.toString(),
                onValueChange = {
                    selectedLimit = it.toInt()
                    if (animeSearchQueryState.limit != selectedLimit) {
                        val updatedQueryState = animeSearchQueryState.copy(
                            limit = selectedLimit, page = 1
                        )
                        viewModel.applyFilters(updatedQueryState)
                    }
                },
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}