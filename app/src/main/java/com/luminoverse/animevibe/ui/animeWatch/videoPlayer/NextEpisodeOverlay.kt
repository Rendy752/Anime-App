package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun NextEpisodeOverlay(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    nextEpisodeName: String,
    onRestart: () -> Unit,
    onSkipNext: () -> Unit
) {
    AnimatedVisibility(
        modifier = modifier.basicContainer(isPrimary = true, innerPadding = PaddingValues(8.dp)),
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box {
            Column(
                modifier = Modifier
                    .widthIn(min = 192.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = nextEpisodeName,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        modifier = Modifier.basicContainer(
                            isTertiary = true,
                            onItemClick = onRestart,
                            outerPadding = PaddingValues(0.dp),
                            innerPadding = PaddingValues(8.dp)
                        ),
                        contentDescription = "Restart",
                        tint = MaterialTheme.colorScheme.onTertiary
                    )

                    Icon(
                        Icons.Filled.SkipNext,
                        modifier = Modifier.basicContainer(
                            isPrimary = true,
                            onItemClick = onSkipNext,
                            outerPadding = PaddingValues(0.dp),
                            innerPadding = PaddingValues(8.dp)
                        ),
                        contentDescription = "Skip Next",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}