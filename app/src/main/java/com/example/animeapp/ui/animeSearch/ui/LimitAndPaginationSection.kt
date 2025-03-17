package com.example.animeapp.ui.animeSearch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import com.example.animeapp.utils.Limit
import com.example.animeapp.ui.animeSearch.components.PaginationButtons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitAndPaginationSection(viewModel: AnimeSearchViewModel) {
    val animeSearchQueryState by viewModel.queryState.collectAsState()
    val animeSearchData by viewModel.animeSearchResults.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var selectedLimit by remember { mutableIntStateOf(animeSearchQueryState.limit ?: 25) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val paginationState = animeSearchData.data?.pagination

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth() // Ensure it fills the available width
        ) {
            OutlinedTextField( // Use OutlinedTextField for better visual
                readOnly = true,
                value = selectedLimit.toString(),
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth(), // Ensure TextField fills the width of the box
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth() // Ensure dropdown fills the width
            ) {
                Limit.limitOptions.forEach { limitOption ->
                    DropdownMenuItem(
                        text = { Text(limitOption.toString()) },
                        onClick = {
                            selectedLimit = limitOption
                            expanded = false
                            if (animeSearchQueryState.limit != selectedLimit) {
                                val updatedQueryState = animeSearchQueryState.copy(
                                    limit = selectedLimit, page = 1
                                )
                                viewModel.applyFilters(updatedQueryState)
                            }
                        }
                    )
                }
            }
        }

        Row(modifier = Modifier.weight(0.5f), horizontalArrangement = Arrangement.End) {
            if (paginationState != null) {
                PaginationButtons(paginationState, viewModel)
            }
        }
    }
}