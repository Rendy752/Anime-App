package com.example.animeapp.ui.animeSearch.bottomSheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.animeapp.R
import com.example.animeapp.models.AnimeSearchQueryState
import com.example.animeapp.models.Producer
import com.example.animeapp.models.ProducersResponse
import com.example.animeapp.models.ProducersSearchQueryState
import com.example.animeapp.ui.animeSearch.components.ApplyButton
import com.example.animeapp.ui.animeSearch.components.CancelButton
import com.example.animeapp.ui.animeSearch.components.ResetButton
import com.example.animeapp.ui.animeSearch.genreProducerFilterField.FilterChipFlow
import com.example.animeapp.ui.animeSearch.genreProducerFilterField.FilterChipFlowSkeleton
import com.example.animeapp.ui.common_ui.LimitAndPaginationQueryState
import com.example.animeapp.ui.common_ui.LimitAndPaginationSection
import com.example.animeapp.ui.common_ui.RetryButton
import com.example.animeapp.ui.common_ui.SearchView
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.Resource

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

    val context = LocalContext.current

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
                    resetProducerSelection()
                    onDismiss()
                },
                Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            ApplyButton(
                context,
                { selectedProducers.isEmpty() },
                {
                    applyProducerFilters()
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
                            message = producers.message ?: "Error loading producers",
                            onClick = { fetchProducers() },
                            modifier = Modifier.padding(16.dp)
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