package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.font.FontWeight
import com.luminoverse.animevibe.models.Track
import com.luminoverse.animevibe.models.trackPlaceholder
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun SubtitleContent(
    tracks: List<Track>,
    selectedSubtitle: Track?,
    onSubtitleSelected: (Track?) -> Unit
) {
    val captionTracks = tracks.filter { it.kind == "captions" }
    val allTracks = if (selectedSubtitle != null && selectedSubtitle != trackPlaceholder) {
        listOf(trackPlaceholder, selectedSubtitle) + captionTracks.filter { it != selectedSubtitle }
    } else {
        listOf(trackPlaceholder) + captionTracks
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Text(
                text = "Select Subtitle",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        }
        items(allTracks) { track ->
            val isSelected = track == selectedSubtitle || (track.label == "None" && selectedSubtitle == null)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicContainer(
                        roundedCornerShape = RoundedCornerShape(0.dp),
                        outerPadding = PaddingValues(0.dp),
                        innerPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        isPrimary = isSelected,
                        onItemClick = { onSubtitleSelected(track) }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = track.label ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}