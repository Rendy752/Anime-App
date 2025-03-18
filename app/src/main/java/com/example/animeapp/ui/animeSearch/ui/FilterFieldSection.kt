package com.example.animeapp.ui.animeSearch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.animeapp.R
import com.example.animeapp.models.Genre
import com.example.animeapp.models.Producer
import com.example.animeapp.ui.animeSearch.components.FilterChipFlow
import com.example.animeapp.ui.animeSearch.components.GenresDropdown
import com.example.animeapp.ui.animeSearch.components.ProducersDropdown
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.utils.Resource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterFieldSection(viewModel: AnimeSearchViewModel) {
    val selectedGenresIds by viewModel.selectedGenreId.collectAsState()
    val selectedProducersIds by viewModel.selectedProducerId.collectAsState()
    val genresResource by viewModel.genres.collectAsState()
    val producersResource by viewModel.producers.collectAsState()
    var showGenresDropdown by remember { mutableStateOf(false) }
    var showProducersDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable {
                    showGenresDropdown = !showGenresDropdown
                    showProducersDropdown = false
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selectedGenres = if (genresResource is Resource.Success) {
                genresResource.data?.data?.filter { it.mal_id in selectedGenresIds } ?: emptyList()
            } else {
                emptyList()
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedGenres.isNotEmpty()) {
                    FilterChipFlow(
                        itemList = selectedGenres,
                        onSetSelectedId = {
                            viewModel.setSelectedGenreId(it)
                            viewModel.applyGenreFilters()
                        },
                        itemName = { "${(it as Genre).name} (${it.count})" },
                        getItemId = { (it as Genre).mal_id },
                        isHorizontal = true,
                        isChecked = true
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.genres_field),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.chevron_down),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable {
                    showProducersDropdown = !showProducersDropdown
                    showGenresDropdown = false
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selectedProducers = if (producersResource is Resource.Success) {
                producersResource.data?.data?.filter { it.mal_id in selectedProducersIds }
                    ?: emptyList()
            } else {
                emptyList()
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedProducers.isNotEmpty()) {
                    FilterChipFlow(
                        itemList = selectedProducers,
                        onSetSelectedId = {
                            viewModel.setSelectedProducerId(it)
                            viewModel.applyProducerFilters()
                        },
                        itemName = { "${(it as Producer).titles?.get(0)?.title ?: "Unknown"} (${it.count})" },
                        getItemId = { (it as Producer).mal_id },
                        isHorizontal = true,
                        isChecked = true
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.producers_field),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.chevron_down),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    if (showGenresDropdown) {
        DropdownContainer {
            GenresDropdown(viewModel, onDismiss = { showGenresDropdown = false })
        }
    }

    if (showProducersDropdown) {
        DropdownContainer {
            ProducersDropdown(viewModel, onDismiss = { showProducersDropdown = false })
        }
    }
}

@Composable
fun DropdownContainer(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        content()
    }
}