package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.animeapp.R
import com.example.animeapp.models.Producer
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.ui.common_ui.DropdownInputField
import com.example.animeapp.ui.common_ui.RetryButton
import com.example.animeapp.ui.common_ui.SearchView
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.Limit
import com.example.animeapp.utils.Resource

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun ProducersBottomSheet(viewModel: AnimeSearchViewModel, onDismiss: () -> Unit) {
    val producers by viewModel.producers.collectAsState()
    val queryState by viewModel.queryState.collectAsState()
    val selectedProducers by viewModel.selectedProducerId.collectAsState()
    val producersQueryState by viewModel.producersQueryState.collectAsState()

    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(producersQueryState.query) }
    val debounce = remember {
        Debounce(scope, 1000L, { newQuery ->
            viewModel.applyProducerQueryStateFilters(
                producersQueryState.copy(
                    query = newQuery,
                    page = 1
                )
            )
        }, viewModel, Debounce.StateType.PRODUCER_SEARCH)
    }

    val context = LocalContext.current
    var selectedLimit by remember { mutableIntStateOf(producersQueryState.limit ?: 10) }
    val pagerState = rememberPagerState { 2 }
    val paginationState = producers.data?.pagination

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CancelButton(
                cancelAction = onDismiss,
                Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            ResetButton(
                context,
                { queryState.isProducersDefault() },
                {
                    viewModel.resetProducerSelection()
                    onDismiss()
                },
                Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            ApplyButton(
                context,
                { selectedProducers.isEmpty() },
                {
                    viewModel.applyProducerFilters()
                    onDismiss()
                },
                Modifier.weight(1f)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            SearchView(
                query = query,
                onQueryChange = {
                    query = it
                    debounce.query(it)
                },
                placeholder = stringResource(id = R.string.search_producer),
                modifier = Modifier.fillMaxWidth()
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            val producerList = producers.data?.data ?: emptyList()
            val selectedList =
                producerList.filter { it.mal_id in selectedProducers }
            if (selectedProducers.isNotEmpty()) {
                FilterChipFlow(
                    itemList = selectedList,
                    onSetSelectedId = { viewModel.setSelectedProducerId(it) },
                    itemName = { "${(it as Producer).titles?.get(0)?.title ?: "Unknown"} (${it.count})" },
                    getItemId = { (it as Producer).mal_id },
                    isHorizontal = true,
                    isChecked = true
                )
            } else {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    text = "No selected producers"
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            when (producers) {
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                is Resource.Success -> {
                    val unselectedList =
                        producerList.filter { it.mal_id !in selectedProducers }

                    FilterChipFlow(
                        itemList = unselectedList,
                        onSetSelectedId = { viewModel.setSelectedProducerId(it) },
                        itemName = { "${(it as Producer).titles?.get(0)?.title ?: "Unknown"} (${it.count})" },
                        getItemId = { (it as Producer).mal_id },
                    )
                }

                is Resource.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RetryButton(
                            message = producers.message ?: "Error loading producers",
                            onClick = { viewModel.fetchProducers() }
                        )
                    }
                }
            }
        }

        if (paginationState != null) HorizontalDivider()
        HorizontalPager(state = pagerState, Modifier.padding(8.dp)) { page ->
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
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
                                viewModel.applyProducerQueryStateFilters(
                                    producersQueryState.copy(
                                        page = pageNumber
                                    )
                                )
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
                            if (producersQueryState.limit != selectedLimit) {
                                val updatedQueryState = producersQueryState.copy(
                                    limit = selectedLimit, page = 1
                                )
                                viewModel.applyProducerQueryStateFilters(updatedQueryState)
                            }
                        },
                        modifier = Modifier.wrapContentSize()
                    )
                }
            }
        }
    }
}