package com.example.animeapp.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.example.animeapp.utils.basicContainer

@Composable
fun NextEpisodeOverlay(
    nextEpisodeName: String,
    onRestart: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier.basicContainer(isPrimary = true, innerPadding = PaddingValues(8.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = nextEpisodeName,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
            Row {
                Column(
                    modifier = Modifier.basicContainer(
                        isTertiary = true,
                        onItemClick = onRestart,
                        innerPadding = PaddingValues(8.dp)
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = "Restart",
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
                Column(
                    modifier = Modifier.basicContainer(
                        isPrimary = true,
                        onItemClick = onSkipNext,
                        innerPadding = PaddingValues(8.dp)
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Skip Next",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}