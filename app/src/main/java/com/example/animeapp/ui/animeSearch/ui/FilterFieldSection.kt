package com.example.animeapp.ui.animeSearch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.animeapp.R
import com.example.animeapp.ui.animeSearch.components.FilterField
import com.example.animeapp.ui.animeSearch.components.GenresDropdown
import com.example.animeapp.ui.animeSearch.components.ProducersDropdown
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterFieldSection(viewModel: AnimeSearchViewModel) {
    var showGenresDropdown by remember { mutableStateOf(false) }
    var showProducersDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterField(
            label = stringResource(id = R.string.genres_field),
            icon = R.drawable.ic_chevron_down_blue_24dp,
            modifier = Modifier.weight(1f),
            onClick = {
                showGenresDropdown = !showGenresDropdown
                showProducersDropdown = false
            }
        )
        FilterField(
            label = stringResource(id = R.string.producers_field),
            icon = R.drawable.ic_chevron_down_blue_24dp,
            modifier = Modifier.weight(1f),
            onClick = {
                showProducersDropdown = !showProducersDropdown
                showGenresDropdown = false
            }
        )
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
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}