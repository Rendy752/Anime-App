package com.luminoverse.animevibe.ui.animeWatch.components

import androidx.compose.animation.AnimatedVisibility
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
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    onRetry: () -> Unit
) {
    AnimatedVisibility(visible = isVisible, modifier = modifier.padding(24.dp)) {
        IconButton(
            onClick = onRetry,
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
}