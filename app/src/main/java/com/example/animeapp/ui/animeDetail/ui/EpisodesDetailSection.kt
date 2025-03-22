package com.example.animeapp.ui.animeDetail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.ui.animeDetail.components.EpisodeInfoRow
import com.example.animeapp.ui.animeDetail.components.EpisodeItem
import com.example.animeapp.ui.common_ui.ErrorMessage
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.basicContainer

@Composable
fun EpisodesDetailSection(
    animeDetailComplement: Resource<AnimeDetailComplement?>?,
    onEpisodeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .basicContainer()
            .fillMaxWidth()
    ) {
        Text(
            text = "Episodes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        when (animeDetailComplement) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is Resource.Success -> {
                if (animeDetailComplement.data != null) {
                    Row(modifier = Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        EpisodeInfoRow(
                            subCount = animeDetailComplement.data.sub,
                            dubCount = animeDetailComplement.data.dub,
                            epsCount = animeDetailComplement.data.eps,
                        )
                    }
                }
                if (animeDetailComplement.data?.episodes?.isNotEmpty() == true) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        val reversedEpisodes =
                            animeDetailComplement.data.episodes.reversed().take(12)

                        items(reversedEpisodes) { episode ->
                            EpisodeItem(episode = episode, onClick = onEpisodeClick)
                        }
                    }
                } else {
                    ErrorMessage(message = "No episodes found")
                }
            }

            is Resource.Error -> {
                ErrorMessage(
                    message = animeDetailComplement.message ?: "Error loading episodes"
                )
            }

            else -> {
                ErrorMessage(message = "Episode data not available")
            }
        }
    }
}