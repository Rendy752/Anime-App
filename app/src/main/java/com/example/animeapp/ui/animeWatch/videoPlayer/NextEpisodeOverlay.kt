package com.example.animeapp.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.animeapp.utils.basicContainer

@Composable
fun NextEpisodeOverlay(
    nextEpisodeName: String,
    onRestart: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.basicContainer(isPrimary = true),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = nextEpisodeName, color = MaterialTheme.colorScheme.onPrimary)
        Row {
            IconButton(onClick = onRestart) {
                Icon(
                    Icons.Filled.RestartAlt,
                    contentDescription = "Restart",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(onClick = onSkipNext) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Skip Next",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}