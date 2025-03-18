package com.example.animeapp.ui.animeSearch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.animeapp.R
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.ui.common_ui.SearchView
import com.example.animeapp.utils.Debounce

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFieldSection(viewModel: AnimeSearchViewModel) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(viewModel.queryState.value.query) }
    val debounce = remember {
        Debounce(scope, 1000L, { newQuery ->
            viewModel.applyFilters(viewModel.queryState.value.copy(query = newQuery, page = 1))
        }, viewModel, Debounce.StateType.ANIME_SEARCH)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        SearchView(
            query = query,
            onQueryChange = {
                query = it
                debounce.query(it)
            },
            placeholder = stringResource(id = R.string.search_anime),
            modifier = Modifier.fillMaxWidth()
        )
    }
}