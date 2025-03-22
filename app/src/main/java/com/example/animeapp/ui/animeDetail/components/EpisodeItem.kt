package com.example.animeapp.ui.animeDetail.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animeapp.models.Episode
import com.example.animeapp.utils.basicContainer

@Composable
fun EpisodeItem(episode: Episode, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .basicContainer(onItemClick = { onClick(episode.episodeId) })
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ep. ${episode.episodeNo}",
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = episode.name,
            modifier = Modifier.weight(1f)
        )
    }
}