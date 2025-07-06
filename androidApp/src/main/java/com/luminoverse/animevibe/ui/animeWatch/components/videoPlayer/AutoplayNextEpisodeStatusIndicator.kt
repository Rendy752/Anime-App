package com.luminoverse.animevibe.ui.animeWatch.components.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AutoplayNextEpisodeStatusIndicator(modifier: Modifier = Modifier, statusText: String?) {
    var currentStatusText by remember { mutableStateOf(statusText) }

    LaunchedEffect(statusText) {
        if (statusText != null) {
            currentStatusText = statusText
        }
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = statusText != null,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(),
        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically()
    ) {
        currentStatusText?.let {
            Text(
                text = it,
                style = TextStyle(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.75f),
                        offset = androidx.compose.ui.geometry.Offset(x = 2f, y = 2f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}