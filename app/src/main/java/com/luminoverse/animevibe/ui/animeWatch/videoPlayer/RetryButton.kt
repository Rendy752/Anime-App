package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RetryButton(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onRetry,
        modifier = modifier.padding(24.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.White,
        )
    ) {
        Icon(
            Icons.Filled.Refresh,
            tint = Color.Black,
            contentDescription = "Retry"
        )
    }
}