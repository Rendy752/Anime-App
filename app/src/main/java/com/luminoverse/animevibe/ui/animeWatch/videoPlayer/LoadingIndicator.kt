package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.luminoverse.animevibe.ui.common.CircularLoadingIndicator

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape),
        enter = fadeIn(tween(300)) + scaleIn(tween(300)),
        exit = fadeOut(tween(300)) + scaleOut(tween(300))
    ) {
        CircularLoadingIndicator(size = 40)
    }
}