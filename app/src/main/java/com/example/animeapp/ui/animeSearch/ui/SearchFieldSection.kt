package com.example.animeapp.ui.animeSearch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.animeapp.R
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.ui.common_ui.SearchView
import com.example.animeapp.utils.Debounce

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFieldSection(
    viewModel: AnimeSearchViewModel,
    showFilterIcon: Boolean = false,
    isFilterBottomSheetShow: Boolean = false,
    onFilterClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(viewModel.queryState.value.query) }
    val debounce = remember {
        Debounce(scope, 1000L, { newQuery ->
            viewModel.applyFilters(viewModel.queryState.value.copy(query = newQuery, page = 1))
        }, viewModel, Debounce.StateType.ANIME_SEARCH)
    }

    var searchViewHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SearchView(
                query = query,
                onQueryChange = {
                    query = it
                    debounce.query(it)
                },
                placeholder = stringResource(id = R.string.search_anime),
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coordinates ->
                        searchViewHeight = with(density) { coordinates.size.height.toDp() }
                    }
            )
            if (showFilterIcon) {
                IconButton(
                    onClick = { onFilterClick?.invoke() },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFilterBottomSheetShow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .size(searchViewHeight)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        contentDescription = stringResource(id = R.string.filter)
                    )
                }
            }
        }
    }
}