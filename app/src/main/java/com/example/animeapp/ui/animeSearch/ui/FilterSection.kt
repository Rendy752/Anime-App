package com.example.animeapp.ui.animeSearch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.animeapp.R
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.Limit
import com.example.animeapp.ui.common_ui.ChipView
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.animeapp.ui.animeSearch.components.FilterField
import com.example.animeapp.ui.animeSearch.components.GenresBottomSheet
import com.example.animeapp.ui.animeSearch.components.PaginationButtons
import com.example.animeapp.ui.animeSearch.components.ProducersBottomSheet
import com.example.animeapp.ui.theme.surfaceVariantLight

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSection(viewModel: AnimeSearchViewModel) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val debounce = remember {
        Debounce(scope, 1000L, { newQuery ->
            viewModel.applyFilters(viewModel.queryState.value.copy(query = newQuery, page = 1))
        }, viewModel, Debounce.StateType.ANIME_SEARCH)
    }

    var showGenresDialog by remember { mutableStateOf(false) }
    var showProducersDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                debounce.query(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(surfaceVariantLight),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            placeholder = { Text(stringResource(id = R.string.search_anime)) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = surfaceVariantLight,
                unfocusedContainerColor = surfaceVariantLight,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterField(
                label = stringResource(id = R.string.genres_field),
                icon = R.drawable.ic_chevron_down_blue_24dp,
                modifier = Modifier.weight(1f)
            ) {
                showGenresDialog = true
            }
            FilterField(
                label = stringResource(id = R.string.producers_field),
                icon = R.drawable.ic_chevron_down_blue_24dp,
                modifier = Modifier.weight(1f)
            ) {
                showProducersDialog = true
            }
        }

        // Display selected Genres
        FlowRow(modifier = Modifier.padding(top = 8.dp)) {
            viewModel.selectedGenreId.collectAsState().value.forEach { genreId ->
                val genre =
                    viewModel.genres.collectAsState().value.data?.data?.find { it.mal_id == genreId }
                genre?.let {
                    ChipView(text = "${it.name} (${it.count})", onClick = {
                        viewModel.setSelectedGenreId(it.mal_id)
                    })
                }
            }
        }

        // Display selected Producers
        FlowRow(modifier = Modifier.padding(top = 8.dp)) {
            viewModel.selectedProducerId.collectAsState().value.forEach { producerId ->
                val producer =
                    viewModel.producers.collectAsState().value.data?.data?.find { it.mal_id == producerId }
                producer?.let {
                    ChipView(
                        text = "${it.titles?.get(0)?.title ?: "Unknown"} (${it.count})",
                        onClick = {
                            viewModel.setSelectedProducerId(it.mal_id)
                        })
                }
            }
        }

        if (showGenresDialog) {
            GenresBottomSheet(viewModel, onDismiss = { showGenresDialog = false })
        }

        // Producers Dialog
        if (showProducersDialog) {
            ProducersBottomSheet(viewModel, onDismiss = { showProducersDialog = false })
        }

        var limitSpinner by remember {
            mutableIntStateOf(
                viewModel.producersQueryState.value.limit ?: 25
            )
        }
        AndroidView(
            modifier = Modifier.padding(top = 8.dp),
            factory = { context ->
                Spinner(context).apply {
                    val currentValue = limitSpinner
                    val limitAdapter = ArrayAdapter(
                        context,
                        android.R.layout.simple_spinner_dropdown_item,
                        Limit.limitOptions
                    )
                    adapter = limitAdapter
                    setSelection(Limit.limitOptions.indexOf(currentValue))
                    onItemSelectedListener =
                        object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: android.widget.AdapterView<*>?,
                                view: android.view.View?,
                                position: Int,
                                id: Long
                            ) {
                                val selectedLimit = Limit.getLimitValue(position)
                                if (viewModel.producersQueryState.value.limit != selectedLimit) {
                                    val updatedQueryState =
                                        viewModel.producersQueryState.value.copy(
                                            limit = selectedLimit, page = 1
                                        )
                                    limitSpinner = selectedLimit
                                    viewModel.applyProducerQueryStateFilters(updatedQueryState)
                                }
                            }

                            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                                viewModel.applyProducerQueryStateFilters(
                                    viewModel.producersQueryState.value.copy(
                                        limit = 25,
                                        page = 1
                                    )
                                )
                            }
                        }
                }
            }
        )

        val paginationState = viewModel.producers.collectAsState().value.data?.pagination
        if (paginationState != null) {
            PaginationButtons(paginationState, viewModel)
        }
    }
}