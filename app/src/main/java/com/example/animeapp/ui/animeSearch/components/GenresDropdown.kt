package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.animeapp.ui.animeSearch.viewmodel.AnimeSearchViewModel
import androidx.compose.ui.platform.LocalConfiguration
import com.example.animeapp.models.Genre
import com.example.animeapp.ui.common_ui.RetryButton
import com.example.animeapp.utils.Resource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenresDropdown(viewModel: AnimeSearchViewModel, onDismiss: () -> Unit) {
    val genres by viewModel.genres.collectAsState()
    val selectedGenres by viewModel.selectedGenreId.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Popup(
        alignment = Alignment.BottomStart,
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                if (genres !is Resource.Error && genres !is Resource.Loading) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                        ResetButton(
                            context,
                            { viewModel.queryState.value.isGenresDefault() },
                            {
                                viewModel.resetGenreSelection()
                                onDismiss()
                            },
                            Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(4.dp))
                        ApplyButton(
                            context,
                            { viewModel.selectedGenreId.value.isEmpty() },
                            {
                                viewModel.applyGenreFilters()
                                onDismiss()
                            },
                            Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .heightIn(max = screenHeight * 0.5f)
                ) {
                    when (genres) {
                        is Resource.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is Resource.Success -> {
                            val genreList = genres.data?.data ?: emptyList()
                            val selectedList = genreList.filter { it.mal_id in selectedGenres }
                            val unselectedList = genreList.filter { it.mal_id !in selectedGenres }

                            FilterChipFlow(
                                selectedList = selectedList,
                                unselectedList = unselectedList,
                                selectedIds = selectedGenres,
                                onSetSelectedId = { viewModel.setSelectedGenreId(it) },
                                itemName = { "${(it as Genre).name} (${it.count})" },
                                getItemId = { (it as Genre).mal_id },
                                noSelectedItemMessage = "No selected genres"
                            )
                        }

                        is Resource.Error -> {
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
}