package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun PlaybackSpeedContent(onSpeedChange: (Float) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = "Playback Speed",
            style = MaterialTheme.typography.titleMedium
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(listOf(0.5f, 1.0f, 1.5f, 2.0f)) { speed ->
                Text(
                    text = "${speed}x",
                    modifier = Modifier
                        .basicContainer(
                            roundedCornerShape = RoundedCornerShape(0.dp),
                            outerPadding = PaddingValues(0.dp),
                            onItemClick = { onSpeedChange(speed) })
                        .fillMaxWidth()
                )
            }
        }
    }
}