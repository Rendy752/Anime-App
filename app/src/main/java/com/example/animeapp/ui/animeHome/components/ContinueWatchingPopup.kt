package com.example.animeapp.ui.animeHome.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.common_ui.AsyncImageWithPlaceholder
import com.example.animeapp.utils.basicContainer

@Composable
fun ContinueWatchingCard(
    isShowPopup: Boolean,
    episode: EpisodeDetailComplement,
    onDismiss: () -> Unit
) {
    if (isShowPopup) {
        Popup(
            alignment = Alignment.BottomEnd,
            offset = IntOffset((-20).dp.value.toInt(), (-20).dp.value.toInt()),
            onDismissRequest = onDismiss
        ) {
            Box {
                Column {
                    Row(
                        modifier = Modifier.basicContainer(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImageWithPlaceholder(
                            model = episode.imageUrl,
                            contentDescription = episode.animeTitle,
                            modifier = Modifier
                                .weight(0.3f)
                                .aspectRatio(2f / 3f)
                        )
                        Column(
                            modifier = Modifier
                                .weight(0.7f)
                                .padding(start = 8.dp)
                        ) {
                            Text(
                                text = episode.animeTitle,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Eps. ${episode.number}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = episode.episodeTitle,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = "Continue Watching",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clickable { onDismiss() }
                )
            }
        }
    }
}