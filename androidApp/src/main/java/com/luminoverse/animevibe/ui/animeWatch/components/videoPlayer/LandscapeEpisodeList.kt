package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.models.Episode
import com.luminoverse.animevibe.models.EpisodeDetailComplement
import com.luminoverse.animevibe.ui.common.ImageAspectRatio
import com.luminoverse.animevibe.ui.common.ImageDisplay
import com.luminoverse.animevibe.utils.resource.Resource

/**
 * A composable that displays a horizontal list of episodes, intended for use in the landscape player.
 *
 * @param modifier The modifier to be applied to the component.
 * @param episodesToShow The list of episodes to display, excluding the current episode.
 * @param episodeDetailComplements A map of episode IDs to their detailed complements for fetching thumbnails.
 * @param onEpisodeSelected A callback invoked when an episode is tapped.
 * @param onClose A callback invoked when the close button is tapped.
 */
@Composable
fun LandscapeEpisodeList(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    episodesToShow: List<Episode>,
    imagePlaceholder: String?,
    episodeDetailComplements: Map<String, Resource<EpisodeDetailComplement>>,
    onEpisodeSelected: (Episode) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { }
                )
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "More Episodes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close episode list",
                    tint = Color.White
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y > 2.5f) {
                            onClose()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (episodesToShow.isEmpty()) {
                Text(
                    text = "There are no more episodes available.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(episodesToShow, key = { it.id }) { episode ->
                        val complement = episodeDetailComplements[episode.id]?.data
                        Column(
                            modifier = Modifier.width(240.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ImageDisplay(
                                modifier = Modifier.fillMaxWidth(),
                                image = complement?.screenshot,
                                imagePlaceholder = imagePlaceholder,
                                ratio = ImageAspectRatio.WIDESCREEN.ratio,
                                contentDescription = "Episode ${episode.episode_no} thumbnail",
                                onClick = { _, _, _ -> onEpisodeSelected(episode); onClose() }
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEpisodeSelected(episode); onClose() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = episode.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Episode ${episode.episode_no}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}