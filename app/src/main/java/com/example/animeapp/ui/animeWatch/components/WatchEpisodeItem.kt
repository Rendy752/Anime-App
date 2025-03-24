package com.example.animeapp.ui.animeWatch.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.utils.WatchUtils.getEpisodeBackgroundColor

@Composable
fun WatchEpisodeItem(
    episodeDetailComplement: EpisodeDetailComplement,
    episode: Episode,
    onEpisodeClick: (String) -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .background(getEpisodeBackgroundColor(episode.filler, episodeDetailComplement))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showTooltip = true },
                    onTap = { onEpisodeClick(episode.episodeId) }
                )
            },
        shape = RoundedCornerShape(8.dp),
        contentColor = Color.White
    ) {
        Text(
            text = episode.episodeNo.toString(),
            modifier = Modifier.padding(16.dp),
            color = if (episode.episodeNo == episodeDetailComplement.servers.episodeNo) Color.White else MaterialTheme.colorScheme.onSurface
        )

        if (showTooltip) {
            Popup(
                alignment = Alignment.TopCenter,
                onDismissRequest = { showTooltip = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = episode.name,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}