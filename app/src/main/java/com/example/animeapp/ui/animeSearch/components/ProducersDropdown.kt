package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.ui.common_ui.FilterChipView
import com.example.animeapp.ui.common_ui.RetryButton
import com.example.animeapp.utils.Resource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProducersDropdown(viewModel: AnimeSearchViewModel, onDismiss: () -> Unit) {
    val producers by viewModel.producers.collectAsState()
    val selectedProducers by viewModel.selectedProducerId.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val retryCount = remember { mutableIntStateOf(0) }

    Popup(
        alignment = Alignment.BottomStart,
        onDismissRequest = { onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                if (producers !is Resource.Error) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
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
                }
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .verticalScroll(scrollState)
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

                            selectedList.forEach { producer ->
                                FilterChipView(
                                    text = "${producer.titles?.get(0)?.title ?: "Unknown"} (${producer.count})",
                                    checked = true,
                                    onCheckedChange = { viewModel.setSelectedProducerId(producer.mal_id) }
                                )
                            }
                            unselectedList.forEach { producer ->
                                FilterChipView(
                                    text = "${producer.titles?.get(0)?.title ?: "Unknown"} (${producer.count})",
                                    checked = false,
                                    onCheckedChange = { viewModel.setSelectedProducerId(producer.mal_id) }
                                )
                            }
                        }

                        is Resource.Error -> {
                            RetryButton(
                                message = producers.message ?: "Error loading genres",
                                onClick = {
                                    viewModel.fetchProducers()
                                    retryCount.intValue++
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}