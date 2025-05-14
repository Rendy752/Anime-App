package com.example.animeapp.ui.animeSearch.searchField

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeSearchQueryState
import com.example.animeapp.ui.common_ui.SearchView
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.basicContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFieldSection(
    queryState: AnimeSearchQueryState,
    onQueryChanged: (AnimeSearchQueryState) -> Unit,
    isFilterBottomSheetShow: Boolean,
    resetBottomSheetFilters: (() -> Unit),
    onFilterClick: (() -> Unit)
) {
    val scope = rememberCoroutineScope()
    var query by remember(queryState) { mutableStateOf(queryState.query) }
    val debounce = remember(queryState) {
        Debounce(scope, 1000L) { newQuery ->
            onQueryChanged(queryState.copy(query = newQuery, page = 1))
        }
    }

    var searchViewHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp),
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
                placeholder = "Search Anime",
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coordinates ->
                        searchViewHeight = with(density) { coordinates.size.height.toDp() }
                    }
            )
            if (!queryState.isDefault()) Text(
                modifier = Modifier
                    .width(110.dp)
                    .basicContainer(
                        isPrimary = true,
                        outerPadding = PaddingValues(0.dp),
                        innerPadding = PaddingValues(4.dp),
                        onItemClick = {
                            resetBottomSheetFilters()
                        }
                    ),
                text = "Filter Applied",
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimary
            )
            IconButton(
                onClick = { onFilterClick.invoke() },
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
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentDescription = "Open Filter"
                )
            }
        }
    }
}