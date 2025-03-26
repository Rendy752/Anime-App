package com.example.animeapp.ui.animeWatch.watchContent

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.common_ui.SkeletonBox
import com.example.animeapp.utils.WatchUtils.getEpisodeBackgroundColor
import com.example.animeapp.utils.basicContainer

@Composable
fun WatchEpisodeItem(
    episodeDetailComplement: EpisodeDetailComplement,
    episode: Episode,
    onEpisodeClick: (String) -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(16.dp)
            ),
    ) {
        val isCurrentEpisode = episodeDetailComplement.servers.episodeNo == episode.episodeNo
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .basicContainer(
                    backgroundBrush = getEpisodeBackgroundColor(
                        episode.filler,
                        episodeDetailComplement,
                        isCurrentEpisode,
                    ),
                    outerPadding = PaddingValues(0.dp),
                    innerPadding = PaddingValues(0.dp),
                )
                .then(
                    if (!isCurrentEpisode) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onEpisodeClick(episode.episodeId) },
                                onLongPress = { showTooltip = true },
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = episode.episodeNo.toString(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }

        if (showTooltip) {
            Popup(
                offset = IntOffset(0, -100),
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

@Preview
@Composable
fun WatchEpisodeItemSkeleton() {
    Surface(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(16.dp)
            ),
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .basicContainer(
                    backgroundBrush = getEpisodeBackgroundColor(false),
                    outerPadding = PaddingValues(0.dp),
                    innerPadding = PaddingValues(0.dp),
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            SkeletonBox(width = 20.dp, height = 20.dp)
        }
    }
}