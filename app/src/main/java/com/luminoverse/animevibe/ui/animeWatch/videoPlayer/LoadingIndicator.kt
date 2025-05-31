package com.luminoverse.animevibe.ui.animeWatch.videoPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.luminoverse.animevibe.ui.common.CircularLoadingIndicator
import com.luminoverse.animevibe.utils.media.HlsPlayerState

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    hlsPlayerState: HlsPlayerState,
    isControlsVisible: Boolean,
    errorMessage: String?
) {
    AnimatedVisibility(
        visible = (hlsPlayerState.playbackState == Player.STATE_BUFFERING || hlsPlayerState.playbackState == Player.STATE_IDLE) && !isControlsVisible && errorMessage == null,
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape),
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        CircularLoadingIndicator(size = 40)
    }
}