package com.example.animeapp.ui.animeSearch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.utils.Limit
import com.example.animeapp.ui.animeSearch.components.PaginationButtons

@Composable
fun LimitAndPaginationSection(viewModel: AnimeSearchViewModel) {
    val producersQueryState by viewModel.producersQueryState.collectAsState()
    val producersData by viewModel.producers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                Spinner(context).apply {
                    val limitAdapter = ArrayAdapter(
                        context,
                        android.R.layout.simple_spinner_dropdown_item,
                        Limit.limitOptions
                    )
                    adapter = limitAdapter
                    setSelection(Limit.limitOptions.indexOf(producersQueryState.limit ?: 25))
                    onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                            val selectedLimit = Limit.getLimitValue(position)
                            if (producersQueryState.limit != selectedLimit) {
                                val updatedQueryState = producersQueryState.copy(
                                    limit = selectedLimit, page = 1
                                )
                                viewModel.applyProducerQueryStateFilters(updatedQueryState)
                            }
                        }

                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                            viewModel.applyProducerQueryStateFilters(
                                producersQueryState.copy(
                                    limit = 25,
                                    page = 1
                                )
                            )
                        }
                    }
                }
            }
        )

        val paginationState = producersData.data?.pagination
        if (paginationState != null) {
            PaginationButtons(paginationState, viewModel)
        }
    }
}