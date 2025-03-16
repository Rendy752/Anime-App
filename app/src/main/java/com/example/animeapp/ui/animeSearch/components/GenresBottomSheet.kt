package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.animeapp.R
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel

@Composable
fun GenresBottomSheet(viewModel: AnimeSearchViewModel, onDismiss: () -> Unit) {
    val genres by viewModel.genres.collectAsState()
    val selectedGenres by viewModel.selectedGenreId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Genres") },
            text = {
                when (genres) {
                    is com.example.animeapp.utils.Resource.Loading -> {
                        CircularProgressIndicator()
                    }
                    is com.example.animeapp.utils.Resource.Success -> {
                        val genreList = genres.data?.data ?: emptyList()
                        LazyColumn {
                            items(genreList) { genre ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setSelectedGenreId(genre.mal_id)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedGenres.contains(genre.mal_id),
                                        onCheckedChange = { viewModel.setSelectedGenreId(genre.mal_id) }
                                    )
                                    Text("${genre.name} (${genre.count})")
                                }
                            }
                        }
                    }
                    is com.example.animeapp.utils.Resource.Error -> {
                        Text(genres.message ?: "Error loading genres")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.applyGenreFilters()
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
        Icon(painterResource(id = R.drawable.ic_chevron_down_blue_24dp), contentDescription = "Select Genres")
    }
}