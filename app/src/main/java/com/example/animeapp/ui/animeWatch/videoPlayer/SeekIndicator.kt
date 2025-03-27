package com.example.animeapp.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun SeekIndicator(seekDirection: Int, seekAmount: Long, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(
            end = if (seekDirection == -1) 180.dp else 0.dp,
            start = if (seekDirection == 1) 180.dp else 0.dp
        )
    ) {
        if (seekDirection == 1) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Forward",
                tint = Color.White
            )
        } else if (seekDirection == -1) {
            Icon(
                imageVector = Icons.Default.FastRewind,
                contentDescription = "Rewind",
                tint = Color.White
            )
        }
        Text(
            text = "${if (seekDirection == -1) "-" else ""}$seekAmount seconds",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}