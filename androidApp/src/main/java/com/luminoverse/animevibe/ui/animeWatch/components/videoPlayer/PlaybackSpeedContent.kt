package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.utils.basicContainer

@Composable
fun PlaybackSpeedContent(
    selectedPlaybackSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(listOf(0.5f, 1.0f, 1.5f, 2.0f)) { speed ->
            val isSelected = speed == selectedPlaybackSpeed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicContainer(
                        roundedCornerShape = RoundedCornerShape(0.dp),
                        outerPadding = PaddingValues(0.dp),
                        innerPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        onItemClick = { onSpeedChange(speed) }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${speed}x",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}