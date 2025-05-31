package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.luminoverse.animevibe.ui.common.CircularLoadingIndicator
import com.luminoverse.animevibe.utils.media.HlsPlayerState

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    hlsPlayerState: HlsPlayerState,
    errorMessage: String?
) {
    AnimatedVisibility(
        visible = (hlsPlayerState.playbackState == Player.STATE_BUFFERING || hlsPlayerState.playbackState == Player.STATE_IDLE) && errorMessage == null,
        modifier = modifier,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        CircularLoadingIndicator(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = CircleShape,
                )
                .padding(8.dp)
        )
    }
}