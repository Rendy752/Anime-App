package com.example.animeapp.ui.animeWatch.videoPlayer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun LockButton(
    icon: ImageVector,
    contentDescription: String,
    onLockToggle: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onLockToggle,
        modifier = modifier.padding(24.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
        )
    ) {
        Icon(
            icon,
            tint = Color.Black,
            contentDescription = contentDescription
        )
    }
}
