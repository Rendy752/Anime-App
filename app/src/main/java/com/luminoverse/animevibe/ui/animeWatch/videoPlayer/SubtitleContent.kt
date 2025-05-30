package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Track

@Composable
fun SubtitleSettingsContent(
    tracks: List<Track>,
    onSubtitleSelected: (Track) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Select Subtitles",
            style = MaterialTheme.typography.titleMedium
        )
        tracks.filter { it.kind == "captions" }.forEach { track ->
            Text(
                text = track.label ?: "Unknown",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSubtitleSelected(track) }
                    .padding(8.dp)
            )
        }
    }
}