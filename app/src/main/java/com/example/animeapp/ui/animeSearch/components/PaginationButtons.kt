package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.ui.common_ui.PaginationButton
import com.example.animeapp.ui.common_ui.PaginationDot

@Composable
fun PaginationButtons(pagination: CompletePagination, viewModel: AnimeSearchViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val currentPage = viewModel.producersQueryState.collectAsState().value.page
        val lastPage = pagination.last_visible_page
        val hasNextPage = pagination.has_next_page

        if (currentPage > 1) {
            PaginationButton(
                text = "<",
                pageNumber = currentPage - 1,
                currentPage = currentPage,
                onPaginationClick = {
                    viewModel.applyProducerQueryStateFilters(
                        viewModel.producersQueryState.value.copy(
                            page = it
                        )
                    )
                })
        }

        if (lastPage <= 4) {
            for (i in 1..lastPage) {
                PaginationButton(
                    text = "$i",
                    pageNumber = i,
                    currentPage = currentPage,
                    onPaginationClick = {
                        viewModel.applyProducerQueryStateFilters(
                            viewModel.producersQueryState.value.copy(
                                page = it
                            )
                        )
                    })
            }
        } else {
            PaginationButton(
                text = "1",
                pageNumber = 1,
                currentPage = currentPage,
                onPaginationClick = {
                    viewModel.applyProducerQueryStateFilters(
                        viewModel.producersQueryState.value.copy(
                            page = it
                        )
                    )
                })
            PaginationButton(
                text = "2",
                pageNumber = 2,
                currentPage = currentPage,
                onPaginationClick = {
                    viewModel.applyProducerQueryStateFilters(
                        viewModel.producersQueryState.value.copy(
                            page = it
                        )
                    )
                })

            if (currentPage > 3) {
                PaginationDot()
            }

            if (currentPage > 2 && currentPage < lastPage - 1) {
                PaginationButton(
                    text = "$currentPage",
                    pageNumber = currentPage,
                    currentPage = currentPage,
                    onPaginationClick = {
                        viewModel.applyProducerQueryStateFilters(
                            viewModel.producersQueryState.value.copy(
                                page = it
                            )
                        )
                    })
            }

            if (currentPage < lastPage - 2) {
                PaginationDot()
            }

            PaginationButton(
                text = "${lastPage - 1}",
                pageNumber = lastPage - 1,
                currentPage = currentPage,
                onPaginationClick = {
                    viewModel.applyProducerQueryStateFilters(
                        viewModel.producersQueryState.value.copy(
                            page = it
                        )
                    )
                })
            PaginationButton(
                text = "$lastPage",
                pageNumber = lastPage,
                currentPage = currentPage,
                onPaginationClick = {
                    viewModel.applyProducerQueryStateFilters(
                        viewModel.producersQueryState.value.copy(
                            page = it
                        )
                    )
                })
        }

        if (hasNextPage) {
            PaginationButton(
                text = ">",
                pageNumber = currentPage + 1,
                currentPage = currentPage,
                onPaginationClick = {
                    viewModel.applyProducerQueryStateFilters(
                        viewModel.producersQueryState.value.copy(
                            page = it
                        )
                    )
                })
        }
    }
}