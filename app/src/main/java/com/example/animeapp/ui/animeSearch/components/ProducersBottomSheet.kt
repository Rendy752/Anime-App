package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.animeapp.R
import com.example.animeapp.models.Producer
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.ui.common_ui.RetryButton
import com.example.animeapp.ui.common_ui.SearchView
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.Resource

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProducersBottomSheet(viewModel: AnimeSearchViewModel, onDismiss: () -> Unit) {
    val producers by viewModel.producers.collectAsState()
    val selectedProducers by viewModel.selectedProducerId.collectAsState()

    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(viewModel.producersQueryState.value.query) }
    val debounce = remember {
        Debounce(scope, 1000L, { newQuery ->
            viewModel.applyProducerQueryStateFilters(
                viewModel.producersQueryState.value.copy(
                    query = newQuery,
                    page = 1
                )
            )
        }, viewModel, Debounce.StateType.PRODUCER_SEARCH)
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    if (producers !is Resource.Error && producers !is Resource.Loading) {
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
                { viewModel.queryState.value.isProducersDefault() },
                {
                    viewModel.resetProducerSelection()
                    onDismiss()
                },
                Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            ApplyButton(
                context,
                { viewModel.selectedProducerId.value.isEmpty() },
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
    }
    Column(
        modifier = Modifier.heightIn(max = screenHeight * 0.5f)
    ) {
        when (producers) {
            is Resource.Loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            is Resource.Success -> {
                val producerList = producers.data?.data ?: emptyList()
                val selectedList =
                    producerList.filter { it.mal_id in selectedProducers }
                val unselectedList =
                    producerList.filter { it.mal_id !in selectedProducers }

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
                FilterChipFlow(
                    itemList = unselectedList,
                    onSetSelectedId = { viewModel.setSelectedProducerId(it) },
                    itemName = { "${(it as Producer).titles?.get(0)?.title ?: "Unknown"} (${it.count})" },
                    getItemId = { (it as Producer).mal_id },
                )
            }

            is Resource.Error -> {
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    RetryButton(
                        message = producers.message ?: "Error loading producers",
                        onClick = { viewModel.fetchProducers() }
                    )
                }
            }
        }
    }
}