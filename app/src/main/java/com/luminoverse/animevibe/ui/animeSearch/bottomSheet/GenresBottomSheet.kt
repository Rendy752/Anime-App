package com.luminoverse.animevibe.ui.animeSearch.bottomSheet

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.luminoverse.animevibe.models.AnimeSearchQueryState
import com.luminoverse.animevibe.models.Genre
import com.luminoverse.animevibe.models.GenresResponse
import com.luminoverse.animevibe.ui.animeSearch.components.ApplyButton
import com.luminoverse.animevibe.ui.animeSearch.components.CancelButton
import com.luminoverse.animevibe.ui.animeSearch.components.ResetButton
import com.luminoverse.animevibe.ui.animeSearch.searchField.FilterChipFlow
import com.luminoverse.animevibe.ui.animeSearch.searchField.FilterChipFlowSkeleton
import com.luminoverse.animevibe.ui.common.RetryButton
import com.luminoverse.animevibe.utils.resource.Resource

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GenresBottomSheet(
    queryState: AnimeSearchQueryState,
    fetchGenres: () -> Unit,
    genres: Resource<GenresResponse>,
    selectedGenres: List<Genre>,
    setSelectedGenre: (Genre) -> Unit,
    resetGenreSelection: () -> Unit,
    applyGenreFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CancelButton(
                cancelAction = onDismiss,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            ResetButton(
                isDefault = { queryState.isGenresDefault() },
                resetAction = {
                    resetGenreSelection()
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            ApplyButton(
                isEmptySelection = { selectedGenres.isEmpty() },
                applyAction = {
                    applyGenreFilters()
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (selectedGenres.isNotEmpty()) {
                FilterChipFlow(
                    itemList = selectedGenres,
                    onSetSelectedId = { setSelectedGenre(it as Genre) },
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
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (genres) {
                    is Resource.Loading -> {
                        FilterChipFlowSkeleton()
                    }

                    is Resource.Success -> {
                        val genreList = genres.data.data
                        FilterChipFlow(
                            itemList = genreList.filter { it !in selectedGenres },
                            onSetSelectedId = { setSelectedGenre(it as Genre) },
                            itemName = {
                                val name = (it as Genre).name
                                if (it.count > 0) "$name (${it.count})"
                                else name
                            },
                            getItemId = { it },
                        )
                    }

                    is Resource.Error -> {
                        RetryButton(
                            modifier = Modifier.padding(16.dp),
                            message = genres.message ?: "Error loading genres",
                            onClick = { fetchGenres() }
                        )
                    }
                }
            }
        }
    }
}