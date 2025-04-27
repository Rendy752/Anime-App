package com.example.animeapp.ui.animeWatch.videoPlayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun LockButton(
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    isControllerVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (isControllerVisible || isLocked) 0.5f else 0.2f),
                shape = CircleShape
            )
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onLockToggle() }
    ) {
        val icon = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen
        val description = if (isLocked) "Unlock player" else "Lock player"

        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center)
        )
    }
}
