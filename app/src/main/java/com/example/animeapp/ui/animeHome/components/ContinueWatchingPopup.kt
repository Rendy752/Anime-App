package com.example.animeapp.ui.animeHome.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavController
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.ui.common_ui.AsyncImageWithPlaceholder
import com.example.animeapp.ui.common_ui.ImageRoundedCorner
import com.example.animeapp.utils.Navigation.navigateToAnimeDetail
import com.example.animeapp.utils.Navigation.navigateToAnimeWatch
import com.example.animeapp.utils.basicContainer
import com.example.animeapp.utils.WatchUtils.getEpisodeBackgroundColor

@Composable
fun ContinueWatchingPopup(
    navController: NavController,
    episodeDetailComplement: EpisodeDetailComplement?,
    isMinimized: Boolean,
    onSetMinimize: (Boolean) -> Unit
) {
    if (episodeDetailComplement != null) {
        Popup(
            alignment = Alignment.BottomEnd,
            offset = IntOffset(0, (-200).dp.value.toInt()),
        ) {
            Row(
                modifier = Modifier
                    .height(75.dp)
                    .basicContainer(
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
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isMinimized) {
                    AsyncImageWithPlaceholder(
                        model = episodeDetailComplement.imageUrl
                            ?: "default_anime_placeholder_${episodeDetailComplement.animeTitle}",
                        contentDescription = episodeDetailComplement.animeTitle,
                        roundedCorners = ImageRoundedCorner.START,
                        modifier = Modifier
                            .width(50.dp)
                            .height(75.dp)
                    )
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .height(75.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.width(250.dp)) {
                            Text(
                                modifier = Modifier.clickable {
                                    navController.navigateToAnimeDetail(
                                        id = episodeDetailComplement.malId,
                                    )
                                },
                                text = episodeDetailComplement.animeTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Eps. ${episodeDetailComplement.number},",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = episodeDetailComplement.episodeTitle,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.basicContainer(
                                isPrimary = true,
                                innerPadding = PaddingValues(4.dp),
                                outerPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                                onItemClick = {
                                    navController.navigateToAnimeWatch(
                                        malId = episodeDetailComplement.malId,
                                        episodeId = episodeDetailComplement.id,
                                    )
                                }
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Continue Watching",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimary)
                            )
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play ${episodeDetailComplement.animeTitle} Episode ${episodeDetailComplement.number}",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = "Minimize popup for ${episodeDetailComplement.animeTitle}",
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable { onSetMinimize(true) }
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = "Restore popup for ${episodeDetailComplement.animeTitle}",
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable { onSetMinimize(false) },
                    )
                }
            }
        }
    }
}