package com.luminoverse.animevibe.ui.common

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun DebouncedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    debounceInterval: Long = 1000L,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var lastClickTimestamp by remember { mutableLongStateOf(0L) }

    IconButton(
        onClick = {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTimestamp >= debounceInterval) {
                lastClickTimestamp = currentTime
                onClick()
            }
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}