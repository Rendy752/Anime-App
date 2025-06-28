package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.ui.common.ImageDisplay

@Composable
fun SeekCancelPreview(
    modifier: Modifier = Modifier,
    visible: Boolean,
    captureScreenshot: suspend () -> String?,
    imageUrl: String?,
    onCancelSeekBarDrag: () -> Unit,
) {
    AnimatedVisibility(
        modifier = modifier
            .size(width = 160.dp, height = 90.dp)
            .padding(16.dp),
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        var screenshot by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            screenshot = captureScreenshot()
        }
        ImageDisplay(
            imageUrl = imageUrl,
            screenshot = screenshot,
            modifier = Modifier.fillMaxSize(),
            onClick = onCancelSeekBarDrag,
        )
    }
}