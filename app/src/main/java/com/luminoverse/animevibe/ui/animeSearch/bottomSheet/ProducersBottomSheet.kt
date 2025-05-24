package com.luminoverse.animevibe.ui.animeSearch.bottomSheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.AnimeSearchQueryState
import com.luminoverse.animevibe.models.Producer
import com.luminoverse.animevibe.models.ProducersResponse
import com.luminoverse.animevibe.models.ProducersSearchQueryState
import com.luminoverse.animevibe.ui.animeSearch.components.ApplyButton
import com.luminoverse.animevibe.ui.animeSearch.components.CancelButton
import com.luminoverse.animevibe.ui.animeSearch.components.ResetButton
import com.luminoverse.animevibe.ui.animeSearch.searchField.FilterChipFlow
import com.luminoverse.animevibe.ui.animeSearch.searchField.FilterChipFlowSkeleton
import com.luminoverse.animevibe.ui.common.LimitAndPaginationQueryState
import com.luminoverse.animevibe.ui.common.LimitAndPaginationSection
import com.luminoverse.animevibe.ui.common.RetryButton
import com.luminoverse.animevibe.ui.common.SearchView
import com.luminoverse.animevibe.utils.Debounce
import com.luminoverse.animevibe.utils.resource.Resource

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun ProducersBottomSheet(
    queryState: AnimeSearchQueryState,
    producers: Resource<ProducersResponse>,
    fetchProducers: () -> Unit,
    selectedProducers: List<Producer>,
    producersQueryState: ProducersSearchQueryState,
    applyProducerQueryStateFilters: (ProducersSearchQueryState) -> Unit,
    setSelectedProducer: (Producer) -> Unit,
    applyProducerFilters: () -> Unit,
    resetProducerSelection: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(producersQueryState.query) }
    val debounce = remember {
        Debounce(scope, 1000L) { newQuery ->
            applyProducerQueryStateFilters(
                producersQueryState.copy(
                    query = newQuery,
                    page = 1
                )
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CancelButton(
                cancelAction = onDismiss,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            ResetButton(
                isDefault = { queryState.isProducersDefault() },
                resetAction = {
                    resetProducerSelection()
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            ApplyButton(
                isEmptySelection = { selectedProducers.isEmpty() },
                applyAction = {
                    applyProducerFilters()
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
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
                placeholder = "Search Producer",
                modifier = Modifier.fillMaxWidth()
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (selectedProducers.isNotEmpty()) {
                FilterChipFlow(
                    itemList = selectedProducers,
                    onSetSelectedId = { setSelectedProducer(it as Producer) },
                    itemName = {
                        val title = (it as Producer).titles?.get(0)?.title ?: "Unknown"
                        if (it.count > 0) "$title (${it.count})"
                        else title
                    },
                    getItemId = { it },
                    isHorizontal = true,
                    isChecked = true
                )
            } else {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    text = "No selected producers"
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (producers) {
                    is Resource.Loading -> {
                        FilterChipFlowSkeleton()
                    }

                    is Resource.Success -> {
                        val producerList = producers.data.data
                        FilterChipFlow(
                            itemList = producerList.filter { it !in selectedProducers },
                            onSetSelectedId = { setSelectedProducer(it as Producer) },
                            itemName = {
                                val title = (it as Producer).titles?.get(0)?.title ?: "Unknown"
                                if (it.count > 0) "$title (${it.count})"
                                else title
                            },
                            getItemId = { it },
                        )
                    }

                    is Resource.Error -> {
                        RetryButton(
                            modifier = Modifier.padding(16.dp),
                            message = producers.message ?: "Error loading producers",
                            onClick = { fetchProducers() }
                        )
                    }
                }
            }
        }

        LimitAndPaginationSection(
            isVisible = producers is Resource.Success,
            pagination = producers.data?.pagination,
            query = LimitAndPaginationQueryState(
                producersQueryState.page,
                producersQueryState.limit
            ),
            onQueryChanged = {
                applyProducerQueryStateFilters(
                    producersQueryState.copy(
                        page = it.page,
                        limit = it.limit
                    )
                )
            }
        )
    }
}