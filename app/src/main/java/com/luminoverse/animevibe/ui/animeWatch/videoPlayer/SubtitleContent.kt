package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Track
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun SubtitleSettingsContent(
    tracks: List<Track>,
    onSubtitleSelected: (Track) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = "Select Subtitles",
            style = MaterialTheme.typography.titleMedium
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(tracks.filter { it.kind == "captions" }) { track ->
                Text(
                    text = track.label ?: "Unknown",
                    modifier = Modifier
                        .basicContainer(
                            roundedCornerShape = RoundedCornerShape(0.dp),
                            outerPadding = PaddingValues(0.dp),
                            onItemClick = { onSubtitleSelected(track) })
                        .fillMaxWidth()
                )
            }
        }
    }
}