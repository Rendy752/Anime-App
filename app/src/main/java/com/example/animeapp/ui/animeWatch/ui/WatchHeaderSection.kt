package com.example.animeapp.ui.animeWatch.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.Server
import com.example.animeapp.ui.animeWatch.components.ServerSegmentedButton
import com.example.animeapp.utils.EpisodeUtils
import com.example.animeapp.utils.basicContainer

@Composable
fun WatchHeaderSection(
    title: String,
    episode: Episode,
    episodeDetailComplement: EpisodeDetailComplement,
    onServerSelected: (Server) -> Unit,
) {
    Column(
        modifier = Modifier
            .basicContainer(
                backgroundBrush = EpisodeUtils.getEpisodeBackgroundColor(
                    episode.filler,
                    episodeDetailComplement
                )
            )
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
//        when (episodeDetailComplement) {
//            is Resource.Loading -> {
//                Row(
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    modifier = Modifier
//                        .padding(4.dp)
//                        .clip(RoundedCornerShape(8.dp))
//                        .fillMaxWidth()
//                ) {
//                    Column {
//                        SkeletonBox(width = 150.dp, height = 20.dp)
//                        SkeletonBox(width = 100.dp, height = 16.dp)
//                    }
//                    SkeletonBox(width = 200.dp, height = 40.dp)
//                }
//            }
//
//            is Resource.Success -> {
        episodeDetailComplement.servers.let { servers ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (episode.name != "Full") episode.name else title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Eps. ${servers.episodeNo}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (servers.sub.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ServerSegmentedButton(
                                servers.sub,
                                onServerSelected = onServerSelected
                            )
                        }
                    }
                    if (servers.dub.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ServerSegmentedButton(
                                servers.dub,
                                onServerSelected = onServerSelected
                            )
                        }
                    }
                    if (servers.raw.isNotEmpty()) {
                        ServerSegmentedButton(
                            servers.raw,
                            onServerSelected = onServerSelected
                        )
                    }
                }
            }
        }
    }

//            is Resource.Error -> {}
//        }
//    }
}