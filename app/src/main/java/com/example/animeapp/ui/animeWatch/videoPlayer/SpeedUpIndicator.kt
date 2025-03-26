package com.example.animeapp.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpeedUpIndicator(
    speedText: String,
    modifier: Modifier = Modifier
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        onClick = { },
        modifier = modifier.padding(16.dp),
        enabled = false
    ) {
        Icon(
            Icons.Filled.Speed,
            contentDescription = "Speed Up",
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = speedText,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            fontSize = 16.sp
        )
    }
}