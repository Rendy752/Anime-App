package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.animeapp.R
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.utils.Debounce

@Composable
fun ProducersBottomSheet(viewModel: AnimeSearchViewModel, onDismiss: () -> Unit) {
    val producers by viewModel.producers.collectAsState()
    val selectedProducers by viewModel.selectedProducerId.collectAsState()
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var producerQuery by remember { mutableStateOf("") }
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Producers") },
            text = {
                Column {
                    TextField(
                        value = producerQuery,
                        onValueChange = {
                            producerQuery = it
                            debounce.query(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search Producers") },
                        singleLine = true
                    )
                    when (producers) {
                        is com.example.animeapp.utils.Resource.Loading -> {
                            CircularProgressIndicator()
                        }

                        is com.example.animeapp.utils.Resource.Success -> {
                            val producerList = producers.data?.data ?: emptyList()
                            LazyColumn {
                                items(producerList) { producer ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.setSelectedProducerId(producer.mal_id)
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = selectedProducers.contains(producer.mal_id),
                                            onCheckedChange = {
                                                viewModel.setSelectedProducerId(
                                                    producer.mal_id
                                                )
                                            }
                                        )
                                        Text("${producer.titles?.get(0)?.title ?: "Unknown"} (${producer.count})")
                                    }
                                }
                            }
                        }

                        is com.example.animeapp.utils.Resource.Error -> {
                            Text(producers.message ?: "Error loading producers")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.applyProducerFilters()
                    onDismiss()
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    IconButton(onClick = { showDialog = true }) {
        Icon(
            painterResource(id = R.drawable.ic_chevron_down_blue_24dp),
            contentDescription = "Select Producers"
        )
    }
}