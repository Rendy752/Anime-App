package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AutoplayNextEpisodeToggle(
    modifier: Modifier = Modifier,
    isAutoplayPlayNextEpisode: Boolean,
    onToggle: () -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (isAutoplayPlayNextEpisode) MaterialTheme.colorScheme.primary else Color.DarkGray,
        animationSpec = tween(300),
        label = "TrackColor"
    )
    val thumbAlignment by animateDpAsState(
        targetValue = if (isAutoplayPlayNextEpisode) 28.dp else 4.dp,
        animationSpec = tween(300),
        label = "ThumbAlignment"
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbAlignment)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            val icon =
                if (isAutoplayPlayNextEpisode) Icons.Default.PlayArrow else Icons.Default.Pause
            val iconColor =
                if (isAutoplayPlayNextEpisode) MaterialTheme.colorScheme.primary else Color.DarkGray

            Icon(
                imageVector = icon,
                contentDescription = if (isAutoplayPlayNextEpisode) "Autoplay is ON" else "Autoplay is OFF",
                modifier = Modifier.size(16.dp),
                tint = iconColor
            )
        }
    }
}