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
import com.example.animeapp.utils.WatchUtils.getEpisodeBackgroundColor

@Composable
fun ContinueWatchingPopup(
    isShowPopup: Boolean,
    episodeDetailComplement: EpisodeDetailComplement?,
    onMinimize: () -> Unit,
    onRestore: () -> Unit,
    isMinimized: Boolean
) {
    if (isShowPopup && episodeDetailComplement != null) {
        Popup(
            alignment = Alignment.BottomEnd,
            offset = IntOffset(0, (-160).dp.value.toInt()),
        ) {
            if (!isMinimized) {
                Column(
                    modifier = Modifier.basicContainer(
                        backgroundBrush = getEpisodeBackgroundColor(
                            episodeDetailComplement.isFiller,
                            episodeDetailComplement
                        ),
                        roundedCornerShape = RoundedCornerShape(
                            topStart = 16.dp,
                            bottomStart = 16.dp,
                            topEnd = 0.dp,
                            bottomEnd = 0.dp
                        ),
                        outerPadding = PaddingValues(0.dp),
                        innerPadding = PaddingValues(0.dp)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImageWithPlaceholder(
                            model = episodeDetailComplement.imageUrl,
                            contentDescription = episodeDetailComplement.animeTitle,
                            roundedCorners = ImageRoundedCorner.START,
                            modifier = Modifier
                                .width(50.dp)
                                .height(75.dp)
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(75.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = episodeDetailComplement.animeTitle,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Eps. ${episodeDetailComplement.number}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = episodeDetailComplement.episodeTitle,
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