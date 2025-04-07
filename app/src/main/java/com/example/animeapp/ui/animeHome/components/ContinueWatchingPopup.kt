package com.example.animeapp.ui.animeHome.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.common_ui.AsyncImageWithPlaceholder
import com.example.animeapp.ui.common_ui.ImageRoundedCorner
import com.example.animeapp.utils.basicContainer

@Composable
fun ContinueWatchingPopup(
    isShowPopup: Boolean,
    episode: EpisodeDetailComplement?,
    onMinimize: () -> Unit,
    onRestore: () -> Unit,
    isMinimized: Boolean
) {
    if (isShowPopup && episode != null) {
        Popup(
            alignment = Alignment.BottomEnd,
            offset = IntOffset((-20).dp.value.toInt(), (-20).dp.value.toInt()),
        ) {
            if (!isMinimized) {
                Column(
                    modifier = Modifier.basicContainer(innerPadding = PaddingValues(0.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImageWithPlaceholder(
                            model = episode.imageUrl,
                            contentDescription = episode.animeTitle,
                            roundedCorners = ImageRoundedCorner.START,
                            modifier = Modifier
                                .width(50.dp)
                                .height(75.dp)
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(75.dp), // Set height to match image height
                            verticalArrangement = Arrangement.SpaceBetween // Justify between elements
                        ) {
                            Column {
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
                            }
                            Text(
                                text = "Continue Watching",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable { onMinimize() }
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                        .clickable { onRestore() }
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Restore",
                        modifier = Modifier
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}