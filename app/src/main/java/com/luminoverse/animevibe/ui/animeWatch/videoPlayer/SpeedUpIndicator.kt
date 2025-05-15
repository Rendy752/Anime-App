package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun SpeedUpIndicator(
    modifier: Modifier = Modifier,
    speedText: String = "2x speed"
) {
    Row(
        modifier = modifier
            .padding(24.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Speed,
            contentDescription = "Speed Up",
            tint = Color.Black,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = speedText,
            color = Color.Black,
            fontSize = 16.sp
        )
    }
}