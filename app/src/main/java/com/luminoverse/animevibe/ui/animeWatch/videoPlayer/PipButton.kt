package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PipButton(
    onEnterPipMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onEnterPipMode,
        modifier = modifier.padding(24.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.White,
        )
    ) {
        Icon(
            Icons.Filled.PictureInPictureAlt,
            tint = Color.Black,
            contentDescription = "PIP"
        )
    }
}
