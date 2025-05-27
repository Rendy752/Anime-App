package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SkipButton(
    label: String,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSkip,
        modifier = modifier.padding(end = 80.dp, bottom = 80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(0.5f),
            contentColor = Color.Black
        )
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}