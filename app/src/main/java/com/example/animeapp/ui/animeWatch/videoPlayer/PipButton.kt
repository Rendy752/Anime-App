package com.example.animeapp.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PipButton(
    onEnterPipMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onEnterPipMode,
        modifier = modifier.padding(16.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
        )
    ) {
        Icon(
            Icons.Filled.PictureInPictureAlt,
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            contentDescription = "PIP"
        )
    }
}
