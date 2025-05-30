package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsContent(
    onSpeedChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Playback Speed",
            style = MaterialTheme.typography.titleMedium
        )
        listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
            Text(
                text = "${speed}x",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSpeedChange(speed) }
                    .padding(8.dp)
            )
        }
    }
}