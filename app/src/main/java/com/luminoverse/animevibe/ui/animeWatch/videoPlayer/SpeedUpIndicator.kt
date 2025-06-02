package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpeedUpIndicator(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    speedText: String
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier.padding(24.dp)
            .background(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp),
    ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Speed,
            contentDescription = "Speed Up",
            tint = Color.White,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = speedText,
            color = Color.White,
            fontSize = 16.sp
        )
    }
        }
}