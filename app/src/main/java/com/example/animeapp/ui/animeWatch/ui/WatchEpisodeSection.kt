package com.example.animeapp.ui.animeWatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.ui.animeWatch.components.WatchEpisodeItem
import com.example.animeapp.utils.basicContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchEpisodeSection(
    animeDetail: AnimeDetail,
    episodeDetailComplement: EpisodeDetailComplement,
    episodes: List<Episode>,
    episodeSourcesQuery: EpisodeSourcesQuery?,
    handleSelectedEpisodeServer: (EpisodeSourcesQuery) -> Unit,
) {
    Column(
        modifier = Modifier
            .basicContainer()
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    val currentEpisodeNo = episodeDetailComplement.servers.episodeNo
                    val previousEpisode = episodes.find { it.episodeNo == currentEpisodeNo - 1 }
                    previousEpisode?.let {
                        handleSelectedEpisodeServer(
                            episodeSourcesQuery?.copy(id = it.episodeId)
                                ?: EpisodeSourcesQuery(
                                    id = it.episodeId,
                                    server = "vidsrc",
                                    category = "sub"
                                )
                        )
                    }
                },
                enabled = episodeDetailComplement.servers.episodeNo > 1
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Episode")
            }

            IconButton(
                onClick = {
                    val currentEpisodeNo = episodeDetailComplement.servers.episodeNo
                    val nextEpisode = episodes.find { it.episodeNo == currentEpisodeNo + 1 }
                    nextEpisode?.let {
                        handleSelectedEpisodeServer(
                            episodeSourcesQuery?.copy(id = it.episodeId)
                                ?: EpisodeSourcesQuery(
                                    id = it.episodeId,
                                    server = "vidsrc",
                                    category = "sub"
                                )
                        )
                    }
                },
                enabled = episodeDetailComplement.servers.episodeNo < animeDetail.episodes
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Episode")
            }
        }

        var episodeNumberInput by remember { mutableStateOf("") }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Episodes: ${animeDetail.episodes}")

            OutlinedTextField(
                value = episodeNumberInput,
                onValueChange = { episodeNumberInput = it },
                label = { Text("Jump to Episode") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            IconButton(onClick = {
                val episodeNo = episodeNumberInput.toIntOrNull()
                episodeNo?.let {
                    val episode = episodes.find { it.episodeNo == episodeNo }
                    episode?.let {
                        handleSelectedEpisodeServer(
                            episodeSourcesQuery?.copy(id = it.episodeId)
                                ?: EpisodeSourcesQuery(
                                    id = it.episodeId,
                                    server = "vidsrc",
                                    category = "sub"
                                )
                        )
                    }
                }
            }) {
                Text("Go")
            }
        }

        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .wrapContentHeight(),
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(episodes) { episode ->
                WatchEpisodeItem(
                    episodeDetailComplement = episodeDetailComplement,
                    episode = episode,
                    onEpisodeClick = { episodeId ->
                        handleSelectedEpisodeServer(
                            episodeSourcesQuery?.copy(id = episodeId)
                                ?: EpisodeSourcesQuery(
                                    id = episodeId,
                                    server = "vidsrc",
                                    category = "sub"
                                )
                        )
                    }
                )
            }
        }
    }
}