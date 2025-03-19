package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import androidx.compose.ui.text.style.TextAlign
import com.example.animeapp.models.Genre
import com.example.animeapp.ui.common_ui.RetryButton
import com.example.animeapp.utils.Resource

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GenresBottomSheet(viewModel: AnimeSearchViewModel, onDismiss: () -> Unit) {
    val genres by viewModel.genres.collectAsState()
    val queryState by viewModel.queryState.collectAsState()
    val selectedGenres by viewModel.selectedGenres.collectAsState()
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
                { queryState.isGenresDefault() },
                {
                    viewModel.resetGenreSelection()
                    onDismiss()
                },
                Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            ApplyButton(
                context,
                { selectedGenres.isEmpty() },
                {
                    viewModel.applyGenreFilters()
                    onDismiss()
                },
                Modifier.weight(1f)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (selectedGenres.isNotEmpty()) {
                FilterChipFlow(
                    itemList = selectedGenres,
                    onSetSelectedId = { viewModel.setSelectedGenre(it as Genre) },
                    itemName = {
                        val name = (it as Genre).name
                        if (it.count > 0) "$name (${it.count})"
                        else name
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
                    text = "No selected genres"
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            when (genres) {
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                is Resource.Success -> {
                    val genreList = genres.data?.data ?: emptyList()
                    FilterChipFlow(
                        itemList = genreList.filter { it !in selectedGenres },
                        onSetSelectedId = { viewModel.setSelectedGenre(it as Genre) },
                        itemName = {
                            val name = (it as Genre).name
                            if (it.count > 0) "$name (${it.count})"
                            else name
                        },
                        getItemId = { it },
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
                            message = genres.message ?: "Error loading genres",
                            onClick = { viewModel.fetchGenres() }
                        )
                    }
                }
            }
        }
    }
}