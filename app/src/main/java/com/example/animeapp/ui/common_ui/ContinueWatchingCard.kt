package com.example.animeapp.ui.common_ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.utils.basicContainer

@Composable
fun ContinueWatchingCard(
    episode: EpisodeDetailComplement,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.basicContainer(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(episode.imageUrl)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Episode Image",
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Continue Watching",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}